package com.andrewbibire.chessanalysis

import kotlinx.cinterop.*
import platform.posix.*

actual class StockfishEngine actual constructor(context: Any?) {

    init {
        println("KOTLIN: StockfishEngine iOS initializing")
        callStockfishInit()
    }

    actual suspend fun evaluatePosition(fen: String, depth: Int): String {
        println("KOTLIN: evaluatePosition called for: $fen")
        return callStockfishEvaluate(fen, depth)
    }

    actual fun close() {
        println("KOTLIN: close called")
        callStockfishCleanup()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun callStockfishInit() {
        // Load the function from the main bundle
        val handle = dlopen(null, RTLD_LAZY)
        val funcPtr = dlsym(handle, "stockfish_init")
        if (funcPtr != null) {
            val func = funcPtr.reinterpret<CFunction<() -> Unit>>()
            func()
        } else {
            println("KOTLIN: ERROR - could not find stockfish_init")
        }
        dlclose(handle)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun callStockfishEvaluate(fen: String, depth: Int): String {
        val handle = dlopen(null, RTLD_LAZY)
        val funcPtr = dlsym(handle, "stockfish_evaluate")

        return if (funcPtr != null) {
            memScoped {
                val fenCString = fen.cstr.ptr
                val func = funcPtr.reinterpret<CFunction<(CPointer<ByteVarOf<Byte>>, Int) -> CPointer<ByteVarOf<Byte>>>>()
                val result = func(fenCString, depth)
                result.toKString()
            }
        } else {
            println("KOTLIN: ERROR - could not find stockfish_evaluate")
            "N/A (Function Not Found)"
        }.also {
            dlclose(handle)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun callStockfishCleanup() {
        val handle = dlopen(null, RTLD_LAZY)
        val funcPtr = dlsym(handle, "stockfish_cleanup")
        if (funcPtr != null) {
            val func = funcPtr.reinterpret<CFunction<() -> Unit>>()
            func()
        }
        dlclose(handle)
    }
}