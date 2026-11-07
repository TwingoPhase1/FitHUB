package handlers

import (
	"encoding/json"
	"net/http"
	"sync"

	"fithub-server/internal/database"
	"fithub-server/internal/models"

	"github.com/go-chi/chi/v5"
	"github.com/gorilla/websocket"
)

var (
	upgrader = websocket.Upgrader{
		CheckOrigin: func(r *http.Request) bool { return true },
	}
	wsClients = make(map[*websocket.Conn]bool)
	wsMutex   sync.RWMutex
)

func FleetRoutes() chi.Router {
	r := chi.NewRouter()
	r.Post("/register", RegisterTablet)
	r.Get("/sync", FleetSync)
	r.Get("/ws", FleetWebSocket)
	return r
}

func RegisterTablet(w http.ResponseWriter, r *http.Request) {
	var req struct {
		DeviceID string `json:"device_id"`
		Name     string `json:"name"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid JSON", http.StatusBadRequest)
		return
	}
	if req.DeviceID == "" {
		http.Error(w, "device_id required", http.StatusBadRequest)
		return
	}

	// Upsert tablet
	_, err := database.DB.Exec(`
		INSERT INTO tablets (device_id, name, last_ip, last_seen, user_id)
		VALUES (?, ?, ?, CURRENT_TIMESTAMP, 1)
		ON CONFLICT(device_id) DO UPDATE SET
			name = excluded.name,
			last_ip = excluded.last_ip,
			last_seen = CURRENT_TIMESTAMP
	`, req.DeviceID, req.Name, r.RemoteAddr)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
}

func FleetSync(w http.ResponseWriter, r *http.Request) {
	// Get all profiles with their workout plans
	rows, err := database.DB.Query(`
		SELECT p.id, p.user_id, p.name, p.weight_kg, p.max_heart_rate, p.resting_heart_rate, p.created_at, p.updated_at
		FROM profiles p
	`)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	var profiles []models.Profile
	for rows.Next() {
		var p models.Profile
		if err := rows.Scan(&p.ID, &p.UserID, &p.Name, &p.WeightKg, &p.MaxHeartRate, &p.RestingHeartRate, &p.CreatedAt, &p.UpdatedAt); err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
		profiles = append(profiles, p)
	}

	// Get all workout plans with steps
	planRows, err := database.DB.Query(`
		SELECT id, profile_id, day_of_week, title, description, machine, steps_json, created_at, updated_at
		FROM workout_plans
	`)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	defer planRows.Close()

	var plans []models.WorkoutPlan
	for planRows.Next() {
		var plan models.WorkoutPlan
		var stepsJSON string
		if err := planRows.Scan(&plan.ID, &plan.ProfileID, &plan.DayOfWeek, &plan.Title, &plan.Description, &plan.Machine, &stepsJSON, &plan.CreatedAt, &plan.UpdatedAt); err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
		if err := json.Unmarshal([]byte(stepsJSON), &plan.Steps); err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
		plans = append(plans, plan)
	}

	response := struct {
		Profiles []models.Profile   `json:"profiles"`
		Plans    []models.WorkoutPlan `json:"plans"`
	}{
		Profiles: profiles,
		Plans:    plans,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

func FleetWebSocket(w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		return
	}
	defer conn.Close()

	wsMutex.Lock()
	wsClients[conn] = true
	wsMutex.Unlock()

	defer func() {
		wsMutex.Lock()
		delete(wsClients, conn)
		wsMutex.Unlock()
	}()

	// Keep connection alive, read to detect close
	for {
		_, _, err := conn.ReadMessage()
		if err != nil {
			break
		}
	}
}

// BroadcastSyncRequired sends SYNC_REQUIRED event to all connected clients
func BroadcastSyncRequired() {
	wsMutex.RLock()
	defer wsMutex.RUnlock()

	msg := map[string]string{"event": "SYNC_REQUIRED"}
	data, _ := json.Marshal(msg)

	for conn := range wsClients {
		if err := conn.WriteMessage(websocket.TextMessage, data); err != nil {
			conn.Close()
			delete(wsClients, conn)
		}
	}
}