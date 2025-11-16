package com.andrewbibire.chessanalysis

data class PVLine(
    val score: String,
    val move: String?,
    val pv: List<String> = emptyList()
)

data class EngineResult(
    val score: String,
    val bestMove: String?,
    val alternativeLines: List<PVLine> = emptyList()
)

expect class StockfishEngine(context: Any?) {
    suspend fun evaluatePosition(fen: String, depth: Int): EngineResult
    suspend fun evaluateWithMultiPV(fen: String, depth: Int, numLines: Int): EngineResult
    fun close()
}