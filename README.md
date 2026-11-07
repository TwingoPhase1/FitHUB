# FitHub OS — Station Sportive Autonome Open-Source

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%20%7C%20Go%20Backend-blue?style=for-the-badge&logo=android" alt="Platform">
  <img src="https://img.shields.io/badge/Kotlin-2.2.10-purple?style=for-the-badge&logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Go-1.22+-cyan?style=for-the-badge&logo=go" alt="Go">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-UI-orange?style=for-the-badge&logo=jetpackcompose" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/BLE-FTMS%20%7C%20Heart%20Rate-blueviolet?style=for-the-badge&logo=bluetooth" alt="BLE">
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" alt="License">
  <img src="https://img.shields.io/badge/Status-Active-brightgreen?style=for-the-badge" alt="Status">
</p>

---

## 🎯 FitHub OS — L'écosystème sportif autonome sur tablette

**FitHub OS** est un écosystème open-source complet transformant une tablette Android en **station sportive autonome** (mode Kiosk/Launcher) avec synchronisation serveur auto-hébergé.

### 🎯 Objectif
Permettre à tout sportif de créer, gérer et exécuter des programmes d'entraînement structurés **entièrement hors-ligne** sur tablette, avec synchronisation optionnelle vers un serveur auto-hébergé pour la flotte et l'IA.

---

## 📦 Architecture Monorepo

```
FitHUB/
├── com_twingo_fithub/          # Android App (Kotlin + Jetpack Compose)
│   ├── app/                    # Module principal
│   └── build.gradle.kts
├── server/                     # Backend Go autonome
│   ├── cmd/server/             # Point d'entrée
│   ├── internal/
│   │   ├── database/           # SQLite (modernc.org/sqlite)
│   │   ├── models/             # Structures de données
│   │   ├── ai/                 # Client IA (OpenRouter)
│   │   └── handlers/           # HTTP handlers (Chi)
│   └── web/templates/          # Templates HTML/Tailwind embarqués
├── .gitignore
├── README.md
├── LICENSE
└── CONTRIBUTING.md
```

---

## 🏋️ Matériel Supporté & Protocoles BLE

| Équipement | Service BLE | Caractéristique | Mode | Pilotage |
|------------|-------------|-----------------|------|----------|
| **Ceinture/Moniteur Cardio** | `0x180D` (Heart Rate) | `0x2A37` (HR Measurement) | Notify | Lecture FC temps réel |
| **Elite Suito-T** (Home-Trainer) | `0x1826` (FTMS) | `0x2AD2` (Indoor Bike Data) + `0x2AD9` (Control Point) | Notify + Write | **Mode ERG automatique**, Watts cibles, Résistance |
| **Domyos RUN 500** (Tapis) | `0x1826` (FTMS) | `0x2ACD` (Treadmill Data) | Notify | Vitesse, Inclinaison, Allure |
| **Domyos Rower 900** (Rameur) | `0x1826` (FTMS) | `0x2AD1` (Rower Data) | Notify | Cadence SPM, Allure 500m, Puissance |

### 🔧 Spécificités BLE Implémentées
- **Queue GATT sérialisée** anti-crash (erreur 133) via `Channel` + `CompletableDeferred`
- **Machine d'états FTMS Control Point** (`0x2AD9`) : `Request Control` → `Start` → `Set Target Power/Incline/Resistance`
- **Parsing binaire complet** little-endian, gestion flags HR (8/16 bits)
- **Foreground Service** Android avec notification persistante pour BLE continu

---

## 📱 Application Android (`com_twingo_fithub/`)

### Stack Technique
| Composant | Technologie |
|-----------|-------------|
| Langage | Kotlin 2.2.10 |
| UI | Jetpack Compose (Material3) + Material Icons Extended |
| Architecture | MVVM + StateFlow + Coroutines |
| Base de données | Room (SQLite) + TypeConverters JSON |
| BLE | Android Bluetooth LE API + Queue GATT sérialisée |
| Navigation | Compose manuelle (sans Navigation Component) |
| Injection | ViewModelProvider.Factory |
| Kiosk | `HOME` + `DEFAULT` intent filters + `FLAG_KEEP_SCREEN_ON` |

