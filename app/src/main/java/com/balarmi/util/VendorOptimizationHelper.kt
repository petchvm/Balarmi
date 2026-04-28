package com.balarmi.util

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log

/**
 * Best-effort deep-links into vivo OriginOS / iQOO settings screens.
 * Component names are not officially documented; they may differ across OriginOS versions.
 * Falls back to the generic app-info screen if none of the components resolve.
 */
object VendorOptimizationHelper {

    private val AUTO_START_COMPONENTS = listOf(
        // iManager (vivo's system optimizer)
        ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
        ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
        ComponentName("com.iqoo.secure", "com.iqoo.secure.MainActivity"),
        // vivo permission manager
        ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
        ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.PurviewTabActivity"),
    )

    fun openAutoStartSettings(context: Context): Boolean {
        for (component in AUTO_START_COMPONENTS) {
            val intent = Intent().apply {
                this.component = component
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                Log.i(TAG, "Opened: $component")
                return true
            } catch (_: ActivityNotFoundException) {
                Log.d(TAG, "Component not found: $component")
            } catch (e: SecurityException) {
                Log.d(TAG, "Security exception for $component: ${e.message}")
            }
        }
        return openAppInfo(context)
    }

    private fun openAppInfo(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Failed to open app info", e)
            false
        }
    }

    private const val TAG = "VendorOptimization"
}
