package com.andrewbibire.chessanalysis

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString

@OptIn(ExperimentalForeignApi::class)
private external fun stockfish_init()

@OptIn(ExperimentalForeignApi::class)
private external fun stockfish_evaluate(
    fen: CPointer<ByteVar>,
    depth: Int
): CPointer<ByteVar>?

@OptIn(ExperimentalForeignApi::class)
private external fun stockfish_cleanup()

@OptIn(ExperimentalForeignApi::class)
actual class StockfishEngine actual constructor(context: Any?) {

    init {
        println("KOTLIN[iOS]: StockfishEngine init -> stockfish_init()")
        stockfish_init()
    }

    actual suspend fun evaluatePosition(fen: String, depth: Int): String {
        println("KOTLIN[iOS]: evaluatePosition($fen, depth=$depth)")
        memScoped {
            val fenC = fen.cstr
            val ptr = stockfish_evaluate(fenC.ptr, depth)
            val result = ptr?.toKString() ?: "bestmove 0000 eval cp 0"
            println("KOTLIN[iOS]: result <- $result")
            return result
        }
    }

    actual fun close() {
        println("KOTLIN[iOS]: close() -> stockfish_cleanup()")
        stockfish_cleanup()
    }
}
