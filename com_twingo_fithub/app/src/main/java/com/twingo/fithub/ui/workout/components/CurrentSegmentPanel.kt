package com.twingo.fithub.ui.workout.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twingo.fithub.domain.model.Profile
import com.twingo.fithub.domain.session.WorkoutSegment
import com.twingo.fithub.ui.theme.FitHubOrange
import com.twingo.fithub.ui.theme.FitHubOutline
import com.twingo.fithub.ui.theme.FitHubSurface
import com.twingo.fithub.ui.theme.FitHubSurfaceElevated
import com.twingo.fithub.ui.theme.FitHubText
import com.twingo.fithub.ui.theme.FitHubTextMuted
import com.twingo.fithub.ui.workout.formatBpmRange
import com.twingo.fithub.ui.workout.formatClock
import com.twingo.fithub.ui.workout.visual
import com.twingo.fithub.ui.workout.zoneColor

@Composable
fun CurrentSegmentPanel(
    segment: WorkoutSegment,
    profile: Profile,
    remainingSeconds: Int,
    segmentNumber: Int,
    segmentCount: Int,
    progress: Float,
    isEndingSoon: Boolean,
    isPaused: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val visual = segment.visual()
    val bpmRange = segment.zoneFrom.bpmRangeFor(profile, segment.zoneTo)
    val zoneColor = segment.zoneFrom.zoneColor()

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = FitHubSurface,
        border = BorderStroke(
            width = if (isEndingSoon || isPaused) 2.dp else 1.dp,
            color = if (isEndingSoon || isPaused) FitHubOrange else FitHubOutline,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (compact) 14.dp else 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SegmentChip(label = visual.label, color = visual.color)
                if (segment.isInterval) {
                    SegmentChip(
                        label = "RÉPÉTITION ${segment.repetition}/${segment.repetitions}",
                        color = FitHubOrange,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "BLOC ${segmentNumber.toString().padStart(2, '0')} / " +
                        segmentCount.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.labelSmall,
                    color = FitHubTextMuted,
                )
            }

            Spacer(modifier = Modifier.height(if (compact) 6.dp else 10.dp))

            Text(
                text = segment.title,
                style = if (compact) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.headlineMedium
                },
                color = FitHubText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                val countdownSize = when {
                    compact -> 84.sp
                    maxWidth >= 520.dp && maxHeight >= 170.dp -> 142.sp
                    maxWidth >= 430.dp && maxHeight >= 150.dp -> 132.sp
                    maxWidth >= 330.dp && maxHeight >= 120.dp -> 108.sp
                    else -> 84.sp
                }
                Text(
                    text = formatClock(remainingSeconds),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = countdownSize,
                        lineHeight = countdownSize * 1.02f,
                        letterSpacing = (-3).sp,
                    ),
                    color = if (isEndingSoon || isPaused) FitHubOrange else FitHubText,
                    maxLines = 1,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 8.dp else 10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(FitHubSurfaceElevated),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(if (isEndingSoon || isPaused) FitHubOrange else visual.color),
                )
            }

            Spacer(modifier = Modifier.height(if (compact) 6.dp else 10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = zoneColor.copy(alpha = 0.16f),
                ) {
                    Text(
                        text = "${segment.zoneLabel} · ${formatBpmRange(bpmRange)}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = zoneColor,
                        maxLines = 1,
                    )
                }
                Text(
                    text = segment.targetDetail,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FitHubTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(if (compact) 6.dp else 10.dp))

            Text(
                text = segment.instruction,
                style = if (compact) {
                    MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 22.sp)
                } else {
                    MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp, lineHeight = 27.sp)
                },
                color = FitHubText,
                maxLines = if (compact) 3 else 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SegmentChip(
    label: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.16f),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}
