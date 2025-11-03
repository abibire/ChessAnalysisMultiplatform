package com.andrewbibire.chessanalysis

data class EngineResult(
    val score: String,
    val bestMove: String?
)

expect class StockfishEngine(context: Any?) {
    suspend fun evaluatePosition(fen: String, depth: Int): EngineResult
    fun close()
}