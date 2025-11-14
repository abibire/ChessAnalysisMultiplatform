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
        println("ANDROID Stockfish: evaluatePosition($fen, depth=$depth)")
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

        println("ANDROID Stockfish: Evaluation complete - score=$lastScore, bestMove=$bestMove")
        EngineResult(lastScore ?: "0.00", bestMove)
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

    actual fun close() {
        writer?.close()
        reader?.close()
        process?.destroy()
        process = null
    }
}