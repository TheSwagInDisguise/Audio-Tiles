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
 * Android Home Screen 4x2 Studio Widget.
 * Segmented ANC/Ambient/Off buttons, L/R/Case battery, Speak-to-Chat, and A/B Bypass.
 */
class SonyAudioStudioWidgetProvider : AppWidgetProvider() {

    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val repository = HeadphoneRepository.getInstance(context)
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, repository)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val repository = HeadphoneRepository.getInstance(context)

        when (intent.action) {
            ACTION_SET_MODE -> {
                val modeOrdinal = intent.getIntExtra(EXTRA_MODE_ORDINAL, 0)
                val targetMode = NoiseControlMode.entries.getOrElse(modeOrdinal) { NoiseControlMode.NOISE_CANCELLING }
                widgetScope.launch {
                    repository.setNoiseControlMode(targetMode)
                    notifyUpdate(context)
                    SonyAudioCompactWidgetProvider.notifyUpdate(context)
                }
            }
            ACTION_TOGGLE_S2C -> {
                widgetScope.launch {
                    val cur = repository.soundFeaturesState.value.speakToChat.enabled
                    repository.setSpeakToChat(!cur)
                    notifyUpdate(context)
                }
            }
            ACTION_TOGGLE_BYPASS -> {
                repository.toggleEqBypass()
                notifyUpdate(context)
            }
        }
    }

    companion object {
        const val ACTION_SET_MODE = "com.sony.xm6controller.widget.ACTION_SET_MODE"
        const val ACTION_TOGGLE_S2C = "com.sony.xm6controller.widget.ACTION_TOGGLE_S2C"
        const val ACTION_TOGGLE_BYPASS = "com.sony.xm6controller.widget.ACTION_TOGGLE_BYPASS"
        const val EXTRA_MODE_ORDINAL = "extra_mode_ordinal"

        fun notifyUpdate(context: Context) {
            val intent = Intent(context, SonyAudioStudioWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                    ComponentName(context, SonyAudioStudioWidgetProvider::class.java)
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
            val views = RemoteViews(context.packageName, R.layout.widget_studio_4x2)

            val conn = repository.connectionState.value
            val nc = repository.noiseControlState.value
            val bat = repository.batteryState.value
            val caps = repository.deviceCapabilities.value
            val features = repository.soundFeaturesState.value
            val tenBand = repository.tenBandEqState.value
            val isBypassed = repository.isEqBypassed.value

            // Header: Model & Connection Status
            views.setTextViewText(R.id.widget_studio_model, caps.modelName)
            if (conn.isConnected) {
                val tele = if (caps.hasMultiBattery && bat.isTws) {
                    "L: ${bat.levelLeft}% | R: ${bat.levelRight}% | Case: ${bat.levelCase}%"
                } else {
                    "${caps.family.displayName} • Active RFCOMM"
                }
                views.setTextViewText(R.id.widget_studio_telemetry, tele)
                views.setTextViewText(R.id.widget_studio_battery_badge, bat.displayString)
            } else {
                views.setTextViewText(R.id.widget_studio_telemetry, "Awaiting Connection")
                views.setTextViewText(R.id.widget_studio_battery_badge, "-- %")
            }

            // Segmented 3-Way Mode Buttons Styling
            val activeColor = 0xFFC6A87C.toInt()
            val inactiveColor = 0xFF242C3E.toInt()

            views.setInt(R.id.widget_btn_nc, "setBackgroundColor",
                if (nc.mode == NoiseControlMode.NOISE_CANCELLING) 0xFF2E7D32.toInt() else inactiveColor)
            views.setInt(R.id.widget_btn_ambient, "setBackgroundColor",
                if (nc.mode == NoiseControlMode.AMBIENT_SOUND) 0xFF1565C0.toInt() else inactiveColor)
            views.setInt(R.id.widget_btn_off, "setBackgroundColor",
                if (nc.mode == NoiseControlMode.OFF) 0xFF616161.toInt() else inactiveColor)

            // Mode Intents
            fun createModePendingIntent(mode: NoiseControlMode, reqCode: Int): PendingIntent {
                val intent = Intent(context, SonyAudioStudioWidgetProvider::class.java).apply {
                    action = ACTION_SET_MODE
                    putExtra(EXTRA_MODE_ORDINAL, mode.ordinal)
                }
                return PendingIntent.getBroadcast(
                    context, reqCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
            views.setOnClickPendingIntent(R.id.widget_btn_nc, createModePendingIntent(NoiseControlMode.NOISE_CANCELLING, 201))
            views.setOnClickPendingIntent(R.id.widget_btn_ambient, createModePendingIntent(NoiseControlMode.AMBIENT_SOUND, 202))
            views.setOnClickPendingIntent(R.id.widget_btn_off, createModePendingIntent(NoiseControlMode.OFF, 203))

            // S2C Action
            val s2cEnabled = features.speakToChat.enabled
            views.setTextViewText(R.id.widget_btn_s2c, if (s2cEnabled) "S2C: ON" else "S2C: OFF")
            views.setInt(R.id.widget_btn_s2c, "setBackgroundColor", if (s2cEnabled) 0xFFD79E66.toInt() else inactiveColor)
            val s2cIntent = Intent(context, SonyAudioStudioWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_S2C
            }
            val s2cPi = PendingIntent.getBroadcast(
                context, 204, s2cIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_s2c, s2cPi)

            // Bypass Action
            views.setTextViewText(R.id.widget_btn_bypass, if (isBypassed) "BYPASS (Flat)" else "A/B Audition")
            views.setInt(R.id.widget_btn_bypass, "setBackgroundColor", if (isBypassed) 0xFFDC2626.toInt() else inactiveColor)
            val bypassIntent = Intent(context, SonyAudioStudioWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_BYPASS
            }
            val bypassPi = PendingIntent.getBroadcast(
                context, 205, bypassIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_bypass, bypassPi)

            // Active Preset Label
            views.setTextViewText(R.id.widget_tv_preset, if (isBypassed) "Bypass: Flat 0dB" else tenBand.profileName)

            // Intent to open Dashboard on header click
            val launchIntent = Intent(context, MainActivity::class.java)
            val launchPi = PendingIntent.getActivity(
                context, 200, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_studio_header, launchPi)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
