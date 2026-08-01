package com.adriant.networkstreamviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.adriant.networkstreamviewer.presentation.NdiApp
import com.adriant.networkstreamviewer.ui.theme.NetworkStreamViewerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NetworkStreamViewerTheme {
                NdiApp()
            }
        }
    }
}
