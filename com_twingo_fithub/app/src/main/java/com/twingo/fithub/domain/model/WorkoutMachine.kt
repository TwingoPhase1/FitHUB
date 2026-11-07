package com.twingo.fithub.domain.model

/**
 * Type d'équipement d'entraînement pour le pilotage FTMS et l'affichage.
 */
enum class WorkoutMachine {
    TREADMILL,
    BIKE,
    ROWER,
    OUTDOOR;

    val displayName: String
        get() = when (this) {
            TREADMILL -> "Tapis"
            BIKE -> "Home-trainer"
            ROWER -> "Rameur"
            OUTDOOR -> "Extérieur"
        }
}