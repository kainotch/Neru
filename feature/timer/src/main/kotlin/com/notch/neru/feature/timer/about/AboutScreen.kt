package com.notch.neru.feature.timer.about

import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notch.neru.feature.timer.R
import com.notch.neru.feature.timer.theme.AppThemes
import com.notch.neru.feature.timer.theme.ProvideAppTheme
import com.notch.neru.feature.timer.theme.appTheme
import com.notch.neru.feature.timer.timer.components.TimerBackground

private const val GITHUB_URL = "https://github.com/kainotch"
private const val INSTAGRAM_URL = "https://instagram.com/kainotch"
private const val REPO_URL = "https://github.com/kainotch/Neru"

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    viewModel: AboutViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    ProvideAppTheme(AppThemes.byId(settings.theme)) {
        AboutContent(
            starsEnabled = settings.starsEnabled,
            onBack = onBack,
        )
    }
}

@Composable
private fun AboutContent(
    starsEnabled: Boolean,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        } catch (_: PackageManager.NameNotFoundException) {
            ""
        }
    }

    val openUrl: (String) -> Unit = { url ->
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    TimerBackground(
        animating = false,
        starsEnabled = starsEnabled,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            AboutTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                // Branded App Logo Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(R.drawable.app_logo),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(22.dp)),
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                        ),
                        color = appTheme().textPrimary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "v$versionName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = appTheme().textDim,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                SectionHeader(stringResource(R.string.category_about))

                AboutRow(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.about_version_title),
                    value = versionName,
                )
                AboutRow(
                    icon = Icons.Outlined.Person,
                    title = stringResource(R.string.about_author_title),
                    value = stringResource(R.string.about_author_value),
                )
                AboutRow(
                    icon = Icons.Outlined.Code,
                    title = stringResource(R.string.about_github_title),
                    value = stringResource(R.string.about_github_value),
                    onClick = { openUrl(GITHUB_URL) },
                )
                AboutRow(
                    icon = Icons.Outlined.CameraAlt,
                    title = stringResource(R.string.about_instagram_title),
                    value = stringResource(R.string.about_instagram_value),
                    onClick = { openUrl(INSTAGRAM_URL) },
                )
                AboutRow(
                    icon = Icons.Outlined.Code,
                    title = stringResource(R.string.about_repo_title),
                    value = stringResource(R.string.about_repo_value),
                    onClick = { openUrl(REPO_URL) },
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun AboutTopBar(onBack: () -> Unit) {
    val theme = appTheme()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(44.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = theme.textPrimary,
            )
        }
        Text(
            text = stringResource(R.string.about_title),
            style = MaterialTheme.typography.titleLarge,
            color = theme.textPrimary,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    val theme = appTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = theme.textMuted,
        )
    }
}

@Composable
private fun AboutRow(
    icon: ImageVector,
    title: String,
    value: String?,
    onClick: (() -> Unit)? = null,
) {
    val theme = appTheme()
    val rowModifier = Modifier
        .fillMaxWidth()
        .let { if (onClick != null) it.clickable { onClick() } else it }
        .padding(horizontal = 20.dp, vertical = 14.dp)

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(theme.surface1),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = theme.accent,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = theme.textPrimary,
            )
            if (!value.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = theme.textDim,
                )
            }
        }
        if (onClick != null) {
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = theme.textMuted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

