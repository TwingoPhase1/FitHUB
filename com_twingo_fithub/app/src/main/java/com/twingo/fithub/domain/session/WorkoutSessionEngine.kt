package com.twingo.fithub.domain.session

import com.twingo.fithub.domain.model.Profile
import com.twingo.fithub.domain.model.WorkoutPlan
import kotlin.math.ceil

/**
 * Machine à états indépendante d'Android qui fait avancer la séance avec le
 * temps réel fourni par l'appelant.
 *
 * Le moteur ne possède ni coroutine ni horloge : le ViewModel lui transmet des
 * deltas en millisecondes. Cela le rend déterministe et testable sur la JVM.
 */
class WorkoutSessionEngine(
    val plan: WorkoutPlan,
    val profile: Profile,
    val segments: List<WorkoutSegment> = WorkoutTimeline.expand(plan),
) {

    init {
        require(segments.isNotEmpty()) { "Une séance doit contenir au moins un segment." }
    }

    var status: WorkoutSessionStatus = WorkoutSessionStatus.IDLE
        private set

    var currentSegmentIndex: Int = 0
        private set

    var elapsedMillis: Long = 0L
        private set

    var segmentElapsedMillis: Long = 0L
        private set

    var completedSegmentCount: Int = 0
        private set

    val totalSegmentCount: Int
        get() = segments.size

    val currentSegment: WorkoutSegment
        get() = segments[currentSegmentIndex]

    val nextSegment: WorkoutSegment?
        get() = segments.getOrNull(currentSegmentIndex + 1)

    val plannedDurationSeconds: Int
        get() = segments.sumOf(WorkoutSegment::durationSeconds)

    val elapsedSeconds: Int
        get() = (elapsedMillis / 1_000L).toInt()

    val segmentRemainingMillis: Long
        get() = (currentSegment.durationMillis - segmentElapsedMillis).coerceAtLeast(0L)

    val segmentRemainingSeconds: Int
        get() = ceil(segmentRemainingMillis / 1_000.0).toInt()

    /**
     * Vrai pendant les trois dernières secondes du segment courant. Ce drapeau
     * permet à l'UI d'anticiper le changement de rythme annoncé par le bloc
     * suivant.
     */
    val isEndingSoon: Boolean
        get() = segmentRemainingSeconds in 1..ENDING_SOON_SECONDS

    val segmentProgress: Float
        get() {
            val duration = currentSegment.durationMillis
            return (segmentElapsedMillis.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        }

    fun start() {
        if (status != WorkoutSessionStatus.IDLE) return
        status = WorkoutSessionStatus.RUNNING
    }

    fun pause() {
        if (status == WorkoutSessionStatus.RUNNING) {
            status = WorkoutSessionStatus.PAUSED
        }
    }

    fun resume() {
        if (status == WorkoutSessionStatus.PAUSED) {
            status = WorkoutSessionStatus.RUNNING
        }
    }

    /**
     * Avance la séance de [deltaMillis]. Les transitions automatiques entre
     * segments (et les boucles d'intervalles) sont absorbées dans la même passe.
     */
    fun advanceBy(deltaMillis: Long) {
        if (status != WorkoutSessionStatus.RUNNING || deltaMillis <= 0L) return

        var remainingDelta = deltaMillis
        while (remainingDelta > 0L && status == WorkoutSessionStatus.RUNNING) {
            val duration = currentSegment.durationMillis
            val remainingInSegment = (duration - segmentElapsedMillis).coerceAtLeast(0L)

            if (remainingDelta < remainingInSegment) {
                segmentElapsedMillis += remainingDelta
                elapsedMillis += remainingDelta
                remainingDelta = 0L
            } else {
                elapsedMillis += remainingInSegment
                remainingDelta -= remainingInSegment
                completedSegmentCount += 1

                if (currentSegmentIndex == segments.lastIndex) {
                    segmentElapsedMillis = duration
                    status = WorkoutSessionStatus.COMPLETED
                } else {
                    currentSegmentIndex += 1
                    segmentElapsedMillis = 0L
                }
            }
        }
    }

    /**
     * Passe immédiatement au segment suivant. Le temps sauté n'est pas ajouté
     * au chronomètre total : celui-ci mesure le temps réellement passé.
     */
    fun skipCurrentSegment() {
        if (status != WorkoutSessionStatus.RUNNING && status != WorkoutSessionStatus.PAUSED) {
            return
        }

        completedSegmentCount = (completedSegmentCount + 1).coerceAtMost(segments.size)
        if (currentSegmentIndex == segments.lastIndex) {
            segmentElapsedMillis = currentSegment.durationMillis
            status = WorkoutSessionStatus.COMPLETED
        } else {
            currentSegmentIndex += 1
            segmentElapsedMillis = 0L
        }
    }

    /**
     * Arrêt volontaire (déjà confirmé par l'utilisateur). La séance revient à
     * l'état initial ; le ViewModel quitte ensuite l'écran d'effort.
     */
    fun stop() {
        status = WorkoutSessionStatus.IDLE
        currentSegmentIndex = 0
        elapsedMillis = 0L
        segmentElapsedMillis = 0L
        completedSegmentCount = 0
    }

    private companion object {
        const val ENDING_SOON_SECONDS = 3
    }
}
