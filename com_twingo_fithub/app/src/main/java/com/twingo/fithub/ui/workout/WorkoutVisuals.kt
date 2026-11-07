package com.twingo.fithub.ui.workout

import androidx.compose.ui.graphics.Color
import com.twingo.fithub.domain.model.HeartRateZone
import com.twingo.fithub.domain.model.WorkoutMachine
import com.twingo.fithub.domain.model.WorkoutPlan
import com.twingo.fithub.domain.session.WorkoutSegment
import com.twingo.fithub.domain.session.WorkoutSegmentKind
import com.twingo.fithub.domain.model.WorkoutStepKind
import com.twingo.fithub.ui.theme.FitHubBlue
import com.twingo.fithub.ui.theme.FitHubError
import com.twingo.fithub.ui.theme.FitHubLime
import com.twingo.fithub.ui.theme.FitHubOrange
import com.twingo.fithub.ui.theme.FitHubTextMuted
import java.util.Locale

fun WorkoutPlan.detectMachine(): WorkoutMachine {
    val source = "$title $description".lowercase(Locale.FRENCH)
    return when {
        "rameur" in source -> WorkoutMachine.ROWER
        "home-trainer" in source || "vélo" in source -> WorkoutMachine.BIKE
        "tapis" in source -> WorkoutMachine.TREADMILL
        else -> WorkoutMachine.OUTDOOR
    }
}

data class WorkoutSegmentVisual(
    val label: String,
    val color: Color,
)

fun WorkoutSegment.visual(): WorkoutSegmentVisual = when (segmentKind) {
    WorkoutSegmentKind.INTERVAL_EFFORT -> WorkoutSegmentVisual("INTERVALLE", FitHubOrange)
    WorkoutSegmentKind.INTERVAL_RECOVERY -> WorkoutSegmentVisual("RÉCUPÉRATION", FitHubBlue)
    WorkoutSegmentKind.STANDARD -> when (sourceKind) {
        WorkoutStepKind.WARM_UP -> WorkoutSegmentVisual("ÉCHAUFFEMENT", FitHubBlue)
        WorkoutStepKind.INTERVAL -> WorkoutSegmentVisual("INTERVALLE", FitHubOrange)
        WorkoutStepKind.ENDURANCE -> WorkoutSegmentVisual("EFFORT PRINCIPAL", FitHubLime)
        WorkoutStepKind.COOL_DOWN -> WorkoutSegmentVisual("RETOUR AU CALME", FitHubTextMuted)
    }
}

fun HeartRateZone.zoneColor(): Color = when (this) {
    HeartRateZone.Z1 -> FitHubBlue
    HeartRateZone.Z2 -> FitHubLime
    HeartRateZone.Z3 -> FitHubLime
    HeartRateZone.Z4 -> FitHubOrange
    HeartRateZone.Z5 -> FitHubError
}