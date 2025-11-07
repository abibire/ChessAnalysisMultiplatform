package com.andrewbibire.chessanalysis

import platform.UIKit.UIPasteboard

actual fun createStockfishEngine(context: Any?): StockfishEngine {
    return StockfishEngine(null)
}

actual suspend fun readClipboard(): String? {
    return try {
        UIPasteboard.generalPasteboard.string
    } catch (e: Exception) {
        null
    }
}