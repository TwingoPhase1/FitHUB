package com.twingo.fithub.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.twingo.fithub.core.ble.BleConnectionState
import com.twingo.fithub.core.ble.BleController
import com.twingo.fithub.core.sync.FleetSyncClient
import com.twingo.fithub.core.sync.SyncState
import com.twingo.fithub.data.repository.FitnessRepository
import com.twingo.fithub.ui.ble.BleDeviceDialog
import com.twingo.fithub.ui.editor.WorkoutEditorScreen
import com.twingo.fithub.ui.profile.ProfileSelectionScreen
import com.twingo.fithub.ui.profile.ProfileSelectionViewModel
import com.twingo.fithub.ui.theme.FitHubBackground
import com.twingo.fithub.ui.workout.ActiveWorkoutScreen
import com.twingo.fithub.ui.workout.ActiveWorkoutViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Racine de navigation volontairement minimale : trois destinations,
 * sans bibliothèque de navigation supplémentaire.
 */
@Composable
fun FitHubApp(
    repository: FitnessRepository,
    bleController: BleController,
) {
    var activeProfileId by rememberSaveable { mutableStateOf<Long?>(null) }
    var activeWorkoutId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showSensors by rememberSaveable { mutableStateOf(false) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingPlanId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingProfileId by rememberSaveable { mutableStateOf<Long?>(null) }

    val context = LocalContext.current
    val fleetSyncClient = remember { FleetSyncClient(context, repository) }

    val profileViewModel: ProfileSelectionViewModel = viewModel(
        factory = ProfileSelectionViewModel.Factory(repository),
    )
    val activeWorkoutViewModel: ActiveWorkoutViewModel = viewModel(
        factory = ActiveWorkoutViewModel.Factory(repository, bleController),
    )

    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val activeWorkoutState by activeWorkoutViewModel.uiState.collectAsStateWithLifecycle()
    val bleState by bleController.uiState.collectAsStateWithLifecycle()

    var syncState by remember { mutableStateOf<SyncState>(SyncState.Idle) }

    val profileId = activeProfileId
    val workoutId = activeWorkoutId

    // Initialize fleet sync client
    LaunchedEffect(Unit) {
        fleetSyncClient.registerTablet()
    }

    fun leaveWorkout() {
        showSensors = false
        activeWorkoutViewModel.leaveSession()
        activeProfileId = null
        activeWorkoutId = null
    }

    fun openEditor(planId: Long? = null, profileIdForNew: Long? = null) {
        editingPlanId = planId
        editingProfileId = profileIdForNew
        showEditor = true
    }

    fun onSyncClick() {
        syncState = SyncState.Syncing
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val result = fleetSyncClient.pullSync()
            if (result.profilesCount > 0 || result.plansCount > 0) {
                syncState = SyncState.Connected
            } else {
                syncState = SyncState.Error("No data received")
            }
        }
    }

    if (showEditor) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = FitHubBackground,
        ) { innerPadding ->
            WorkoutEditorScreen(
                onSave = { plan ->
                    if (editingPlanId != null) {
                        // Update existing plan
                    } else {
                        // Create new plan
                    }
                    showEditor = false
                    editingPlanId = null
                    editingProfileId = null
                },
                onCancel = {
                    showEditor = false
                    editingPlanId = null
                    editingProfileId = null
                },
                existingPlan = null,
                selectedProfileId = editingProfileId ?: profileState.profiles.firstOrNull()?.id ?: 0,
                modifier = Modifier.padding(innerPadding),
            )
        }
    } else if (profileId != null && workoutId != null) {
        LaunchedEffect(profileId, workoutId) {
            activeWorkoutViewModel.start(profileId, workoutId)
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = FitHubBackground,
        ) { innerPadding ->
            ActiveWorkoutScreen(
                state = activeWorkoutState,
                onTogglePause = activeWorkoutViewModel::togglePause,
                onSkipSegment = activeWorkoutViewModel::skipSegment,
                onExit = ::leaveWorkout,
                onAdjustPower = activeWorkoutViewModel::adjustTargetPower,
                onAdjustIncline = activeWorkoutViewModel::adjustTargetIncline,
                onAdjustResistance = activeWorkoutViewModel::adjustTargetResistance,
                connectedSensorCount = bleState.connections.count {
                    it.state == BleConnectionState.CONNECTED
                },
                onOpenSensors = { showSensors = true },
                modifier = Modifier.padding(innerPadding),
            )
        }

        if (showSensors) {
            BleDeviceDialog(
                controller = bleController,
                state = bleState,
                onDismiss = { showSensors = false },
            )
        }
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
        ) { innerPadding ->
            ProfileSelectionScreen(
                state = profileState,
                onProfileSelected = profileViewModel::selectProfile,
                onStartWorkout = { plan ->
                    activeProfileId = plan.profileId
                    activeWorkoutId = plan.id
                },
                onOpenEditor = { openEditor() },
                onSyncClick = { onSyncClick() },
                syncState = syncState,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}