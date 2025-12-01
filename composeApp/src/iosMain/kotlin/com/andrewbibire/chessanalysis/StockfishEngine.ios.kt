package com.andrewbibire.chessanalysis

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import stockfish.*

@OptIn(ExperimentalForeignApi::class)
actual class StockfishEngine actual constructor(context: Any?) {

    init {
        stockfish_init()
    }

    actual suspend fun evaluatePosition(fen: String, depth: Int): EngineResult {
        val resultPtr = stockfish_evaluate(fen, depth)
        val resultStr = resultPtr?.toKString() ?: "0.00|"

        val parts = resultStr.split("|")
        val score = parts.getOrNull(0) ?: "0.00"
        val bestMove = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }

        return EngineResult(score, bestMove)
    }

    actual suspend fun evaluateWithMultiPV(fen: String, depth: Int, numLines: Int): EngineResult {
        val resultPtr = stockfish_evaluate_multipv(fen, depth, numLines)
        val resultStr = resultPtr?.toKString() ?: "0.00||"

        // Format: score|bestMove||line1Score:line1Move:pv1,pv2,pv3|line2Score:line2Move:pv1,pv2,pv3|...
        val mainParts = resultStr.split("||", limit = 2)
        val basicParts = mainParts[0].split("|")
        val score = basicParts.getOrNull(0) ?: "0.00"
        val bestMove = basicParts.getOrNull(1)?.takeIf { it.isNotEmpty() }

        val alternativeLines = try {
            if (mainParts.size > 1 && mainParts[1].isNotEmpty()) {
                mainParts[1].split("|").mapNotNull { lineStr ->
                    if (lineStr.isEmpty() || lineStr.isBlank()) return@mapNotNull null

                    try {
                        val lineParts = lineStr.split(":", limit = 3)
                        if (lineParts.isEmpty()) return@mapNotNull null

                        val lineScore = lineParts.getOrNull(0)?.takeIf { it.isNotEmpty() } ?: "0.00"
                        val lineMove = lineParts.getOrNull(1)?.takeIf { it.isNotEmpty() }
                        val pv = if (lineParts.size > 2 && lineParts[2].isNotEmpty()) {
                            lineParts[2].split(",").filter { it.isNotEmpty() && it.isNotBlank() }
                        } else {
                            emptyList()
                        }

                        PVLine(lineScore, lineMove, pv)
                    } catch (e: Exception) {
                        null
                    }
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }

        return EngineResult(score, bestMove, alternativeLines)
    }

    actual fun close() {
        stockfish_cleanup()
    }
}