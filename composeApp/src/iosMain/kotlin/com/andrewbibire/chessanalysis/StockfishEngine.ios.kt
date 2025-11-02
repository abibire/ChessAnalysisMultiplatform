package com.andrewbibire.chessanalysis

import kotlinx.cinterop.ExperimentalForeignApi
import stockfish.*

@OptIn(ExperimentalForeignApi::class)
actual class StockfishEngine actual constructor(context: Any?) {

    init {
        println("KOTLIN[iOS]: StockfishEngine init -> stockfish_init()")
        stockfish_init()
    }

    actual suspend fun evaluatePosition(fen: String, depth: Int): String {
        println("KOTLIN[iOS]: evaluatePosition($fen, depth=$depth)")
        val result = (stockfish_evaluate(fen, depth) as? String) ?: "bestmove 0000 eval cp 0"
        println("KOTLIN[iOS]: result <- $result")
        return result
    }

    actual fun close() {
        println("KOTLIN[iOS]: close() -> stockfish_cleanup()")
        stockfish_cleanup()
    }
}