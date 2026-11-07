package com.twingo.fithub.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class WorkoutStepKind {
    WARM_UP,
    INTERVAL,
    ENDURANCE,
    COOL_DOWN,
}

/**
 * Bloc élémentaire d'une séance.
 *
 * Un intervalle se décrit avec [repetitions] > 1, [durationSeconds] pour l'effort
 * et [recoverySeconds] pour la récupération entre chaque répétition.
 *
 * Les champs de cible structurés (power, resistance, incline, cadence, speed)
 * permettent le pilotage automatique FTMS. Ils sont optionnels pour rester
 * compatible avec les anciennes séances.
 */
@Serializable
data class WorkoutStep(
    val title: String,
    val kind: WorkoutStepKind,
    val durationSeconds: Int,
    val repetitions: Int = 1,
    val recoverySeconds: Int = 0,
    val zoneFrom: HeartRateZone,
    val zoneTo: HeartRateZone = zoneFrom,
    val targetDetail: String,
    val instruction: String,
    /** Puissance cible en watts (mode ERG pour home-trainer). */
    val targetPowerWatts: Int? = null,
    /** Niveau de résistance cible (0-255). */
    val targetResistanceLevel: Int? = null,
    /** Inclinaison cible en pourcentage (ex: 5.5 pour 5,5%). */
    val targetInclinePercent: Float? = null,
    /** Cadence cible en tr/min (vélo) ou coups/min (rameur). */
    val targetCadenceRpm: Int? = null,
    /** Vitesse cible en km/h (tapis). */
    val targetSpeedKmh: Float? = null,
) {
    val isInterval: Boolean
        get() = repetitions > 1

    val zoneLabel: String
        get() = if (zoneFrom == zoneTo) {
            zoneFrom.label
        } else {
            "${zoneFrom.label}-${zoneTo.label}"
        }

    val totalDurationSeconds: Int
        get() {
            val safeRepetitions = repetitions.coerceAtLeast(1)
            val safeDuration = durationSeconds.coerceAtLeast(0)
            val safeRecovery = recoverySeconds.coerceAtLeast(0)
            return safeRepetitions * (safeDuration + safeRecovery)
        }
}