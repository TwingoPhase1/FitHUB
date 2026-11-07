package com.twingo.fithub.ui.workout

import com.twingo.fithub.core.ble.FtmsControlPointManager
import com.twingo.fithub.domain.model.Profile
import com.twingo.fithub.domain.model.WorkoutMachine
import com.twingo.fithub.domain.model.WorkoutPlan
import com.twingo.fithub.domain.session.WorkoutSegment
import com.twingo.fithub.domain.session.WorkoutSessionStatus

sealed interface ActiveWorkoutUiState {

    data object Loading : ActiveWorkoutUiState

    data class Unavailable(
        val message: String,
    ) : ActiveWorkoutUiState

    data class Active(
        val profile: Profile,
        val plan: WorkoutPlan,
        val status: WorkoutSessionStatus,
        val currentSegment: WorkoutSegment,
        val nextSegment: WorkoutSegment?,
        val elapsedSeconds: Int,
        val remainingSeconds: Int,
        val isEndingSoon: Boolean,
        val segmentNumber: Int,
        val segmentCount: Int,
        val completedSegmentCount: Int,
        val segmentProgress: Float,
        val metrics: LiveWorkoutMetrics = LiveWorkoutMetrics(),
        val summary: WorkoutSummary? = null,
    ) : ActiveWorkoutUiState
}

/**
 * Valeurs temps réel destinées au futur flux BLE. Tant qu'aucun capteur n'est
 * connecté, les champs restent nuls et les tuiles affichent "--".
 */
data class LiveWorkoutMetrics(
    val heartRateBpm: Int? = null,
    val powerWatts: Int? = null,
    val targetPowerWatts: Int? = null,
    val cadenceRpm: Int? = null,
    val paceMinPerKm: Double? = null,
    val paceSecondsPer500m: Int? = null,
    val speedKmh: Double? = null,
    val inclinePercent: Double? = null,
    val distanceMeters: Int? = null,
    /**
     * Type d'équipement réellement connecté. Prioritaire sur la détection par
     * le titre de la séance pour choisir les tuiles affichées.
     */
    val machine: WorkoutMachine? = null,
    /** État de la prise de contrôle FTMS (si contrôle acquis). */
    val controlState: FtmsControlPointManager.ControlState? = null,
    /** Adresse de l'équipement FTMS connecté (pour envoyer les commandes). */
    val machineAddress: String? = null,
)

data class WorkoutSummary(
    val elapsedSeconds: Int,
    val plannedSeconds: Int,
    val completedSegments: Int,
    val totalSegments: Int,
)
