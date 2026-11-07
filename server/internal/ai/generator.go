package ai

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"fithub-server/internal/models"
)

type OpenRouterClient struct {
	APIKey string
	Model  string
	Client *http.Client
}

type OpenRouterRequest struct {
	Model          string          `json:"model"`
	Messages       []Message       `json:"messages"`
	ResponseFormat ResponseFormat  `json:"response_format"`
	Temperature    float64         `json:"temperature"`
	MaxTokens      int             `json:"max_tokens"`
}

type Message struct {
	Role    string `json:"role"`
	Content string `json:"content"`
}

type ResponseFormat struct {
	Type string `json:"type"`
}

type OpenRouterResponse struct {
	Choices []Choice `json:"choices"`
	Error   *APIError `json:"error,omitempty"`
}

type Choice struct {
	Message Message `json:"message"`
}

type APIError struct {
	Message string `json:"message"`
	Type    string `json:"type"`
	Code    int    `json:"code"`
}

func NewClient(apiKey, model string) *OpenRouterClient {
	return &OpenRouterClient{
		APIKey: apiKey,
		Model:  model,
		Client: &http.Client{Timeout: 60 * time.Second},
	}
}

func (c *OpenRouterClient) GenerateWorkout(req models.AIWorkoutRequest, profile models.Profile) (*models.AIWorkoutResponse, error) {
	prompt := c.buildPrompt(req, profile)

	request := OpenRouterRequest{
		Model: c.Model,
		Messages: []Message{
			{Role: "system", Content: c.systemPrompt()},
			{Role: "user", Content: prompt},
		},
		ResponseFormat: ResponseFormat{Type: "json_object"},
		Temperature:    0.3,
		MaxTokens:      4000,
	}

	body, err := json.Marshal(request)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal request: %w", err)
	}

	httpReq, err := http.NewRequest("POST", "https://openrouter.ai/api/v1/chat/completions", bytes.NewBuffer(body))
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	httpReq.Header.Set("Content-Type", "application/json")
	httpReq.Header.Set("Authorization", "Bearer "+c.APIKey)
	httpReq.Header.Set("HTTP-Referer", "https://fithub.local")
	httpReq.Header.Set("X-Title", "FitHub")

	resp, err := c.Client.Do(httpReq)
	if err != nil {
		return nil, fmt.Errorf("HTTP request failed: %w", err)
	}
	defer resp.Body.Close()

	var orResp OpenRouterResponse
	if err := json.NewDecoder(resp.Body).Decode(&orResp); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	if orResp.Error != nil {
		return nil, fmt.Errorf("OpenRouter API error: %s (code: %d)", orResp.Error.Message, orResp.Error.Code)
	}

	if len(orResp.Choices) == 0 {
		return nil, fmt.Errorf("no choices in response")
	}

	var aiResp models.AIWorkoutResponse
	if err := json.Unmarshal([]byte(orResp.Choices[0].Message.Content), &aiResp); err != nil {
		return nil, fmt.Errorf("failed to parse AI response JSON: %w", err)
	}

	// Validate and enrich
	for i := range aiResp.Plans {
		aiResp.Plans[i].ProfileID = req.ProfileID
		if aiResp.Plans[i].DayOfWeek < 1 || aiResp.Plans[i].DayOfWeek > 7 {
			return nil, fmt.Errorf("invalid day_of_week in plan %d", i)
		}
		if len(aiResp.Plans[i].Steps) == 0 {
			return nil, fmt.Errorf("plan %d has no steps", i)
		}
		for j := range aiResp.Plans[i].Steps {
			if aiResp.Plans[i].Steps[j].DurationSeconds <= 0 {
				return nil, fmt.Errorf("step %d in plan %d has invalid duration", j, i)
			}
		}
	}

	return &aiResp, nil
}

func (c *OpenRouterClient) systemPrompt() string {
	return `Tu es un coach sportif expert qui génère des plans d'entraînement structurés en JSON.
RÈGLES STRICTES :
1. Sortie EXCLUSIVEMENT en JSON valide, aucun texte hors JSON.
2. Structure exacte : {"plans": [{"day_of_week": 1-7, "title": "...", "description": "...", "machine": "BIKE|TREADMILL|ROWER|OUTDOOR", "steps": [...]}]}
3. Chaque step : {"title": "...", "kind": "WARM_UP|INTERVAL|ENDURANCE|COOL_DOWN", "duration_seconds": int, "repetitions": int, "recovery_seconds": int, "zone_from": "Z1-Z5", "zone_to": "Z1-Z5", "target_power_watts": int|null, "target_incline_pct": float|null, "target_speed_kmh": float|null, "target_cadence_spm": int|null, "instruction": "..."}
3. Calcul physiologique OBLIGATOIRE :
   - FC cible = FC_repos + (FC_max - FC_repos) × intensité (Karvonen)
   - Puissance (Watts) ≈ Poids(kg) × 2.5 (Z2) à × 5.5 (Z5)
   - Vitesse course ≈ 8-18 km/h selon zone
   - Pente tapis 0-12%
   - Cadence vélo 70-110 RPM, rameur 18-32 SPM
4. Respecter EXACTEMENT les machines, volume, durée, intensité demandés.
5. Répartition équilibrée des jours selon machines cochées.
6. Chaque séance : échauffement (10-15%), blocs principaux, retour au calme (5-10%).
7. Intervalles : effort + récupération, watts/pente/cadence précis.`
}

func (c *OpenRouterClient) buildPrompt(req models.AIWorkoutRequest, profile models.Profile) string {
	hrReserve := float64(profile.MaxHeartRate - profile.RestingHeartRate)
	
	intensityMap := map[string]float64{
		models.IntensityDouce:    0.6,
		models.IntensityModeree:  0.75,
		models.IntensitySoutenue: 0.85,
	}
	targetIntensity := intensityMap[req.Intensity]
	if targetIntensity == 0 {
		targetIntensity = 0.7
	}

	targetHR := profile.RestingHeartRate + int(hrReserve*targetIntensity)
	estimatedFTP := profile.WeightKg * 3.0 // approximation

	machinesStr := ""
	for _, m := range req.Machines {
		machinesStr += "- " + m + "\n"
	}

	return fmt.Sprintf(`Génère un plan hebdomadaire de %d séances (%d min chacune) pour :
Profil : %s (%.1f kg, FCmax %d, FCrepos %d)
Objectif : %s
Machines disponibles :
%s
Intensité globale : %s (FC cible ~%d bpm, FTP estimé ~%d W)
Durée par séance : %d min

Contraintes :
- Répartir les %d séances sur 7 jours (lundi=1 à dimanche=7)
- Utiliser UNIQUEMENT les machines listées
- Chaque séance : échauffement, blocs principaux, retour au calme
- Pour BIKE : watts cibles + cadence RPM
- Pour TREADMILL : vitesse km/h + pente %%
- Pour ROWER : cadence SPM + allure /500m
- Pour OUTDOOR : vitesse km/h + FC cible
- JSON ONLY, aucune explication.`,
		req.SessionsPerWeek, req.SessionDurationMin,
		profile.Name, profile.WeightKg, profile.MaxHeartRate, profile.RestingHeartRate,
		req.Goal,
		machinesStr,
		req.Intensity, targetHR, estimatedFTP,
		req.SessionDurationMin,
		req.SessionsPerWeek,
	)
}