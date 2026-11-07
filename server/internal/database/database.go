package database

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"log"

	"fithub-server/internal/models"

	_ "modernc.org/sqlite"
)

var DB *sql.DB

func Init(dbPath string) error {
	var err error
	DB, err = sql.Open("sqlite", dbPath)
	if err != nil {
		return fmt.Errorf("failed to open database: %w", err)
	}

	if err = DB.Ping(); err != nil {
		return fmt.Errorf("failed to ping database: %w", err)
	}

	// Enable foreign keys
	if _, err = DB.Exec("PRAGMA foreign_keys = ON"); err != nil {
		return fmt.Errorf("failed to enable foreign keys: %w", err)
	}

	return createTables()
}

func createTables() error {
	queries := []string{
		`CREATE TABLE IF NOT EXISTS users (
			id INTEGER PRIMARY KEY AUTOINCREMENT,
			username TEXT UNIQUE NOT NULL,
			email TEXT UNIQUE NOT NULL,
			password_hash TEXT NOT NULL,
			created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
			updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
		)`,
		`CREATE TABLE IF NOT EXISTS tablets (
			id INTEGER PRIMARY KEY AUTOINCREMENT,
			name TEXT NOT NULL,
			device_id TEXT UNIQUE NOT NULL,
			user_id INTEGER NOT NULL,
			last_ip TEXT,
			last_seen DATETIME,
			created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
			updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
			FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
		)`,
		`CREATE TABLE IF NOT EXISTS settings (
			id INTEGER PRIMARY KEY AUTOINCREMENT,
			tablet_id INTEGER NOT NULL,
			key TEXT NOT NULL,
			value TEXT NOT NULL,
			created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
			updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
			FOREIGN KEY (tablet_id) REFERENCES tablets(id) ON DELETE CASCADE,
			UNIQUE(tablet_id, key)
		)`,
		`CREATE TABLE IF NOT EXISTS global_settings (
			key TEXT PRIMARY KEY,
			value TEXT NOT NULL,
			updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
		)`,
		`CREATE TABLE IF NOT EXISTS profiles (
			id INTEGER PRIMARY KEY AUTOINCREMENT,
			user_id INTEGER NOT NULL,
			name TEXT NOT NULL,
			weight_kg REAL NOT NULL,
			max_heart_rate INTEGER NOT NULL,
			resting_heart_rate INTEGER NOT NULL,
			created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
			updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
			FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
		)`,
		`CREATE TABLE IF NOT EXISTS workout_plans (
			id INTEGER PRIMARY KEY AUTOINCREMENT,
			profile_id INTEGER NOT NULL,
			day_of_week INTEGER NOT NULL CHECK(day_of_week BETWEEN 1 AND 7),
			title TEXT NOT NULL,
			description TEXT,
			machine TEXT NOT NULL,
			steps_json TEXT NOT NULL,
			created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
			updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
			FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE
		)`,
		`CREATE INDEX IF NOT EXISTS idx_tablets_user_id ON tablets(user_id)`,
		`CREATE INDEX IF NOT EXISTS idx_settings_tablet_id ON settings(tablet_id)`,
		`CREATE INDEX IF NOT EXISTS idx_profiles_user_id ON profiles(user_id)`,
		`CREATE INDEX IF NOT EXISTS idx_workout_plans_profile_id ON workout_plans(profile_id)`,
		`CREATE INDEX IF NOT EXISTS idx_workout_plans_day ON workout_plans(day_of_week)`,
	}

	for _, query := range queries {
		if _, err := DB.Exec(query); err != nil {
			return fmt.Errorf("failed to execute query: %w", err)
		}
	}

	// Migration: add last_ip column to tablets if missing
	if _, err := DB.Exec(`ALTER TABLE tablets ADD COLUMN last_ip TEXT`); err != nil {
		// Ignore "duplicate column" error
	}

	// Migration: add updated_at to workout_plans if missing
	if _, err := DB.Exec(`ALTER TABLE workout_plans ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP`); err != nil {
		// Ignore "duplicate column" error
	}

	// Insert default global settings if not exist
	defaults := map[string]string{
		"ai_model":           "deepseek/deepseek-chat",
		"registrations_open": "true",
	}
	for k, v := range defaults {
		_, _ = DB.Exec(`INSERT OR IGNORE INTO global_settings (key, value) VALUES (?, ?)`, k, v)
	}

	log.Println("Database tables created successfully")
	return nil
}

// Global Settings Management
func GetGlobalSetting(key string) (string, error) {
	var value string
	err := DB.QueryRow("SELECT value FROM global_settings WHERE key = ?", key).Scan(&value)
	if err == sql.ErrNoRows {
		return "", nil
	}
	return value, err
}

