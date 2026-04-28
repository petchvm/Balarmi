package com.balarmi.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.balarmi.data.ServiceMode
import com.balarmi.data.SettingsRepository

/**
 * Restarts the monitoring service after device reboot, but only in ALWAYS_ON mode.
 * In CHARGING_ONLY mode, the service is started by PowerConnectionReceiver on next plug-in.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val s = SettingsRepository(context).current
        if (s.monitoringEnabled && s.serviceMode == ServiceMode.ALWAYS_ON) {
            Log.i(TAG, "Boot completed — restarting always-on service")
            ChargingMonitorService.start(context)
        }
    }

    private companion object {
        const val TAG = "BootReceiver"
    }
}