### Fonctionnalités Clés
- **Multi-profils locaux** : Poids, FC Max, FC Repos → Calcul zones Karvonen (Z1-Z5) instantané
- **Éditeur de séances visuel** : Étapes ordonnées (Échauffement, Intervalles, Endurance, Récupération, Retour au calme)
- **Groupe d'intervalles** : `N × (Effort + Récup)` avec cibles Watts/Pente/Vitesse
- **Mode Kiosk/Launcher** : `HOME` + `DEFAULT` categories, plein écran, verrouillage veille
- **Synchro Flotte** : Enregistrement tablette, pull sync (profils + plans), WebSocket `SYNC_REQUIRED`

---

## 🖥️ Serveur Go (`server/`)

### Stack Technique
| Composant | Technologie |
|-----------|-------------|
| Langage | Go 1.22+ |
| Router | Chi v5 |
| Base de données | SQLite pur Go (`modernc.org/sqlite`) — **sans CGO** |
| Templates | `html/template` + **Tailwind CSS CDN** embarqués via `//go:embed` |
| WebSockets | `gorilla/websocket` |
| IA | OpenRouter API (`response_format: json_object`) |

### Endpoints Principaux

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/` | GET | Page d'accueil (Tailwind) |
| `/admin` | GET | Panneau d'admin (settings + profils CRUD) |
| `/admin/api/settings` | GET/POST | Clé OpenRouter, modèle IA, inscriptions |
| `/admin/api/profiles` | GET/POST | CRUD profils |
| `/workouts/new-ai` | GET | Formulaire IA "Zéro free-text" |
| `/workouts/api/generate` | POST | Génération IA → JSON structuré |
| `/workouts/api/save` | POST | Persistance plan + **Broadcast `SYNC_REQUIRED`** |
| `/workouts/{id}` | GET | Visualisation plan |
| `/api/fleet/register` | POST | Enregistrement tablette (`device_id`, `name`) |
| `/api/fleet/sync` | GET | Pull sync (profils + plans JSON) |
| `/api/fleet/ws` | WS | WebSocket pour `SYNC_REQUIRED` |
| `/api/health` | GET | Health check |

### Module IA "Zéro Free-Text" (`internal/ai/generator.go`)

> **Aucun champ texte libre** — Sécurité maximale pour clé API partagée

**Formulaire interactif** (`/workouts/new-ai`) :
- **Profil ciblé** → Injection auto Poids/FC Max/FC Repos
- **Objectif physiologique** : `VO2_MAX` | `THRESHOLD` | `AEROBIC_BASE` | `RECOVERY_FAT_BURN`
- **Machines** : Multi-sélection (Bike, Treadmill, Rower, Outdoor)
- **Volume** : 2-5 séances/semaine × 20-60 min
- **Intensité** : Douce / Modérée / Soutenue

**Prompt système strict** :
```text
Sortie EXCLUSIVEMENT en JSON valide.
Structure : {"plans": [{"day_of_week": 1-7, "title": "...", "machine": "BIKE|TREADMILL|ROWER|OUTDOOR", "steps": [...]}]}
Calculs physiologiques OBLIGATOIRES (Karvonen, Watts ≈ Poids×2.5 à ×5.5, etc.)
```

**Appel OpenRouter** :
```go
POST https://openrouter.ai/api/v1/chat/completions
Headers: Authorization: Bearer <API_KEY>, response_format: {"type": "json_object"}
```

---

## 🚀 Quickstart — Démarrage Rapide

### 1. Serveur Go
```bash
cd server
go mod download
go run ./cmd/server
# → Server starting on :8080
```

### 2. Panneau d'Administration
Ouvrir `http://<IP_SERVER>:8080/admin`
- **Settings** : Clé OpenRouter, Modèle IA (`deepseek/deepseek-chat`, `google/gemini-2.5-flash`, etc.), Inscriptions
- **Profils** : Créer/Éditer/Supprimer (Nom, Poids, FC Max, FC Repos)

### 3. Application Android
```bash
cd com_twingo_fithub
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```
Installer sur tablette (Android 14+, minSdk 35).