func SetGlobalSetting(key, value string) error {
	_, err := DB.Exec(`
		INSERT INTO global_settings (key, value, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP)
		ON CONFLICT(key) DO UPDATE SET value = excluded.value, updated_at = CURRENT_TIMESTAMP
	`, key, value)
	return err
}

func GetAllGlobalSettings() (map[string]string, error) {
	rows, err := DB.Query("SELECT key, value FROM global_settings")
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	settings := make(map[string]string)
	for rows.Next() {
		var k, v string
		if err := rows.Scan(&k, &v); err != nil {
			return nil, err
		}
		settings[k] = v
	}
	return settings, rows.Err()
}

// Profile Management
func CreateProfile(p models.Profile) (int64, error) {
	result, err := DB.Exec(`
		INSERT INTO profiles (user_id, name, weight_kg, max_heart_rate, resting_heart_rate)
		VALUES (?, ?, ?, ?, ?)
	`, p.UserID, p.Name, p.WeightKg, p.MaxHeartRate, p.RestingHeartRate)
	if err != nil {
		return 0, err
	}
	return result.LastInsertId()
}

func GetProfile(id int64) (*models.Profile, error) {
	var p models.Profile
	err := DB.QueryRow(`
		SELECT id, user_id, name, weight_kg, max_heart_rate, resting_heart_rate, created_at, updated_at
		FROM profiles WHERE id = ?
	`, id).Scan(&p.ID, &p.UserID, &p.Name, &p.WeightKg, &p.MaxHeartRate, &p.RestingHeartRate, &p.CreatedAt, &p.UpdatedAt)
	if err == sql.ErrNoRows {
		return nil, nil
	}
	return &p, err
}

func GetProfilesByUser(userID int64) ([]models.Profile, error) {
	rows, err := DB.Query(`
		SELECT id, user_id, name, weight_kg, max_heart_rate, resting_heart_rate, created_at, updated_at
		FROM profiles WHERE user_id = ? ORDER BY name
	`, userID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var profiles []models.Profile
	for rows.Next() {
		var p models.Profile
		if err := rows.Scan(&p.ID, &p.UserID, &p.Name, &p.WeightKg, &p.MaxHeartRate, &p.RestingHeartRate, &p.CreatedAt, &p.UpdatedAt); err != nil {
			return nil, err
		}
		profiles = append(profiles, p)
	}
	return profiles, rows.Err()
}

// Workout Plan Management
func SaveWorkoutPlan(plan models.WorkoutPlan) (int64, error) {
	stepsJSON, err := json.Marshal(plan.Steps)
	if err != nil {
		return 0, err
	}

	result, err := DB.Exec(`
		INSERT INTO workout_plans (profile_id, day_of_week, title, description, machine, steps_json)
		VALUES (?, ?, ?, ?, ?, ?)
	`, plan.ProfileID, plan.DayOfWeek, plan.Title, plan.Description, plan.Machine, string(stepsJSON))
	if err != nil {
		return 0, err
	}
	return result.LastInsertId()
}

func GetWorkoutPlan(id int64) (*models.WorkoutPlan, error) {
	var plan models.WorkoutPlan
	var stepsJSON string
	err := DB.QueryRow(`
		SELECT id, profile_id, day_of_week, title, description, machine, steps_json, created_at, updated_at
		FROM workout_plans WHERE id = ?
	`, id).Scan(&plan.ID, &plan.ProfileID, &plan.DayOfWeek, &plan.Title, &plan.Description, &plan.Machine, &stepsJSON, &plan.CreatedAt, &plan.UpdatedAt)
	if err == sql.ErrNoRows {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	if err := json.Unmarshal([]byte(stepsJSON), &plan.Steps); err != nil {
		return nil, err
	}
	return &plan, nil
}

func GetWorkoutPlansByProfile(profileID int64) ([]models.WorkoutPlan, error) {
	rows, err := DB.Query(`
		SELECT id, profile_id, day_of_week, title, description, machine, steps_json, created_at, updated_at
		FROM workout_plans WHERE profile_id = ? ORDER BY day_of_week
	`, profileID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var plans []models.WorkoutPlan
	for rows.Next() {
		var plan models.WorkoutPlan
		var stepsJSON string
		if err := rows.Scan(&plan.ID, &plan.ProfileID, &plan.DayOfWeek, &plan.Title, &plan.Description, &plan.Machine, &stepsJSON, &plan.CreatedAt, &plan.UpdatedAt); err != nil {
			return nil, err
		}
		if err := json.Unmarshal([]byte(stepsJSON), &plan.Steps); err != nil {
			return nil, err
		}
		plans = append(plans, plan)
	}
	return plans, rows.Err()
}

func Close() error {
	if DB != nil {
		return DB.Close()
	}
	return nil
}