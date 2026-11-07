package com.twingo.fithub.ui.ble

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twingo.fithub.core.ble.BleConnectionState
import com.twingo.fithub.core.ble.BleController
import com.twingo.fithub.core.ble.BleDevice
import com.twingo.fithub.core.ble.BleDeviceRole
import com.twingo.fithub.core.ble.BlePermissions
import com.twingo.fithub.core.ble.BleUiState
import com.twingo.fithub.ui.theme.FitHubBackground
import com.twingo.fithub.ui.theme.FitHubError
import com.twingo.fithub.ui.theme.FitHubLime
import com.twingo.fithub.ui.theme.FitHubOrange
import com.twingo.fithub.ui.theme.FitHubOutline
import com.twingo.fithub.ui.theme.FitHubSurface
import com.twingo.fithub.ui.theme.FitHubSurfaceElevated
import com.twingo.fithub.ui.theme.FitHubText
import com.twingo.fithub.ui.theme.FitHubTextMuted

/**
 * Dialogue de sélection des capteurs BLE. Il gère lui-même la demande de
 * permissions, l'activation du Bluetooth, le scan et les connexions.
 */
@Composable
fun BleDeviceDialog(
    controller: BleController,
    state: BleUiState,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var permissionsGranted by remember { mutableStateOf(controller.hasPermissions()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionsGranted = controller.hasPermissions()
        controller.refreshBluetoothState()
        if (permissionsGranted) {
            controller.startScan()
        } else {
            controller.reportPermissionDenied()
        }
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        controller.refreshBluetoothState()
        if (controller.uiState.value.bluetoothEnabled) {
            controller.startScan()
        }
    }

    fun close() {
        controller.stopScan()
        onDismiss()
    }

    fun requestPermissionsAndScan() {
        permissionsGranted = controller.hasPermissions()
        if (permissionsGranted) {
            controller.refreshBluetoothState()
            controller.startScan()
        } else {
            permissionLauncher.launch(BlePermissions.runtimePermissions())
        }
    }

    fun requestBluetoothEnable() {
        if (!controller.hasPermissions()) {
            requestPermissionsAndScan()
            return
        }
        runCatching {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }.onFailure {
            controller.reportPermissionDenied()
        }
    }

    LaunchedEffect(Unit) {
        controller.refreshBluetoothState()
        if (controller.hasPermissions()) {
            permissionsGranted = true
            controller.startScan()
        } else {
            permissionLauncher.launch(BlePermissions.runtimePermissions())
        }
    }

    DisposableEffect(Unit) {
        onDispose { controller.stopScan() }
    }

    AlertDialog(
        onDismissRequest = ::close,
        containerColor = FitHubSurface,
        titleContentColor = FitHubText,
        textContentColor = FitHubTextMuted,
        title = {
            Column {
                Text(
                    text = "CAPTEURS BLUETOOTH",
                    style = MaterialTheme.typography.labelSmall,
                    color = FitHubLime,
                    letterSpacing = 2.sp,
                )
                Text(
                    text = "Cardio & équipements",
                    style = MaterialTheme.typography.headlineSmall,
                    color = FitHubText,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    !permissionsGranted -> PermissionsMissingSection(
                        onRequest = ::requestPermissionsAndScan,
                        onOpenSettings = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            )
                            runCatching { context.startActivity(intent) }
                        },
                    )

                    !state.bluetoothEnabled -> BluetoothDisabledSection(
                        onEnable = ::requestBluetoothEnable,
                    )

                    else -> ScanStatus(state = state)
                }

                state.message?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = FitHubOrange,
                    )
                }

                if (state.connections.isNotEmpty()) {
                    Text(
                        text = "APPAREILS CONNECTÉS",
                        style = MaterialTheme.typography.labelSmall,
                        color = FitHubTextMuted,
                        letterSpacing = 1.5.sp,
                    )
                    state.connections.forEach { connection ->
                        ConnectedDeviceRow(
                            name = connection.name,
                            detail = connection.roles.roleLabel(
                                fallback = if (connection.state == BleConnectionState.CONNECTING) {
                                    "CONNEXION…"
                                } else {
                                    "APPAREIL BLE"
                                },
                            ),
                            isConnecting = connection.state == BleConnectionState.CONNECTING,
                            onDisconnect = { controller.disconnect(connection.address) },
                        )
                    }
                }

                if (permissionsGranted && state.bluetoothEnabled) {
                    Text(
                        text = "CAPTEURS DÉTECTÉS",
                        style = MaterialTheme.typography.labelSmall,
                        color = FitHubTextMuted,
                        letterSpacing = 1.5.sp,
                    )
                    if (state.discoveredDevices.isEmpty()) {
                        Text(
                            text = if (state.isScanning) {
                                "Recherche en cours… Approchez la ceinture cardio ou l'équipement."
                            } else {
                                "Aucun capteur détecté. Relancez la recherche."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = FitHubTextMuted,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.discoveredDevices, key = { it.address }) { device ->
                                DiscoveredDeviceRow(
                                    device = device,
                                    connectionState = state.connections
                                        .firstOrNull { it.address == device.address }
                                        ?.state,
                                    onConnect = { controller.connect(device) },
                                    onDisconnect = { controller.disconnect(device.address) },
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Une ceinture cardio et un équipement FTMS (vélo, tapis, rameur) " +
                            "peuvent être connectés simultanément.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = FitHubTextMuted,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = ::requestPermissionsAndScan) {
                Text(
                    text = "RECHERCHER",
                    style = MaterialTheme.typography.labelLarge,
                    color = FitHubLime,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = ::close) {
                Text(
                    text = "FERMER",
                    style = MaterialTheme.typography.labelLarge,
                    color = FitHubTextMuted,
                )
            }
        },
    )
}

@Composable
private fun ScanStatus(state: BleUiState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (state.isScanning) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = FitHubLime,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Recherche Bluetooth active…",
                style = MaterialTheme.typography.bodyMedium,
                color = FitHubLime,
            )
        } else {
            Text(
                text = "Recherche arrêtée.",
                style = MaterialTheme.typography.bodyMedium,
                color = FitHubTextMuted,
            )
        }
    }
}

