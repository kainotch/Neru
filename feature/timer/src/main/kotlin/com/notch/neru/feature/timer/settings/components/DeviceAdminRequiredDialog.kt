package com.notch.neru.feature.timer.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.notch.neru.feature.timer.R

@Composable
fun DeviceAdminRequiredDialog(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null) },
        title = { Text(stringResource(R.string.admin_dialog_title)) },
        text = { Text(stringResource(R.string.admin_body_required)) },
        confirmButton = {
            TextButton(onClick = onRequestPermission) {
                Text(stringResource(R.string.admin_action_grant))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_action_ignore))
            }
        },
    )
}
