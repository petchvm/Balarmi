package com.balarmi

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager

class BalarmiApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java) ?: return

        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM,
            getString(R.string.channel_alarm_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.channel_alarm_desc)
            setBypassDnd(true)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val monitorChannel = NotificationChannel(
            CHANNEL_MONITOR,
            getString(R.string.channel_monitor_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.channel_monitor_desc)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }

        nm.createNotificationChannels(listOf(alarmChannel, monitorChannel))
    }

    companion object {
        const val CHANNEL_ALARM = "balarmi_alarm"
        const val CHANNEL_MONITOR = "balarmi_monitor"
    }
}
