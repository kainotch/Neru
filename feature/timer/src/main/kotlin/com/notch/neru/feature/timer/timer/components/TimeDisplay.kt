package com.notch.neru.feature.timer.timer.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notch.neru.feature.timer.R
import com.notch.neru.feature.timer.theme.appTheme

@Composable
fun TimeDisplay(
    totalMinutes: Int,
    modifier: Modifier = Modifier,
    remainingMillis: Long? = null,
) {
    val theme = appTheme()

    val timeString: String = if (remainingMillis != null && remainingMillis > 0) {
        val totalSec = (remainingMillis / 1000).toInt()
        val hrs = totalSec / 3600
        val mins = (totalSec % 3600) / 60
        val secs = totalSec % 60
        if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    } else {
        val hrs = totalMinutes / 60
        val remMin = totalMinutes % 60
        if (hrs > 0) {
            String.format("%02d:%02d:00", hrs, remMin)
        } else {
            String.format("%02d:00", remMin)
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = timeString,
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 54.sp,
            ),
            color = theme.textPrimary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.min_sec_label),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
            ),
            color = theme.textDim,
        )
    }
}
