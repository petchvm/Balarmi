package com.balarmi

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.balarmi.data.ServiceMode
import com.balarmi.data.SettingsRepository
import com.balarmi.service.ChargingMonitorService
import com.balarmi.ui.PermissionStates
import com.balarmi.ui.SettingsScreen
import com.balarmi.ui.theme.BalarmiTheme
import com.balarmi.util.PermissionHelper
import com.balarmi.util.VendorOptimizationHelper

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

        setContent {
            BalarmiTheme {
                val settingsState by remember { settings.observe() }
                    .collectAsState(initial = settings.current)

                SettingsScreen(
                    settings = settingsState,
                    permissions = permissionStates,
                    ringtoneTitle = ringtoneTitleFor(settingsState.ringtoneUri),
                    onMonitoringToggled = ::onMonitoringToggled,
                    onThresholdChanged = settings::setThreshold,
                    onModeChanged = ::onModeChanged,
                    onVibrateToggled = settings::setVibrate,
                    onPickRingtone = ::launchRingtonePicker,
                    onTestAlarm = { ChargingMonitorService.testAlarm(this) },
                    onGrantNotifications = ::requestNotifications,
                    onGrantFullScreen = ::openFullScreenIntentSettings,
                    onGrantBatteryOpt = ::requestBatteryOptExempt,
                    onOpenVivoAutoStart = ::openVivoAutoStart,
                    onMarkLockRecentsAcked = { settings.setSetupLockRecentsAcked(true) },
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
            notifications = permissionHelper.notificationsGranted,
            fullScreenIntent = permissionHelper.canUseFullScreenIntent,
            batteryOptExempt = permissionHelper.isIgnoringBatteryOptimization,
        )
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

    private fun openVivoAutoStart() {
        VendorOptimizationHelper.openAutoStartSettings(this)
        settings.setSetupAutostartAcked(true)
    }

    private fun onMonitoringToggled(enabled: Boolean) {
        settings.setMonitoringEnabled(enabled)
        if (enabled) {
            ChargingMonitorService.start(this)
        } else {
            ChargingMonitorService.stop(this)
        }
    }

    private fun onModeChanged(mode: ServiceMode) {
        settings.setServiceMode(mode)
        if (settings.current.monitoringEnabled) {
            ChargingMonitorService.start(this)
        }
    }
}
