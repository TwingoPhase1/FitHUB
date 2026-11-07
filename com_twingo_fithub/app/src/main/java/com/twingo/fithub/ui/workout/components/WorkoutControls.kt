package com.twingo.fithub.ui.workout.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twingo.fithub.ui.theme.FitHubError
import com.twingo.fithub.ui.theme.FitHubLime
import com.twingo.fithub.ui.theme.FitHubOutline
import com.twingo.fithub.ui.theme.FitHubSurfaceElevated
import com.twingo.fithub.ui.theme.FitHubText

@Composable
fun WorkoutControls(
    isPaused: Boolean,
    compact: Boolean,
    onTogglePause: () -> Unit,
    onSkip: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonShape = RoundedCornerShape(20.dp)
    val buttonTextStyle = MaterialTheme.typography.titleMedium.copy(
        fontSize = if (compact) 18.sp else 20.sp,
        fontWeight = FontWeight.Bold,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (compact) 62.dp else 76.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 16.dp),
    ) {
        Button(
            onClick = onTogglePause,
            modifier = Modifier
                .weight(1.35f)
                .fillMaxHeight(),
            shape = buttonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = FitHubLime,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = if (isPaused) "REPRENDRE" else "PAUSE",
                style = buttonTextStyle,
            )
        }

        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier
                .weight(0.9f)
                .fillMaxHeight(),
            shape = buttonShape,
            border = BorderStroke(1.5.dp, FitHubOutline),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = FitHubSurfaceElevated,
                contentColor = FitHubText,
            ),
        ) {
            Text(
                text = "PASSER",
                style = buttonTextStyle,
            )
        }

        OutlinedButton(
            onClick = onStop,
            modifier = Modifier
                .weight(0.85f)
                .fillMaxHeight(),
            shape = buttonShape,
            border = BorderStroke(1.5.dp, FitHubError),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = FitHubError.copy(alpha = 0.08f),
                contentColor = FitHubError,
            ),
        ) {
            Text(
                text = "ARRÊTER",
                style = buttonTextStyle,
            )
        }
    }
}
