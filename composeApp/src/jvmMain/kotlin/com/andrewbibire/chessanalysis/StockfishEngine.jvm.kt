package com.andrewbibire.chessanalysis

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

actual class StockfishEngine actual constructor(context: Any?) {
    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null
    private var executableFile: File? = null

    actual suspend fun evaluatePosition(fen: String, depth: Int): EngineResult = withContext(Dispatchers.IO) {
        try {
            initializeIfNeeded()

            writer?.write("position fen $fen\n")
            writer?.write("go depth $depth\n")
            writer?.flush()

            var lastScore: String? = null
            var bestMove: String? = null

            while (true) {
                val line = reader?.readLine()?.trim() ?: break

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
        } catch (e: Exception) {
            throw e
        }
    }

    actual suspend fun evaluateWithMultiPV(fen: String, depth: Int, numLines: Int): EngineResult = withContext(Dispatchers.IO) {
        try {
            initializeIfNeeded()

            writer?.write("setoption name MultiPV value $numLines\n")
            writer?.write("position fen $fen\n")
            writer?.write("go depth $depth\n")
            writer?.flush()

            val pvLines = mutableMapOf<Int, PVLine>()
            var lastScore: String? = null
            var bestMove: String? = null

            while (true) {
                val line = reader?.readLine()?.trim() ?: break

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

            writer?.write("setoption name MultiPV value 1\n")
            writer?.flush()

            val alternativeLines = pvLines.values.sortedBy {
                pvLines.entries.find { entry -> entry.value == it }?.key ?: Int.MAX_VALUE
            }

            EngineResult(lastScore ?: "0.00", bestMove, alternativeLines)
        } catch (e: Exception) {
            throw e
        }
    }

    private fun initializeIfNeeded() {
        if (process == null) {
            val stockfishBinary = extractStockfishBinary()

            if (!System.getProperty("os.name").lowercase().contains("windows")) {
                stockfishBinary.setExecutable(true)
            }

            process = ProcessBuilder(stockfishBinary.absolutePath)
                .redirectErrorStream(true)
                .start()
            writer = BufferedWriter(OutputStreamWriter(process!!.outputStream, Charsets.UTF_8))
            reader = BufferedReader(InputStreamReader(process!!.inputStream, Charsets.UTF_8))

            writer?.write("uci\n")
            writer?.flush()

            while (true) {
                val line = reader?.readLine()?.trim() ?: break
                if (line.contains("uciok")) {
                    break
                }
            }
        }
    }

    private fun extractStockfishBinary(): File {
        val (resourcePath, fileName) = getStockfishResourcePath()

        val tempDir = File(System.getProperty("java.io.tmpdir"), "stockfish-jvm")
        tempDir.mkdirs()

        val executableFile = File(tempDir, fileName)

        val resourceStream = this::class.java.getResourceAsStream("/$resourcePath")
            ?: throw IllegalStateException("Stockfish binary not found in resources: /$resourcePath")

        resourceStream.use { input ->
            executableFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        this.executableFile = executableFile
        return executableFile
    }

    private fun detectWindowsArchitecture(): String? {
        return try {
            val process = ProcessBuilder("cmd.exe", "/c", "systeminfo | findstr /B /C:\"System Type\"")
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()

            when {
                output.contains("ARM64", ignoreCase = true) -> "arm64"
                output.contains("x64", ignoreCase = true) -> "x64"
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getStockfishResourcePath(): Pair<String, String> {
        val osName = System.getProperty("os.name").lowercase()
        val osArch = System.getProperty("os.arch").lowercase()

        return when {
            osName.contains("mac") || osName.contains("darwin") -> {
                when {
                    osArch.contains("aarch64") || osArch.contains("arm") ->
                        Pair("stockfish/macos-aarch64/stockfish", "stockfish")
                    else ->
                        Pair("stockfish/macos-x86-64/stockfish", "stockfish")
                }
            }
            osName.contains("windows") -> {
                val processorArch = System.getenv("PROCESSOR_ARCHITECTURE")?.lowercase()
                val processorArchW6432 = System.getenv("PROCESSOR_ARCHITEW6432")?.lowercase()
                val systemArch = detectWindowsArchitecture()

                val isWindowsArm = processorArch?.contains("arm") == true ||
                                   processorArchW6432?.contains("arm") == true ||
                                   osArch.contains("aarch64") ||
                                   osArch.contains("arm") ||
                                   systemArch == "arm64"

                when {
                    isWindowsArm -> Pair("stockfish/windows-aarch64/stockfish.exe", "stockfish.exe")
                    else -> Pair("stockfish/windows-x86-64/stockfish.exe", "stockfish.exe")
                }
            }
            osName.contains("linux") -> {
                Pair("stockfish/linux-x86-64/stockfish", "stockfish")
            }
            else -> throw IllegalStateException("Unsupported operating system: $osName")
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
        try {
            writer?.write("quit\n")
            writer?.flush()
        } catch (e: Exception) {
        }

        writer?.close()
        reader?.close()
        process?.destroy()
        process = null

        executableFile?.delete()
    }
}
