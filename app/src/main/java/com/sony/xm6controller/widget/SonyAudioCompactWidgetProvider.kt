package com.sony.xm6controller.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.sony.xm6controller.R
import com.sony.xm6controller.domain.HeadphoneRepository
import com.sony.xm6controller.domain.NoiseControlMode
import com.sony.xm6controller.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Android Home Screen & Lock Screen Compact 2x1 Widget.
 * Shows current Noise Control Mode and Battery, with one-tap mode cycling.
 */
class SonyAudioCompactWidgetProvider : AppWidgetProvider() {

    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val repository = HeadphoneRepository.getInstance(context)
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, repository)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_CYCLE_MODE) {
            val repository = HeadphoneRepository.getInstance(context)
            widgetScope.launch {
                repository.cycleNoiseControlMode()
                notifyUpdate(context)
                SonyAudioStudioWidgetProvider.notifyUpdate(context)
            }
        }
    }

    companion object {
        const val ACTION_CYCLE_MODE = "com.sony.xm6controller.widget.ACTION_CYCLE_MODE"

        fun notifyUpdate(context: Context) {
            val intent = Intent(context, SonyAudioCompactWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                    ComponentName(context, SonyAudioCompactWidgetProvider::class.java)
                )
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            repository: HeadphoneRepository
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_compact_2x1)

            val conn = repository.connectionState.value
            val nc = repository.noiseControlState.value
            val bat = repository.batteryState.value
            val caps = repository.deviceCapabilities.value

            // Model Name & Battery
            views.setTextViewText(R.id.widget_compact_model, caps.modelName)
            if (conn.isConnected) {
                views.setTextViewText(R.id.widget_compact_battery, "Battery: ${bat.displayString}")
            } else {
                views.setTextViewText(R.id.widget_compact_battery, "Disconnected")
            }

            // Mode Button UI & Tint
            val (modeLabel, iconRes, colorHex) = when (nc.mode) {
                NoiseControlMode.NOISE_CANCELLING -> Triple("ANC", R.drawable.ic_tile_nc, 0xFF81C784.toInt())
                NoiseControlMode.AMBIENT_SOUND -> Triple("Ambient", R.drawable.ic_tile_ambient, 0xFF64B5F6.toInt())
                NoiseControlMode.OFF -> Triple("Off", R.drawable.ic_tile_off, 0xFF9E9E9E.toInt())
            }
            views.setTextViewText(R.id.widget_compact_mode_text, modeLabel)
            views.setImageViewResource(R.id.widget_compact_mode_icon, iconRes)
            views.setInt(R.id.widget_compact_mode_icon, "setColorFilter", colorHex)

            // Intent to open Dashboard
            val launchIntent = Intent(context, MainActivity::class.java)
            val launchPi = PendingIntent.getActivity(
                context, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_compact_info_area, launchPi)

            // Intent to cycle Noise Control Mode
            val cycleIntent = Intent(context, SonyAudioCompactWidgetProvider::class.java).apply {
                action = ACTION_CYCLE_MODE
            }
            val cyclePi = PendingIntent.getBroadcast(
                context, 101, cycleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_compact_action_btn, cyclePi)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
