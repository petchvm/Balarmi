package com.balarmi.util

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

class PermissionHelper(private val context: Context) {

    val notificationsGranted: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else true

    val canUseFullScreenIntent: Boolean
        // canUseFullScreenIntent() only exists on Android 14+ (API 34). On older versions the
        // USE_FULL_SCREEN_INTENT permission is granted at install time and is always usable, so
        // report true. The previous guard (API 31) invoked a non-existent method on Android
        // 12/13, throwing NoSuchMethodError and crashing the app on launch.
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .canUseFullScreenIntent()
        } else true

    val isIgnoringBatteryOptimization: Boolean
        get() {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return pm.isIgnoringBatteryOptimizations(context.packageName)
        }

    /**
     * Returns the system intent for managing the full-screen-intent permission for this app,
     * available on Android 14 (API 34) and above. On older versions, returns the generic
     * app-info screen as a fallback.
     */
    fun fullScreenIntentSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                .setData(Uri.fromParts("package", context.packageName, null))
        } else {
            appInfoIntent()
        }
    }

    @SuppressLint("BatteryLife")
    fun batteryOptIntent(): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.fromParts("package", context.packageName, null))
    }

    fun appInfoIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", context.packageName, null))
    }
}
