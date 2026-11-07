# FitHub Server & Fleet Ecosystem — Spécifications Architecturales

---

## 1. Stack Technique Validée : 100% Go (Golang)

La stack retenue pour le serveur est optimisée pour la simplicité, la légèreté (~20 Mo de RAM) et l'efficacité absolue avec les agents d'IA.

* **Langage :** Go (v1.22+)
* **Routeur HTTP :** `go-chi/chi/v5` (léger, idiomatique, zéro allocation inutile, compatible `net/http`).
* **Base de données :** SQLite pur Go via `modernc.org/sqlite` (**zéro dépendance C / CGO**, compilation statique universelle).
* **Accès aux données :** SQL brut typé ou `sqlc` (rapide, sans ORM lourd).
* **Frontend Web embarqué :** HTML5 + Tailwind CSS + Alpine.js, packagé directement dans le binaire via la directive standard `//go:embed web/*`.
* **Synchronisation Flotte :** WebSockets (via `gorilla/websocket` ou `nhooyr.io/websocket`) pour pousser les mises à jour aux tablettes en temps réel.
* **Module IA :** Client HTTP standard vers OpenRouter / OpenAI avec enforcement strict du `response_format: { type: "json_object" }`.

---

## 2. Arborescence du Projet Serveur (`fithub-server`)

```text
fithub-server/
├── cmd/
│   └── server/
│       └── main.go              # Point d'entrée, initialisation DB & serveur HTTP
├── internal/
│   ├── auth/                    # Inscription, login, tokens de session/tablette
│   ├── database/                # Migrations SQLite et requêtes
│   ├── fleet/                   # Gestion des tablettes (heartbeats, sync push)
│   ├── ai/                      # Module de génération de routines sécurisé
│   ├── handler/                 # Contrôleurs HTTP et WebSockets
│   └── model/                   # Structures Go (User, Profile, WorkoutPlan, Device)
├── web/                         # Frontend embarqué dans le binaire Go
│   ├── static/                  # CSS (Tailwind compilé), images, icônes
│   └── templates/               # Pages HTML (Dashboard flotte, formulaires d'édition)
├── go.mod
└── go.sum
```

---

## 3. Sécurité de l'IA : Le Moteur "Zero Free-Text"

Pour empêcher toute fuite de token ou génération de contenu malveillant, **aucun champ de saisie libre** n'est accessible aux utilisateurs :

```
[Utilisateur sur Web]
   │  Sélectionne :
   │  - Machine : Home Trainer
   │  - Objectif : Accélérations courtes (VO2 Max)
   │  - Durée : 30 min
   │  - Profil : Yanis (75 kg, FC Max 190)
   ▼
[Backend Go]
   │  1. Récupère la clé d'API globale configurée par l'Admin.
   │  2. Compose le prompt système strict en coulisse.
   │  3. Appelle l'API (OpenRouter) avec un JSON Schema obligatoire.
   ▼
[Modèle IA (DeepSeek / OpenAI)]
   │  Retourne STRICTEMENT le JSON des WorkoutSteps (échauffement, intervalles, cibles W).
   ▼
[Validation Go]
   │  Vérification de la validité du JSON et des limites physiologiques (ex: Watts < 1000).
   ▼
[Base SQLite & Sync Tablettes]
```

### Le Prompt Système Go injecté automatiquement
```go
const SystemPrompt = `Tu es le moteur d'entraînement de FitHub. 
Tu reçois des paramètres physiologiques stricts et tu DOIS générer une séance de sport structurée.
Tu ne dois répondre RIEN d'autre qu'un objet JSON respectant exactement le schéma suivant :
{
  "title": string,
  "description": string,
  "steps": [
    {
      "type": "WARMUP" | "INTERVAL" | "RECOVERY" | "COOLDOWN",
      "duration_seconds": number,
      "instruction": string,
      "target_power_watts": number | null,
      "target_incline_percent": number | null,
      "target_cadence_rpm": number | null
    }
  ]
}`
```

---

## 4. Gestion de Flotte (Fleet Management) & Sync Tablettes

### Cycle de vie d'une tablette :
1. **Enrôlement :**
   * Dans l'écran Réglages Kiosk de la tablette Android, l'utilisateur tape l'URL du serveur (ex: `http://192.168.1.50:8080`).
   * La tablette génère un UUID matériel unique et s'enregistre via `POST /api/fleet/register`.
   * Le serveur lui attribue un jeton d'authentification machine permanent.
2. **Synchronisation :**
   * La tablette ouvre une connexion WebSocket persistante : `GET /api/fleet/ws`.
   * Dès qu'une modification a lieu sur le web (nouveau profil, séance modifiée, routine IA générée), le serveur Go émet un événement `WORKOUT_SYNC_REQUIRED`.
   * La tablette télécharge le delta et met à jour sa base Room locale.
3. **Résilience Hors-ligne :**
   * Si la connexion au serveur est coupée, la tablette passe son statut en "Mode Autonome" et fonctionne normalement sur ses données locales sans jamais bloquer l'utilisateur.

---

## 5. Panneau d'Administration Web

Accessible aux utilisateurs avec le rôle `admin` :
* **Toggle Inscriptions :** `Ouvertes` (n'importe qui sur le réseau peut créer un compte), `Sur Code Invitation` ou `Fermées`.
* **Configuration IA partagée :**
  * Fournisseur (OpenRouter, OpenAI, Groq).
  * Clé API unique partagée pour toute l'instance.
  * Modèle choisi (ex: `deepseek/deepseek-chat`).
* **Monitoring des Tablettes :**
  * Liste des tablettes enregistrées avec dernière vue, état de synchronisation et séance en cours.
* **Gestion des Utilisateurs :**
  * Suppression, réinitialisation de mot de passe, association de profils physiques.