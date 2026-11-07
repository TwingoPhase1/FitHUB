package models

type Profile struct {
	ID              int64   `json:"id"`
	UserID          int64   `json:"user_id"`
	Name            string  `json:"name"`
	WeightKg        float64 `json:"weight_kg"`
	MaxHeartRate    int     `json:"max_heart_rate"`
	RestingHeartRate int    `json:"resting_heart_rate"`
	CreatedAt       string  `json:"created_at"`
	UpdatedAt       string  `json:"updated_at"`
}

type Tablet struct {
	ID        int64  `json:"id"`
	Name      string `json:"name"`
	DeviceID  string `json:"device_id"`
	UserID    int64  `json:"user_id"`
	LastSeen  string `json:"last_seen,omitempty"`
	CreatedAt string `json:"created_at"`
	UpdatedAt string `json:"updated_at"`
}

type WorkoutStep struct {
	Title             string   `json:"title"`
	Kind              string   `json:"kind"`               // WARM_UP, INTERVAL, ENDURANCE, COOL_DOWN
	DurationSeconds   int      `json:"duration_seconds"`
	Repetitions       int      `json:"repetitions,omitempty"`
	RecoverySeconds   int      `json:"recovery_seconds,omitempty"`
	ZoneFrom          string   `json:"zone_from"`          // Z1-Z5
	ZoneTo            string   `json:"zone_to,omitempty"`  // Z1-Z5
	TargetPowerWatts  *int     `json:"target_power_watts,omitempty"`
	TargetInclinePct  *float64 `json:"target_incline_pct,omitempty"`
	TargetSpeedKmh    *float64 `json:"target_speed_kmh,omitempty"`
	TargetCadenceSPM  *int     `json:"target_cadence_spm,omitempty"`
	Instruction       string   `json:"instruction"`
}

type WorkoutPlan struct {
	ID          int64        `json:"id"`
	ProfileID   int64        `json:"profile_id"`
	DayOfWeek   int          `json:"day_of_week"` // 1=Monday, 7=Sunday
	Title       string       `json:"title"`
	Description string       `json:"description"`
	Machine     string       `json:"machine"` // TREADMILL, BIKE, ROWER, OUTDOOR
	Steps       []WorkoutStep `json:"steps"`
	CreatedAt   string       `json:"created_at"`
	UpdatedAt   string       `json:"updated_at"`
}

type AIWorkoutRequest struct {
	ProfileID          int64    `json:"profile_id"`
	Goal               string   `json:"goal"`                  // VO2_MAX, THRESHOLD, AEROBIC_BASE, RECOVERY_FAT_BURN
	Machines           []string `json:"machines"`              // BIKE, TREADMILL, ROWER, OUTDOOR
	SessionsPerWeek    int      `json:"sessions_per_week"`     // 2-5
	SessionDurationMin int      `json:"session_duration_min"`  // 20, 30, 45, 60
	Intensity          string   `json:"intensity"`             // DOUCE, MODEREE, SOUTENUE
}

type AIWorkoutResponse struct {
	Plans []WorkoutPlan `json:"plans"`
}

type Settings struct {
	OpenRouterAPIKey string `json:"openrouter_api_key"`
	AIModel          string `json:"ai_model"`
	RegistrationsOpen bool   `json:"registrations_open"`
}

const (
	GoalVO2Max          = "VO2_MAX"
	GoalThreshold       = "THRESHOLD"
	GoalAerobicBase     = "AEROBIC_BASE"
	GoalRecoveryFatBurn = "RECOVERY_FAT_BURN"

	MachineBike       = "BIKE"
	MachineTreadmill  = "TREADMILL"
	MachineRower      = "ROWER"
	MachineOutdoor    = "OUTDOOR"

	IntensityDouce     = "DOUCE"
	IntensityModeree   = "MODEREE"
	IntensitySoutenue  = "SOUTENUE"
)