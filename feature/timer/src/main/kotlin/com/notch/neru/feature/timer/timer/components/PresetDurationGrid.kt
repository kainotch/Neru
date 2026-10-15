package com.notch.neru.feature.timer.timer.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notch.neru.feature.timer.R
import com.notch.neru.feature.timer.theme.appTheme

@Composable
fun PresetDurationGrid(
    selectedMinutes: Int,
    onPresetSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val presets = listOf(
        listOf(5, 10),
        listOf(30, 45),
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        for (row in presets) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                for (preset in row) {
                    PresetCard(
                        minutes = preset,
                        isSelected = selectedMinutes == preset,
                        onClick = { onPresetSelected(preset) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetCard(
    minutes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val theme = appTheme()
    val shape = RoundedCornerShape(12.dp)

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF1B1F27) else Color(0xFF13161C),
        label = "preset_bg",
    )

    Box(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .background(bgColor)
            .border(1.dp, Color(0xFF222630), shape)
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = minutes.toString(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.preset_min),
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = if (isSelected) Color.White else theme.textDim,
            )
        }
    }
}
