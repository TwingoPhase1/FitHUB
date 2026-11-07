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

class WorkoutSessionEngineTest {

    @Test
    fun advancesAcrossSegmentsAndLoopsThroughIntervals() {
        val engine = newEngine()
        engine.start()

        assertEquals(WorkoutSessionStatus.RUNNING, engine.status)
        assertEquals(0, engine.currentSegmentIndex)
        assertEquals(10, engine.segmentRemainingSeconds)

        engine.advanceBy(7_000)

        assertEquals(7, engine.elapsedSeconds)
        assertEquals(3, engine.segmentRemainingSeconds)
        assertTrue(engine.isEndingSoon)

        engine.advanceBy(3_000)

        assertEquals(1, engine.currentSegmentIndex)
        assertEquals(1, engine.completedSegmentCount)
        assertEquals(WorkoutSegmentKind.INTERVAL_EFFORT, engine.currentSegment.segmentKind)
        assertEquals(10, engine.elapsedSeconds)

        engine.advanceBy(5_000)

        assertEquals(2, engine.currentSegmentIndex)
        assertEquals(WorkoutSegmentKind.INTERVAL_RECOVERY, engine.currentSegment.segmentKind)

        engine.advanceBy(3_000)

        assertEquals(3, engine.currentSegmentIndex)
        assertEquals(WorkoutSegmentKind.INTERVAL_EFFORT, engine.currentSegment.segmentKind)
        assertEquals(2, engine.currentSegment.repetition)
    }

    @Test
    fun aSingleLargeTickCanCrossSeveralSegments() {
        val engine = newEngine()
        engine.start()

        engine.advanceBy(18_000)

        assertEquals(3, engine.currentSegmentIndex)
        assertEquals(3, engine.completedSegmentCount)
        assertEquals(18, engine.elapsedSeconds)
        assertEquals(WorkoutSegmentKind.INTERVAL_EFFORT, engine.currentSegment.segmentKind)
        assertEquals(2, engine.currentSegment.repetition)
    }

    @Test
    fun pauseFreezesCountdownAndResumeRestartsIt() {
        val engine = newEngine()
        engine.start()
        engine.advanceBy(2_000)
        engine.pause()

        assertEquals(WorkoutSessionStatus.PAUSED, engine.status)
        val remaining = engine.segmentRemainingMillis
        val elapsed = engine.elapsedMillis

        engine.advanceBy(5_000)

        assertEquals(remaining, engine.segmentRemainingMillis)
        assertEquals(elapsed, engine.elapsedMillis)

        engine.resume()
        engine.advanceBy(1_000)

        assertEquals(WorkoutSessionStatus.RUNNING, engine.status)
        assertEquals(3, engine.elapsedSeconds)
    }

    @Test
    fun skipMovesImmediatelyWithoutCountingSkippedTime() {
        val engine = newEngine()
        engine.start()

        engine.skipCurrentSegment()

        assertEquals(1, engine.currentSegmentIndex)
        assertEquals(1, engine.completedSegmentCount)
        assertEquals(0, engine.elapsedMillis)
        assertEquals(WorkoutSegmentKind.INTERVAL_EFFORT, engine.currentSegment.segmentKind)
    }

    @Test
    fun skippingLastSegmentCompletesSession() {
        val engine = newEngine()
        engine.start()

        repeat(engine.totalSegmentCount) {
            engine.skipCurrentSegment()
        }

        assertEquals(WorkoutSessionStatus.COMPLETED, engine.status)
        assertEquals(engine.totalSegmentCount, engine.completedSegmentCount)
        assertEquals(0, engine.segmentRemainingMillis)
        assertFalse(engine.isEndingSoon)
    }

    @Test
    fun autoCompletesAfterPlannedDuration() {
        val engine = newEngine()
        engine.start()

        engine.advanceBy(engine.plannedDurationSeconds * 1_000L)

        assertEquals(WorkoutSessionStatus.COMPLETED, engine.status)
        assertEquals(engine.plannedDurationSeconds, engine.elapsedSeconds)
        assertEquals(engine.totalSegmentCount, engine.completedSegmentCount)
        assertEquals(0, engine.segmentRemainingSeconds)
        assertEquals(1f, engine.segmentProgress)
    }

    @Test
    fun endingSoonFlagStartsAtThreeRemainingSeconds() {
        val engine = newEngine()
        engine.start()

        engine.advanceBy(6_999)
        assertFalse(engine.isEndingSoon)

        engine.advanceBy(1)
        assertTrue(engine.isEndingSoon)
        assertEquals(3, engine.segmentRemainingSeconds)
    }

    @Test
    fun stopReturnsToIdleAndResetsProgress() {
        val engine = newEngine()
        engine.start()
        engine.advanceBy(8_000)
        engine.stop()

        assertEquals(WorkoutSessionStatus.IDLE, engine.status)
        assertEquals(0, engine.currentSegmentIndex)
        assertEquals(0, engine.elapsedMillis)
        assertEquals(0, engine.completedSegmentCount)
    }

    private fun newEngine(): WorkoutSessionEngine = WorkoutSessionEngine(
        plan = PLAN,
        profile = PROFILE,
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

        val PLAN = WorkoutPlan(
            id = 1L,
            profileId = PROFILE.id,
            dayOfWeek = DayOfWeek.MONDAY,
            title = "Séance test",
            description = "Description test",
            steps = listOf(
                WorkoutStep(
                    title = "Échauffement",
                    kind = WorkoutStepKind.WARM_UP,
                    durationSeconds = 10,
                    zoneFrom = HeartRateZone.Z1,
                    targetDetail = "Lent",
                    instruction = "Démarre.",
                ),
                WorkoutStep(
                    title = "Accélérations",
                    kind = WorkoutStepKind.INTERVAL,
                    durationSeconds = 5,
                    repetitions = 2,
                    recoverySeconds = 3,
                    zoneFrom = HeartRateZone.Z4,
                    zoneTo = HeartRateZone.Z5,
                    targetDetail = "Rapide",
                    instruction = "Accélère.",
                ),
                WorkoutStep(
                    title = "Retour au calme",
                    kind = WorkoutStepKind.COOL_DOWN,
                    durationSeconds = 4,
                    zoneFrom = HeartRateZone.Z1,
                    targetDetail = "Très lent",
                    instruction = "Récupère.",
                ),
            ),
        )
    }
}
