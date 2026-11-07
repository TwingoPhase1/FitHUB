package handlers

import (
	"encoding/json"
	"net/http"

	"fithub-server/internal/database"
	"fithub-server/internal/models"

	"github.com/go-chi/chi/v5"
)

func AdminRoutes() chi.Router {
	r := chi.NewRouter()
	r.Get("/", AdminDashboard)
	r.Get("/api/settings", GetSettings)
	r.Post("/api/settings", UpdateSettings)
	r.Get("/api/profiles", ListProfiles)
	r.Post("/api/profiles", CreateProfile)
	return r
}

func AdminDashboard(w http.ResponseWriter, r *http.Request) {
	if err := Templates.ExecuteTemplate(w, "admin.html", nil); err != nil {
		http.Error(w, "Template error", http.StatusInternalServerError)
	}
}

func GetSettings(w http.ResponseWriter, r *http.Request) {
	settings, err := database.GetAllGlobalSettings()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	// Ensure all keys exist
	defaults := map[string]string{
		"ai_model":           "deepseek/deepseek-chat",
		"openrouter_api_key": "",
		"registrations_open": "true",
	}
	for k, v := range defaults {
		if _, ok := settings[k]; !ok {
			settings[k] = v
		}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(settings)
}

func UpdateSettings(w http.ResponseWriter, r *http.Request) {
	var req map[string]string
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid JSON", http.StatusBadRequest)
		return
	}

	allowedKeys := map[string]bool{
		"ai_model":           true,
		"openrouter_api_key": true,
		"registrations_open": true,
	}

	for k, v := range req {
		if !allowedKeys[k] {
			continue
		}
		if k == "registrations_open" && v != "true" && v != "false" {
			http.Error(w, "registrations_open must be true or false", http.StatusBadRequest)
			return
		}
		if err := database.SetGlobalSetting(k, v); err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
	}

	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
}

func ListProfiles(w http.ResponseWriter, r *http.Request) {
	// For now, assume user_id = 1 (demo)
	profiles, err := database.GetProfilesByUser(1)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	json.NewEncoder(w).Encode(profiles)
}

func CreateProfile(w http.ResponseWriter, r *http.Request) {
	var p models.Profile
	if err := json.NewDecoder(r.Body).Decode(&p); err != nil {
		http.Error(w, "Invalid JSON", http.StatusBadRequest)
		return
	}
	p.UserID = 1 // demo user
	id, err := database.CreateProfile(p)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	p.ID = id
	json.NewEncoder(w).Encode(p)
}