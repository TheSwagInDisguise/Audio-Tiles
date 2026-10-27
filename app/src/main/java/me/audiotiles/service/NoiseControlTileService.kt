package me.audiotiles.service

import android.graphics.drawable.Icon
import android.os.Build
import android.os.PowerManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import me.audiotiles.R
import me.audiotiles.data.bluetooth.ConnectionState
import me.audiotiles.domain.HeadphoneRepository
import me.audiotiles.domain.NoiseControlMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android Quick Settings Tile for Noise Control.
 *
 * Cycles noise control modes on tap:
 * Noise Cancelling -> Ambient Sound -> Off -> Noise Cancelling
 *
 * ColorOS / Oppo Find X9 Ultra Guardrails:
 * - Employs short-lived CPU WakeLock during tap execution to survive ColorOS aggressive sleep.
 * - Non-blocking coroutine execution to avoid freezing SystemUI shade.
 * - Graceful fallback toast if headphone is off or out of range.
 */
class NoiseControlTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var listeningJob: Job? = null
    private lateinit var repository: HeadphoneRepository

    override fun onCreate() {
        super.onCreate()
        repository = HeadphoneRepository.getInstance(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        // Collect reactive state flows while QS shade is pulled down
        listeningJob?.cancel()
        listeningJob = serviceScope.launch {
            launch {
                repository.noiseControlState.collect { state ->
                    val conn = repository.connectionState.value
                    updateTileUi(conn, state.mode)
                }
            }
            launch {
                repository.connectionState.collect { conn ->
                    val mode = repository.noiseControlState.value.mode
                    updateTileUi(conn, mode)
                }
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        listeningJob?.cancel()
        listeningJob = null
    }

    override fun onClick() {
        super.onClick()

        // Acquire temporary WakeLock to ensure ColorOS CPU scheduler does not suspend
        // the background Bluetooth socket thread while SystemUI collapses the notification shade.
        val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "XM6Controller:TileTapWakeLock"
        )
        wakeLock?.acquire(10000L) // Safe 10s maximum timeout

        // Temporary feedback on tile
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.label = "Switching..."
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val devName = repository.targetDevice.value?.name ?: "Headset"
            tile.subtitle = "Connecting to $devName..."
        }
        tile.updateTile()

        serviceScope.launch {
            try {
                // If disconnected, try auto-connecting first
                val conn = repository.connectionState.value
                if (!conn.isConnected) {
                    val connectResult = withContext(Dispatchers.IO) {
                        repository.connect()
                    }
                    if (connectResult.isFailure) {
                        val devName = repository.targetDevice.value?.name ?: "Wireless Headset"
                        showErrorFeedback("$devName Not Found (Check Bluetooth & Power)")
                        updateTileUi(ConnectionState.Error("Device not found"), repository.noiseControlState.value.mode)
                        return@launch
                    }
                }

                // Cycle mode: NC -> Ambient -> Off -> NC
                val cycleResult = withContext(Dispatchers.IO) {
                    repository.cycleMode()
                }

                if (cycleResult.isSuccess) {
                    val newMode = cycleResult.getOrThrow()
                    updateTileUi(repository.connectionState.value, newMode)
                } else {
                    val err = cycleResult.exceptionOrNull()?.message ?: "Communication failed"
                    showErrorFeedback("Error: $err")
                    updateTileUi(ConnectionState.Error(err), repository.noiseControlState.value.mode)
                }
            } catch (e: Exception) {
                val devName = repository.targetDevice.value?.name ?: "Wireless Headset"
                showErrorFeedback("$devName Not Found (Check Bluetooth & Power)")
                updateTileUi(ConnectionState.Error(e.message ?: "Failed"), repository.noiseControlState.value.mode)
            } finally {
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun showErrorFeedback(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun updateTileUi(connection: ConnectionState, mode: NoiseControlMode) {
        val tile = qsTile ?: return

        when (connection) {
            is ConnectionState.Connected -> {
                when (mode) {
                    NoiseControlMode.NOISE_CANCELLING -> {
                        tile.state = Tile.STATE_ACTIVE
                        tile.label = "NC On"
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            tile.subtitle = "Noise Cancelling"
                        }
                        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_nc)
                    }
                    NoiseControlMode.AMBIENT_SOUND -> {
                        tile.state = Tile.STATE_ACTIVE
                        tile.label = "Ambient"
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            tile.subtitle = "Ambient Sound"
                        }
                        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_ambient)
                    }
                    NoiseControlMode.OFF -> {
                        tile.state = Tile.STATE_INACTIVE
                        tile.label = "ANC Off"
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            tile.subtitle = "Noise Control Off"
                        }
                        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_off)
                    }
                }
            }
            is ConnectionState.Connecting -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Connecting..."
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val devName = repository.targetDevice.value?.name ?: "Headset"
                    tile.subtitle = devName
                }
                tile.icon = Icon.createWithResource(this, R.drawable.ic_headphones)
            }
            is ConnectionState.Error -> {
                tile.state = Tile.STATE_INACTIVE
                val devName = repository.targetDevice.value?.name ?: "Wireless Headset"
                tile.label = "$devName Not Found"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Tap to retry"
                }
                tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_off)
            }
            is ConnectionState.Disconnected -> {
                tile.state = Tile.STATE_INACTIVE
                val devName = repository.targetDevice.value?.name ?: "Wireless Headset"
                tile.label = "$devName Disconnected"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Tap to connect"
                }
                tile.icon = Icon.createWithResource(this, R.drawable.ic_headphones)
            }
        }

        tile.updateTile()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
