package com.balarmi.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.balarmi.AlarmActivity
import com.balarmi.BalarmiApplication
import com.balarmi.MainActivity
import com.balarmi.R
import com.balarmi.alarm.AlarmController
import com.balarmi.alarm.AlarmEvent
import com.balarmi.alarm.AlarmEvents
import com.balarmi.data.ServiceMode
import com.balarmi.data.SettingsRepository

class ChargingMonitorService : Service() {

    private lateinit var settings: SettingsRepository
    private lateinit var alarmController: AlarmController
    private val handler = Handler(Looper.getMainLooper())

    private var batteryReceiverRegistered = false
    private var lastBatteryPct: Int = -1
    private var lastIsCharging: Boolean = false

    // Hysteresis: per-charging-session, in-memory only
    private var armed = true
    private var lastFiredPct = -1

    // True if this run was started solely for a one-shot test, with no real monitoring context.
    private var startedForTestOnly = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_BATTERY_CHANGED -> handleBatteryStatus(intent)
                Intent.ACTION_POWER_DISCONNECTED -> handleUnplug()
            }
        }
    }

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            SettingsRepository.KEY_THRESHOLD -> {
                armed = true
                lastFiredPct = -1
                if (lastBatteryPct >= 0 && lastIsCharging) evaluateThreshold(lastBatteryPct)
            }
            SettingsRepository.KEY_ENABLED -> {
                if (!settings.current.monitoringEnabled) stopAllAndExit()
            }
        }
    }

    private val timeoutRunnable = Runnable {
        Log.i(TAG, "Alarm timeout (3 min) — auto-stopping")
        stopAlarmInternal()
        if (startedForTestOnly) stopAllAndExit()
    }

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        alarmController = AlarmController(this)
        settings.registerListener(prefsListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat(buildMonitorNotification(charging = false, pct = -1))

        when (intent?.action) {
            ACTION_START -> startMonitoring()
            ACTION_TEST_ALARM -> {
                if (!batteryReceiverRegistered) startedForTestOnly = true
                fireAlarm(isTest = true, pct = lastBatteryPct.takeIf { it >= 0 } ?: 100)
            }
            ACTION_STOP_ALARM -> {
                stopAlarmInternal()
                if (startedForTestOnly) stopAllAndExit()
            }
            ACTION_STOP_MONITORING -> stopAllAndExit()
            else -> {
                // System restart of a sticky service
                if (settings.current.monitoringEnabled) startMonitoring() else stopAllAndExit()
            }
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        startedForTestOnly = false
        ensureBatteryReceiver()
        currentBatteryStatus()?.let { handleBatteryStatus(it) }
        if (settings.current.serviceMode == ServiceMode.CHARGING_ONLY && !lastIsCharging) {
            Log.i(TAG, "Started in CHARGING_ONLY mode but not charging — exiting")
            stopAllAndExit()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timeoutRunnable)
        unregisterBatteryReceiver()
        settings.unregisterListener(prefsListener)
        alarmController.stop()
    }

    // ---------- Foreground / notifications ----------

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIF_MONITOR_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIF_MONITOR_ID, notification)
        }
    }

    private fun buildMonitorNotification(charging: Boolean, pct: Int): Notification {
        val text = if (charging && pct >= 0) {
            getString(
                R.string.monitor_notification_text_charging,
                settings.current.threshold,
                pct,
            )
        } else {
            getString(R.string.monitor_notification_text_idle)
        }
        val tapPi = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, BalarmiApplication.CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_battery_alert)
            .setContentTitle(getString(R.string.monitor_notification_title))
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(tapPi)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateMonitorNotification(charging: Boolean, pct: Int) {
        val nm = NotificationManagerCompat.from(this)
        if (nm.areNotificationsEnabled()) {
            nm.notify(NOTIF_MONITOR_ID, buildMonitorNotification(charging, pct))
        }
    }

    private fun postAlarmNotification(pct: Int, threshold: Int, isTest: Boolean) {
        val fsIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(AlarmActivity.EXTRA_PCT, pct)
            putExtra(AlarmActivity.EXTRA_THRESHOLD, threshold)
            putExtra(AlarmActivity.EXTRA_IS_TEST, isTest)
        }
        val fsPi = PendingIntent.getActivity(
            this,
            1,
            fsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(this, BalarmiApplication.CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_battery_alert)
            .setContentTitle(getString(R.string.alarm_notification_title, pct))
            .setContentText(getString(R.string.alarm_notification_text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fsPi)
            .setFullScreenIntent(fsPi, true)
            .build()
        val nm = NotificationManagerCompat.from(this)
        if (nm.areNotificationsEnabled()) nm.notify(NOTIF_ALARM_ID, notif)
    }

    private fun cancelAlarmNotification() {
        NotificationManagerCompat.from(this).cancel(NOTIF_ALARM_ID)
    }

    // ---------- Battery monitoring ----------

    private fun ensureBatteryReceiver() {
        if (batteryReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(batteryReceiver, filter)
        }
        batteryReceiverRegistered = true
    }

    private fun unregisterBatteryReceiver() {
        if (!batteryReceiverRegistered) return
        runCatching { unregisterReceiver(batteryReceiver) }
        batteryReceiverRegistered = false
    }

    private fun currentBatteryStatus(): Intent? =
        registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    private fun handleBatteryStatus(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return
        val pct = (level * 100) / scale
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val isCharging = plugged != 0

        lastBatteryPct = pct
        lastIsCharging = isCharging

        updateMonitorNotification(charging = isCharging, pct = pct)

        if (isCharging) evaluateThreshold(pct)
    }

    private fun evaluateThreshold(pct: Int) {
        val threshold = settings.current.threshold
        if (armed && pct >= threshold) {
            armed = false
            lastFiredPct = pct
            fireAlarm(isTest = false, pct = pct)
        } else if (!armed && lastFiredPct >= 0 && pct <= lastFiredPct - REARM_DROP) {
            armed = true
            lastFiredPct = -1
        }
    }

    private fun handleUnplug() {
        Log.i(TAG, "Power disconnected")
        if (alarmController.isActive) stopAlarmInternal()
        armed = true
        lastFiredPct = -1
        if (settings.current.serviceMode == ServiceMode.CHARGING_ONLY) stopAllAndExit()
    }

    // ---------- Alarm ----------

    private fun fireAlarm(isTest: Boolean, pct: Int) {
        if (alarmController.isActive) return

        val s = settings.current
        val ringtoneUri: Uri? = s.ringtoneUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
        alarmController.start(ringtoneUri, s.vibrate)

        handler.removeCallbacks(timeoutRunnable)
        handler.postDelayed(timeoutRunnable, ALARM_TIMEOUT_MS)

        postAlarmNotification(pct = pct, threshold = s.threshold, isTest = isTest)
    }

    private fun stopAlarmInternal() {
        handler.removeCallbacks(timeoutRunnable)
        alarmController.stop()
        cancelAlarmNotification()
        AlarmEvents.emit(AlarmEvent.Stopped)
    }

    private fun stopAllAndExit() {
        handler.removeCallbacks(timeoutRunnable)
        alarmController.stop()
        cancelAlarmNotification()
        AlarmEvents.emit(AlarmEvent.Stopped)
        unregisterBatteryReceiver()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        private const val TAG = "ChargingMonitorService"
        private const val NOTIF_MONITOR_ID = 1
        private const val NOTIF_ALARM_ID = 2
        private const val REARM_DROP = 3
        private const val ALARM_TIMEOUT_MS = 3 * 60 * 1000L

        const val ACTION_START = "com.balarmi.action.START"
        const val ACTION_STOP_MONITORING = "com.balarmi.action.STOP_MONITORING"
        const val ACTION_STOP_ALARM = "com.balarmi.action.STOP_ALARM"
        const val ACTION_TEST_ALARM = "com.balarmi.action.TEST_ALARM"

        fun start(context: Context) = sendAction(context, ACTION_START)
        fun stop(context: Context) = sendAction(context, ACTION_STOP_MONITORING)
        fun stopAlarm(context: Context) = sendAction(context, ACTION_STOP_ALARM)
        fun testAlarm(context: Context) = sendAction(context, ACTION_TEST_ALARM)

        private fun sendAction(context: Context, action: String) {
            val i = Intent(context, ChargingMonitorService::class.java).setAction(action)
            context.startForegroundService(i)
        }
    }
}
