package com.balarmi.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class BalarmiSettings(
    val threshold: Int,
    val ringtoneUri: String?,
    val vibrate: Boolean,
)

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val current: BalarmiSettings
        get() = BalarmiSettings(
            threshold = prefs.getInt(KEY_THRESHOLD, DEFAULT_THRESHOLD),
            ringtoneUri = prefs.getString(KEY_RINGTONE, null),
            vibrate = prefs.getBoolean(KEY_VIBRATE, true),
        )

    fun observe(): Flow<BalarmiSettings> = callbackFlow {
        trySend(current)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(current) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun setThreshold(value: Int) = prefs.edit { putInt(KEY_THRESHOLD, value.coerceIn(1, 100)) }
    fun setRingtoneUri(value: String?) = prefs.edit { putString(KEY_RINGTONE, value) }
    fun setVibrate(value: Boolean) = prefs.edit { putBoolean(KEY_VIBRATE, value) }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        const val PREFS_NAME = "balarmi"
        const val DEFAULT_THRESHOLD = 90

        const val KEY_THRESHOLD = "threshold"
        const val KEY_RINGTONE = "ringtone_uri"
        const val KEY_VIBRATE = "vibrate"
    }
}
