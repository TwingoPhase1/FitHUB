package com.twingo.fithub.ui.workout.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.twingo.fithub.domain.session.WorkoutSegment
import com.twingo.fithub.ui.theme.FitHubLime
import com.twingo.fithub.ui.theme.FitHubOrange
import com.twingo.fithub.ui.theme.FitHubOutline
import com.twingo.fithub.ui.theme.FitHubSurfaceElevated
import com.twingo.fithub.ui.theme.FitHubText
import com.twingo.fithub.ui.theme.FitHubTextMuted
import com.twingo.fithub.ui.workout.formatSegmentDuration
import com.twingo.fithub.ui.workout.visual

@Composable
fun NextSegmentBanner(
    nextSegment: WorkoutSegment?,
    isEndingSoon: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val visual = nextSegment?.visual()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (isEndingSoon) {
            FitHubOrange.copy(alpha = 0.12f)
        } else {
            FitHubSurfaceElevated
        },
        border = BorderStroke(
            width = if (isEndingSoon) 2.dp else 1.dp,
            color = if (isEndingSoon) FitHubOrange else FitHubOutline,
        ),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 16.dp else 22.dp,
                vertical = if (compact) 10.dp else 14.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "À SUIVRE",
                        style = MaterialTheme.typography.labelSmall,
                        color = FitHubLime,
                    )
                    if (visual != null) {
                        Text(
                            text = "· ${visual.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = visual.color,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (isEndingSoon) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = if (nextSegment == null) {
                                "FIN IMMINENTE"
                            } else {
                                "CHANGEMENT IMMINENT"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = FitHubOrange,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(if (compact) 2.dp else 4.dp))
                Text(
                    text = nextSegment?.title ?: "Dernier bloc · termine au mieux",
                    style = if (compact) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                    color = FitHubText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (nextSegment != null) {
                Text(
                    text = formatSegmentDuration(nextSegment),
                    style = if (compact) {
                        MaterialTheme.typography.titleLarge
                    } else {
                        MaterialTheme.typography.headlineMedium
                    },
                    color = if (isEndingSoon) FitHubOrange else FitHubText,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Text(
                    text = "DERNIER",
                    style = MaterialTheme.typography.labelMedium,
                    color = FitHubTextMuted,
                )
            }
        }
    }
}
