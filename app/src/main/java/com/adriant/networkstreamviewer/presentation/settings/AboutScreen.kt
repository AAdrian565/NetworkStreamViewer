package com.adriant.networkstreamviewer.presentation.settings

import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen(
    developerOptionsUnlocked: Boolean,
    onUnlockDeveloperOptions: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val version = remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
        } catch (_: PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }
    var versionTapCount by remember { mutableIntStateOf(0) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsHeader(title = "About", onBack = onBack)
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text("Network Stream Viewer", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.padding(4.dp))
                Text(
                    text = "Version $version",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable {
                        if (!developerOptionsUnlocked) {
                            versionTapCount++
                            if (versionTapCount >= DEVELOPER_UNLOCK_TAPS) {
                                onUnlockDeveloperOptions()
                            }
                        }
                    }
                )
                Spacer(Modifier.padding(12.dp))
                Text("NDI® attribution", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.padding(4.dp))
                Text(
                    "NDI® is a registered trademark of Vizrt NDI AB. This app uses the licensed NDI Advanced SDK."
                )
                TextButton(onClick = { uriHandler.openUri("https://ndi.video") }) {
                    Text("Visit ndi.video")
                }
                Spacer(Modifier.padding(8.dp))
                Text("Open-source licenses", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.padding(4.dp))
                Text(
                    "AndroidX, Jetpack Compose, Material 3, and Kotlin are distributed under " +
                        "the Apache License 2.0."
                )
                TextButton(
                    onClick = { uriHandler.openUri("https://www.apache.org/licenses/LICENSE-2.0") }
                ) {
                    Text("Apache License 2.0")
                }
            }
        }
    }
}

private const val DEVELOPER_UNLOCK_TAPS = 7
