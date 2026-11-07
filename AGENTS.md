# AGENTS.md — Directives de développement FitHub

## 1. Contexte du Projet & Objectifs
- **Application :** FitHub (Application Android native servant de Dashboard / Launcher Kiosk d'entraînement).
- **Cible matérielle :** Tablette Android entrée de gamme (~4 Go de RAM, ex: Redmi Pad SE).
- **Contraintes de performance :** Zéro surcouche lourde, Jetpack Compose pur, pas de fuite mémoire sur les flux Bluetooth.
- **Rôle de l'application :** 
  - Mode Launcher / Kiosk plein écran avec verrouillage veille (`FLAG_KEEP_SCREEN_ON`).
  - Gestion multi-profils locale (poids, FC max, FC repos, calcul Z1-Z5).
  - Moteur de séance structuré (échauffement, intervalles répétés, récupération, consigne textuelle).
  - Connectivité BLE bidirectionnelle avec des équipements sportifs.

---

## 2. Périphériques & Spécifications Bluetooth (BLE)

### A. Capteur Cardio Fréquencemètre (Ceinture / Montre)
- **Service Heart Rate standard :** `0x180D`
- **Caractéristique Heart Rate Measurement :** `0x2A37` (Activer le CCCD `0x2902` pour les notifications).
- **Parsing BPM :** Lire le bit 0 de l'octet de flags. Si bit 0 = 0 -> valeur sur 1 octet (uint8), si bit 0 = 1 -> valeur sur 2 octets (uint16, little-endian).

### B. Équipements de fitness (Standard FTMS)
- **Service FTMS (Fitness Machine Service) :** `0x1826`
- **Données descendantes (Télémétrie en lecture/notif) :**
  - **Treadmill Data (Domyos RUN 500) :** `0x2ACD`
  - **Indoor Bike Data (Elite Suito-T) :** `0x2AD2`
  - **Rower Data (Domyos Rower 900) :** `0x2AD1`
- **Contrôle montant (Écriture sur Elite Suito-T) :**
  - **Fitness Machine Control Point :** `0x2AD9`
  - **Machine à états obligatoire avant envoi d'ordre :**
    1. S'abonner aux indications sur `0x2AD9` (CCCD `0x2902` avec `0x02, 0x00`).
    2. Envoyer la commande `Request Control` (`0x00`).
    3. Attendre la confirmation positive (`0x80 0x00 0x01`).
    4. Envoyer `Start or Resume` (`0x07`).
    5. Envoyer le mode ERG (`0x05` + Target Power en Watts sur 2 octets signés en little-endian) ou Résistance (`0x04` + niveau).

---

## 3. Architecture Technique & Bonnes Pratiques

### A. Stack Technologique
- **Langage :** Kotlin pur (coroutine, flows réactifs).
- **UI :** Jetpack Compose (Material3 minimaliste, contraste élevé lisible pendant l'effort).
- **Base de données :** Room (SQLite) pour persister les profils, les plans de séances et l'historique.
- **Cycle de vie BLE :** Géré impérativement dans un `ForegroundService` Android avec notification persistante.

### B. Règles strictes de programmation BLE
- **Pas de requêtes concurrentes :** L'API `BluetoothGatt` d'Android crashe (Erreur 133) si des opérations se chevauchent.
- **Queue sérialisée :** Toujours faire transiter les commandes BLE (`connect`, `discoverServices`, `readCharacteristic`, `writeCharacteristic`, `writeDescriptor`) par un système de file d'attente FIFO (Channel ou Mutex) qui attend le callback Android correspondant avant de dépiler l'action suivante.
- **Gestion des ressources :** Appeler systématiquement `gatt.disconnect()` puis `gatt.close()` lors de l'arrêt d'une connexion.

### C. Optimisation Tablette 4 Go RAM
- Éviter les bibliothèques d'affichage de courbes tierces lourdes ; utiliser l'API `Canvas` de Compose pour tracer les watts/BPM.
- Aucun parsing lourd sur le thread UI (`Dispatchers.Main`). Tout le décodage de trames FTMS doit tourner sur `Dispatchers.Default` ou `Dispatchers.IO`.

---

## 4. Règles pour l'Agent de Codage
1. **Découpage modulaire :** Ne produis jamais de fichiers fourre-tout de 1000 lignes. Sépare clairement :
   - `core/ble/` : File d'attente GATT, parseurs de paquets binaires.
   - `data/` : Entités Room, DAO, TypeConverters.
   - `domain/` : Modèle de la machine à états de la séance.
   - `ui/` : Écrans Compose séparés (Profils, Dashboard d'effort, Éditeur).
2. **Pas de placeholders non implémentés :** Fournis des parseurs binaires complets et testables. Gère correctement l'endianness (`ByteOrder.LITTLE_ENDIAN`).
3. **Simplicité :** Priorise le bon fonctionnement du mode local avant toute tentative de synchronisation réseau ou d'API externe.