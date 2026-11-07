package com.twingo.fithub.core.ble

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Permissions BLE demandées à l'exécution.
 *
 * Android 12+ utilise BLUETOOTH_SCAN/CONNECT. Les versions antérieures
 * utilisent ACCESS_FINE_LOCATION pour la découverte. minSdk est 35 mais la
 * branche historique reste déclarée dans le manifeste.
 */
object BlePermissions {

    fun runtimePermissions(): Array<String> = buildList {
        addAll(requiredBlePermissions().toList())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    fun requiredBlePermissions(): Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun hasBlePermissions(context: Context): Boolean = requiredBlePermissions().all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