@Composable
private fun PermissionsMissingSection(
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Text(
        text = "FitHub a besoin des autorisations Bluetooth pour détecter et connecter " +
            "vos capteurs. Aucune donnée de localisation n'est utilisée.",
        style = MaterialTheme.typography.bodyMedium,
        color = FitHubTextMuted,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(
                containerColor = FitHubLime,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(text = "AUTORISER")
        }
        TextButton(onClick = onOpenSettings) {
            Text(text = "RÉGLAGES", color = FitHubTextMuted)
        }
    }
}

@Composable
private fun BluetoothDisabledSection(onEnable: () -> Unit) {
    Text(
        text = "Le Bluetooth est désactivé sur la tablette.",
        style = MaterialTheme.typography.bodyMedium,
        color = FitHubOrange,
    )
    Button(
        onClick = onEnable,
        colors = ButtonDefaults.buttonColors(
            containerColor = FitHubLime,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(text = "ACTIVER LE BLUETOOTH")
    }
}

@Composable
private fun DiscoveredDeviceRow(
    device: BleDevice,
    connectionState: BleConnectionState?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = FitHubSurfaceElevated,
        border = BorderStroke(1.dp, FitHubOutline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FitHubText,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${device.address} · ${device.rssi} dBm",
                    style = MaterialTheme.typography.labelSmall,
                    color = FitHubTextMuted,
                )
                Text(
                    text = device.roleHint(),
                    style = MaterialTheme.typography.labelSmall,
                    color = FitHubLime,
                )
            }
            when (connectionState) {
                BleConnectionState.CONNECTING -> TextButton(onClick = onDisconnect) {
                    Text(text = "ANNULER", color = FitHubOrange)
                }

                BleConnectionState.CONNECTED -> TextButton(onClick = onDisconnect) {
                    Text(text = "DÉCONNECTER", color = FitHubError)
                }

                null -> Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FitHubLime,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(text = "CONNECTER")
                }
            }
        }
    }
}

@Composable
private fun ConnectedDeviceRow(
    name: String,
    detail: String,
    isConnecting: Boolean,
    onDisconnect: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = FitHubBackground,
        border = BorderStroke(1.dp, FitHubOutline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FitHubText,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (isConnecting) "CONNEXION EN COURS" else detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isConnecting) FitHubOrange else FitHubLime,
                )
            }
            TextButton(onClick = onDisconnect) {
                Text(
                    text = if (isConnecting) "ANNULER" else "DÉCONNECTER",
                    color = if (isConnecting) FitHubOrange else FitHubError,
                )
            }
        }
    }
}

private fun BleDevice.roleHint(): String = buildList {
    if (supportsHeartRate) add("CARDIO")
    if (supportsFtms) add("FTMS")
}.joinToString(" · ").ifBlank { "APPAREIL BLE" }

private fun Set<BleDeviceRole>.roleLabel(fallback: String): String {
    val labels = map { role ->
        when (role) {
            BleDeviceRole.HEART_RATE -> "CARDIO"
            BleDeviceRole.BIKE -> "VÉLO"
            BleDeviceRole.TREADMILL -> "TAPIS"
            BleDeviceRole.ROWER -> "RAMEUR"
        }
    }
    return labels.joinToString(" · ").ifBlank { fallback }
}
