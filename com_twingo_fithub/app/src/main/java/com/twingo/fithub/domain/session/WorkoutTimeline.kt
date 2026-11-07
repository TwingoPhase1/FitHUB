package com.twingo.fithub.domain.session

import com.twingo.fithub.domain.model.HeartRateZone
import com.twingo.fithub.domain.model.WorkoutPlan
import com.twingo.fithub.domain.model.WorkoutStep

/**
 * Transforme les blocs du plan en une timeline de segments prêts à être joués.
 *
 * Un intervalle de 8 x 20 s / 40 s produit 8 efforts de 20 s et 8 récupérations
 * de 40 s. La récupération après la dernière répétition est conservée pour
 * respecter la durée totale annoncée du plan.
 */
object WorkoutTimeline {

    fun expand(plan: WorkoutPlan): List<WorkoutSegment> = buildList {
        plan.steps.forEachIndexed { stepIndex, step ->
            if (step.isInterval) {
                val repetitions = step.repetitions.coerceAtLeast(1)
                repeat(repetitions) { repetitionIndex ->
                    val repetition = repetitionIndex + 1
                    add(
                        step.toSegment(
                            stepIndex = stepIndex,
                            segmentKind = WorkoutSegmentKind.INTERVAL_EFFORT,
                            title = step.title,
                            repetition = repetition,
                            repetitions = repetitions,
                        ),
                    )

                    if (step.recoverySeconds > 0) {
                        val recoveryEnding = if (repetition < repetitions) {
                            "prépare la répétition suivante."
                        } else {
                            "laisse redescendre le rythme avant le bloc suivant."
                        }
                        add(
                            WorkoutSegment(
                                stepIndex = stepIndex,
                                sourceKind = step.kind,
                                segmentKind = WorkoutSegmentKind.INTERVAL_RECOVERY,
                                title = "Récupération",
                                instruction = "Récupère activement pendant " +
                                    "${step.recoverySeconds} s : respiration calme, geste " +
                                    "relâché, $recoveryEnding",
                                targetDetail = "Récupération active",
                                zoneFrom = HeartRateZone.Z1,
                                durationSeconds = step.recoverySeconds,
                                repetition = repetition,
                                repetitions = repetitions,
                            ),
                        )
                    }
                }
            } else {
                add(
                    step.toSegment(
                        stepIndex = stepIndex,
                        segmentKind = WorkoutSegmentKind.STANDARD,
                        title = step.title,
                    ),
                )
            }
        }
    }

    private fun WorkoutStep.toSegment(
        stepIndex: Int,
        segmentKind: WorkoutSegmentKind,
        title: String,
        repetition: Int = 1,
        repetitions: Int = 1,
    ): WorkoutSegment = WorkoutSegment(
        stepIndex = stepIndex,
        sourceKind = kind,
        segmentKind = segmentKind,
        title = title,
        instruction = instruction,
        targetDetail = targetDetail,
        zoneFrom = zoneFrom,
        zoneTo = zoneTo,
        durationSeconds = durationSeconds,
        repetition = repetition,
        repetitions = repetitions,
        targetPowerWatts = targetPowerWatts,
        targetResistanceLevel = targetResistanceLevel,
        targetInclinePercent = targetInclinePercent,
        targetCadenceRpm = targetCadenceRpm,
        targetSpeedKmh = targetSpeedKmh,
    )
}
