package com.twingo.fithub.ui.profile

import com.twingo.fithub.domain.model.Profile
import com.twingo.fithub.domain.model.WorkoutPlan
import java.time.LocalDate

data class ProfileSelectionUiState(
    val profiles: List<Profile> = emptyList(),
    val selectedProfile: Profile? = null,
    val today: LocalDate = LocalDate.now(),
    val todayWorkout: WorkoutPlan? = null,
    val isLoading: Boolean = true,
)
