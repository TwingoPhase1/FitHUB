package main

import (
	"embed"
	"html/template"
	"log"
	"net/http"

	"fithub-server/internal/database"
	"fithub-server/internal/handlers"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
)

//go:embed web/templates/*.html web/templates/admin/*.html web/templates/workouts/*.html
var templateFS embed.FS

var templates = template.Must(template.ParseFS(templateFS, "web/templates/*.html", "web/templates/admin/*.html", "web/templates/workouts/*.html"))

func main() {
	// Initialize database
	if err := database.Init("fithub.db"); err != nil {
		log.Fatalf("Failed to initialize database: %v", err)
	}
	defer database.Close()

	handlers.SetTemplates(templates)

	r := chi.NewRouter()

	// Middleware
	r.Use(middleware.Logger)
	r.Use(middleware.Recoverer)
	r.Use(middleware.RequestID)
	r.Use(middleware.RealIP)

	// Health check endpoint
	r.Get("/api/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{"status":"ok"}`))
	})

	// Serve homepage
	r.Get("/", func(w http.ResponseWriter, r *http.Request) {
		if err := templates.ExecuteTemplate(w, "index.html", nil); err != nil {
			log.Printf("Template error: %v", err)
			http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		}
	})

	// Admin panel
	r.Mount("/admin", handlers.AdminRoutes())

	// Workout generation
	r.Mount("/workouts", handlers.WorkoutRoutes())

	// Fleet sync
	r.Mount("/api/fleet", handlers.FleetRoutes())

	// API routes
	r.Route("/api", func(r chi.Router) {
		r.Get("/tablets", func(w http.ResponseWriter, r *http.Request) {
			w.Header().Set("Content-Type", "application/json")
			w.Write([]byte(`{"tablets":[]}`))
		})
	})

	log.Println("Server starting on :8080")
	if err := http.ListenAndServe(":8080", r); err != nil {
		log.Fatalf("Server failed: %v", err)
	}
}