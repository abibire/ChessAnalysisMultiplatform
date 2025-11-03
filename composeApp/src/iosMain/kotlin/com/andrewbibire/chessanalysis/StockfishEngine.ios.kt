package com.andrewbibire.chessanalysis

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import stockfish.*

@OptIn(ExperimentalForeignApi::class)
actual class StockfishEngine actual constructor(context: Any?) {

    init {
        println("KOTLIN[iOS]: StockfishEngine init -> stockfish_init()")
        stockfish_init()
    }

    actual suspend fun evaluatePosition(fen: String, depth: Int): EngineResult {
        println("KOTLIN[iOS]: evaluatePosition($fen, depth=$depth)")
        val resultPtr = stockfish_evaluate(fen, depth)
        val resultStr = resultPtr?.toKString() ?: "0.00|"
        println("KOTLIN[iOS]: result <- $resultStr")

        val parts = resultStr.split("|")
        val score = parts.getOrNull(0) ?: "0.00"
        val bestMove = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }

        return EngineResult(score, bestMove)
    }

    actual fun close() {
        println("KOTLIN[iOS]: close() -> stockfish_cleanup()")
        stockfish_cleanup()
    }
}