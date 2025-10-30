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

    actual suspend fun evaluatePosition(fen: String, depth: Int): String = withContext(Dispatchers.IO) {
        initializeIfNeeded()
        writer?.write("position fen $fen\n")
        writer?.write("go depth $depth\n")
        writer?.flush()

        var score = "N/A"
        while (true) {
            val line = reader?.readLine() ?: break
            if (line.startsWith("info depth") && line.contains("score")) {
                score = parseScore(line, fen)
            }
            if (line.startsWith("bestmove")) break
        }
        score
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

    private fun parseScore(line: String, fen: String): String {
        return when {
            line.contains("mate") -> {
                val moves = line.split("score mate")[1].trim().split(" ")[0].toInt()
                "Mate in $moves"
            }
            line.contains("cp") -> {
                val cp = line.split("score cp")[1].trim().split(" ")[0].toDouble()
                val score = cp / 100
                if (fen.contains(" b ")) (-score).toString() else score.toString()
            }
            else -> "N/A"
        }
    }

    actual fun close() {
        writer?.close()
        reader?.close()
        process?.destroy()
        process = null
    }
}