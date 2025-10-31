package com.andrewbibire.chessanalysis

import com.andrewbibire.chessanalysis.native.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString

@OptIn(ExperimentalForeignApi::class)
actual class StockfishEngine actual constructor(context: Any?) {

    init {
        stockfish_init()
    }

    actual suspend fun evaluatePosition(fen: String, depth: Int): String {
        val result = stockfish_evaluate(fen, depth)
        return result?.toKString() ?: "N/A"
    }

    actual fun close() {
        stockfish_cleanup()
    }
}