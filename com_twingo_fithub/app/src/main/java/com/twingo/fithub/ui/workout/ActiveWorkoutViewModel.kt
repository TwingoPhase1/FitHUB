package com.twingo.fithub.ui.workout

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.twingo.fithub.core.ble.BleController
import com.twingo.fithub.core.ble.BleDeviceRole
import com.twingo.fithub.core.ble.FtmsControlPointManager
import com.twingo.fithub.data.repository.FitnessRepository
import com.twingo.fithub.domain.model.WorkoutMachine
import com.twingo.fithub.domain.session.WorkoutSessionEngine
import com.twingo.fithub.domain.session.WorkoutSessionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ActiveWorkoutViewModel(
    private val repository: FitnessRepository,
    private val elapsedRealtime: () -> Long = { SystemClock.elapsedRealtime() },
    private val bleController: BleController? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ActiveWorkoutUiState>(ActiveWorkoutUiState.Loading)
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private var engine: WorkoutSessionEngine? = null
    private var loadJob: Job? = null
    private var tickerJob: Job? = null
    private var lastTickAtMillis: Long? = null
    private var loadedProfileId: Long? = null
    private var loadedWorkoutId: Long? = null
    private var metrics = LiveWorkoutMetrics()
    private var currentMachineAddress: String? = null
    private var lastSentTargetPower: Int? = null
    private var lastSentTargetIncline: Float? = null
    private var lastSentTargetResistance: Int? = null
    private var controlAcquisitionJob: Job? = null

    init {
        bleController?.let { controller ->
            viewModelScope.launch {
                combine(controller.metrics, controller.uiState) { ble, connectionState ->
                    val machineConnection = connectionState.connections
                        .asSequence()
                        .firstOrNull { it.roles.any { it.isMachine } }
                    val machineRole = machineConnection?.roles
                        ?.asSequence()
                        ?.firstOrNull { it.isMachine }
                    LiveWorkoutMetrics(
                        heartRateBpm = ble.heartRateBpm,
                        powerWatts = ble.powerWatts,
                        cadenceRpm = ble.cadenceRpm,
                        paceMinPerKm = ble.paceMinPerKm,
                        paceSecondsPer500m = ble.paceSecondsPer500m,
                        speedKmh = ble.speedKmh,
                        inclinePercent = ble.inclinePercent,
                        distanceMeters = ble.distanceMeters,
                        machine = machineRole.toWorkoutMachine(),
                        controlState = machineConnection?.controlState,
                        machineAddress = machineConnection?.address,
                    )
                }.collect { values ->
                    updateMetrics(values)
                }
            }
        }
    }

    /**
     * Charge la séance une seule fois pour un couple (profil, séance). Le garde
     * évite un redémarrage du chronomètre lors d'une recomposition ou d'une
     * rotation d'écran.
     */
    fun start(profileId: Long, workoutId: Long) {
        val alreadyLoaded = loadedProfileId == profileId && loadedWorkoutId == workoutId
        if (alreadyLoaded && _uiState.value !is ActiveWorkoutUiState.Unavailable) return

        loadedProfileId = profileId
        loadedWorkoutId = workoutId
        loadJob?.cancel()
        tickerJob?.cancel()
        lastTickAtMillis = null
        engine = null
        currentMachineAddress = null
        lastSentTargetPower = null
        lastSentTargetIncline = null
        lastSentTargetResistance = null
        controlAcquisitionJob?.cancel()
        _uiState.value = ActiveWorkoutUiState.Loading

        loadJob = viewModelScope.launch {
            try {
                val profile = repository.getProfile(profileId)
                val plan = repository.getWorkout(workoutId)
                when {
                    profile == null || plan == null -> showUnavailable()
                    plan.profileId != profileId -> showUnavailable()
                    plan.steps.isEmpty() -> showUnavailable()
                    else -> {
                        val session = WorkoutSessionEngine(plan = plan, profile = profile)
                        session.start()
                        engine = session
                        publish()
                        startTicker()
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                showUnavailable()
            }
        }
    }

    fun togglePause() {
        val session = engine ?: return
        when (session.status) {
            WorkoutSessionStatus.RUNNING -> {
                tickerJob?.cancel()
                advanceToNow()
                session.pause()
                publish()
                viewModelScope.launch { pauseMachineIfNeeded() }
            }

            WorkoutSessionStatus.PAUSED -> {
                session.resume()
                publish()
                startTicker()
                viewModelScope.launch { resumeMachineIfNeeded() }
            }

            else -> Unit
        }
    }

    fun skipSegment() {
        val session = engine ?: return
        if (session.status != WorkoutSessionStatus.RUNNING &&
            session.status != WorkoutSessionStatus.PAUSED
        ) {
            return
        }

        tickerJob?.cancel()
        advanceToNow()
        session.skipCurrentSegment()
        publish()
        if (session.status == WorkoutSessionStatus.RUNNING) {
            startTicker()
            sendTargetsForCurrentSegment()
        }
    }

    fun adjustTargetPower(deltaWatts: Int) {
        val currentTarget = metrics.targetPowerWatts ?: lastSentTargetPower ?: return
        val newTarget = (currentTarget + deltaWatts).coerceAtLeast(0)
        viewModelScope.launch { sendTargetPower(newTarget, isManual = true) }
    }

    /**
     * Ajustement manuel de l'inclinaison (+/- 0.5%).
     */
    fun adjustTargetIncline(deltaPercent: Float) {
        val currentTarget = metrics.inclinePercent?.toFloat() ?: lastSentTargetIncline ?: return
        val newTarget = (currentTarget + deltaPercent).coerceAtLeast(0f)
        viewModelScope.launch { sendTargetIncline(newTarget, isManual = true) }
    }

    /**
     * Ajustement manuel de la résistance (+/- 1 niveau).
     */
    fun adjustTargetResistance(deltaLevel: Int) {
        val currentTarget = lastSentTargetResistance ?: return
        val newTarget = (currentTarget + deltaLevel).coerceIn(0, 255)
        viewModelScope.launch { sendTargetResistance(newTarget, isManual = true) }
    }

    /**
     * Met à disposition les valeurs d'un futur capteur BLE. Les tuiles se
     * mettront à jour sans modifier le moteur de séance.
     */
    fun updateMetrics(values: LiveWorkoutMetrics) {
        val previousMachineAddress = currentMachineAddress
        val newMachineAddress = values.machineAddress

        metrics = values
        currentMachineAddress = newMachineAddress

        // Si la machine vient de changer, réinitialiser l'état d'envoi
        if (previousMachineAddress != newMachineAddress) {
            lastSentTargetPower = null
            lastSentTargetIncline = null
            lastSentTargetResistance = null
            controlAcquisitionJob?.cancel()
            controlAcquisitionJob = null
            // Tenter d'acquérir le contrôle sur la nouvelle machine
            if (newMachineAddress != null && bleController != null) {
                controlAcquisitionJob = viewModelScope.launch {
                    acquireControlAndSendTargets(newMachineAddress)
                }
            }
        }

        if (_uiState.value is ActiveWorkoutUiState.Active) {
            publish()
        }
    }

    private suspend fun acquireControlAndSendTargets(address: String) {
        val controller = bleController ?: return
        val acquired = controller.acquireControl(address)
        if (acquired) {
            // Attendre un peu que l'état CONTROL_ACQUIRED soit propagé
            delay(500)
            sendTargetsForCurrentSegment()
        }
    }

    private fun sendTargetsForCurrentSegment() {
        val session = engine ?: return
        val segment = session.currentSegment
        val machine = metrics.machine
        val address = currentMachineAddress ?: return
        val controller = bleController ?: return

        viewModelScope.launch {
            when (machine) {
                WorkoutMachine.BIKE -> {
                    segment.targetPowerWatts?.let { watts ->
                        sendTargetPower(watts)
                    }
                    segment.targetResistanceLevel?.let { level ->
                        sendTargetResistance(level)
                    }
                }
                WorkoutMachine.TREADMILL -> {
                    segment.targetInclinePercent?.let { incline ->
                        sendTargetIncline(incline)
                    }
                    segment.targetSpeedKmh?.let { speed ->
                        // Pour l'instant on envoie l'inclinaison, la vitesse pourrait être ajoutée
                    }
                }
                WorkoutMachine.ROWER -> {
                    // Le rameur peut utiliser la résistance
                    segment.targetResistanceLevel?.let { level ->
                        sendTargetResistance(level)
                    }
                }
                WorkoutMachine.OUTDOOR -> {
                    // Pas de contrôle FTMS pour l'extérieur
                }
                null -> {
                    // Pas de machine connectée
                }
            }
        }
    }

    private suspend fun sendTargetPower(watts: Int, isManual: Boolean = false) {
        val address = currentMachineAddress ?: return
        val controller = bleController ?: return
        if (isManual || lastSentTargetPower != watts) {
            controller.setTargetPower(address, watts)
            lastSentTargetPower = watts
            if (isManual) {
                // Mettre à jour l'affichage immédiatement pour le retour utilisateur
                metrics = metrics.copy(targetPowerWatts = watts)
                publish()
            }
        }
    }

    private suspend fun sendTargetResistance(level: Int, isManual: Boolean = false) {
        val address = currentMachineAddress ?: return
        val controller = bleController ?: return
        if (isManual || lastSentTargetResistance != level) {
            controller.setTargetResistance(address, level)
            lastSentTargetResistance = level
        }
    }

    private suspend fun sendTargetIncline(percent: Float, isManual: Boolean = false) {
        val address = currentMachineAddress ?: return
        val controller = bleController ?: return
        if (isManual || lastSentTargetIncline != percent) {
            controller.setTargetIncline(address, percent)
            lastSentTargetIncline = percent
            if (isManual) {
                metrics = metrics.copy(inclinePercent = percent.toDouble())
                publish()
            }
        }
    }

    private suspend fun pauseMachineIfNeeded() {
        val address = currentMachineAddress ?: return
        val controller = bleController ?: return
        val state = metrics.controlState
        if (state == FtmsControlPointManager.ControlState.CONTROL_ACQUIRED) {
            controller.pauseMachine(address)
        }
    }

    private suspend fun resumeMachineIfNeeded() {
        val address = currentMachineAddress ?: return
        val controller = bleController ?: return
        val state = metrics.controlState
        if (state == FtmsControlPointManager.ControlState.CONTROL_ACQUIRED) {
            controller.resumeMachine(address)
        }
    }

    /**
     * Quitte la séance (fin, arrêt confirmé ou erreur). Le prochain démarrage
     * recréera un moteur neuf.
     */
    fun leaveSession() {
        loadJob?.cancel()
        tickerJob?.cancel()
        controlAcquisitionJob?.cancel()
        lastTickAtMillis = null
        engine?.stop()
        engine = null
        loadedProfileId = null
        loadedWorkoutId = null
        currentMachineAddress = null
        lastSentTargetPower = null
        lastSentTargetIncline = null
        lastSentTargetResistance = null
        _uiState.value = ActiveWorkoutUiState.Loading
    }

    private fun startTicker() {
        tickerJob?.cancel()
        lastTickAtMillis = elapsedRealtime()

        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(TICK_INTERVAL_MS)
                advanceToNow()

                val session = engine ?: return@launch
                publish()
                if (session.status == WorkoutSessionStatus.COMPLETED) break
            }
        }
    }

    /**
     * Absorbe le temps écoulé depuis le dernier tick avant une action manuelle
     * (pause, skip) afin de ne pas perdre la fraction de seconde en cours.
     */
    private fun advanceToNow() {
        val session = engine ?: return
        val now = elapsedRealtime()
        val previous = lastTickAtMillis ?: now
        lastTickAtMillis = now
        if (session.status == WorkoutSessionStatus.RUNNING) {
            session.advanceBy((now - previous).coerceAtLeast(0L))
        }
    }

    private fun publish() {
        val session = engine ?: return
        val status = session.status
        val summary = if (status == WorkoutSessionStatus.COMPLETED) {
            WorkoutSummary(
                elapsedSeconds = session.elapsedSeconds,
                plannedSeconds = session.plannedDurationSeconds,
                completedSegments = session.completedSegmentCount,
                totalSegments = session.totalSegmentCount,
            )
        } else {
            null
        }

        _uiState.value = ActiveWorkoutUiState.Active(
            profile = session.profile,
            plan = session.plan,
            status = status,
            currentSegment = session.currentSegment,
            nextSegment = if (status == WorkoutSessionStatus.COMPLETED) {
                null
            } else {
                session.nextSegment
            },
            elapsedSeconds = session.elapsedSeconds,
            remainingSeconds = session.segmentRemainingSeconds,
            isEndingSoon = session.isEndingSoon,
            segmentNumber = session.currentSegmentIndex + 1,
            segmentCount = session.totalSegmentCount,
            completedSegmentCount = session.completedSegmentCount,
            segmentProgress = session.segmentProgress,
            metrics = metrics,
            summary = summary,
        )
    }

    private fun showUnavailable() {
        engine = null
        tickerJob?.cancel()
        lastTickAtMillis = null
        _uiState.value = ActiveWorkoutUiState.Unavailable(
            message = "Impossible de charger cette séance. Revenez à l'accueil pour réessayer.",
        )
    }

    override fun onCleared() {
        tickerJob?.cancel()
        loadJob?.cancel()
        controlAcquisitionJob?.cancel()
        engine?.stop()
        super.onCleared()
    }

    class Factory(
        private val repository: FitnessRepository,
        private val bleController: BleController? = null,
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ActiveWorkoutViewModel::class.java)) {
                "ViewModel inconnu : ${modelClass.name}"
            }
            @Suppress("UNCHECKED_CAST")
            return ActiveWorkoutViewModel(
                repository = repository,
                bleController = bleController,
            ) as T
        }
    }

    private companion object {
        const val TICK_INTERVAL_MS = 1_000L
    }
}

private fun BleDeviceRole?.toWorkoutMachine(): WorkoutMachine? = when (this) {
    BleDeviceRole.BIKE -> WorkoutMachine.BIKE
    BleDeviceRole.TREADMILL -> WorkoutMachine.TREADMILL
    BleDeviceRole.ROWER -> WorkoutMachine.ROWER
    BleDeviceRole.HEART_RATE,
    null,
    -> null
}