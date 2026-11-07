package handlers

import (
	"encoding/json"
	"net/http"
	"strconv"

	"fithub-server/internal/ai"
	"fithub-server/internal/database"
	"fithub-server/internal/models"

	"github.com/go-chi/chi/v5"
)

func WorkoutRoutes() chi.Router {
	r := chi.NewRouter()
	r.Get("/new-ai", NewAIWorkoutPage)
	r.Post("/api/generate", GenerateAIWorkout)
	r.Post("/api/save", SaveWorkoutPlan)
	r.Get("/{id}", ViewWorkoutPlan)
	return r
}

func NewAIWorkoutPage(w http.ResponseWriter, r *http.Request) {
	profiles, err := database.GetProfilesByUser(1)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	data := struct {
		Profiles []models.Profile
		Goals    []GoalOption
		Machines []MachineOption
		Intensities []IntensityOption
		Durations []int
		SessionsRange []int
	}{
		Profiles: profiles,
		Goals: []GoalOption{
			{Value: models.GoalVO2Max, Label: "VO₂ Max — Fractionné haute intensité (Z5)", Icon: "⚡"},
			{Value: models.GoalThreshold, Label: "Seuil anaérobie — Tempo (Z3-Z4)", Icon: "🎯"},
			{Value: models.GoalAerobicBase, Label: "Endurance fondamentale — Zone 2 (Aisance)", Icon: "🌿"},
			{Value: models.GoalRecoveryFatBurn, Label: "Récupération active & Capillarisation", Icon: "🧘"},
		},
		Machines: []MachineOption{
			{Value: models.MachineBike, Label: "Home-Trainer (Elite Suito-T — Watts & ERG)", Icon: "🚴"},
			{Value: models.MachineTreadmill, Label: "Tapis de course (Domyos RUN 500 — Vitesse & Pente)", Icon: "🏃"},
			{Value: models.MachineRower, Label: "Rameur (Domyos Rower 900 — Cadence & Allure)", Icon: "🚣"},
			{Value: models.MachineOutdoor, Label: "Poids du corps / Renfo / Extérieur", Icon: "💪"},
		},
		Intensities: []IntensityOption{
			{Value: models.IntensityDouce, Label: "Douce", Description: "~60% réserve FC", Color: "green"},
			{Value: models.IntensityModeree, Label: "Modérée", Description: "~75% réserve FC", Color: "yellow"},
			{Value: models.IntensitySoutenue, Label: "Soutenue", Description: "~85% réserve FC", Color: "red"},
		},
		Durations:      []int{20, 30, 45, 60},
		SessionsRange:  []int{2, 3, 4, 5},
	}

	if err := Templates.ExecuteTemplate(w, "new-ai.html", data); err != nil {
		http.Error(w, "Template error", http.StatusInternalServerError)
	}
}

type GoalOption struct {
	Value string
	Label string
	Icon  string
}
type MachineOption struct {
	Value string
	Label string
	Icon  string
}
type IntensityOption struct {
	Value, Label, Description, Color string
}

func GenerateAIWorkout(w http.ResponseWriter, r *http.Request) {
	var req models.AIWorkoutRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid JSON", http.StatusBadRequest)
		return
	}

	// Validate required fields
	if req.ProfileID == 0 || req.Goal == "" || len(req.Machines) == 0 || req.SessionsPerWeek < 2 || req.SessionsPerWeek > 5 {
		http.Error(w, "Missing required fields", http.StatusBadRequest)
		return
	}

	// Get profile
	profile, err := database.GetProfile(req.ProfileID)
	if err != nil || profile == nil {
		http.Error(w, "Profile not found", http.StatusNotFound)
		return
	}

	// Get AI settings
	apiKey, _ := database.GetGlobalSetting("openrouter_api_key")
	model, _ := database.GetGlobalSetting("ai_model")
	if apiKey == "" {
		http.Error(w, "OpenRouter API key not configured in admin", http.StatusBadRequest)
		return
	}
	if model == "" {
		model = "deepseek/deepseek-chat"
	}

	// Generate
	client := ai.NewClient(apiKey, model)
	resp, err := client.GenerateWorkout(req, *profile)
	if err != nil {
		http.Error(w, "AI generation failed: "+err.Error(), http.StatusInternalServerError)
		return
	}

	json.NewEncoder(w).Encode(resp)
}

func SaveWorkoutPlan(w http.ResponseWriter, r *http.Request) {
	var plans []models.WorkoutPlan
	if err := json.NewDecoder(r.Body).Decode(&plans); err != nil {
		http.Error(w, "Invalid JSON", http.StatusBadRequest)
		return
	}

	savedIDs := make([]int64, 0, len(plans))
	for _, plan := range plans {
		id, err := database.SaveWorkoutPlan(plan)
		if err != nil {
			http.Error(w, "Failed to save: "+err.Error(), http.StatusInternalServerError)
			return
		}
		savedIDs = append(savedIDs, id)
	}

	// Broadcast sync event to all connected tablets
	BroadcastSyncRequired()

	json.NewEncoder(w).Encode(map[string]interface{}{
		"status": "ok",
		"ids":    savedIDs,
	})
}

func ViewWorkoutPlan(w http.ResponseWriter, r *http.Request) {
	idStr := chi.URLParam(r, "id")
	id, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil {
		http.Error(w, "Invalid ID", http.StatusBadRequest)
		return
	}

	plan, err := database.GetWorkoutPlan(id)
	if err != nil || plan == nil {
		http.Error(w, "Not found", http.StatusNotFound)
		return
	}

	data := struct {
		Plan *models.WorkoutPlan
	}{
		Plan: plan,
	}

	if err := Templates.ExecuteTemplate(w, "view.html", data); err != nil {
		http.Error(w, "Template error", http.StatusInternalServerError)
	}
}