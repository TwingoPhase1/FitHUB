package com.twingo.fithub.core.ble

import kotlinx.coroutines.flow.StateFlow

/**
 * Rôle fonctionnel d'un périphérique BLE connecté.
 */
enum class BleDeviceRole {
    HEART_RATE,
    BIKE,
    TREADMILL,
    ROWER,
    ;

    val isMachine: Boolean
        get() = this != HEART_RATE
}

data class BleDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
    val supportsHeartRate: Boolean,
    val supportsFtms: Boolean,
) {
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() } ?: "Appareil BLE"
}

enum class BleConnectionState {
    CONNECTING,
    CONNECTED,
}

data class BleConnectionInfo(
    val address: String,
    val name: String,
    val roles: Set<BleDeviceRole>,
    val state: BleConnectionState,
    /** État de la prise de contrôle FTMS (si équipement FTMS connecté). */
    val controlState: FtmsControlPointManager.ControlState? = null,
)

/**
 * Télémétrie agrégée, indépendante de la séance en cours.
 *
 * Les champs nuls signifient qu'aucune valeur n'a encore été reçue pour la
 * grandeur concernée.
 */
data class BleMetrics(
    val heartRateBpm: Int? = null,
    val powerWatts: Int? = null,
    val cadenceRpm: Int? = null,
    val paceMinPerKm: Double? = null,
    val paceSecondsPer500m: Int? = null,
    val speedKmh: Double? = null,
    val inclinePercent: Double? = null,
    val distanceMeters: Int? = null,
)

data class BleUiState(
    val isScanning: Boolean = false,
    val bluetoothEnabled: Boolean = true,
    val discoveredDevices: List<BleDevice> = emptyList(),
    val connections: List<BleConnectionInfo> = emptyList(),
    val message: String? = null,
)
