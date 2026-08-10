package com.adriant.networkstreamviewer.presentation

import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

internal const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

internal class LocalNetworkPermissionState(
    val isGranted: Boolean,
    val request: () -> Unit,
)

@Composable
internal fun rememberLocalNetworkPermissionState(): LocalNetworkPermissionState {
    val context = LocalContext.current
    var isGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 37 ||
                context.checkSelfPermission(LOCAL_NETWORK_PERMISSION) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted -> isGranted = granted }

    return LocalNetworkPermissionState(
        isGranted = isGranted,
        request = { launcher.launch(LOCAL_NETWORK_PERMISSION) },
    )
}

@Composable
internal fun LocalNetworkMulticastEffect(enabled: Boolean) {
    val context = LocalContext.current
    val multicastLock =
        remember {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.createMulticastLock("network-stream-viewer-discovery").apply {
                setReferenceCounted(false)
            }
        }

    DisposableEffect(enabled) {
        if (enabled && !multicastLock.isHeld) multicastLock.acquire()
        onDispose {
            if (multicastLock.isHeld) multicastLock.release()
        }
    }
}
