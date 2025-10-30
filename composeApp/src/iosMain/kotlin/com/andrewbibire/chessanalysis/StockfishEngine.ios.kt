package com.andrewbibire.chessanalysis

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

actual class StockfishEngine actual constructor(context: Any?) {
    actual suspend fun evaluatePosition(fen: String, depth: Int): String = withContext(Dispatchers.IO) {
        "N/A"
    }

    actual fun close() {
    }
}