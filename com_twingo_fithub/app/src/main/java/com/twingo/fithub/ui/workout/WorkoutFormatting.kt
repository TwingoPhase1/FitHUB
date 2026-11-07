package com.twingo.fithub.ui.workout

import com.twingo.fithub.domain.session.WorkoutSegment
import java.util.Locale

fun formatClock(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val hours = safeSeconds / 3_600
    val minutes = (safeSeconds % 3_600) / 60
    val seconds = safeSeconds % 60
    return if (hours > 0) {
        String.format(Locale.FRENCH, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.FRENCH, "%02d:%02d", minutes, seconds)
    }
}

fun formatSegmentDuration(segment: WorkoutSegment): String {
    val seconds = segment.durationSeconds.coerceAtLeast(1)
    val minutes = seconds / 60
    val remainder = seconds % 60
    return when {
        minutes == 0 -> "$seconds s"
        remainder == 0 -> "$minutes min"
        else -> "$minutes min $remainder s"
    }
}

fun formatMetricValue(value: Int?): String = value?.toString() ?: "--"

fun formatMetricValue(value: Double?): String = when {
    value == null -> "--"
    value % 1.0 == 0.0 -> value.toInt().toString()
    else -> String.format(Locale.FRENCH, "%.1f", value)
}

fun formatBpmRange(range: IntRange): String = "${range.first}–${range.last} bpm"

fun formatPercentage(value: Double?): String =
    if (value == null) "-- %" else "${formatMetricValue(value)} %"

/**
 * Allure rameur exprimée en secondes par 500 m, rendue au format m:ss.
 */
fun formatPaceSecondsPer500m(seconds: Int?): String {
    if (seconds == null || seconds <= 0) return "--"
    return String.format(Locale.FRENCH, "%d:%02d", seconds / 60, seconds % 60)
}

fun formatDistanceMeters(meters: Int?): String {
    if (meters == null) return "--"
    return if (meters >= 1_000) {
        String.format(Locale.FRENCH, "%.2f km", meters / 1_000.0)
    } else {
        "$meters m"
    }
}
