package com.twingo.fithub.ui.workout

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.twingo.fithub.domain.model.HeartRateZone
import com.twingo.fithub.domain.model.Profile
import com.twingo.fithub.domain.model.WorkoutPlan
import com.twingo.fithub.domain.model.WorkoutStep
import com.twingo.fithub.domain.model.WorkoutStepKind
import com.twingo.fithub.domain.session.WorkoutSessionStatus
import com.twingo.fithub.domain.session.WorkoutTimeline
import com.twingo.fithub.ui.theme.FitHUBTheme
import com.twingo.fithub.ui.workout.components.WorkoutSummaryContent
import java.time.DayOfWeek

@Preview(
    name = "Tablette paysage · Séance active",
    widthDp = 1280,
    heightDp = 800,
    showBackground = true,
)
@Composable
private fun ActiveWorkoutPreview() {
    FitHUBTheme {
        ActiveWorkoutScreen(
            state = previewActiveState(),
            onTogglePause = {},
            onSkipSegment = {},
            onExit = {},
            onAdjustPower = {},
            onAdjustIncline = {},
            onAdjustResistance = {},
        )
    }
}

@Preview(
    name = "Tablette 960 × 600 · Séance active",
    widthDp = 960,
    heightDp = 600,
    showBackground = true,
)
@Composable
private fun CompactActiveWorkoutPreview() {
    FitHUBTheme {
        ActiveWorkoutScreen(
            state = previewActiveState().copy(
                metrics = LiveWorkoutMetrics(
                    heartRateBpm = 142,
                    powerWatts = 128,
                    cadenceRpm = 96,
                    speedKmh = 28.4,
                ),
            ),
            onTogglePause = {},
            onSkipSegment = {},
            onExit = {},
            onAdjustPower = {},
            onAdjustIncline = {},
            onAdjustResistance = {},
        )
    }
}

@Preview(
    name = "Tablette paysage · Récapitulatif",
    widthDp = 1280,
    heightDp = 800,
    showBackground = true,
)
@Composable
private fun WorkoutSummaryPreview() {
    FitHUBTheme {
        WorkoutSummaryContent(
            profile = PREVIEW_PROFILE,
            workoutTitle = PREVIEW_PLAN.title,
            summary = WorkoutSummary(
                elapsedSeconds = 30 * 60 - 42,
                plannedSeconds = 30 * 60,
                completedSegments = 18,
                totalSegments = 18,
            ),
            onExit = {},
        )
    }
}

private fun previewActiveState(): ActiveWorkoutUiState.Active {
    val segments = WorkoutTimeline.expand(PREVIEW_PLAN)
    return ActiveWorkoutUiState.Active(
        profile = PREVIEW_PROFILE,
        plan = PREVIEW_PLAN,
        status = WorkoutSessionStatus.RUNNING,
        currentSegment = segments[2],
        nextSegment = segments[3],
        elapsedSeconds = 12 * 60 + 30,
        remainingSeconds = 30,
        isEndingSoon = false,
        segmentNumber = 3,
        segmentCount = segments.size,
        completedSegmentCount = 2,
        segmentProgress = 0.25f,
    )
}

private val PREVIEW_PROFILE = Profile(
    id = 1L,
    name = "Yanis",
    weightKg = 75.0,
    maxHeartRate = 190,
    restingHeartRate = 55,
    isGuest = false,
)

private val PREVIEW_PLAN = WorkoutPlan(
    id = 2L,
    profileId = PREVIEW_PROFILE.id,
    dayOfWeek = DayOfWeek.WEDNESDAY,
    title = "Home-trainer · accélérations courtes",
    description = "30 min de vélo avec 8 accélérations de 20 s et 40 s de récupération.",
    steps = listOf(
        WorkoutStep(
            title = "Échauffement",
            kind = WorkoutStepKind.WARM_UP,
            durationSeconds = 12 * 60,
            zoneFrom = HeartRateZone.Z2,
            targetDetail = "Home-trainer · 80 à 90 tr/min",
            instruction = "Pédalage fluide, résistance légère. Prépare les jambes sans " +
                "monter dans les tours.",
        ),
        WorkoutStep(
            title = "Accélérations courtes",
            kind = WorkoutStepKind.INTERVAL,
            durationSeconds = 20,
            repetitions = 8,
            recoverySeconds = 40,
            zoneFrom = HeartRateZone.Z4,
            zoneTo = HeartRateZone.Z5,
            targetDetail = "Home-trainer · plus de 100 tr/min · braquet léger",
            instruction = "Accélération explosive en restant assis. Récupère complètement " +
                "pendant les 40 s.",
        ),
        WorkoutStep(
            title = "Retour au calme",
            kind = WorkoutStepKind.COOL_DOWN,
            durationSeconds = 10 * 60,
            zoneFrom = HeartRateZone.Z1,
            targetDetail = "Home-trainer · 70 à 80 tr/min",
            instruction = "Récupération active, hydratation et respiration calme.",
        ),
    ),
)
