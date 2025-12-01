package com.andrewbibire.chessanalysis

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class StockfishEngine actual constructor(context: Any?) {
    private val context: Context = context as Context
    private var process: Process? = null
    private var writer: java.io.BufferedWriter? = null
    private var reader: java.io.BufferedReader? = null

    actual suspend fun evaluatePosition(fen: String, depth: Int): EngineResult = withContext(Dispatchers.IO) {
        initializeIfNeeded()

        writer?.write("position fen $fen\n")
        writer?.write("go depth $depth\n")
        writer?.flush()

        var lastScore: String? = null
        var bestMove: String? = null

        while (true) {
            val line = reader?.readLine() ?: break

            if (line.startsWith("info depth") && line.contains("score")) {
                lastScore = parseScoreFromInfo(line)
            }

            if (line.startsWith("bestmove")) {
                val parts = line.split(" ")
                if (parts.size > 1) {
                    bestMove = parts[1]
                }
                break
            }
        }

        EngineResult(lastScore ?: "0.00", bestMove)
    }

    actual suspend fun evaluateWithMultiPV(fen: String, depth: Int, numLines: Int): EngineResult = withContext(Dispatchers.IO) {
        initializeIfNeeded()

        writer?.write("setoption name MultiPV value $numLines\n")
        writer?.write("position fen $fen\n")
        writer?.write("go depth $depth\n")
        writer?.flush()

        val pvLines = mutableMapOf<Int, PVLine>()
        var lastScore: String? = null
        var bestMove: String? = null

        while (true) {
            val line = reader?.readLine() ?: break

            if (line.startsWith("info depth") && line.contains("score") && line.contains("multipv")) {
                val multipvIndex = parseMultiPVIndex(line)
                val score = parseScoreFromInfo(line)
                val move = parseFirstMove(line)
                val pv = parsePV(line)

                pvLines[multipvIndex] = PVLine(score, move, pv)

                if (multipvIndex == 1) {
                    lastScore = score
                }
            }

            if (line.startsWith("bestmove")) {
                val parts = line.split(" ")
                if (parts.size > 1) {
                    bestMove = parts[1]
                }
                break
            }
        }

        // Reset MultiPV to 1 for future single-line evaluations
        writer?.write("setoption name MultiPV value 1\n")
        writer?.flush()

        val alternativeLines = pvLines.values.sortedBy {
            pvLines.entries.find { entry -> entry.value == it }?.key ?: Int.MAX_VALUE
        }

        EngineResult(lastScore ?: "0.00", bestMove, alternativeLines)
    }

    private fun initializeIfNeeded() {
        if (process == null) {
            val stockfishFile = File(context.applicationInfo.nativeLibraryDir, "libstockfish.so")
            process = ProcessBuilder(stockfishFile.absolutePath).start()
            writer = process!!.outputStream.bufferedWriter()
            reader = process!!.inputStream.bufferedReader()

            writer?.write("uci\n")
            writer?.flush()

            while (true) {
                val line = reader?.readLine() ?: break
                if (line.contains("uciok")) break
            }
        }
    }

    private fun parseScoreFromInfo(line: String): String {
        return when {
            line.contains("score mate") -> {
                val after = line.substringAfter("score mate").trim()
                val movesToken = after.split(" ")[0]
                val movesInt = movesToken.toIntOrNull()
                if (movesInt != null) {
                    "mate $movesInt"
                } else {
                    "mate 0"
                }
            }
            line.contains("score cp") -> {
                val after = line.substringAfter("score cp").trim()
                val cpToken = after.split(" ")[0]
                val cpInt = cpToken.toIntOrNull()
                if (cpInt != null) {
                    val pawns = cpInt.toDouble() / 100.0
                    String.format("%.2f", pawns)
                } else {
                    "0.00"
                }
            }
            else -> {
                "0.00"
            }
        }
    }

    private fun parseMultiPVIndex(line: String): Int {
        return if (line.contains("multipv")) {
            val after = line.substringAfter("multipv").trim()
            after.split(" ")[0].toIntOrNull() ?: 1
        } else {
            1
        }
    }

    private fun parseFirstMove(line: String): String? {
        return if (line.contains(" pv ")) {
            val after = line.substringAfter(" pv ").trim()
            after.split(" ").firstOrNull()
        } else {
            null
        }
    }

    private fun parsePV(line: String): List<String> {
        return if (line.contains(" pv ")) {
            val after = line.substringAfter(" pv ").trim()
            after.split(" ").filter { it.isNotEmpty() }
        } else {
            emptyList()
        }
    }

    actual fun close() {
        writer?.close()
        reader?.close()
        process?.destroy()
        process = null
    }
}