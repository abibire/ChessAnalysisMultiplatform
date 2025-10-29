package com.andrewbibire.chessanalysis

expect class StockfishEngine {
    suspend fun evaluatePosition(fen: String, depth: Int): String
    fun close()
}