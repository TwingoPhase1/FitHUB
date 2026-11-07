package com.twingo.fithub.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twingo.fithub.core.ble.FtmsControlPointManager
import com.twingo.fithub.domain.session.WorkoutSessionStatus
import com.twingo.fithub.ui.theme.FitHubBackground
import com.twingo.fithub.ui.theme.FitHubError
import com.twingo.fithub.ui.theme.FitHubLime
import com.twingo.fithub.ui.theme.FitHubOrange
import com.twingo.fithub.ui.theme.FitHubSurface
import com.twingo.fithub.ui.theme.FitHubSurfaceElevated
import com.twingo.fithub.ui.theme.FitHubText
import com.twingo.fithub.ui.theme.FitHubTextMuted
import com.twingo.fithub.ui.workout.components.CurrentSegmentPanel
import com.twingo.fithub.ui.workout.components.NextSegmentBanner
import com.twingo.fithub.ui.workout.components.WorkoutControls
import com.twingo.fithub.ui.workout.components.WorkoutManualControlPanel
import com.twingo.fithub.ui.workout.components.WorkoutMetricsGrid
import com.twingo.fithub.ui.workout.components.WorkoutSummaryContent
import com.twingo.fithub.ui.workout.components.WorkoutTopBar

@Composable
fun ActiveWorkoutScreen(
    state: ActiveWorkoutUiState,
    onTogglePause: () -> Unit,
    onSkipSegment: () -> Unit,
    onExit: () -> Unit,
    onAdjustPower: (Int) -> Unit,
    onAdjustIncline: (Float) -> Unit,
    onAdjustResistance: (Int) -> Unit,
    modifier: Modifier = Modifier,
    connectedSensorCount: Int = 0,
    onOpenSensors: () -> Unit = {},
) {
    var showStopConfirmation by rememberSaveable { mutableStateOf(false) }
    val activeState = state as? ActiveWorkoutUiState.Active
    val isCompleted = activeState?.status == WorkoutSessionStatus.COMPLETED

    LaunchedEffect(isCompleted) {
        if (isCompleted) showStopConfirmation = false
    }

    if (!LocalInspectionMode.current) {
        BackHandler {
            when {
                isCompleted -> onExit()
                activeState != null -> showStopConfirmation = true
                else -> onExit()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FitHubBackground),
    ) {
        when (state) {
            ActiveWorkoutUiState.Loading -> LoadingContent()
            is ActiveWorkoutUiState.Unavailable -> UnavailableContent(
                message = state.message,
                onExit = onExit,
            )

            is ActiveWorkoutUiState.Active -> {
                if (state.status == WorkoutSessionStatus.COMPLETED && state.summary != null) {
                    WorkoutSummaryContent(
                        profile = state.profile,
                        workoutTitle = state.plan.title,
                        summary = state.summary,
                        onExit = onExit,
                    )
                } else {
                    LiveWorkoutContent(
                        state = state,
                        onTogglePause = onTogglePause,
                        onSkipSegment = onSkipSegment,
                        onStop = { showStopConfirmation = true },
                        onAdjustPower = onAdjustPower,
                        onAdjustIncline = onAdjustIncline,
                        onAdjustResistance = onAdjustResistance,
                        connectedSensorCount = connectedSensorCount,
                        onOpenSensors = onOpenSensors,
                    )
                }
            }
        }
    }

    if (showStopConfirmation && activeState != null && !isCompleted) {
        StopWorkoutDialog(
            onDismiss = { showStopConfirmation = false },
            onConfirm = {
                showStopConfirmation = false
                onExit()
            },
        )
    }
}

@Composable
private fun LiveWorkoutContent(
    state: ActiveWorkoutUiState.Active,
    onTogglePause: () -> Unit,
    onSkipSegment: () -> Unit,
    onStop: () -> Unit,
    onAdjustPower: (Int) -> Unit,
    onAdjustIncline: (Float) -> Unit,
    onAdjustResistance: (Int) -> Unit,
    connectedSensorCount: Int,
    onOpenSensors: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxHeight < 640.dp
        val horizontalPadding = if (compact) 14.dp else 22.dp
        val verticalPadding = if (compact) 10.dp else 18.dp
        val sectionGap = if (compact) 8.dp else 14.dp
        val isPaused = state.status == WorkoutSessionStatus.PAUSED
        val timerWeight = if (maxWidth >= 1_100.dp) 0.44f else 0.46f
        val stacked = maxWidth < 900.dp && maxHeight > maxWidth
        val panelCompact = compact || (stacked && maxWidth < 700.dp)
        val chromeCompact = compact || maxWidth < 760.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            verticalArrangement = Arrangement.spacedBy(sectionGap),
        ) {
            WorkoutTopBar(
                profile = state.profile,
                workoutTitle = state.plan.title,
                elapsedSeconds = state.elapsedSeconds,
                isPaused = isPaused,
                compact = chromeCompact,
                connectedSensorCount = connectedSensorCount,
                onOpenSensors = onOpenSensors,
                controlState = state.metrics.controlState,
            )

            NextSegmentBanner(
                nextSegment = state.nextSegment,
                isEndingSoon = state.isEndingSoon,
                compact = chromeCompact,
            )

            SessionMainArea(
                state = state,
                compact = panelCompact,
                stacked = stacked,
                timerWeight = timerWeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            WorkoutControls(
                isPaused = isPaused,
                compact = chromeCompact,
                onTogglePause = onTogglePause,
                onSkip = onSkipSegment,
                onStop = onStop,
            )

            WorkoutManualControlPanel(
                state = state,
                compact = chromeCompact,
                onAdjustPower = onAdjustPower,
                onAdjustIncline = onAdjustIncline,
                onAdjustResistance = onAdjustResistance,
            )
        }
    }
}

