package com.twingo.fithub.ui.workout.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twingo.fithub.core.ble.FtmsControlPointManager
import com.twingo.fithub.domain.model.WorkoutMachine
import com.twingo.fithub.domain.session.WorkoutSessionStatus
import com.twingo.fithub.ui.theme.FitHubBackground
import com.twingo.fithub.ui.theme.FitHubLime
import com.twingo.fithub.ui.theme.FitHubOrange
import com.twingo.fithub.ui.theme.FitHubOutline
import com.twingo.fithub.ui.theme.FitHubSurface
import com.twingo.fithub.ui.theme.FitHubSurfaceElevated
import com.twingo.fithub.ui.theme.FitHubText
import com.twingo.fithub.ui.theme.FitHubTextMuted
import com.twingo.fithub.ui.workout.ActiveWorkoutUiState

@Composable
fun WorkoutManualControlPanel(
    state: ActiveWorkoutUiState.Active,
    compact: Boolean,
    onAdjustPower: (Int) -> Unit,
    onAdjustIncline: (Float) -> Unit,
    onAdjustResistance: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val machine = state.metrics.machine
    val controlState = state.metrics.controlState
    val hasControl = controlState == FtmsControlPointManager.ControlState.CONTROL_ACQUIRED

    // Ne pas afficher si pas de machine, pas de contrôle, ou séance finie
    if (machine == null || !hasControl || state.isCompleted) return

    val gap = if (compact) 8.dp else 12.dp
    val btnSize = if (compact) 56.dp else 64.dp
    val iconSize = if (compact) 24.dp else 28.dp
    val textSize = if (compact) 12.sp else 14.sp

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = FitHubSurface,
        border = BorderStroke(1.dp, FitHubOutline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            // En-tête
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "PILOTAGE MANUEL",
                    style = MaterialTheme.typography.labelSmall,
                    color = FitHubLime,
                    letterSpacing = 1.5.sp,
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(FitHubLime, RoundedCornerShape(4.dp)),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CONTRÔLE ACTIF",
                    style = MaterialTheme.typography.labelSmall,
                    color = FitHubLime,
                    fontWeight = FontWeight.Bold,
                )
            }

            when (machine) {
                WorkoutMachine.BIKE -> BikeControlRow(
                    currentPower = state.metrics.targetPowerWatts ?: state.metrics.powerWatts,
                    onPowerChange = onAdjustPower,
                    currentResistance = state.metrics.cadenceRpm,
                    onResistanceChange = onAdjustResistance,
                    compact = compact,
                    btnSize = btnSize,
                    iconSize = iconSize,
                    textSize = textSize,
                )

                WorkoutMachine.TREADMILL -> TreadmillControlRow(
                    currentIncline = state.metrics.inclinePercent?.toFloat(),
                    onInclineChange = onAdjustIncline,
                    compact = compact,
                    btnSize = btnSize,
                    iconSize = iconSize,
                    textSize = textSize,
                )

                WorkoutMachine.ROWER -> RowerControlRow(
                    currentResistance = state.metrics.cadenceRpm,
                    onResistanceChange = onAdjustResistance,
                    compact = compact,
                    btnSize = btnSize,
                    iconSize = iconSize,
                    textSize = textSize,
                )

                WorkoutMachine.OUTDOOR -> {
                    // Pas de contrôle pour l'extérieur
                }
            }
        }
    }
}

@Composable
private fun BikeControlRow(
    currentPower: Int?,
    onPowerChange: (Int) -> Unit,
    currentResistance: Int?,
    onResistanceChange: (Int) -> Unit,
    compact: Boolean,
    btnSize: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    textSize: androidx.compose.ui.unit.TextUnit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Ligne puissance
        ControlRow(
            label = "PUISSANCE CIBLE",
            value = currentPower?.let { "$it W" } ?: "-- W",
            color = FitHubOrange,
            onDecrease = { onPowerChange(-10) },
            onIncrease = { onPowerChange(10) },
            compact = compact,
            btnSize = btnSize,
            iconSize = iconSize,
            textSize = textSize,
        )

        // Ligne résistance (si disponible)
        ControlRow(
            label = "RÉSISTANCE",
            value = currentResistance?.let { "Niv. $it" } ?: "--",
            color = FitHubLime,
            onDecrease = { onResistanceChange(-1) },
            onIncrease = { onResistanceChange(1) },
            compact = compact,
            btnSize = btnSize,
            iconSize = iconSize,
            textSize = textSize,
        )
    }
}

@Composable
private fun TreadmillControlRow(
    currentIncline: Float?,
    onInclineChange: (Float) -> Unit,
    compact: Boolean,
    btnSize: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    textSize: androidx.compose.ui.unit.TextUnit,
) {
    ControlRow(
        label = "INCLINAISON CIBLE",
        value = currentIncline?.let { "${"%.1f".format(it)} %" } ?: "-- %",
        color = FitHubLime,
        onDecrease = { onInclineChange(-0.5f) },
        onIncrease = { onInclineChange(0.5f) },
        compact = compact,
        btnSize = btnSize,
        iconSize = iconSize,
        textSize = textSize,
    )
}

@Composable
private fun RowerControlRow(
    currentResistance: Int?,
    onResistanceChange: (Int) -> Unit,
    compact: Boolean,
    btnSize: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    textSize: androidx.compose.ui.unit.TextUnit,
) {
    ControlRow(
        label = "RÉSISTANCE CIBLE",
        value = currentResistance?.let { "Niv. $it" } ?: "--",
        color = FitHubLime,
        onDecrease = { onResistanceChange(-1) },
        onIncrease = { onResistanceChange(1) },
        compact = compact,
        btnSize = btnSize,
        iconSize = iconSize,
        textSize = textSize,
    )
}

@Composable
private fun ControlRow(
    label: String,
    value: String,
    color: Color,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    compact: Boolean,
    btnSize: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    textSize: androidx.compose.ui.unit.TextUnit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier
                .width(if (compact) 120.dp else 150.dp)
                .height(btnSize),
            style = MaterialTheme.typography.labelSmall,
            color = FitHubTextMuted,
            fontWeight = FontWeight.SemiBold,
        )

        // Bouton -
        IconButton(
            onClick = onDecrease,
            modifier = Modifier.size(btnSize),
        ) {
            Text(
                text = "−",
                fontSize = if (compact) 28.sp else 32.sp,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }

        // Valeur centrale
        Box(
            modifier = Modifier
                .weight(1f)
                .height(btnSize)
                .background(FitHubSurfaceElevated, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = textSize,
                    fontWeight = FontWeight.Bold,
                ),
                color = color,
            )
        }

        // Bouton +
        IconButton(
            onClick = onIncrease,
            modifier = Modifier.size(btnSize),
        ) {
            Text(
                text = "+",
                fontSize = if (compact) 28.sp else 32.sp,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

private fun ActiveWorkoutUiState.Active.isPaused(): Boolean =
    status == WorkoutSessionStatus.PAUSED

private val ActiveWorkoutUiState.Active.isCompleted: Boolean
    get() = status == WorkoutSessionStatus.COMPLETED

private fun ActiveWorkoutUiState.Active.isRunning(): Boolean =
    status == WorkoutSessionStatus.RUNNING