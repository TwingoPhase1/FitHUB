package com.twingo.fithub.core.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import java.util.UUID

class BleException(message: String) : Exception(message)

/**
 * Une connexion GATT complète : ouverture, découverte, abonnements et parsing
 * des notifications. Toutes les opérations GATT passent par [BleQueueManager].
 *
 * Pour les équipements FTMS, gère aussi le Control Point (0x2AD9) pour le
 * pilotage bidirectionnel (mode ERG, résistance, inclinaison).
 */
@SuppressLint("MissingPermission")
class BleDeviceConnection(
    private val context: Context,
    private val scope: CoroutineScope,
    val device: BluetoothDevice,
    val displayName: String,
    private val listener: Listener,
) {

    interface Listener {
        fun onStateChanged(connection: BleDeviceConnection, state: BleConnectionState)
        fun onTelemetry(connection: BleDeviceConnection, metrics: BleMetrics)
        fun onControlStateChanged(connection: BleDeviceConnection, controlState: FtmsControlPointManager.ControlState)
        fun onDisconnected(connection: BleDeviceConnection, reason: String?)
    }

    private data class MachineDescriptor(
        val characteristic: BluetoothGattCharacteristic,
        val role: BleDeviceRole,
    )

    private val queue = BleQueueManager(scope)

    @Volatile
    private var gatt: BluetoothGatt? = null
    @Volatile
    private var connectPending: CompletableDeferred<Boolean>? = null
    @Volatile
    private var servicesPending: CompletableDeferred<Boolean>? = null
    @Volatile
    private var mtuPending: CompletableDeferred<Boolean>? = null
    @Volatile
    private var descriptorPending: CompletableDeferred<Boolean>? = null

    @Volatile private var connectionError: String? = null
    @Volatile private var closed = false

    @Volatile private var heartRateCharacteristic: BluetoothGattCharacteristic? = null
    @Volatile private var machineCharacteristic: BluetoothGattCharacteristic? = null
    @Volatile private var controlPointCharacteristic: BluetoothGattCharacteristic? = null

    @Volatile var state: BleConnectionState = BleConnectionState.CONNECTING
        private set

    @Volatile var roles: Set<BleDeviceRole> = emptySet()
        private set

    private val _telemetry = MutableStateFlow(BleMetrics())
    val telemetry: StateFlow<BleMetrics> = _telemetry.asStateFlow()

    private var controlManager: FtmsControlPointManager? = null
    val controlState: StateFlow<FtmsControlPointManager.ControlState>
        get() = controlManager?.state ?: MutableStateFlow(FtmsControlPointManager.ControlState.IDLE).asStateFlow()

    suspend fun connect() {
        try {
            listener.onStateChanged(this, BleConnectionState.CONNECTING)
            openGatt()
            requestLargerMtu()
            discoverServices()
            resolveCharacteristics()
            if (roles.isEmpty()) throw BleException("Aucune donnée sportive reconnue sur cet appareil.")
            subscribe()
            initControlPointIfAvailable()
            state = BleConnectionState.CONNECTED
            listener.onStateChanged(this, state)
        } catch (cancellation: CancellationException) {
            disconnect()
            throw cancellation
        } catch (error: Throwable) {
            disconnect(error.message)
            throw error
        }
    }

    fun disconnect() {
        disconnect(null)
    }

    private fun disconnect(reason: String?) {
        if (closed) return
        closed = true
        controlManager?.releaseControl()
        completeConnect(false)
        completeServices(false)
        completeMtu(false)
        completeDescriptor(false)
        closeGatt()
        queue.close()
        listener.onDisconnected(this, reason)
    }

    private suspend fun openGatt() {
        val connected = queue.execute(CONNECT_TIMEOUT_MS) {
            if (closed) throw BleException("Connexion annulée.")
            val pending = CompletableDeferred<Boolean>()
            connectPending = pending
            try {
                val opened = device.connectGatt(
                    context,
                    false,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE,
                )
                if (opened == null) throw BleException("Ouverture de la connexion GATT impossible.")
                gatt = opened
                if (closed) {
                    runCatching { opened.disconnect() }
                    runCatching { opened.close() }
                    throw BleException("Connexion annulée.")
                }
                pending.await()
            } finally {
                if (connectPending === pending) connectPending = null
            }
        }
        if (!connected) throw BleException(connectionError ?: "Connexion GATT refusée.")
    }

    private suspend fun requestLargerMtu() {
        val gatt = gatt ?: return
        runCatching {
            queue.execute(MTU_TIMEOUT_MS) {
                val pending = CompletableDeferred<Boolean>()
                mtuPending = pending
                try {
                    if (!gatt.requestMtu(DESIRED_MTU)) return@execute false
                    pending.await()
                } finally {
                    if (mtuPending === pending) mtuPending = null
                }
            }
        }
    }

    private suspend fun discoverServices() {
        val gatt = gatt ?: throw BleException("GATT fermé.")
        val discovered = queue.execute(DISCOVER_TIMEOUT_MS) {
            val pending = CompletableDeferred<Boolean>()
            servicesPending = pending
            try {
                if (!gatt.discoverServices()) throw BleException("Découverte des services refusée.")
                pending.await()
            } finally {
                if (servicesPending === pending) servicesPending = null
            }
        }
        if (!discovered) throw BleException(connectionError ?: "Découverte des services échouée.")
    }

    private fun resolveCharacteristics() {
        val services = gatt?.services ?: throw BleException("Services GATT indisponibles.")
        val heartRateService = services.firstOrNull { it.uuid == BleUuids.HEART_RATE_SERVICE }
        val ftmsService = services.firstOrNull { it.uuid == BleUuids.FTMS_SERVICE }

        val heartRate = heartRateService?.getCharacteristic(BleUuids.HEART_RATE_MEASUREMENT)
        val machine = when {
            ftmsService?.getCharacteristic(BleUuids.INDOOR_BIKE_DATA) != null ->
                MachineDescriptor(
                    ftmsService.getCharacteristic(BleUuids.INDOOR_BIKE_DATA),
                    BleDeviceRole.BIKE,
                )

            ftmsService?.getCharacteristic(BleUuids.TREADMILL_DATA) != null ->
                MachineDescriptor(
                    ftmsService.getCharacteristic(BleUuids.TREADMILL_DATA),
                    BleDeviceRole.TREADMILL,
                )

            ftmsService?.getCharacteristic(BleUuids.ROWER_DATA) != null ->
                MachineDescriptor(
                    ftmsService.getCharacteristic(BleUuids.ROWER_DATA),
                    BleDeviceRole.ROWER,
                )

            else -> null
        }

        controlPointCharacteristic = ftmsService?.getCharacteristic(BleUuids.FTMS_CONTROL_POINT)

        heartRateCharacteristic = heartRate
        machineCharacteristic = machine?.characteristic
        roles = buildSet {
            if (heartRate != null) add(BleDeviceRole.HEART_RATE)
            if (machine != null) add(machine.role)
        }
    }

    private suspend fun subscribe() {
        var subscribed = 0
        heartRateCharacteristic?.let {
            enableNotifications(it)
            subscribed += 1
        }
        machineCharacteristic?.let {
            enableNotifications(it)
            subscribed += 1
        }
        if (subscribed == 0) throw BleException("Aucune notification disponible.")
    }

    private fun initControlPointIfAvailable() {
        if (controlPointCharacteristic != null) {
            val manager = FtmsControlPointManager(scope, queue) { cmd ->
                writeControlPointCommand(cmd)
            }
            controlManager = manager
            scope.launch {
                manager.state.collect { cs ->
                    listener.onControlStateChanged(this@BleDeviceConnection, cs)
                }
            }
        }
    }

    private suspend fun writeControlPointCommand(cmd: ByteArray): Boolean {
        val gatt = gatt ?: return false
        val characteristic = controlPointCharacteristic ?: return false
        val descriptor = characteristic.getDescriptor(BleUuids.CLIENT_CHARACTERISTIC_CONFIG)
            ?: return false

        return queue.execute(DESCRIPTOR_TIMEOUT_MS) {
            val pending = CompletableDeferred<Boolean>()
            descriptorPending = pending
            try {
                // S'assurer que les indications sont activées (CCCD = 0x02 0x00)
                val enableIndication = byteArrayOf(0x02.toByte(), 0x00.toByte())
                val status = gatt.writeDescriptor(descriptor, enableIndication)
                if (status != BluetoothStatusCodes.SUCCESS) {
                    throw BleException("Activation indications Control Point refusée (code $status).")
                }
                if (!pending.await()) throw BleException("Écriture CCCD Control Point échouée.")

                // Écrire la commande
                // writeCharacteristic avec WRITE_TYPE_DEFAULT pour compatibilité API 33+
                val writeResult = gatt.writeCharacteristic(
                    characteristic,
                    cmd,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                )
                val writeOk = writeResult == BluetoothStatusCodes.SUCCESS
                if (!writeOk) {
                    throw BleException("Écriture Control Point refusée.")
                }
                true
            } finally {
                if (descriptorPending === pending) descriptorPending = null
            }
        }
    }

    private suspend fun enableNotifications(characteristic: BluetoothGattCharacteristic) {
        val gatt = gatt ?: throw BleException("GATT fermé.")
        val descriptor = characteristic.getDescriptor(BleUuids.CLIENT_CHARACTERISTIC_CONFIG)
            ?: throw BleException("Descripteur CCCD absent.")

        queue.execute(DESCRIPTOR_TIMEOUT_MS) {
            val pending = CompletableDeferred<Boolean>()
            descriptorPending = pending
            try {
                if (!gatt.setCharacteristicNotification(characteristic, true)) {
                    throw BleException("Activation des notifications refusée.")
                }
                val status = gatt.writeDescriptor(
                    descriptor,
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE,
                )
                if (status != BluetoothStatusCodes.SUCCESS) {
                    throw BleException("Écriture du CCCD refusée (code $status).")
                }
                if (!pending.await()) throw BleException("Écriture du CCCD échouée.")
            } finally {
                if (descriptorPending === pending) descriptorPending = null
            }
        }
    }

    private fun closeGatt() {
        val current = gatt ?: return
        gatt = null
        runCatching { current.disconnect() }
        runCatching { current.close() }
    }

    private fun completeConnect(value: Boolean) {
        val pending = connectPending ?: return
        connectPending = null
        pending.complete(value)
    }

    private fun completeServices(value: Boolean) {
        val pending = servicesPending ?: return
        servicesPending = null
        pending.complete(value)
    }

    private fun completeMtu(value: Boolean) {
        val pending = mtuPending ?: return
        mtuPending = null
        pending.complete(value)
    }

    private fun completeDescriptor(value: Boolean) {
        val pending = descriptorPending ?: return
        descriptorPending = null
        pending.complete(value)
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                completeConnect(true)
                return
            }

            if (newState == BluetoothProfile.STATE_DISCONNECTED ||
                status != BluetoothGatt.GATT_SUCCESS
            ) {
                connectionError = if (status == GATT_ERROR_133) {
                    "Erreur GATT 133 (opération concurrente ou périphérique occupé)."
                } else {
                    "Connexion interrompue (statut $status)."
                }
                completeConnect(false)
                completeServices(false)
                completeMtu(false)
                completeDescriptor(false)
                if (!closed) {
                    disconnect(connectionError)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                connectionError = "Découverte des services échouée (statut $status)."
            }
            completeServices(status == BluetoothGatt.GATT_SUCCESS)
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            completeMtu(status == BluetoothGatt.GATT_SUCCESS)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                connectionError = "Abonnement BLE échoué (statut $status)."
            }
            completeDescriptor(status == BluetoothGatt.GATT_SUCCESS)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            if (closed) return
            val uuid = characteristic.uuid
            if (uuid == BleUuids.HEART_RATE_MEASUREMENT) {
                handleHeartRate(value)
            } else if (uuid == machineCharacteristic?.uuid) {
                handleMachine(uuid, value)
            } else if (uuid == BleUuids.FTMS_CONTROL_POINT) {
                handleControlPointIndication(value)
            }
        }
    }

    private fun handleHeartRate(value: ByteArray) {
        scope.launch(Dispatchers.Default) {
            val bpm = HeartRateParser.parse(value)?.takeIf { it in 1..MAX_HEART_RATE }
                ?: return@launch
            val updated = _telemetry.updateAndGet { it.copy(heartRateBpm = bpm) }
            listener.onTelemetry(this@BleDeviceConnection, updated)
        }
    }

    private fun handleMachine(uuid: UUID, value: ByteArray) {
        scope.launch(Dispatchers.Default) {
            val updated = when (uuid) {
                BleUuids.INDOOR_BIKE_DATA -> FtmsBikeParser.parse(value)?.let { sample ->
                    _telemetry.updateAndGet { current ->
                        current.copy(
                            speedKmh = sample.speedKmh ?: current.speedKmh,
                            cadenceRpm = sample.cadenceRpm ?: current.cadenceRpm,
                            powerWatts = sample.powerWatts ?: current.powerWatts,
                            distanceMeters = sample.totalDistanceMeters ?: current.distanceMeters,
                        )
                    }
                }

                BleUuids.TREADMILL_DATA -> FtmsTreadmillParser.parse(value)?.let { sample ->
                    _telemetry.updateAndGet { current ->
                        current.copy(
                            speedKmh = sample.speedKmh ?: current.speedKmh,
                            distanceMeters = sample.totalDistanceMeters ?: current.distanceMeters,
                            inclinePercent = sample.inclinePercent ?: current.inclinePercent,
                            powerWatts = sample.powerWatts ?: current.powerWatts,
                            heartRateBpm = sample.heartRateBpm ?: current.heartRateBpm,
                            paceMinPerKm = sample.speedKmh
                                ?.takeIf { it > 0.0 }
                                ?.let { 60.0 / it }
                                ?: current.paceMinPerKm,
                        )
                    }
                }

                BleUuids.ROWER_DATA -> FtmsRowerParser.parse(value)?.let { sample ->
                    _telemetry.updateAndGet { current ->
                        current.copy(
                            cadenceRpm = sample.strokeRateSpm ?: current.cadenceRpm,
                            paceSecondsPer500m = sample.paceSecondsPer500m
                                ?: current.paceSecondsPer500m,
                            powerWatts = sample.powerWatts ?: current.powerWatts,
                            distanceMeters = sample.totalDistanceMeters ?: current.distanceMeters,
                            heartRateBpm = sample.heartRateBpm ?: current.heartRateBpm,
                        )
                    }
                }

                else -> null
            } ?: return@launch
            listener.onTelemetry(this@BleDeviceConnection, updated)
        }
    }

    private fun handleControlPointIndication(value: ByteArray) {
        controlManager?.handleIndication(value)
    }

    /** API publique pour le pilotage */
    suspend fun acquireControl(): Boolean = controlManager?.acquireControl() ?: false

    suspend fun setTargetPower(watts: Int): Boolean = controlManager?.setTargetPower(watts) ?: false

    suspend fun setTargetResistance(level: Int): Boolean = controlManager?.setTargetResistance(level) ?: false

    suspend fun setTargetIncline(percent: Float): Boolean = controlManager?.setTargetIncline(percent) ?: false

    suspend fun pauseMachine(): Boolean = controlManager?.pause() ?: false

    suspend fun resumeMachine(): Boolean = controlManager?.resume() ?: false

    fun releaseControl() {
        controlManager?.releaseControl()
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 20_000L
        const val DISCOVER_TIMEOUT_MS = 12_000L
        const val MTU_TIMEOUT_MS = 3_000L
        const val DESCRIPTOR_TIMEOUT_MS = 10_000L
        const val DESIRED_MTU = 185
        const val MAX_HEART_RATE = 300
        const val GATT_ERROR_133 = 133
    }
}