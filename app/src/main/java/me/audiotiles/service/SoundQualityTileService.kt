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
 * Dedicated Quick Settings Tile for Sound Quality Priority (LDAC vs Stable Connection).
 */
class SoundQualityTileService : TileService() {

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
                    updateTileUi(conn, features.soundQualityPriority)
                }
            }
            launch {
                repository.connectionState.collect { conn ->
                    val sq = repository.soundFeaturesState.value.soundQualityPriority
                    updateTileUi(conn, sq)
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
            "AudioTiles:SoundQualityWakeLock"
        )?.apply {
            acquire(3000)
        }

        serviceScope.launch {
            try {
                val current = repository.soundFeaturesState.value.soundQualityPriority
                val target = !current
                val res = repository.setSoundQualityPriority(target)
                withContext(Dispatchers.Main) {
                    if (res.isSuccess) {
                        updateTileUi(repository.connectionState.value, target)
                        val label = if (target) "Priority on Sound Quality (LDAC)" else "Priority on Stable Connection"
                        Toast.makeText(applicationContext, label, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(applicationContext, "Failed to switch Sound Quality mode", Toast.LENGTH_SHORT).show()
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

    private fun updateTileUi(conn: ConnectionState, priorityQuality: Boolean) {
        val tile = qsTile ?: return
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_ldac)
        tile.label = getString(R.string.tile_soundquality_label)

        if (conn.isConnected) {
            tile.state = if (priorityQuality) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (priorityQuality) "Sound Quality (LDAC)" else "Stable Connection"
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
