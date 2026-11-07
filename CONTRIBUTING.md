# Guide de Contribution — FitHub OS

Merci de votre intérêt pour contribuer à FitHub OS ! Ce guide vous aidera à démarrer.

---

## 🚀 Workflow de Contribution

### 1. Fork & Clone
```bash
git clone https://github.com/<votre-fork>/FitHUB.git
cd FitHUB
```

### 2. Branche de travail
```bash
git checkout -b feat/ma-nouvelle-fonctionnalite
# ou fix/..., docs/..., refactor/...
```

### 3. Développement
- Respectez les conventions de code (voir ci-dessous)
- Ajoutez des tests si applicable
- Mettez à jour la documentation si nécessaire

### 4. Tests & Vérifications
```bash
# Android
cd com_twingo_fithub
./gradlew testDebugUnitTest          # Tests unitaires (doivent passer)
./gradlew lint                       # Lint (aucun warning bloquant)

# Go Server
cd ../server
go build ./cmd/server                # Compilation
go vet ./...                          # Vérification statique
go test ./...                         # Tests (si existants)
```

### 5. Commit & Push
```bash
git add .
git commit -m "feat: description claire de la fonctionnalité"
git push origin feat/ma-nouvelle-fonctionnalite
```

### 6. Pull Request
- Ouvrez une PR vers `main`
- Décrivez les changements et la motivation
- Référencez l'issue si applicable
- Attendez la review

---

## 📝 Standards de Code

### Kotlin (Android)
- **ktlint** : `./gradlew ktlintCheck` (config standard Android)
- **Nommage** : `camelCase` variables/fonctions, `PascalCase` classes
- **Coroutines** : `suspend` functions, `Dispatchers.IO` pour I/O
- **StateFlow** pour l'état UI, `MutableStateFlow` en privé
- **Suspend functions** pour tout I/O (DB, Réseau, BLE)

### Go (Backend)
- **gofmt / gofumpt** : `gofmt -w .` avant commit
- **go vet** : `go vet ./...` sans avertissement
- **Nommage** : `PascalCase` exporté, `camelCase` privé
- **Erreurs** : Toujours retourner `error` en dernier, wrapper avec `fmt.Errorf("...: %w", err)`
- **Context** : Premier paramètre `ctx context.Context` pour toutes les fonctions I/O

### Git Commit Messages
```
<type>(<scope>): <description courte>

[Corps optionnel : pourquoi, quoi, comment]

Fixes #<issue-number>
```

**Types** : `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `perf`, `ci`

Exemples :
```
feat(android): add sync status indicator in AppHeader
fix(go): handle null profile in FleetSync pullSync
docs: update README with quickstart guide
refactor(ble): extract GATT queue to separate class
test(ai): add unit tests for prompt builder
```

---

## ✅ Checklist PR

Avant de soumettre, vérifiez :

- [ ] Code compile sans erreur (`./gradlew compileDebugKotlin` / `go build`)
- [ ] Tests unitaires passent (`./gradlew testDebugUnitTest` / `go test ./...`)
- [ ] Lint passe (`./gradlew lint` / `go vet ./...`)
- [ ] Pas de warnings bloquants
- [ ] Documentation mise à jour si API changée
- [ ] Tests ajoutés pour nouvelles fonctionnalités
- [ ] Pas de fichiers sensibles dans le commit (`.env`, `*.db`, `*.keystore`, etc.)
- [ ] Commit messages conformes
- [ ] Pas de fichiers binaires/compilés commités

---

## 🧪 Tests

### Android
```bash
cd com_twingo_fithub
./gradlew testDebugUnitTest          # Tests unitaires Room, ViewModels, etc.
./gradlew connectedAndroidTest       # Tests instrumentés (device/emulator)
```

### Go Server
```bash
cd server
go test -v ./...                      # Tous les packages
go test -race ./...                   # Détection de race conditions
go test -coverprofile=coverage.out ./...  # Couverture
go tool cover -html=coverage.out      # Rapport HTML
```

---

## 🐛 Signaler un Bug

Utilisez le template GitHub Issues :
- **Description** claire du problème
- **Étapes de reproduction** (minimales)
- **Comportement attendu** vs **comportement réel**
- **Logs** pertinents (logcat Android, logs serveur)
- **Environnement** : Android version, Go version, matériel BLE testé

---

## 💡 Idées de Contributions Bienvenues

- **Android** : Tests UI Compose, accessibilité, thèmes sombres/clair
- **BLE** : Support nouveaux équipements FTMS (Wahoo, Tacx, etc.)
- **Go** : Tests d'intégration, métriques Prometheus, rate limiting
- **IA** : Nouveaux prompts objectifs, support modèles locaux (Ollama)
- **UI** : Animations, transitions, tablettes pliables
- **Docs** : Traduction anglais, guides matériel, FAQ

---

## 📞 Contact

- **Issues** : GitHub Issues pour bugs/features
- **Discussions** : GitHub Discussions pour questions/architectures
- **Security** : Email privé pour failles de sécurité

---

Merci de contribuer à FitHub OS ! 🏋️💚