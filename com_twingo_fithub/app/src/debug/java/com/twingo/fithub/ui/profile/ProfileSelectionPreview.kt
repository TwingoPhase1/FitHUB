package com.twingo.fithub.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.twingo.fithub.domain.model.HeartRateZone
import com.twingo.fithub.domain.model.Profile
import com.twingo.fithub.domain.model.WorkoutPlan
import com.twingo.fithub.domain.model.WorkoutStep
import com.twingo.fithub.domain.model.WorkoutStepKind
import com.twingo.fithub.ui.theme.FitHUBTheme
import java.time.DayOfWeek
import java.time.LocalDate

@Preview(
    name = "Tablette paysage · Séance sélectionnée",
    widthDp = 1280,
    heightDp = 800,
    showBackground = true,
)
@Composable
private fun ProfileSelectionScreenPreview() {
    val yanis = Profile(
        id = 1L,
        name = "Yanis",
        weightKg = 75.0,
        maxHeartRate = 190,
        restingHeartRate = 55,
        isGuest = false,
    )
    val guest = Profile(
        id = 2L,
        name = "Invité",
        weightKg = 70.0,
        maxHeartRate = 185,
        restingHeartRate = 60,
        isGuest = true,
    )
    val today = LocalDate.of(2026, 11, 2)
    val plan = WorkoutPlan(
        id = 1L,
        profileId = yanis.id,
        dayOfWeek = DayOfWeek.MONDAY,
        title = "Marche inclinée sur tapis",
        description = "35 min de marche en côte pour construire l'endurance de base " +
            "sans impact articulaire.",
        steps = listOf(
            WorkoutStep(
                title = "Échauffement progressif",
                kind = WorkoutStepKind.WARM_UP,
                durationSeconds = 5 * 60,
                zoneFrom = HeartRateZone.Z1,
                zoneTo = HeartRateZone.Z2,
                targetDetail = "Tapis · 3 à 5 % · 4,5 km/h",
                instruction = "Marche souple, épaules relâchées. Monte progressivement " +
                    "l'inclinaison et la vitesse.",
            ),
            WorkoutStep(
                title = "Marche inclinée continue",
                kind = WorkoutStepKind.ENDURANCE,
                durationSeconds = 25 * 60,
                zoneFrom = HeartRateZone.Z2,
                targetDetail = "Tapis · 7 % · 5,5 km/h",
                instruction = "Garde le buste droit sans t'accrocher aux poignées. " +
                    "Respiration régulière, foulée active.",
            ),
            WorkoutStep(
                title = "Retour au calme",
                kind = WorkoutStepKind.COOL_DOWN,
                durationSeconds = 5 * 60,
                zoneFrom = HeartRateZone.Z1,
                targetDetail = "Tapis · 0 à 2 % · 4 km/h",
                instruction = "Réduis l'inclinaison puis la vitesse. Termine par quelques " +
                    "étirements des mollets.",
            ),
        ),
    )

    FitHUBTheme {
        ProfileSelectionScreen(
            state = ProfileSelectionUiState(
                profiles = listOf(yanis, guest),
                selectedProfile = yanis,
                today = today,
                todayWorkout = plan,
                isLoading = false,
            ),
            onProfileSelected = {},
            onStartWorkout = {},
            onOpenEditor = {},
        )
    }
}