### 4. Connexion & Synchronisation
1. Lancer l'app → Écran d'accueil (mode Kiosk)
2. ⚙️ **Engrenage** → Saisir URL serveur (`http://192.168.1.50:8080`)
3. 🔄 **Sync** → Pull profils + plans depuis serveur
4. 🏋️ Sélectionner profil → Séance du jour → **Démarrer**

---

## 🔒 Sécurité & Confidentialité

| Aspect | Implémentation |
|--------|----------------|
| **Hors-ligne complet** | Aucune connexion Internet requise pour l'entraînement |
| **Clé API partagée** | Stockée côté serveur uniquement (`/admin`), jamais sur tablette |
| **Base locale** | Room (SQLite) chiffrée par Android, aucune télémétrie |
| **BLE** | Pairing standard Android, pas de cloud |
| **Serveur** | Auto-hébergé, pas de cloud tiers |

---

## 📁 Arborescence Détaillée

```
FitHUB/
├── .gitignore
├── README.md
├── LICENSE
├── CONTRIBUTING.md
├── com_twingo_fithub/                 # Android App
│   ├── app/
│   │   ├── src/main/java/com/twingo/fithub/
│   │   │   ├── core/
│   │   │   │   ├── ble/              # Queue GATT, parsers FTMS, Heart Rate
│   │   │   │   └── sync/             # FleetSyncClient, SyncState
│   │   │   ├── data/
│   │   │   │   ├── database/         # Room, DAO, Entities, Converters
│   │   │   │   ├── entity/
│   │   │   │   ├── mapper/
│   │   │   │   ├── network/          # FleetApiService (kserialization)
│   │   │   │   └── repository/
│   │   │   ├── domain/
│   │   │   │   ├── model/            # Profile, WorkoutPlan, WorkoutStep, HR Zones
│   │   │   │   └── session/          # WorkoutSessionEngine
│   │   │   └── ui/
│   │   │       ├── ble/              # BleDeviceDialog
│   │   │       ├── editor/           # WorkoutEditorScreen
│   │   │       ├── profile/          # ProfileSelectionScreen, Editor
│   │   │       └── workout/          # ActiveWorkoutScreen, MetricsGrid
│   │   └── build.gradle.kts
│   └── build.gradle.kts
├── server/                           # Backend Go
│   ├── cmd/server/main.go
│   ├── internal/
│   │   ├── database/                 # SQLite + migrations
│   │   ├── models/                   # Profile, WorkoutPlan, WorkoutStep, etc.
│   │   ├── ai/                       # OpenRouter client (generator.go)
│   │   └── handlers/                 # admin, workouts, fleet
│   ├── web/templates/                # index.html, admin/, workouts/
│   ├── go.mod / go.sum
│   └── fithub.db                     # SQLite (gitignored)
├── .gitignore
├── README.md
├── LICENSE
└── CONTRIBUTING.md
```

---

## 🛠️ Commandes Utiles

```bash
# Android
cd com_twingo_fithub
./gradlew assembleDebug              # Build APK debug
./gradlew testDebugUnitTest          # Tests unitaires (67 tests)
./gradlew lint                        # Lint

# Go Server
cd server
go build ./cmd/server                 # Build binaire
go test ./...                         # Tests (si ajoutés)
go run ./cmd/server                   # Dev server :8080

# Base de données
sqlite3 server/fithub.db ".tables"
sqlite3 server/fithub.db "SELECT * FROM profiles;"
```

---

## 🤝 Contribution

Voir [`CONTRIBUTING.md`](CONTRIBUTING.md) pour :
- Workflow Git / PR
- Standards de code (ktlint, gofmt)
- Tests requis avant PR

---

## 📄 Licence

Ce projet est sous licence **MIT** — voir [`LICENSE`](LICENSE).

---

## 🙏 Remerciements

- **Jetpack Compose** & **Material3** pour l'UI déclarative
- **modernc.org/sqlite** pour SQLite pur Go sans CGO
- **Chi** pour le router HTTP minimaliste
- **OpenRouter** pour l'accès unifié aux modèles IA
- **Communauté FTMS** pour la documentation BLE

---

<p align="center">
  <strong>FitHub OS — Votre station sportive, votre data, votre contrôle.</strong>
</p>