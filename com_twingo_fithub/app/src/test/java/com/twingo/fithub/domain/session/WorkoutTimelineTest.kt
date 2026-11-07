package com.twingo.fithub.domain.session

import com.twingo.fithub.domain.model.HeartRateZone
import com.twingo.fithub.domain.model.Profile
import com.twingo.fithub.domain.model.WorkoutPlan
import com.twingo.fithub.domain.model.WorkoutStep
import com.twingo.fithub.domain.model.WorkoutStepKind
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutTimelineTest {

    @Test
    fun intervalBlockExpandsIntoEffortsAndRecoveries() {
        val plan = planWith(
            WorkoutStep(
                title = "Accélérations courtes",
                kind = WorkoutStepKind.INTERVAL,
                durationSeconds = 20,
                repetitions = 8,
                recoverySeconds = 40,
                zoneFrom = HeartRateZone.Z4,
                zoneTo = HeartRateZone.Z5,
                targetDetail = "Plus de 100 tr/min",
                instruction = "Accélère puis récupère.",
            ),
        )

        val segments = WorkoutTimeline.expand(plan)

        assertEquals(16, segments.size)
        assertEquals(8 * 60, segments.sumOf(WorkoutSegment::durationSeconds))
        assertEquals(8, segments.count { it.segmentKind == WorkoutSegmentKind.INTERVAL_EFFORT })
        assertEquals(8, segments.count { it.segmentKind == WorkoutSegmentKind.INTERVAL_RECOVERY })

        val firstEffort = segments[0]
        val firstRecovery = segments[1]
        assertEquals(20, firstEffort.durationSeconds)
        assertEquals(40, firstRecovery.durationSeconds)
        assertEquals(1, firstEffort.repetition)
        assertEquals(1, firstRecovery.repetition)
        assertEquals(8, firstEffort.repetitions)
        assertEquals(HeartRateZone.Z1, firstRecovery.zoneFrom)
        assertEquals("Récupération", firstRecovery.title)
        assertEquals(2, segments[2].repetition)

        val finalRecovery = segments[15]
        assertTrue(finalRecovery.instruction.contains("bloc suivant"))
        assertFalse(finalRecovery.instruction.contains("répétition suivante"))
    }

    @Test
    fun standardBlocksRemainSingleSegments() {
        val plan = planWith(
            WorkoutStep(
                title = "Échauffement",
                kind = WorkoutStepKind.WARM_UP,
                durationSeconds = 5 * 60,
                zoneFrom = HeartRateZone.Z1,
                zoneTo = HeartRateZone.Z2,
                targetDetail = "Tapis",
                instruction = "Démarre doucement.",
            ),
            WorkoutStep(
                title = "Effort",
                kind = WorkoutStepKind.ENDURANCE,
                durationSeconds = 20 * 60,
                zoneFrom = HeartRateZone.Z2,
                targetDetail = "Régulier",
                instruction = "Garde le rythme.",
            ),
        )

        val segments = WorkoutTimeline.expand(plan)

        assertEquals(2, segments.size)
        assertEquals(listOf(0, 1), segments.map { it.stepIndex })
        assertEquals(
            listOf(WorkoutSegmentKind.STANDARD, WorkoutSegmentKind.STANDARD),
            segments.map { it.segmentKind },
        )
    }

    private fun planWith(vararg steps: WorkoutStep): WorkoutPlan = WorkoutPlan(
        id = 1L,
        profileId = PROFILE.id,
        dayOfWeek = DayOfWeek.MONDAY,
        title = "Séance test",
        description = "Description test",
        steps = steps.toList(),
    )

    private companion object {
        val PROFILE = Profile(
            id = 1L,
            name = "Yanis",
            weightKg = 75.0,
            maxHeartRate = 190,
            restingHeartRate = 55,
            isGuest = false,
        )
    }
}
