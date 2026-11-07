package com.twingo.fithub.ui.profile.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.twingo.fithub.domain.model.Profile
import com.twingo.fithub.domain.model.WorkoutStep
import com.twingo.fithub.domain.model.WorkoutStepKind
import com.twingo.fithub.ui.profile.formatStepDuration
import com.twingo.fithub.ui.theme.FitHubBlue
import com.twingo.fithub.ui.theme.FitHubLime
import com.twingo.fithub.ui.theme.FitHubOrange
import com.twingo.fithub.ui.theme.FitHubOutline
import com.twingo.fithub.ui.theme.FitHubSurface
import com.twingo.fithub.ui.theme.FitHubSurfaceElevated
import com.twingo.fithub.ui.theme.FitHubText
import com.twingo.fithub.ui.theme.FitHubTextMuted

@Composable
fun WorkoutStepCard(
    index: Int,
    step: WorkoutStep,
    profile: Profile,
    modifier: Modifier = Modifier,
) {
    val visual = step.kind.visual()
    val bpmRange = step.zoneFrom.bpmRangeFor(profile, step.zoneTo)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = FitHubSurfaceElevated,
        border = BorderStroke(1.dp, FitHubOutline.copy(alpha = 0.65f)),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(visual.color.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (index + 1).toString().padStart(length = 2, padChar = '0'),
                        style = MaterialTheme.typography.labelMedium,
                        color = visual.color,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = visual.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = visual.color,
                    )
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = FitHubText,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FitHubSurface,
                ) {
                    Text(
                        text = formatStepDuration(step),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = FitHubText,
                    )
                }
            }

            Spacer(modifier = Modifier.size(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(modifier = Modifier.weight(0.42f)) {
                    Text(
                        text = "CIBLE",
                        style = MaterialTheme.typography.labelSmall,
                        color = FitHubTextMuted,
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "${step.zoneLabel} · ${bpmRange.first}–${bpmRange.last} bpm",
                        style = MaterialTheme.typography.titleMedium,
                        color = visual.color,
                    )
                    Spacer(modifier = Modifier.size(2.dp))
                    Text(
                        text = step.targetDetail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = FitHubTextMuted,
                    )
                }

                Column(modifier = Modifier.weight(0.58f)) {
                    Text(
                        text = "CONSIGNE",
                        style = MaterialTheme.typography.labelSmall,
                        color = FitHubTextMuted,
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = step.instruction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = FitHubText,
                    )
                }
            }
        }
    }
}

private data class StepVisual(
    val label: String,
    val color: Color,
)

private fun WorkoutStepKind.visual(): StepVisual = when (this) {
    WorkoutStepKind.WARM_UP -> StepVisual(label = "ÉCHAUFFEMENT", color = FitHubBlue)
    WorkoutStepKind.INTERVAL -> StepVisual(label = "INTERVALLES", color = FitHubOrange)
    WorkoutStepKind.ENDURANCE -> StepVisual(label = "EFFORT PRINCIPAL", color = FitHubLime)
    WorkoutStepKind.COOL_DOWN -> StepVisual(label = "RETOUR AU CALME", color = FitHubTextMuted)
}
