package me.audiotiles.ui

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast

/**
 * ColorOS & Oppo Find X9 Ultra Background Optimization Helper.
 *
 * Provides targeted deep-linking and guardrails to prevent ColorOS aggressive process freezing
 * from suspending Bluetooth Classic RFCOMM sockets or blocking Quick Settings Tile taps.
 */
object ColorOSHelper {

    /**
     * Detects if the device runs ColorOS / RealmeUI / OxygenOS (Oppo / OnePlus / Realme ecosystem).
     */
    fun isColorOSDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return manufacturer.contains("oppo") ||
                brand.contains("oppo") ||
                manufacturer.contains("oneplus") ||
                brand.contains("oneplus") ||
                manufacturer.contains("realme") ||
                brand.contains("realme")
    }

    /**
     * Checks if standard Android battery optimization is ignored ("Don't optimize" / "Unrestricted").
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    /**
     * Prompts the user to exclude the app from Android battery optimization.
     */
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimization(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback to general battery optimization list
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Please disable battery optimization in System Settings", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Attempts to launch ColorOS-specific Auto-launch / Startup Manager screen.
     * Oppo Find X9 Ultra requires "Allow background activity" and "Allow auto-launch".
     */
    fun openColorOSAutoLaunchSettings(context: Context): Boolean {
        val colorOsIntents = listOf(
            // ColorOS 13 / 14 / 15 Startup Manager
            Intent().setComponent(
                ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            ),
            Intent().setComponent(
                ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.startupapp.StartupAppListActivity"
                )
            ),
            // Oplus Battery / Background freeze settings
            Intent().setComponent(
                ComponentName(
                    "com.oplus.battery",
                    "com.oplus.battery.BatteryActivity"
                )
            ),
            // Older ColorOS Power manager
            Intent().setComponent(
                ComponentName(
                    "com.coloros.oppoguardelf",
                    "com.coloros.powermanager.fuelgaard.PowerConsumptionOptimizationActivity"
                )
            ),
            // ColorOS Permission Manager
            Intent().setComponent(
                ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.PermissionManagerActivity"
                )
            )
        )

        for (intent in colorOsIntents) {
            try {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return true
            } catch (_: Exception) {
                // Continue to next intent
            }
        }

        // Fallback: Open system application details
        openAppDetailsSettings(context)
        return false
    }

    /**
     * Opens system app settings for the app.
     */
    fun openAppDetailsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open app settings", Toast.LENGTH_SHORT).show()
        }
    }
}
