package com.balarmi.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class ServiceMode {
    CHARGING_ONLY, ALWAYS_ON;

    companion object {
        fun fromString(value: String?): ServiceMode = when (value) {
            ALWAYS_ON.name -> ALWAYS_ON
            else -> CHARGING_ONLY
        }
    }
}

data class BalarmiSettings(
    val monitoringEnabled: Boolean,
    val threshold: Int,
    val serviceMode: ServiceMode,
    val ringtoneUri: String?,
    val vibrate: Boolean,
    val setupAutostartAcked: Boolean,
    val setupLockRecentsAcked: Boolean,
)

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val current: BalarmiSettings
        get() = BalarmiSettings(
            monitoringEnabled = prefs.getBoolean(KEY_ENABLED, false),
            threshold = prefs.getInt(KEY_THRESHOLD, DEFAULT_THRESHOLD),
            serviceMode = ServiceMode.fromString(prefs.getString(KEY_MODE, null)),
            ringtoneUri = prefs.getString(KEY_RINGTONE, null),
            vibrate = prefs.getBoolean(KEY_VIBRATE, true),
            setupAutostartAcked = prefs.getBoolean(KEY_SETUP_AUTOSTART, false),
            setupLockRecentsAcked = prefs.getBoolean(KEY_SETUP_LOCK_RECENTS, false),
        )

    fun observe(): Flow<BalarmiSettings> = callbackFlow {
        trySend(current)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(current) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun setMonitoringEnabled(value: Boolean) = prefs.edit { putBoolean(KEY_ENABLED, value) }
    fun setThreshold(value: Int) = prefs.edit { putInt(KEY_THRESHOLD, value.coerceIn(1, 100)) }
    fun setServiceMode(value: ServiceMode) = prefs.edit { putString(KEY_MODE, value.name) }
    fun setRingtoneUri(value: String?) = prefs.edit { putString(KEY_RINGTONE, value) }
    fun setVibrate(value: Boolean) = prefs.edit { putBoolean(KEY_VIBRATE, value) }
    fun setSetupAutostartAcked(value: Boolean) = prefs.edit { putBoolean(KEY_SETUP_AUTOSTART, value) }
    fun setSetupLockRecentsAcked(value: Boolean) = prefs.edit { putBoolean(KEY_SETUP_LOCK_RECENTS, value) }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        const val PREFS_NAME = "balarmi"
        const val DEFAULT_THRESHOLD = 90

        const val KEY_ENABLED = "monitoring_enabled"
        const val KEY_THRESHOLD = "threshold"
        const val KEY_MODE = "service_mode"
        const val KEY_RINGTONE = "ringtone_uri"
        const val KEY_VIBRATE = "vibrate"
        const val KEY_SETUP_AUTOSTART = "setup_autostart_acked"
        const val KEY_SETUP_LOCK_RECENTS = "setup_lock_recents_acked"
    }
}
