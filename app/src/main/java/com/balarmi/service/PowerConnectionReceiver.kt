package com.balarmi.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.balarmi.data.SettingsRepository

/**
 * Manifest receiver. ACTION_POWER_CONNECTED is exempt from background-broadcast restrictions,
 * so this fires reliably even with the app process not running — provided the OS hasn't
 * force-stopped the app (vivo OriginOS sometimes does).
 */
class PowerConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_POWER_CONNECTED) return
        val settings = SettingsRepository(context)
        if (!settings.current.monitoringEnabled) {
            Log.d(TAG, "Power connected but monitoring disabled — ignoring")
            return
        }
        Log.i(TAG, "Power connected — starting monitor service")
        ChargingMonitorService.start(context)
    }

    private companion object {
        const val TAG = "PowerConnectionReceiver"
    }
}
