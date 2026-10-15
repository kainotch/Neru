package com.notch.neru.feature.timer.timer.components

import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.notch.neru.feature.timer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppEntry(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerDialog(
    selectedPackages: Set<String>,
    onDismiss: () -> Unit,
    onDone: (Set<String>) -> Unit,
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledAppEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentSelected by remember(selectedPackages) { mutableStateOf(selectedPackages) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolved = pm.queryIntentActivities(intent, 0)
            val list = resolved.mapNotNull { ri ->
                val pkg = ri.activityInfo.packageName
                if (pkg == context.packageName) null
                else {
                    val label = ri.loadLabel(pm).toString()
                    val icon = try {
                        ri.loadIcon(pm)
                    } catch (e: Exception) {
                        null
                    }
                    InstalledAppEntry(pkg, label, icon)
                }
            }.sortedBy { it.label.lowercase() }
            withContext(Dispatchers.Main) {
                apps = list
                isLoading = false
            }
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E222B),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.select_target_app_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                    )
                    if (currentSelected.isNotEmpty()) {
                        Text(
                            text = "${currentSelected.size} selected",
                            color = Color(0xFFC9B8FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Loading apps...",
                            color = Color(0xFF8F96A3),
                            fontSize = 14.sp,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(apps, key = { it.packageName }) { app ->
                            val isChecked = currentSelected.contains(app.packageName)
                            val rowBg = if (isChecked) Color(0xFF282C37) else Color.Transparent

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(rowBg)
                                    .clickable {
                                        currentSelected = if (isChecked) {
                                            currentSelected - app.packageName
                                        } else {
                                            currentSelected + app.packageName
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (app.icon != null) {
                                    val bitmap = remember(app.packageName) {
                                        try {
                                            app.icon.toBitmap(width = 72, height = 72).asImageBitmap()
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = null,
                                            modifier = Modifier.size(36.dp),
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color(0xFF282C37), RoundedCornerShape(8.dp)),
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFF282C37), RoundedCornerShape(8.dp)),
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        color = if (isChecked) Color.White else Color(0xFFE2E4E9),
                                        fontSize = 14.sp,
                                        fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = app.packageName,
                                        color = Color(0xFF8F96A3),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }

                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        currentSelected = if (checked) {
                                            currentSelected + app.packageName
                                        } else {
                                            currentSelected - app.packageName
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF6C5CE7),
                                        checkmarkColor = Color.White,
                                        uncheckedColor = Color(0xFF555B68),
                                    ),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Cancel",
                            color = Color(0xFF8F96A3),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onDone(currentSelected)
                            onDismiss()
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.action_done),
                            color = Color(0xFFC9B8FF),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
