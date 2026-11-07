package com.twingo.fithub.data.converter

import com.twingo.fithub.domain.model.HeartRateZone
import com.twingo.fithub.domain.model.WorkoutStep
import com.twingo.fithub.domain.model.WorkoutStepKind
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutStepListConverterTest {

    private val converter = WorkoutStepListConverter()

    @Test
    fun roundTripPreservesAllFields() {
        val steps = listOf(
            WorkoutStep(
                title = "Échauffement",
                kind = WorkoutStepKind.WARM_UP,
                durationSeconds = 12 * 60,
                zoneFrom = HeartRateZone.Z2,
                targetDetail = "Home-trainer · 80 à 90 tr/min",
                instruction = "Pédalage fluide.",
            ),
            WorkoutStep(
                title = "Accélérations courtes",
                kind = WorkoutStepKind.INTERVAL,
                durationSeconds = 20,
                repetitions = 8,
                recoverySeconds = 40,
                zoneFrom = HeartRateZone.Z4,
                zoneTo = HeartRateZone.Z5,
                targetDetail = "Plus de 100 tr/min",
                instruction = "Reste assis et récupère complètement.",
            ),
        )

        val json = converter.fromWorkoutSteps(steps)
        val decoded = converter.toWorkoutSteps(json)

        assertEquals(steps, decoded)
    }

    @Test
    fun emptyJsonProducesEmptyList() {
        assertEquals(emptyList<WorkoutStep>(), converter.toWorkoutSteps("[]"))
    }

    @Test
    fun blankJsonProducesEmptyList() {
        assertEquals(emptyList<WorkoutStep>(), converter.toWorkoutSteps(""))
    }
}
