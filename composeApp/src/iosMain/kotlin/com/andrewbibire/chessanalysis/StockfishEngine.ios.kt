package com.andrewbibire.chessanalysis

import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
actual class StockfishEngine actual constructor(context: Any?) {

    private val handle: COpaquePointer? = dlopen(null, RTLD_LAZY)

    private val initFunc: CPointer<CFunction<() -> Unit>>? =
        dlsym(handle, "stockfish_init")?.reinterpret()

    private val evalFunc: CPointer<CFunction<(CPointer<ByteVarOf<Byte>>, Int) -> CPointer<ByteVarOf<Byte>>>>? =
        dlsym(handle, "stockfish_evaluate")?.reinterpret()

    private val cleanupFunc: CPointer<CFunction<() -> Unit>>? =
        dlsym(handle, "stockfish_cleanup")?.reinterpret()

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
        if (handle != null) {
            dlclose(handle)
        }
    }

    private fun callStockfishInit() {
        val f = initFunc
        if (f == null) {
            println("KOTLIN: ERROR - could not find stockfish_init")
            return
        }
        f.invoke()
    }

    private fun callStockfishEvaluate(fen: String, depth: Int): String {
        val f = evalFunc
        if (f == null) {
            println("KOTLIN: ERROR - could not find stockfish_evaluate")
            return "N/A (Function Not Found)"
        }

        return memScoped {
            val fenCString = fen.cstr.ptr
            val resultPtr = f.invoke(fenCString, depth)
            resultPtr.toKString()
        }
    }

    private fun callStockfishCleanup() {
        val f = cleanupFunc
        if (f == null) {
            return
        }
        f.invoke()
    }
}
