package com.twingo.fithub.ui.workout.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twingo.fithub.core.ble.FtmsControlPointManager
import com.twingo.fithub.domain.model.Profile
import com.twingo.fithub.ui.theme.FitHubBlue
import com.twingo.fithub.ui.theme.FitHubError
import com.twingo.fithub.ui.theme.FitHubLime
import com.twingo.fithub.ui.theme.FitHubOrange
import com.twingo.fithub.ui.theme.FitHubSurfaceElevated
import com.twingo.fithub.ui.theme.FitHubText
import com.twingo.fithub.ui.theme.FitHubTextMuted
import com.twingo.fithub.ui.workout.formatClock
import java.util.Locale

@Composable
fun WorkoutTopBar(
    profile: Profile,
    workoutTitle: String,
    elapsedSeconds: Int,
    isPaused: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
    connectedSensorCount: Int = 0,
    onOpenSensors: () -> Unit = {},
    controlState: FtmsControlPointManager.ControlState? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "FITHUB · ${profile.name.uppercase(Locale.FRENCH)}",
                style = MaterialTheme.typography.labelSmall,
                color = FitHubLime,
            )
            Text(
                text = workoutTitle,
                style = if (compact) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.headlineMedium
                },
                color = FitHubText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Surface(
            modifier = Modifier.clickable(onClick = onOpenSensors),
            shape = RoundedCornerShape(50),
            color = if (connectedSensorCount > 0) {
                FitHubLime.copy(alpha = 0.14f)
            } else {
                FitHubSurfaceElevated
            },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(
                            color = if (connectedSensorCount > 0) FitHubLime else FitHubTextMuted,
                            shape = CircleShape,
                        ),
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = if (connectedSensorCount > 0) {
                        "CAPTEURS · $connectedSensorCount"
                    } else {
                        "CAPTEURS"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (connectedSensorCount > 0) FitHubLime else FitHubTextMuted,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(modifier = Modifier.width(if (compact) 8.dp else 12.dp))

        // Badge d'état du contrôle FTMS
        controlState?.let { state ->
            val (label, color) = when (state) {
                FtmsControlPointManager.ControlState.CONTROL_ACQUIRED ->
                    "CONTRÔLE ACTIF" to FitHubLime
                FtmsControlPointManager.ControlState.REQUESTING_CONTROL,
                FtmsControlPointManager.ControlState.STARTING,
                FtmsControlPointManager.ControlState.SUBSCRIBING_INDICATIONS ->
                    "CONNEXION FTMS…" to FitHubOrange
                FtmsControlPointManager.ControlState.FAILED ->
                    "CONTRÔLE ÉCHOUÉ" to FitHubError
                else -> "LECTURE SEULE" to FitHubTextMuted
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = color.copy(alpha = 0.14f),
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.width(if (compact) 8.dp else 12.dp))
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = if (isPaused) {
                FitHubOrange.copy(alpha = 0.16f)
            } else {
                FitHubLime.copy(alpha = 0.14f)
            },
        ) {
            Text(
                text = if (isPaused) "EN PAUSE" else "EN COURS",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (isPaused) FitHubOrange else FitHubLime,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.width(if (compact) 16.dp else 24.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "TEMPS TOTAL",
                style = MaterialTheme.typography.labelSmall,
                color = FitHubTextMuted,
            )
            Text(
                text = formatClock(elapsedSeconds),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                ),
                color = if (isPaused) FitHubOrange else FitHubText,
            )
        }
    }
}
