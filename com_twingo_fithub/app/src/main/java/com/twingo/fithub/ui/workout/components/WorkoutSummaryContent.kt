package com.twingo.fithub.ui.workout.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twingo.fithub.domain.model.Profile
import com.twingo.fithub.ui.theme.FitHubLime
import com.twingo.fithub.ui.theme.FitHubOutline
import com.twingo.fithub.ui.theme.FitHubSurface
import com.twingo.fithub.ui.theme.FitHubText
import com.twingo.fithub.ui.theme.FitHubTextMuted
import com.twingo.fithub.ui.workout.WorkoutSummary
import com.twingo.fithub.ui.workout.formatClock

@Composable
fun WorkoutSummaryContent(
    profile: Profile,
    workoutTitle: String,
    summary: WorkoutSummary,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 980.dp)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "SÉANCE TERMINÉE",
                style = MaterialTheme.typography.labelSmall,
                color = FitHubLime,
                letterSpacing = 2.sp,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = workoutTitle,
                style = MaterialTheme.typography.headlineLarge,
                color = FitHubText,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Bien joué ${profile.name}. Prenez le temps de récupérer et de vous hydrater.",
                style = MaterialTheme.typography.bodyLarge,
                color = FitHubTextMuted,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SummaryStat(
                    label = "DURÉE RÉELLE",
                    value = formatClock(summary.elapsedSeconds),
                    modifier = Modifier.weight(1f),
                )
                SummaryStat(
                    label = "DURÉE PRÉVUE",
                    value = formatClock(summary.plannedSeconds),
                    modifier = Modifier.weight(1f),
                )
                SummaryStat(
                    label = "BLOCS TRAITÉS",
                    value = "${summary.completedSegments} / ${summary.totalSegments}",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = onExit,
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .height(76.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FitHubLime,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = "Retour à l'accueil",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SummaryStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = FitHubSurface,
        border = BorderStroke(1.dp, FitHubOutline),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = FitHubTextMuted,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = FitHubText,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
