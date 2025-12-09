package com.andrewbibire.chessanalysis.domain

import com.andrewbibire.chessanalysis.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Utility class for analyzing chess games
 * Wraps Stockfish engine and provides analysis operations
 * No state management - pure functions only
 */
class GameAnalyzer(private val stockfishEngine: StockfishEngine) {

    /**
     * Analyze all positions in a game
     * @param positions The positions to analyze (will be modified with scores)
     * @param depth Analysis depth for Stockfish
     * @param onProgress Called with (currentIndex, totalMoves) for each position analyzed
     * @return true if analysis completed successfully, false if cancelled
     */
    suspend fun analyzeGame(
        positions: List<Position>,
        depth: Int,
        onProgress: ((Int, Int) -> Unit)? = null
    ): Boolean {
        if (positions.isEmpty()) return false

        return try {
            withContext(Dispatchers.Default) {
                for ((index, position) in positions.withIndex()) {
                    if (!coroutineContext.isActive) return@withContext false

                    // Analyze this position
                    val result = stockfishEngine.evaluatePosition(
                        position.fenString,
                        depth = depth
                    )
                    position.score = result.score
                    position.bestMove = result.bestMove

                    // Report progress
                    onProgress?.invoke(index + 1, positions.size)
                }

                if (coroutineContext.isActive) {
                    // Classify all positions after analysis
                    classifyPositions(positions)
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Analyze a single position
     * @return The evaluation result or null if failed
     */
    suspend fun analyzePosition(
        fen: String,
        depth: Int
    ): EngineResult? {
        return try {
            stockfishEngine.evaluatePosition(fen, depth)
        } catch (e: Exception) {
            println("ERROR: Failed to analyze position: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Get alternative lines for a position
     * @param fen The position to analyze
     * @param depth Analysis depth
     * @param numLines Number of alternative lines to get (default 3)
     * @return List of alternative lines or empty list if failed
     */
    suspend fun getAlternativeLines(
        fen: String,
        depth: Int,
        numLines: Int = 3
    ): List<PVLine> {
        return try {
            val result = stockfishEngine.evaluateWithMultiPV(fen, depth, numLines)
            result.alternativeLines
        } catch (e: Exception) {
            emptyList()
        }
    }
}
