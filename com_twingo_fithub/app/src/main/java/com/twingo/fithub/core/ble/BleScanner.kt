package com.twingo.fithub.core.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context

/**
 * Scan BLE simple, actif uniquement pendant l'ouverture du dialogue capteurs.
 */
@SuppressLint("MissingPermission")
class BleScanner(
    context: Context,
    private val onDeviceFound: (BleDevice) -> Unit,
    private val onScanError: (String) -> Unit,
) {

    private val adapter: BluetoothAdapter? =
        context.getSystemService(BluetoothManager::class.java)?.adapter

    private var scanning = false

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val record = result.scanRecord
            val serviceUuids = record?.serviceUuids.orEmpty().map { it.uuid }
            val supportsHeartRate = BleUuids.HEART_RATE_SERVICE in serviceUuids
            val supportsFtms = BleUuids.FTMS_SERVICE in serviceUuids
            val name = record?.deviceName
                ?: runCatching { result.device.name }.getOrNull()

            if (name.isNullOrBlank() && !supportsHeartRate && !supportsFtms) return

            onDeviceFound(
                BleDevice(
                    address = result.device.address,
                    name = name,
                    rssi = result.rssi,
                    supportsHeartRate = supportsHeartRate,
                    supportsFtms = supportsFtms,
                ),
            )
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
        }

        override fun onScanFailed(errorCode: Int) {
            scanning = false
            onScanError("Scan Bluetooth impossible (code $errorCode).")
        }
    }

    fun start(): Boolean {
        if (scanning) return true
        if (adapter == null || !adapter.isEnabled) {
            onScanError("Bluetooth désactivé. Activez-le pour rechercher les capteurs.")
            return false
        }
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            onScanError("Scanner Bluetooth indisponible sur cet appareil.")
            return false
        }

        return try {
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            scanner.startScan(null, settings, callback)
            scanning = true
            true
        } catch (_: SecurityException) {
            onScanError("Autorisation Bluetooth refusée.")
            false
        } catch (_: Exception) {
            onScanError("Impossible de démarrer le scan Bluetooth.")
            false
        }
    }

    fun stop() {
        if (!scanning) return
        scanning = false
        runCatching { adapter?.bluetoothLeScanner?.stopScan(callback) }
    }
}
