package com.notch.neru.feature.timer.timer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicOff
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notch.neru.feature.timer.R
import com.notch.neru.feature.timer.theme.appTheme

@Composable
fun QuickToggleCards(
    stopMediaPlayback: Boolean,
    onStopMediaPlaybackChanged: (Boolean) -> Unit,
    screenOff: Boolean,
    onScreenOffChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QuickToggleCard(
            icon = Icons.Outlined.MusicOff,
            title = stringResource(R.string.turn_off_music_title),
            subtitle = stringResource(R.string.turn_off_music_desc),
            checked = stopMediaPlayback,
            onCheckedChange = onStopMediaPlaybackChanged,
        )

        QuickToggleCard(
            icon = Icons.Outlined.ScreenLockPortrait,
            title = stringResource(R.string.lock_screen_title),
            subtitle = stringResource(R.string.lock_screen_desc),
            checked = screenOff,
            onCheckedChange = onScreenOffChanged,
        )
    }
}

@Composable
private fun QuickToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = appTheme()
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(theme.surface1)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(theme.surface2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                ),
                color = theme.textPrimary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                ),
                color = theme.textDim,
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = theme.accentInk,
                checkedTrackColor = Color.White,
                uncheckedThumbColor = theme.textDim,
                uncheckedTrackColor = theme.surface2,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}
