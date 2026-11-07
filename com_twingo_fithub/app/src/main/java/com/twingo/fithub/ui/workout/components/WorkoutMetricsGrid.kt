package com.twingo.fithub.ui.workout.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twingo.fithub.domain.model.HeartRateZone
import com.twingo.fithub.domain.model.WorkoutMachine
import com.twingo.fithub.ui.theme.FitHubError
import com.twingo.fithub.ui.theme.FitHubOrange
import com.twingo.fithub.ui.theme.FitHubOutline
import com.twingo.fithub.ui.theme.FitHubSurface
import com.twingo.fithub.ui.theme.FitHubText
import com.twingo.fithub.ui.theme.FitHubTextMuted
import com.twingo.fithub.ui.workout.ActiveWorkoutUiState
import com.twingo.fithub.ui.workout.detectMachine
import com.twingo.fithub.ui.workout.formatBpmRange
import com.twingo.fithub.ui.workout.formatDistanceMeters
import com.twingo.fithub.ui.workout.formatMetricValue
import com.twingo.fithub.ui.workout.formatPaceSecondsPer500m
import com.twingo.fithub.ui.workout.formatPercentage
import com.twingo.fithub.ui.workout.zoneColor
import kotlin.Triple

@Composable
fun WorkoutMetricsGrid(
    state: ActiveWorkoutUiState.Active,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val segment = state.currentSegment
    val profile = state.profile
    val metrics = state.metrics
    val planMachine = remember(state.plan.id) { state.plan.detectMachine() }
    val machine = metrics.machine ?: planMachine

    val measuredZone = metrics.heartRateBpm?.let { bpm -> HeartRateZone.fromBpm(profile, bpm) }
    val targetRange = segment.zoneFrom.bpmRangeFor(profile, segment.zoneTo)
    val displayedRange = measuredZone?.bpmRangeFor(profile) ?: targetRange
    val heartRateFooter = when {
        measuredZone != null -> "${measuredZone.label} · ${formatBpmRange(displayedRange)}"
        metrics.heartRateBpm != null -> "HORS ZONE"
        else -> "${segment.zoneLabel} · ${formatBpmRange(targetRange)}"
    }
    val heartRateColor = when {
        measuredZone != null -> measuredZone.zoneColor()
        metrics.heartRateBpm != null -> FitHubError
        else -> segment.zoneFrom.zoneColor()
    }

    val cadenceData = when (machine) {
        WorkoutMachine.BIKE -> Triple("CADENCE", "tr/min", formatMetricValue(metrics.cadenceRpm))
        WorkoutMachine.ROWER -> Triple("CADENCE", "coups/min", formatMetricValue(metrics.cadenceRpm))
        WorkoutMachine.TREADMILL, WorkoutMachine.OUTDOOR -> {
            val pace = metrics.paceMinPerKm
                ?: metrics.speedKmh?.takeIf { it > 0.0 }?.let { 60.0 / it }
            Triple("ALLURE", "min/km", formatMetricValue(pace))
        }
    }
    val cadenceTitle = cadenceData.first
    val cadenceUnit = cadenceData.second
    val cadenceValue = cadenceData.third

    val secondaryMetric = when (machine) {
        WorkoutMachine.ROWER -> MetricTileData(
            title = "ALLURE / 500 M",
            value = formatPaceSecondsPer500m(metrics.paceSecondsPer500m),
            unit = "/500 m",
            footerLabel = "DISTANCE",
            footerValue = formatDistanceMeters(metrics.distanceMeters),
            footerColor = FitHubTextMuted,
        )

        WorkoutMachine.BIKE -> MetricTileData(
            title = "VITESSE",
            value = formatMetricValue(metrics.speedKmh),
            unit = "km/h",
            footerLabel = "DISTANCE",
            footerValue = formatDistanceMeters(metrics.distanceMeters),
            footerColor = FitHubTextMuted,
        )

        WorkoutMachine.TREADMILL, WorkoutMachine.OUTDOOR -> MetricTileData(
            title = "VITESSE",
            value = formatMetricValue(metrics.speedKmh),
            unit = "km/h",
            footerLabel = "INCLINAISON",
            footerValue = formatPercentage(metrics.inclinePercent),
            footerColor = FitHubTextMuted,
        )
    }

    val gap = if (compact) 10.dp else 14.dp
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            MetricTile(
                data = MetricTileData(
                    title = "FRÉQUENCE CARDIAQUE",
                    value = formatMetricValue(metrics.heartRateBpm),
                    unit = "bpm",
                    footerLabel = if (metrics.heartRateBpm == null) "ZONE CIBLE" else "ZONE ACTUELLE",
                    footerValue = heartRateFooter,
                    footerColor = heartRateColor,
                ),
                compact = compact,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            MetricTile(
                data = MetricTileData(
                    title = "PUISSANCE",
                    value = formatMetricValue(metrics.powerWatts),
                    unit = "W",
                    footerLabel = "CIBLE",
                    footerValue = metrics.targetPowerWatts?.let { "$it W" } ?: "-- W",
                    footerColor = if (metrics.targetPowerWatts != null) {
                        FitHubOrange
                    } else {
                        FitHubTextMuted
                    },
                ),
                compact = compact,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            MetricTile(
                data = MetricTileData(
                    title = cadenceTitle,
                    value = cadenceValue,
                    unit = cadenceUnit,
                    footerLabel = "CIBLE",
                    footerValue = segment.targetDetail,
                    footerColor = FitHubTextMuted,
                ),
                compact = compact,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            MetricTile(
                data = secondaryMetric,
                compact = compact,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

private data class MetricTileData(
    val title: String,
    val value: String,
    val unit: String,
    val footerLabel: String,
    val footerValue: String,
    val footerColor: Color,
)

@Composable
private fun MetricTile(
    data: MetricTileData,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = FitHubSurface,
        border = BorderStroke(1.dp, FitHubOutline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (compact) 12.dp else 18.dp),
        ) {
            Text(
                text = data.title,
                style = MaterialTheme.typography.labelSmall,
                color = FitHubTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = data.value,
                    fontSize = if (compact) 34.sp else 44.sp,
                    lineHeight = if (compact) 38.sp else 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (data.value == "--") FitHubTextMuted else FitHubText,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = data.unit,
                    modifier = Modifier.padding(bottom = if (compact) 4.dp else 7.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FitHubTextMuted,
                    maxLines = 1,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = data.footerLabel,
                style = MaterialTheme.typography.labelSmall,
                color = FitHubTextMuted,
            )
            Text(
                text = data.footerValue,
                style = if (compact) {
                    MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 15.sp)
                } else {
                    MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 17.sp)
                },
                color = data.footerColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
