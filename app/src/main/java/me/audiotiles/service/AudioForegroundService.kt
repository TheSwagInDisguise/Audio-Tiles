package me.audiotiles.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import me.audiotiles.R
import me.audiotiles.data.bluetooth.ConnectionState
import me.audiotiles.domain.HeadphoneRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Foreground Service for wireless audio connection.
 *
 * Keeps the Bluetooth RFCOMM socket connection alive in the background
 * to prevent ColorOS / Oppo Find X9 Ultra from freezing or killing the background task.
 * Enables instant Quick Settings Tile response (<50ms) and live sync of physical headset button presses.
 */
class AudioForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateObserverJob: Job? = null
    private lateinit var repository: HeadphoneRepository

    override fun onCreate() {
        super.onCreate()
        _isServiceRunning.value = true
        repository = HeadphoneRepository.getInstance(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_MODE -> {
                serviceScope.launch {
                    repository.cycleMode()
                }
            }
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val hasBtPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.BLUETOOTH_CONNECT
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (!hasBtPermission) {
                        android.util.Log.w(TAG, "BLUETOOTH_CONNECT missing in onStartCommand, satisfying startForeground then stopping")
                        try {
                            val fallbackNotif = buildNotification(title = "Audio Tiles", content = "Bluetooth permission required")
                            startForeground(NOTIFICATION_ID, fallbackNotif)
                            stopForeground(STOP_FOREGROUND_REMOVE)
                        } catch (e: Exception) {
                            android.util.Log.w(TAG, "Fallback notification error: ${e.message}")
                        }
                        stopSelf()
                        return START_NOT_STICKY
                    }
                }
                startForegroundWithNotification()
                startStateMonitoring()
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification(
            title = "Audio Tiles",
            content = "Connecting to headset..."
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startStateMonitoring() {
        stateObserverJob?.cancel()
        stateObserverJob = serviceScope.launch {
            launch {
                repository.noiseControlState.collect { state ->
                    val conn = repository.connectionState.value
                    updateNotification(conn, state.mode.label)
                }
            }
            launch {
                repository.connectionState.collect { conn ->
                    val mode = repository.noiseControlState.value.mode.label
                    updateNotification(conn, mode)
                }
            }
            // Ensure connection is established
            if (!repository.connectionState.value.isConnected) {
                repository.connect()
            }
        }
    }

    private var lastNotifiedContent: String? = null

    private fun updateNotification(connection: ConnectionState, modeLabel: String) {
        val content = when (connection) {
            is ConnectionState.Connected -> "Active Mode: $modeLabel"
            is ConnectionState.Connecting -> "Connecting to Headset..."
            is ConnectionState.Error -> "Error: ${connection.message}"
            is ConnectionState.Disconnected -> "Disconnected"
        }

        if (content == lastNotifiedContent) return
        lastNotifiedContent = content

        val notification = buildNotification(
            title = "Audio Tiles",
            content = content
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(title: String, content: String): Notification {
        val openAppIntent = packageManager.getLaunchIntentForPackage(packageName) ?: Intent()
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val toggleIntent = Intent(this, AudioForegroundService::class.java).apply {
            action = ACTION_TOGGLE_MODE
        }
        val togglePendingIntent = PendingIntent.getService(
            this,
            1,
            toggleIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val icon = Icon.createWithResource(this, R.drawable.ic_headphones)
        val cycleAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_tile_nc),
            "Cycle Mode",
            togglePendingIntent
        ).build()

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(cycleAction)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audio Tiles Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Bluetooth RFCOMM connection active for instant Quick Settings response."
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "AudioForegroundService"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        const val CHANNEL_ID = "xm6_connection_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_SERVICE = "me.audiotiles.action.START_SERVICE"
        const val ACTION_STOP_SERVICE = "me.audiotiles.action.STOP_SERVICE"
        const val ACTION_TOGGLE_MODE = "me.audiotiles.action.TOGGLE_MODE"

        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val hasBtPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!hasBtPermission) {
                    android.util.Log.w(TAG, "Cannot start AudioForegroundService: BLUETOOTH_CONNECT permission not granted")
                    return
                }
            }
            val intent = Intent(context, AudioForegroundService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to start foreground service: ${e.message}")
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AudioForegroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to stop foreground service: ${e.message}")
            }
        }
    }
}
