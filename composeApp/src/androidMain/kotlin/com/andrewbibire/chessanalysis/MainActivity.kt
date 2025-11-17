package com.andrewbibire.chessanalysis

import android.os.Bundle
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import android.graphics.Color
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContextHolder.context = this
        // Force portrait orientation FIRST, then lock it
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        // Small delay to ensure portrait is set, then lock
        window.decorView.post {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            App(this)
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-enforce locked portrait orientation when activity resumes
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.decorView.post {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        }
    }

    override fun onStart() {
        super.onStart()
        // Re-enforce locked portrait orientation when activity starts
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.decorView.post {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // If configuration somehow changes, force back to portrait and lock
        Log.d("MainActivity", "Configuration changed - forcing portrait lock. Orientation: ${newConfig.orientation}")
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.decorView.post {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(context = null)
}