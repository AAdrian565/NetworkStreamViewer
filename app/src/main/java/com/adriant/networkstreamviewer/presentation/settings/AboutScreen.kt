package com.adriant.networkstreamviewer.presentation.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.adriant.networkstreamviewer.presentation.UpdateStatus
import com.adriant.networkstreamviewer.presentation.UpdateUiState
import java.io.File

@Composable
fun AboutScreen(
    developerOptionsUnlocked: Boolean,
    updateState: UpdateUiState,
    onUnlockDeveloperOptions: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val version =
        remember(context) {
            try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
            } catch (_: PackageManager.NameNotFoundException) {
                "Unknown"
            }
        }
    var versionTapCount by remember { mutableIntStateOf(0) }

    Scaffold { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
        ) {
            SettingsHeader(title = "About", onBack = onBack)
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text("Network Stream Viewer", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.padding(4.dp))
                Text(
                    text = "Version $version",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier.clickable {
                            if (!developerOptionsUnlocked) {
                                versionTapCount++
                                if (versionTapCount >= DEVELOPER_UNLOCK_TAPS) {
                                    onUnlockDeveloperOptions()
                                }
                            }
                        },
                )
                Spacer(Modifier.padding(12.dp))
                Text("NDI® attribution", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.padding(4.dp))
                Text(
                    "NDI® is a registered trademark of Vizrt NDI AB. This app uses the licensed NDI Advanced SDK.",
                )
                TextButton(onClick = { uriHandler.openUri("https://ndi.video") }) {
                    Text("Visit ndi.video")
                }
                Spacer(Modifier.padding(8.dp))
                Text("Updates", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.padding(4.dp))
                Text(updateState.message)
                Spacer(Modifier.padding(4.dp))
                when (updateState.status) {
                    UpdateStatus.AVAILABLE ->
                        Button(onClick = onDownloadUpdate) {
                            Text("Download update")
                        }
                    UpdateStatus.READY ->
                        Button(
                            onClick = {
                                updateState.downloadedApkPath?.let { path ->
                                    installUpdate(context, path)
                                }
                            },
                        ) {
                            Text("Install update")
                        }
                    UpdateStatus.CHECKING,
                    UpdateStatus.DOWNLOADING,
                    -> Unit
                    UpdateStatus.IDLE,
                    UpdateStatus.UP_TO_DATE,
                    UpdateStatus.ERROR,
                    ->
                        TextButton(onClick = onCheckForUpdates) {
                            Text("Check for updates")
                        }
                }
                Spacer(Modifier.padding(8.dp))
                Text("Open-source licenses", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.padding(4.dp))
                Text(
                    "AndroidX, Jetpack Compose, Material 3, and Kotlin are distributed under " +
                        "the Apache License 2.0.",
                )
                TextButton(
                    onClick = { uriHandler.openUri("https://www.apache.org/licenses/LICENSE-2.0") },
                ) {
                    Text("Apache License 2.0")
                }
            }
        }
    }
}

private val UpdateUiState.message: String
    get() =
        when (status) {
            UpdateStatus.IDLE -> "Check GitHub for the latest release."
            UpdateStatus.CHECKING -> "Checking GitHub for updates…"
            UpdateStatus.UP_TO_DATE -> "You are using the latest version."
            UpdateStatus.AVAILABLE -> "Version ${update?.version} is available."
            UpdateStatus.DOWNLOADING -> "Downloading version ${update?.version}…"
            UpdateStatus.READY -> "Version ${update?.version} is ready to install."
            UpdateStatus.ERROR -> errorMessage ?: "Update check failed."
        }

private fun installUpdate(
    context: android.content.Context,
    path: String,
) {
    val apkFile = File(path)
    if (!apkFile.isFile) return

    if (!context.packageManager.canRequestPackageInstalls()) {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                "package:${context.packageName}".toUri(),
            ),
        )
        return
    }

    val apkUri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
    context.startActivity(
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        },
    )
}

private const val DEVELOPER_UNLOCK_TAPS = 7
