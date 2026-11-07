package com.twingo.fithub.domain.session

import com.twingo.fithub.domain.model.HeartRateZone
import com.twingo.fithub.domain.model.WorkoutStepKind

/**
 * Plus petite unité temporelle d'une séance.
 *
 * Un bloc d'intervalle est découpé en une phase d'effort et, si nécessaire,
 * une phase de récupération. Chaque phase est un [WorkoutSegment] distinct afin
 * que le moteur puisse passer automatiquement de l'une à l'autre.
 */
enum class WorkoutSegmentKind {
    STANDARD,
    INTERVAL_EFFORT,
    INTERVAL_RECOVERY,
}

data class WorkoutSegment(
    val stepIndex: Int,
    val sourceKind: WorkoutStepKind,
    val segmentKind: WorkoutSegmentKind,
    val title: String,
    val instruction: String,
    val targetDetail: String,
    val zoneFrom: HeartRateZone,
    val zoneTo: HeartRateZone = zoneFrom,
    val durationSeconds: Int,
    val repetition: Int = 1,
    val repetitions: Int = 1,
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

    val durationMillis: Long
        get() = durationSeconds.coerceAtLeast(1) * 1_000L
}
