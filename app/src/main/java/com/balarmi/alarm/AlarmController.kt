package com.balarmi.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class AlarmController(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    val isActive: Boolean get() = mediaPlayer != null

    fun start(ringtoneUri: Uri?, vibrate: Boolean) {
        if (mediaPlayer != null) return

        acquireWakeLock()
        startAudio(ringtoneUri)
        if (vibrate) startVibration()
    }

    fun stop() {
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null

        runCatching { vibrator?.cancel() }
        vibrator = null

        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private fun acquireWakeLock() {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Balarmi:AlarmWakeLock",
        ).apply {
            setReferenceCounted(false)
            // 3 min + 5 sec safety buffer; auto-release prevents leaks if stop() is missed
            acquire(3 * 60 * 1000L + 5_000L)
        }
    }

    private fun startAudio(preferredUri: Uri?) {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val candidates = listOfNotNull(
            preferredUri,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
        )

        for (uri in candidates) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(attrs)
                    setDataSource(context, uri)
                    isLooping = true
                    setOnPreparedListener { it.start() }
                    setOnErrorListener { _, _, _ -> true }
                    prepareAsync()
                }
                return
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load ringtone $uri", e)
                runCatching { mediaPlayer?.release() }
                mediaPlayer = null
            }
        }
        Log.e(TAG, "No usable ringtone found")
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        val pattern = longArrayOf(0, 1000, 500)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private companion object {
        const val TAG = "AlarmController"
    }
}
