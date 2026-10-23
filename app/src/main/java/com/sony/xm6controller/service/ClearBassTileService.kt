package com.sony.xm6controller.service

import android.graphics.drawable.Icon
import android.os.Build
import android.os.PowerManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.sony.xm6controller.R
import com.sony.xm6controller.data.bluetooth.ConnectionState
import com.sony.xm6controller.domain.EqPreset
import com.sony.xm6controller.domain.HeadphoneRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dedicated Quick Settings Tile for Sony Clear Bass hardware stepper.
 * Cycles: 0 (Flat) -> +4 (Punchy) -> +8 (Slam) -> -2 (Subtle) -> 0
 */
class ClearBassTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var listeningJob: Job? = null
    private lateinit var repository: HeadphoneRepository

    private val BASS_STEPS = listOf(0, 4, 8, -2)

    override fun onCreate() {
        super.onCreate()
        repository = HeadphoneRepository.getInstance(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        listeningJob?.cancel()
        listeningJob = serviceScope.launch {
            launch {
                repository.equalizerState.collect { eq ->
                    val conn = repository.connectionState.value
                    updateTileUi(conn, eq.clearBass)
                }
            }
            launch {
                repository.connectionState.collect { conn ->
                    val cb = repository.equalizerState.value.clearBass
                    updateTileUi(conn, cb)
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
            "SonyAudioTiles:ClearBassTileWakeLock"
        )?.apply {
            acquire(3000)
        }

        serviceScope.launch {
            try {
                val current = repository.equalizerState.value.clearBass
                val currentIndex = BASS_STEPS.indexOf(current)
                val next = if (currentIndex != -1) {
                    BASS_STEPS[(currentIndex + 1) % BASS_STEPS.size]
                } else {
                    4
                }

                val currentBands = repository.equalizerState.value.bands
                val res = repository.setEqualizerBands(
                    preset = EqPreset.CUSTOM_1,
                    clearBass = next,
                    bands = currentBands
                )

                withContext(Dispatchers.Main) {
                    if (res.isSuccess) {
                        updateTileUi(repository.connectionState.value, next)
                        val text = if (next > 0) "+$next dB" else if (next == 0) "Flat 0 dB" else "$next dB"
                        Toast.makeText(applicationContext, "Clear Bass: $text", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(applicationContext, "Failed to adjust Clear Bass", Toast.LENGTH_SHORT).show()
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

    private fun updateTileUi(conn: ConnectionState, clearBass: Int) {
        val tile = qsTile ?: return
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_bass)
        tile.label = getString(R.string.tile_clearbass_label)

        if (conn.isConnected) {
            tile.state = if (clearBass != 0) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (clearBass > 0) "+$clearBass dB" else if (clearBass == 0) "Flat 0 dB" else "$clearBass dB"
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
