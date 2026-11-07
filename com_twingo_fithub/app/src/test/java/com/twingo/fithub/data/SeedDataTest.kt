package com.twingo.fithub.data

import com.twingo.fithub.data.mapper.toDomain
import com.twingo.fithub.domain.model.WorkoutStepKind
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedDataTest {

    @Test
    fun databaseSeedContainsTwoProfilesAndFourSessionsEach() {
        val profiles = SeedData.profiles()

        assertEquals(2, profiles.size)
        assertEquals(listOf("Yanis", "Invité"), profiles.map { it.name })

        val yanis = profiles.first { !it.isGuest }
        assertEquals(75.0, yanis.weightKg, 0.0)
        assertEquals(190, yanis.maxHeartRate)
        assertEquals(55, yanis.restingHeartRate)
        assertTrue(profiles.any { it.isGuest })

        assertEquals(8, SeedData.weeklyPlans().size)
        profiles.forEach { profile ->
            assertEquals(4, SeedData.plansForProfile(profile.id).size)
        }
    }

    @Test
    fun weeklySessionsMatchRequestedDaysAndDurations() {
        val plans = SeedData.plansForProfile(SeedData.YANIS_PROFILE_ID)

        assertEquals(
            listOf(
                DayOfWeek.MONDAY.value,
                DayOfWeek.WEDNESDAY.value,
                DayOfWeek.FRIDAY.value,
                DayOfWeek.SUNDAY.value,
            ),
            plans.map { it.dayOfWeek },
        )
        assertEquals(35 * 60, totalDuration(plans, DayOfWeek.MONDAY))
        assertEquals(30 * 60, totalDuration(plans, DayOfWeek.WEDNESDAY))
        assertEquals(25 * 60, totalDuration(plans, DayOfWeek.FRIDAY))
        assertEquals(40 * 60, totalDuration(plans, DayOfWeek.SUNDAY))
    }

    @Test
    fun wednesdayContainsEightShortAccelerations() {
        val wednesday = SeedData.plansForProfile(SeedData.YANIS_PROFILE_ID)
            .first { it.dayOfWeek == DayOfWeek.WEDNESDAY.value }
            .toDomain()
        val interval = wednesday.steps.first { it.kind == WorkoutStepKind.INTERVAL }

        assertEquals(8, interval.repetitions)
        assertEquals(20, interval.durationSeconds)
        assertEquals(40, interval.recoverySeconds)
        assertEquals(8 * 60, interval.totalDurationSeconds)
    }

    private fun totalDuration(
        plans: List<com.twingo.fithub.data.entity.WorkoutPlanEntity>,
        day: DayOfWeek,
    ): Int = plans.first { it.dayOfWeek == day.value }
        .toDomain()
        .steps
        .sumOf { step -> step.totalDurationSeconds }
}
