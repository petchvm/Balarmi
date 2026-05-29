package com.balarmi

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.balarmi.data.SettingsRepository
import com.balarmi.service.ChargingMonitorService
import com.balarmi.state.MonitorState
import com.balarmi.ui.PermissionStates
import com.balarmi.ui.SettingsScreen
import com.balarmi.ui.theme.BalarmiTheme
import com.balarmi.util.PermissionHelper

class MainActivity : ComponentActivity() {

    private lateinit var settings: SettingsRepository
    private lateinit var permissionHelper: PermissionHelper
    private var permissionStates by mutableStateOf(PermissionStates())

    private val ringtonePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val uri = result.data?.let { extractRingtoneUri(it) }
        settings.setRingtoneUri(uri?.toString())
    }

    private val notificationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* state will refresh in onResume */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        settings = SettingsRepository(this)
        permissionHelper = PermissionHelper(this)
        refreshPermissionStates()

        // The user opened the app to start a charging session — bring up the monitor.
        // The service is idempotent: subsequent startForegroundService calls just re-deliver onStartCommand.
        ChargingMonitorService.start(this)

        setContent {
            BalarmiTheme {
                val settingsState by remember { settings.observe() }
                    .collectAsState(initial = settings.current)
                val monitor by MonitorState.state.collectAsState()

                SettingsScreen(
                    settings = settingsState,
                    permissions = permissionStates,
                    monitor = monitor,
                    ringtoneTitle = ringtoneTitleFor(settingsState.ringtoneUri),
                    onThresholdChanged = settings::setThreshold,
                    onVibrateToggled = settings::setVibrate,
                    onPickRingtone = ::launchRingtonePicker,
                    onTestAlarm = { ChargingMonitorService.testAlarm(this) },
                    onGrantNotifications = ::requestNotifications,
                    onGrantFullScreen = ::openFullScreenIntentSettings,
                    onGrantBatteryOpt = ::requestBatteryOptExempt,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStates()
    }

    private fun refreshPermissionStates() {
        permissionStates = PermissionStates(
            notifications = safePermissionProbe { permissionHelper.notificationsGranted },
            fullScreenIntent = safePermissionProbe { permissionHelper.canUseFullScreenIntent },
            batteryOptExempt = safePermissionProbe { permissionHelper.isIgnoringBatteryOptimization },
        )
    }

    // Permission probes touch platform APIs whose availability and behaviour vary across Android
    // versions and OEM builds. A single failing probe must never crash startup (this runs in
    // onCreate/onResume), so degrade to "not granted" — the setup checklist will simply prompt
    // the user — and log it for diagnosis. Catches Throwable on purpose to also absorb linkage
    // errors like NoSuchMethodError from version/API mismatches.
    private fun safePermissionProbe(probe: () -> Boolean): Boolean =
        try {
            probe()
        } catch (t: Throwable) {
            Log.w("MainActivity", "Permission probe failed; treating as not granted", t)
            false
        }

    private fun ringtoneTitleFor(uriStr: String?): String {
        val uri = uriStr?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        return runCatching {
            RingtoneManager.getRingtone(this, uri)?.getTitle(this)
        }.getOrNull() ?: getString(R.string.settings_ringtone_default)
    }

    private fun extractRingtoneUri(data: Intent): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
    }

    private fun launchRingtonePicker() {
        val existing = settings.current.ringtoneUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.ringtone_picker_title))
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            existing?.let { putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it) }
        }
        ringtonePicker.launch(intent)
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionRequest.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openFullScreenIntentSettings() {
        startActivity(permissionHelper.fullScreenIntentSettingsIntent())
    }

    private fun requestBatteryOptExempt() {
        startActivity(permissionHelper.batteryOptIntent())
    }
}
