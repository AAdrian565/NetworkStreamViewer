package com.adriant.networkstreamviewer.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun ScreenOrientationEffect(showingPlayer: Boolean) {
    val activity = LocalContext.current.findActivity() ?: return
    val requestedOrientation =
        if (showingPlayer) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

    DisposableEffect(activity, requestedOrientation) {
        activity.requestedOrientation = requestedOrientation
        onDispose { }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
