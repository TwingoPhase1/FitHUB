package com.twingo.fithub.ui.profile

import com.twingo.fithub.domain.model.Profile
import com.twingo.fithub.domain.model.WorkoutStep
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val fullDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)

private val weekdayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE", Locale.FRENCH)

fun formatFullDate(date: LocalDate): String =
    date.format(fullDateFormatter).capitalizeFirstLetter()

fun formatWeekday(date: LocalDate): String =
    date.format(weekdayFormatter).capitalizeFirstLetter()

fun formatDuration(seconds: Int): String {
    val totalMinutes = seconds / 60
    return when {
        totalMinutes >= 60 -> {
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            if (minutes == 0) "$hours h" else "$hours h $minutes min"
        }

        totalMinutes > 0 -> "$totalMinutes min"
        else -> "$seconds s"
    }
}

fun formatStepDuration(step: WorkoutStep): String =
    if (step.isInterval) {
        "${step.repetitions} × ${formatShortDuration(step.durationSeconds)} / " +
            formatShortDuration(step.recoverySeconds)
    } else {
        formatDuration(step.durationSeconds)
    }

fun formatWeight(weightKg: Double): String =
    if (weightKg % 1.0 == 0.0) {
        "${weightKg.toInt()} kg"
    } else {
        String.format(Locale.FRENCH, "%.1f kg", weightKg)
    }

fun Profile.initials(): String =
    name.trim()
        .split(" ")
        .filter(String::isNotBlank)
        .take(2)
        .joinToString(separator = "") { word -> word.first().uppercaseChar().toString() }

private fun formatShortDuration(seconds: Int): String =
    if (seconds >= 60 && seconds % 60 == 0) {
        "${seconds / 60} min"
    } else {
        "$seconds s"
    }

private fun String.capitalizeFirstLetter(): String =
    replaceFirstChar { character ->
        if (character.isLowerCase()) character.titlecase(Locale.FRENCH) else character.toString()
    }
