package com.adriant.networkstreamviewer.presentation.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
internal fun ImmersiveSystemBarsEffect() {
    val activity = LocalContext.current.findActivity() ?: return
    val view = LocalView.current

    DisposableEffect(activity, view) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        WindowCompat.getInsetsController(activity.window, view).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            WindowCompat.getInsetsController(activity.window, view)
                .show(WindowInsetsCompat.Type.systemBars())
            WindowCompat.setDecorFitsSystemWindows(activity.window, true)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
