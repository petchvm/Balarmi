package com.balarmi.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun SetupBanner(
    permissions: PermissionStates,
    onGrantNotifications: () -> Unit,
    onGrantFullScreen: () -> Unit,
    onGrantBatteryOpt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        Triple(R.string.checklist_notifications, permissions.notifications, onGrantNotifications),
        Triple(R.string.checklist_full_screen, permissions.fullScreenIntent, onGrantFullScreen),
        Triple(R.string.checklist_battery_opt, permissions.batteryOptExempt, onGrantBatteryOpt),
    )
    val total = items.size
    val done = items.count { it.second }
    if (done == total) return

    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.setup_banner_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.setup_banner_subtitle, done, total),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    items.forEach { (labelRes, isDone, action) ->
                        ChecklistItem(
                            label = stringResource(labelRes),
                            done = isDone,
                            onAction = action,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistItem(
    label: String,
    done: Boolean,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (done) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (!done) {
            TextButton(onClick = onAction) {
                Text(stringResource(R.string.checklist_grant))
            }
        }
    }
}
