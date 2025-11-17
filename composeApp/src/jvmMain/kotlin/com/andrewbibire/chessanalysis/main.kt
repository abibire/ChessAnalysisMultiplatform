package com.andrewbibire.chessanalysis

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.useResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.WindowPlacement
import java.awt.Taskbar
import javax.imageio.ImageIO

fun main() = application {
    // Set dock icon for macOS
    if (Taskbar.isTaskbarSupported()) {
        try {
            val taskbar = Taskbar.getTaskbar()
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                val icon = useResource("app-icon.png") { ImageIO.read(it) }
                taskbar.iconImage = icon
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "ChessAnalysis",
        icon = painterResource("app-icon.png"),
        state = WindowState(placement = WindowPlacement.Maximized)
    ) {
        App()
    }
}