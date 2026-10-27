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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dedicated Quick Settings Tile for Speak-to-Chat toggle.
 */
class SpeakToChatTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var listeningJob: Job? = null
    private lateinit var repository: HeadphoneRepository

    override fun onCreate() {
        super.onCreate()
        repository = HeadphoneRepository.getInstance(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        listeningJob?.cancel()
        listeningJob = serviceScope.launch {
            launch {
                repository.soundFeaturesState.collect { features ->
                    val conn = repository.connectionState.value
                    updateTileUi(conn, features.speakToChat.enabled)
                }
            }
            launch {
                repository.connectionState.collect { conn ->
                    val s2c = repository.soundFeaturesState.value.speakToChat.enabled
                    updateTileUi(conn, s2c)
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
        val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AudioTiles:SpeakToChatTileWakeLock"
        )?.apply {
            acquire(3000)
        }

        serviceScope.launch {
            try {
                val current = repository.soundFeaturesState.value.speakToChat.enabled
                val target = !current
                val res = repository.setSpeakToChat(target)
                withContext(Dispatchers.Main) {
                    if (res.isSuccess) {
                        updateTileUi(repository.connectionState.value, target)
                        Toast.makeText(
                            applicationContext,
                            "Speak-to-Chat: ${if (target) "ON" else "OFF"}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Failed to toggle Speak-to-Chat",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                wakeLock?.let { if (it.isHeld) it.release() }
            }
        }
    }

    private fun updateTileUi(conn: ConnectionState, enabled: Boolean) {
        val tile = qsTile ?: return
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_s2c)
        tile.label = getString(R.string.tile_s2c_label)

        if (conn.isConnected) {
            tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (enabled) "Enabled" else "Disabled"
            }
        } else {
            tile.state = Tile.STATE_UNAVAILABLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Disconnected"
            }
        }
        tile.updateTile()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
