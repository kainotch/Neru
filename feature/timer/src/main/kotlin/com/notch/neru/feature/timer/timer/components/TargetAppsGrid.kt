package com.notch.neru.feature.timer.timer.components

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.notch.neru.feature.timer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

private val CUSTOM_PALETTE = listOf(
    Color(0xFF00E5FF), // Cyan
    Color(0xFFFFB74D), // Amber
    Color(0xFFC9B8FF), // Lavender
    Color(0xFF1DE9B6), // Teal
    Color(0xFFFF6E40), // Coral
    Color(0xFFC6FF00), // Lime
    Color(0xFFFF4081), // Pink
    Color(0xFF7C4DFF), // Purple
)

data class GridItem(
    val packageName: String,
    val name: String,
    val accentColor: Color,
    val icon: Drawable? = null,
    val isMoreButton: Boolean = false,
)

@Composable
fun TargetAppsGrid(
    targetAllApps: Boolean,
    selectedPackages: Set<String>,
    customPackages: Set<String>,
    onToggleAllApps: (Boolean) -> Unit,
    onToggleApp: (packageName: String) -> Unit,
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    // Resolve grid items (defaults + customs + "+ More")
    var gridItems by remember { mutableStateOf<List<GridItem>>(emptyList()) }

    LaunchedEffect(customPackages) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val defaultApps = TargetAppDefaults.DEFAULT_APPS
            val defaultPkgSet = defaultApps.map { it.packageName }.toSet()
            val list = mutableListOf<GridItem>()

            // 1. Default apps
            for (app in defaultApps) {
                val icon = try {
                    pm.getApplicationIcon(app.packageName)
                } catch (_: Exception) {
                    null
                }
                list.add(GridItem(app.packageName, app.name, app.accentColor, icon))
            }

            // 2. Custom apps added by the user
            for (pkg in customPackages) {
                if (pkg !in defaultPkgSet) {
                    val label = try {
                        val info = pm.getApplicationInfo(pkg, 0)
                        pm.getApplicationLabel(info).toString()
                    } catch (_: Exception) {
                        pkg.substringAfterLast('.')
                    }
                    val icon = try {
                        pm.getApplicationIcon(pkg)
                    } catch (_: Exception) {
                        null
                    }
                    val colorIndex = abs(pkg.hashCode()) % CUSTOM_PALETTE.size
                    list.add(GridItem(pkg, label, CUSTOM_PALETTE[colorIndex], icon))
                }
            }

            // 3. "+ More" button at the end
            list.add(
                GridItem(
                    packageName = "",
                    name = context.getString(R.string.select_other_app),
                    accentColor = Color(0xFFC9B8FF),
                    icon = null,
                    isMoreButton = true,
                ),
            )

            withContext(Dispatchers.Main) {
                gridItems = list
            }
        }
    }

    val isAnySelected = selectedPackages.isNotEmpty()
    val rows = remember(gridItems) { gridItems.chunked(4) }
    val totalRows = rows.size
    val hasExtraRows = totalRows > 2

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        // Top Row: "All Apps" toggle switch and direct "+" action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                    tint = if (targetAllApps) Color(0xFFC9B8FF) else Color(0xFF8F96A3),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.target_all_apps_title),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = if (targetAllApps) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (targetAllApps) Color.White else Color(0xFF8F96A3),
                    ),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (targetAllApps || !isAnySelected) {
                        "• " + stringResource(R.string.target_all_apps_desc)
                    } else {
                        "• ${selectedPackages.size} selected"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        color = if (targetAllApps || !isAnySelected) Color(0xFF6B7280) else Color(0xFFC9B8FF),
                    ),
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenPicker,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.select_other_app),
                        tint = Color(0xFFC9B8FF),
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = targetAllApps,
                    onCheckedChange = onToggleAllApps,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF6C5CE7),
                        uncheckedThumbColor = Color(0xFF8F96A3),
                        uncheckedTrackColor = Color(0xFF1E222B),
                    ),
                    modifier = Modifier.size(width = 38.dp, height = 24.dp),
                )
            }
        }

        // Always visible: First 2 rows (up to 8 items)
        val initialRows = rows.take(2)
        initialRows.forEachIndexed { rowIndex, rowItems ->
            if (rowIndex > 0) Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rowItems.forEach { item ->
                    if (item.isMoreButton) {
                        AppBoxItem(
                            name = item.name,
                            icon = null,
                            accentColor = item.accentColor,
                            isSelected = false,
                            isDimmed = targetAllApps || isAnySelected,
                            onClick = onOpenPicker,
                            isMoreButton = true,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        val isSelected = !targetAllApps && selectedPackages.contains(item.packageName)
                        AppBoxItem(
                            name = item.name,
                            icon = item.icon,
                            accentColor = item.accentColor,
                            isSelected = isSelected,
                            isDimmed = targetAllApps || (!isSelected && isAnySelected),
                            onClick = {
                                onToggleApp(item.packageName)
                                if (targetAllApps) onToggleAllApps(false)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                repeat(4 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // Expandable: Additional rows beyond row 2
        if (hasExtraRows) {
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(250)),
                exit = shrinkVertically(animationSpec = tween(250)) + fadeOut(animationSpec = tween(250)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Spacer(modifier = Modifier.height(6.dp))
                    rows.drop(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            rowItems.forEach { item ->
                                if (item.isMoreButton) {
                                    AppBoxItem(
                                        name = item.name,
                                        icon = null,
                                        accentColor = item.accentColor,
                                        isSelected = false,
                                        isDimmed = targetAllApps || isAnySelected,
                                        onClick = onOpenPicker,
                                        isMoreButton = true,
                                        modifier = Modifier.weight(1f),
                                    )
                                } else {
                                    val isSelected = !targetAllApps && selectedPackages.contains(item.packageName)
                                    AppBoxItem(
                                        name = item.name,
                                        icon = item.icon,
                                        accentColor = item.accentColor,
                                        isSelected = isSelected,
                                        isDimmed = targetAllApps || (!isSelected && isAnySelected),
                                        onClick = {
                                            onToggleApp(item.packageName)
                                            if (targetAllApps) onToggleAllApps(false)
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                            repeat(4 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Sleek Arrow Expand/Collapse Button
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val remainingApps = (gridItems.size - 8).coerceAtLeast(1)
                Surface(
                    onClick = { isExpanded = !isExpanded },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1E222B),
                    border = BorderStroke(1.dp, Color(0xFF282C37)),
                    modifier = Modifier.height(28.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) {
                                stringResource(R.string.show_less_apps)
                            } else {
                                stringResource(R.string.show_more_apps, remainingApps)
                            },
                            tint = Color(0xFFC9B8FF),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isExpanded) {
                                stringResource(R.string.show_less_apps)
                            } else {
                                stringResource(R.string.show_more_apps, remainingApps)
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = Color(0xFFC9B8FF),
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppBoxItem(
    name: String,
    icon: Drawable?,
    accentColor: Color,
    isSelected: Boolean,
    isDimmed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isMoreButton: Boolean = false,
) {
    val shape = RoundedCornerShape(12.dp)

    val bgColor by animateColorAsState(
        targetValue = when {
            isSelected -> accentColor.copy(alpha = 0.22f)
            else -> Color(0xFF14171E)
        },
        animationSpec = tween(200),
        label = "bgColor",
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> accentColor
            else -> Color(0xFF222630)
        },
        animationSpec = tween(200),
        label = "borderColor",
    )

    val contentAlpha = when {
        isSelected -> 1.0f
        isDimmed -> 0.40f
        else -> 0.65f
    }

    Box(
        modifier = modifier
            .height(50.dp)
            .clip(shape)
            .background(bgColor)
            .border(BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (isMoreButton) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = if (isSelected) accentColor else Color.White.copy(alpha = contentAlpha),
                    modifier = Modifier.size(18.dp),
                )
            } else if (icon != null) {
                val bitmap = remember(icon) {
                    try {
                        icon.toBitmap(width = 48, height = 48).asImageBitmap()
                    } catch (e: Exception) {
                        null
                    }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = name,
                        alpha = contentAlpha,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(accentColor.copy(alpha = contentAlpha), CircleShape),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(accentColor.copy(alpha = contentAlpha), CircleShape),
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = Color.White.copy(alpha = contentAlpha),
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
