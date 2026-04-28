package com.balarmi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.balarmi.R
import com.balarmi.data.BalarmiSettings
import com.balarmi.data.ServiceMode
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: BalarmiSettings,
    permissions: PermissionStates,
    ringtoneTitle: String,
    onMonitoringToggled: (Boolean) -> Unit,
    onThresholdChanged: (Int) -> Unit,
    onModeChanged: (ServiceMode) -> Unit,
    onVibrateToggled: (Boolean) -> Unit,
    onPickRingtone: () -> Unit,
    onTestAlarm: () -> Unit,
    onGrantNotifications: () -> Unit,
    onGrantFullScreen: () -> Unit,
    onGrantBatteryOpt: () -> Unit,
    onOpenVivoAutoStart: () -> Unit,
    onMarkLockRecentsAcked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Battery alarm",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))

        SetupChecklist(
            permissions = permissions,
            autostartAcked = settings.setupAutostartAcked,
            lockRecentsAcked = settings.setupLockRecentsAcked,
            onGrantNotifications = onGrantNotifications,
            onGrantFullScreen = onGrantFullScreen,
            onGrantBatteryOpt = onGrantBatteryOpt,
            onOpenVivoAutoStart = onOpenVivoAutoStart,
            onMarkLockRecentsAcked = onMarkLockRecentsAcked,
        )

        EnableMonitoringCard(
            enabled = settings.monitoringEnabled,
            onToggle = onMonitoringToggled,
        )

        ThresholdCard(
            threshold = settings.threshold,
            onChanged = onThresholdChanged,
        )

        ServiceModeCard(
            mode = settings.serviceMode,
            onChanged = onModeChanged,
        )

        RingtoneCard(
            title = ringtoneTitle,
            onClick = onPickRingtone,
        )

        VibrateCard(
            enabled = settings.vibrate,
            onToggle = onVibrateToggled,
        )

        Button(
            onClick = onTestAlarm,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.settings_test))
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun EnableMonitoringCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.settings_enable),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.settings_enable_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun ThresholdCard(threshold: Int, onChanged: (Int) -> Unit) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.settings_threshold),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "$threshold%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(4.dp))
            Slider(
                value = threshold.toFloat(),
                onValueChange = { onChanged(it.roundToInt()) },
                valueRange = 50f..100f,
                steps = 49,
            )
        }
    }
}

@Composable
private fun ServiceModeCard(mode: ServiceMode, onChanged: (ServiceMode) -> Unit) {
    val modes = listOf(ServiceMode.CHARGING_ONLY, ServiceMode.ALWAYS_ON)
    Card {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.settings_mode),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                modes.forEachIndexed { index, m ->
                    SegmentedButton(
                        selected = mode == m,
                        onClick = { onChanged(m) },
                        shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                    ) {
                        Text(
                            when (m) {
                                ServiceMode.CHARGING_ONLY -> stringResource(R.string.settings_mode_charging_only)
                                ServiceMode.ALWAYS_ON -> stringResource(R.string.settings_mode_always_on)
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = when (mode) {
                    ServiceMode.CHARGING_ONLY -> stringResource(R.string.settings_mode_charging_only_desc)
                    ServiceMode.ALWAYS_ON -> stringResource(R.string.settings_mode_always_on_desc)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RingtoneCard(title: String, onClick: () -> Unit) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.settings_ringtone),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun VibrateCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.settings_vibrate),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}
