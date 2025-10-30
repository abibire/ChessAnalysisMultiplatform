package com.andrewbibire.chessanalysis

expect class StockfishEngine(context: Any?) {
    suspend fun evaluatePosition(fen: String, depth: Int): String
    fun close()
}