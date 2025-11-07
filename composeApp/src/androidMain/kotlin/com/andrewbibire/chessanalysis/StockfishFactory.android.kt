package com.andrewbibire.chessanalysis

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat.getSystemService

actual fun createStockfishEngine(context: Any?): StockfishEngine {
    return StockfishEngine(context)
}

actual suspend fun readClipboard(): String? {
    return try {
        val context = AppContextHolder.context ?: return null
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
    } catch (e: Exception) {
        null
    }
}

object AppContextHolder {
    var context: Context? = null
}