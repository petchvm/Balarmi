package com.balarmi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.balarmi.R

data class PermissionStates(
    val notifications: Boolean = false,
    val fullScreenIntent: Boolean = false,
    val batteryOptExempt: Boolean = false,
)

@Composable
fun SetupChecklist(
    permissions: PermissionStates,
    autostartAcked: Boolean,
    lockRecentsAcked: Boolean,
    onGrantNotifications: () -> Unit,
    onGrantFullScreen: () -> Unit,
    onGrantBatteryOpt: () -> Unit,
    onOpenVivoAutoStart: () -> Unit,
    onMarkLockRecentsAcked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.checklist_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.checklist_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            ChecklistItem(
                label = stringResource(R.string.checklist_notifications),
                done = permissions.notifications,
                onAction = onGrantNotifications,
            )
            ChecklistItem(
                label = stringResource(R.string.checklist_full_screen),
                done = permissions.fullScreenIntent,
                onAction = onGrantFullScreen,
            )
            ChecklistItem(
                label = stringResource(R.string.checklist_battery_opt),
                done = permissions.batteryOptExempt,
                onAction = onGrantBatteryOpt,
            )
            ChecklistItem(
                label = stringResource(R.string.checklist_vivo_autostart),
                done = autostartAcked,
                onAction = onOpenVivoAutoStart,
            )
            ChecklistItem(
                label = stringResource(R.string.checklist_lock_recents),
                description = stringResource(R.string.checklist_lock_recents_desc),
                done = lockRecentsAcked,
                actionLabel = stringResource(R.string.checklist_done),
                onAction = onMarkLockRecentsAcked,
            )
        }
    }
}

@Composable
private fun ChecklistItem(
    label: String,
    done: Boolean,
    onAction: () -> Unit,
    description: String? = null,
    actionLabel: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!done) {
            TextButton(onClick = onAction) {
                Text(actionLabel ?: stringResource(R.string.checklist_grant))
            }
        }
    }
}
