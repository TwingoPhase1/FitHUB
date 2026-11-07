package com.twingo.fithub.core.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Orchestrateur BLE unique de l'application.
 *
 * - un seul capteur cardio et un seul équipement FTMS à la fois ;
 * - agrège la télémétrie de toutes les connexions dans [metrics] ;
 * - démarre [WorkoutBleService] dès qu'une connexion est demandée afin que
 *   les capteurs restent actifs lorsque l'écran se verrouille.
 */
@SuppressLint("MissingPermission")
class BleController internal constructor(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val adapter: BluetoothAdapter? =
        appContext.getSystemService(BluetoothManager::class.java)?.adapter

    private val connectionsLock = Any()
    private val connections = linkedMapOf<String, BleDeviceConnection>()

    private var scanner: BleScanner? = null

    private val _uiState = MutableStateFlow(BleUiState())
    val uiState: StateFlow<BleUiState> = _uiState.asStateFlow()

    private val _metrics = MutableStateFlow(BleMetrics())
    val metrics: StateFlow<BleMetrics> = _metrics.asStateFlow()

    init {
        refreshBluetoothState()
    }

    fun hasPermissions(): Boolean = BlePermissions.hasBlePermissions(appContext)

    fun refreshBluetoothState() {
        val enabled = adapter?.let { current ->
            runCatching { current.isEnabled }.getOrDefault(false)
        } ?: false
        _uiState.update { it.copy(bluetoothEnabled = enabled) }
    }

    fun startScan() {
        if (!hasPermissions()) {
            reportPermissionDenied()
            return
        }
        val currentAdapter = adapter
        if (currentAdapter == null) {
            _uiState.update {
                it.copy(
                    isScanning = false,
                    bluetoothEnabled = false,
                    message = "Bluetooth non disponible sur cet appareil.",
                )
            }
            return
        }
        val enabled = runCatching { currentAdapter.isEnabled }.getOrDefault(false)
        if (!enabled) {
            stopScan()
            _uiState.update {
                it.copy(
                    isScanning = false,
                    bluetoothEnabled = false,
                    message = "Bluetooth désactivé. Activez-le pour rechercher les capteurs.",
                )
            }
            return
        }

        stopScan()
        _uiState.update {
            it.copy(
                isScanning = true,
                bluetoothEnabled = true,
                discoveredDevices = emptyList(),
                message = null,
            )
        }

        val newScanner = BleScanner(
            context = appContext,
            onDeviceFound = ::onDeviceFound,
            onScanError = { message ->
                scanner = null
                _uiState.update { it.copy(isScanning = false, message = message) }
            },
        )
        scanner = newScanner
        if (!newScanner.start()) {
            scanner = null
            _uiState.update { it.copy(isScanning = false) }
        }
    }

    fun stopScan() {
        scanner?.stop()
        scanner = null
        _uiState.update { it.copy(isScanning = false) }
    }

    fun reportPermissionDenied() {
        _uiState.update {
            it.copy(
                isScanning = false,
                message = "Autorisations Bluetooth requises pour rechercher et connecter les capteurs.",
            )
        }
    }

    fun connect(device: BleDevice) {
        if (!hasPermissions()) {
            reportPermissionDenied()
            return
        }
        val currentAdapter = adapter
        if (currentAdapter == null) {
            _uiState.update { it.copy(message = "Bluetooth non disponible sur cet appareil.") }
            return
        }
        if (synchronized(connectionsLock) { connections.containsKey(device.address) }) {
            return
        }

        val bluetoothDevice = try {
            currentAdapter.getRemoteDevice(device.address)
        } catch (_: IllegalArgumentException) {
            _uiState.update { it.copy(message = "Adresse Bluetooth invalide.") }
            return
        } catch (_: SecurityException) {
            reportPermissionDenied()
            return
        }

        val connection = BleDeviceConnection(
            context = appContext,
            scope = scope,
            device = bluetoothDevice,
            displayName = device.displayName,
            listener = connectionListener,
        )

        synchronized(connectionsLock) {
            connections[device.address] = connection
        }
        _uiState.update { state ->
            state.copy(
                connections = state.connections
                    .filterNot { it.address == device.address } +
                    BleConnectionInfo(
                        address = device.address,
                        name = device.displayName,
                        roles = emptySet(),
                        state = BleConnectionState.CONNECTING,
                    ),
                message = null,
            )
        }

        WorkoutBleService.start(appContext)

        scope.launch {
            try {
                connection.connect()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                val reason = error.message ?: "connexion impossible"
                removeConnection(connection, "Connexion à ${device.displayName} échouée : $reason")
            }
        }
    }

    fun disconnect(address: String) {
        val connection = synchronized(connectionsLock) { connections[address] } ?: return
        connection.disconnect()
    }

    /** Demande la prise de contrôle FTMS sur l'équipement connecté. */
    suspend fun acquireControl(address: String): Boolean {
        val connection = synchronized(connectionsLock) { connections[address] } ?: return false
        return connection.acquireControl()
    }

    /** Envoie une cible de puissance (mode ERG) en watts. */
    suspend fun setTargetPower(address: String, watts: Int): Boolean {
        val connection = synchronized(connectionsLock) { connections[address] } ?: return false
        return connection.setTargetPower(watts)
    }

    /** Envoie une cible de résistance (niveau 0-255). */
    suspend fun setTargetResistance(address: String, level: Int): Boolean {
        val connection = synchronized(connectionsLock) { connections[address] } ?: return false
        return connection.setTargetResistance(level)
    }

    /** Envoie une cible d'inclinaison en pourcentage (ex: 5.5 pour 5,5%). */
    suspend fun setTargetIncline(address: String, percent: Float): Boolean {
        val connection = synchronized(connectionsLock) { connections[address] } ?: return false
        return connection.setTargetIncline(percent)
    }

    /** Met la machine en pause (si contrôle acquis). */
    suspend fun pauseMachine(address: String): Boolean {
        val connection = synchronized(connectionsLock) { connections[address] } ?: return false
        return connection.pauseMachine()
    }

    /** Reprend la machine (si contrôle acquis). */
    suspend fun resumeMachine(address: String): Boolean {
        val connection = synchronized(connectionsLock) { connections[address] } ?: return false
        return connection.resumeMachine()
    }

    fun disconnectAll() {
        val snapshot = synchronized(connectionsLock) { connections.values.toList() }
        snapshot.forEach { it.disconnect() }
    }

    private val connectionListener = object : BleDeviceConnection.Listener {

        override fun onStateChanged(
            connection: BleDeviceConnection,
            state: BleConnectionState,
        ) {
            _uiState.update { ui ->
                ui.copy(
                    connections = ui.connections.map { info ->
                        if (info.address == connection.device.address) {
                            info.copy(
                                name = connection.displayName,
                                roles = connection.roles,
                                state = state,
                                controlState = connection.controlState.value,
                            )
                        } else {
                            info
                        }
                    },
                )
            }

            if (state == BleConnectionState.CONNECTED) {
                enforceRoleExclusivity(connection)
                publishMetrics()
            }
        }

        override fun onControlStateChanged(
            connection: BleDeviceConnection,
            controlState: FtmsControlPointManager.ControlState,
        ) {
            _uiState.update { ui ->
                ui.copy(
                    connections = ui.connections.map { info ->
                        if (info.address == connection.device.address) {
                            info.copy(controlState = controlState)
                        } else {
                            info
                        }
                    },
                )
            }
        }

        override fun onTelemetry(connection: BleDeviceConnection, metrics: BleMetrics) {
            publishMetrics()
        }

        override fun onDisconnected(connection: BleDeviceConnection, reason: String?) {
            removeConnection(connection, reason)
        }
    }

    private fun enforceRoleExclusivity(connected: BleDeviceConnection) {
        val groups = connected.roles.map { role -> role.exclusivityGroup() }.toSet()
        if (groups.isEmpty()) return

        val conflicting = synchronized(connectionsLock) {
            connections.values.filter { other ->
                other !== connected &&
                    other.roles.any { role -> role.exclusivityGroup() in groups }
            }
        }
        conflicting.forEach { it.disconnect() }
    }

    private fun removeConnection(connection: BleDeviceConnection, reason: String?) {
        val removed = synchronized(connectionsLock) {
            if (connections[connection.device.address] === connection) {
                connections.remove(connection.device.address)
            } else {
                null
            }
        }
        if (removed == null) return

        _uiState.update { state ->
            state.copy(
                connections = state.connections.filterNot {
                    it.address == connection.device.address
                },
                message = reason ?: state.message,
            )
        }
        publishMetrics()

        val empty = synchronized(connectionsLock) { connections.isEmpty() }
        if (empty) {
            WorkoutBleService.stop(appContext)
        }
    }

    private fun publishMetrics() {
        val snapshot = synchronized(connectionsLock) {
            connections.values.map { connection ->
                connection.roles to connection.telemetry.value
            }
        }
        val heartRate = snapshot
            .firstOrNull { (roles, _) -> BleDeviceRole.HEART_RATE in roles }
            ?.second
        val machine = snapshot
            .firstOrNull { (roles, _) -> roles.any { it.isMachine } }
            ?.second

        _metrics.value = BleMetrics(
            heartRateBpm = heartRate?.heartRateBpm ?: machine?.heartRateBpm,
            powerWatts = machine?.powerWatts,
            cadenceRpm = machine?.cadenceRpm,
            paceMinPerKm = machine?.paceMinPerKm,
            paceSecondsPer500m = machine?.paceSecondsPer500m,
            speedKmh = machine?.speedKmh,
            inclinePercent = machine?.inclinePercent,
            distanceMeters = machine?.distanceMeters,
        )
    }

    private fun onDeviceFound(device: BleDevice) {
        _uiState.update { state ->
            val existing = state.discoveredDevices.firstOrNull { it.address == device.address }
            val merged = if (existing == null) {
                device
            } else {
                device.copy(
                    name = device.name ?: existing.name,
                    supportsHeartRate = existing.supportsHeartRate || device.supportsHeartRate,
                    supportsFtms = existing.supportsFtms || device.supportsFtms,
                )
            }
            val devices = state.discoveredDevices
                .filterNot { it.address == device.address } + merged
            state.copy(
                discoveredDevices = devices.sortedWith(
                    compareByDescending<BleDevice> { it.supportsHeartRate || it.supportsFtms }
                        .thenByDescending { it.rssi },
                ),
            )
        }
    }

    private fun BleDeviceRole.exclusivityGroup(): String =
        if (isMachine) "machine" else "heart-rate"
}

/**
 * Instance partagée entre l'activité et le service de premier plan. Le service
 * garantit que le processus reste vivant tant qu'un capteur est connecté.
 */
object BleControllerProvider {

    @Volatile
    private var instance: BleController? = null

    fun get(context: Context): BleController =
        instance ?: synchronized(this) {
            instance ?: BleController(context.applicationContext).also { instance = it }
        }
}
