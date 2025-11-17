package com.andrewbibire.chessanalysis

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val controller = ComposeUIViewController {
        App(context = Unit)
    }
    return controller
}