package com.sony.xm6controller.service

import android.graphics.drawable.Icon
import android.os.Build
import android.os.PowerManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.sony.xm6controller.R
import com.sony.xm6controller.data.bluetooth.ConnectionState
import com.sony.xm6controller.domain.HeadphoneRepository
import com.sony.xm6controller.domain.NoiseControlMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dedicated Quick Settings Tile for Sony Ambient Sound "Focus on Voice" toggle.
 */
class FocusOnVoiceTileService : TileService() {

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
                repository.noiseControlState.collect { nc ->
                    val conn = repository.connectionState.value
                    updateTileUi(conn, nc.mode, nc.focusOnVoice)
                }
            }
            launch {
                repository.connectionState.collect { conn ->
                    val nc = repository.noiseControlState.value
                    updateTileUi(conn, nc.mode, nc.focusOnVoice)
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
            "SonyAudioTiles:FocusOnVoiceWakeLock"
        )?.apply {
            acquire(3000)
        }

        serviceScope.launch {
            try {
                val nc = repository.noiseControlState.value
                val nextVoiceFocus = !nc.focusOnVoice
                // If not currently in Ambient Sound mode, switch to Ambient with Voice Focus
                val res = repository.setAmbientSound(nc.ambientLevel, nextVoiceFocus)
                withContext(Dispatchers.Main) {
                    if (res.isSuccess) {
                        updateTileUi(repository.connectionState.value, NoiseControlMode.AMBIENT_SOUND, nextVoiceFocus)
                        Toast.makeText(
                            applicationContext,
                            "Voice Focus: ${if (nextVoiceFocus) "ON" else "OFF"}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(applicationContext, "Failed to toggle Voice Focus", Toast.LENGTH_SHORT).show()
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

    private fun updateTileUi(conn: ConnectionState, mode: NoiseControlMode, focusOnVoice: Boolean) {
        val tile = qsTile ?: return
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_voice)
        tile.label = getString(R.string.tile_voice_label)

        if (conn.isConnected) {
            val isActive = (mode == NoiseControlMode.AMBIENT_SOUND && focusOnVoice)
            tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (isActive) "Active" else if (mode != NoiseControlMode.AMBIENT_SOUND) "Requires Ambient" else "Off"
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
