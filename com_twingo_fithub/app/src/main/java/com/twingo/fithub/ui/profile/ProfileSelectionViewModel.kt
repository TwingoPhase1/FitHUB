package com.twingo.fithub.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.twingo.fithub.data.repository.FitnessRepository
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive

class ProfileSelectionViewModel(
    private val repository: FitnessRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val selectedProfileId = MutableStateFlow<Long?>(null)

    /**
     * Réémet la date chaque minute afin que le kiosk bascule automatiquement
     * sur la séance du nouveau jour sans redémarrage.
     */
    private val currentDate: Flow<LocalDate> = flow {
        while (currentCoroutineContext().isActive) {
            emit(LocalDate.now(clock))
            delay(DATE_REFRESH_INTERVAL_MS)
        }
    }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ProfileSelectionUiState> = combine(
        repository.observeProfiles(),
        selectedProfileId,
        currentDate,
    ) { profiles, profileId, date ->
        Triple(profiles, profileId, date)
    }.flatMapLatest { (profiles, profileId, date) ->
        val selectedProfile = profiles.firstOrNull { profile -> profile.id == profileId }
        if (selectedProfile == null) {
            flowOf(
                ProfileSelectionUiState(
                    profiles = profiles,
                    selectedProfile = null,
                    today = date,
                    todayWorkout = null,
                    isLoading = false,
                ),
            )
        } else {
            repository.observeWorkoutForDay(selectedProfile.id, date.dayOfWeek)
                .map { workout ->
                    ProfileSelectionUiState(
                        profiles = profiles,
                        selectedProfile = selectedProfile,
                        today = date,
                        todayWorkout = workout,
                        isLoading = false,
                    )
                }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = ProfileSelectionUiState(today = LocalDate.now(clock)),
    )

    fun selectProfile(profileId: Long) {
        selectedProfileId.value = profileId
    }

    class Factory(
        private val repository: FitnessRepository,
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ProfileSelectionViewModel::class.java)) {
                "ViewModel inconnu : ${modelClass.name}"
            }
            @Suppress("UNCHECKED_CAST")
            return ProfileSelectionViewModel(repository) as T
        }
    }

    private companion object {
        const val DATE_REFRESH_INTERVAL_MS = 60_000L
    }
}