@Composable
private fun SessionMainArea(
    state: ActiveWorkoutUiState.Active,
    compact: Boolean,
    stacked: Boolean,
    timerWeight: Float,
    modifier: Modifier = Modifier,
) {
    val isPaused = state.status == WorkoutSessionStatus.PAUSED

    if (stacked) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CurrentSegmentPanel(
                segment = state.currentSegment,
                profile = state.profile,
                remainingSeconds = state.remainingSeconds,
                segmentNumber = state.segmentNumber,
                segmentCount = state.segmentCount,
                progress = state.segmentProgress,
                isEndingSoon = state.isEndingSoon,
                isPaused = isPaused,
                compact = compact,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
            WorkoutMetricsGrid(
                state = state,
                compact = compact,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 20.dp),
        ) {
            CurrentSegmentPanel(
                segment = state.currentSegment,
                profile = state.profile,
                remainingSeconds = state.remainingSeconds,
                segmentNumber = state.segmentNumber,
                segmentCount = state.segmentCount,
                progress = state.segmentProgress,
                isEndingSoon = state.isEndingSoon,
                isPaused = isPaused,
                compact = compact,
                modifier = Modifier
                    .weight(timerWeight)
                    .fillMaxHeight(),
            )
            WorkoutMetricsGrid(
                state = state,
                compact = compact,
                modifier = Modifier
                    .weight(1f - timerWeight)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(44.dp),
                color = FitHubLime,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Préparation de la séance…",
                style = MaterialTheme.typography.titleMedium,
                color = FitHubTextMuted,
            )
        }
    }
}

@Composable
private fun UnavailableContent(
    message: String,
    onExit: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "SÉANCE INDISPONIBLE",
                style = MaterialTheme.typography.labelSmall,
                color = FitHubError,
                letterSpacing = 2.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = FitHubTextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onExit,
                modifier = Modifier.height(60.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FitHubLime,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(text = "Retour à l'accueil")
            }
        }
    }
}

@Composable
private fun StopWorkoutDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FitHubSurface,
        titleContentColor = FitHubText,
        textContentColor = FitHubTextMuted,
        title = {
            Text(
                text = "Arrêter la séance ?",
                style = MaterialTheme.typography.headlineMedium,
                color = FitHubText,
            )
        },
        text = {
            Text(
                text = "Le chronomètre sera interrompu et vous reviendrez à l'accueil.",
                style = MaterialTheme.typography.bodyLarge,
                color = FitHubTextMuted,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CONTINUER",
                    style = MaterialTheme.typography.labelLarge,
                    color = FitHubLime,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "ARRÊTER",
                    style = MaterialTheme.typography.labelLarge,
                    color = FitHubError,
                )
            }
        },
    )
}
