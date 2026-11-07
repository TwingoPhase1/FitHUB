package com.twingo.fithub.core.ble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.twingo.fithub.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Maintient les connexions GATT actives quand l'écran est éteint ou que
 * l'activité passe en arrière-plan. Le service s'arrête de lui-même dès que
 * plus aucun capteur n'est connecté.
 */
class WorkoutBleService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var controller: BleController
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        controller = BleControllerProvider.get(this)
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(controller.uiState.value))

        scope.launch {
            controller.uiState.collectLatest { state ->
                notificationManager.notify(NOTIFICATION_ID, buildNotification(state))
                if (state.connections.isEmpty()) {
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISCONNECT_ALL) {
            controller.disconnectAll()
            if (controller.uiState.value.connections.isEmpty()) {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Capteurs Bluetooth",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Maintient les capteurs BLE connectés pendant la séance."
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(state: BleUiState): Notification {
        val connected = state.connections.isNotEmpty()
        val text = if (connected) {
            state.connections.joinToString(" · ") { info ->
                if (info.state == BleConnectionState.CONNECTING) {
                    "${info.name} (connexion…)"
                } else {
                    info.name
                }
            }
        } else {
            "Aucun capteur connecté"
        }

        val contentIntent = packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val disconnectIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, WorkoutBleService::class.java).setAction(ACTION_DISCONNECT_ALL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bluetooth)
            .setContentTitle(
                if (connected) "FitHub · Capteurs connectés" else "FitHub · Bluetooth",
            )
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .addAction(
                R.drawable.ic_bluetooth,
                "Déconnecter",
                disconnectIntent,
            )
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "fithub_ble"
        private const val NOTIFICATION_ID = 41
        private const val ACTION_DISCONNECT_ALL =
            "com.twingo.fithub.action.BLE_DISCONNECT_ALL"

        fun start(context: Context) {
            val intent = Intent(context, WorkoutBleService::class.java)
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, WorkoutBleService::class.java)) }
        }
    }
}
