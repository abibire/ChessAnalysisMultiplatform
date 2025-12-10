package com.andrewbibire.chessanalysis

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.useResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.WindowPlacement
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import coil3.util.DebugLogger
import java.awt.Taskbar
import javax.imageio.ImageIO
import okio.Path.Companion.toOkioPath

fun main() = application {
    // Configure Coil ImageLoader for desktop with SVG support
    setSingletonImageLoaderFactory { context ->
        // Use user's cache directory for sandboxed apps (App Store)
        val cacheDir = try {
            val userHome = System.getProperty("user.home")
            java.io.File(userHome, "Library/Caches/com.andrewbibire.chessanalysismac/coil_cache")
        } catch (e: Exception) {
            // Fallback to temp directory for non-sandboxed builds
            java.io.File(System.getProperty("java.io.tmpdir"), "coil_cache")
        }

        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.toOkioPath())
                    .maxSizeBytes(512L * 1024 * 1024) // 512MB
                    .build()
            }
            .crossfade(true)
            .logger(DebugLogger())
            .build()
    }
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