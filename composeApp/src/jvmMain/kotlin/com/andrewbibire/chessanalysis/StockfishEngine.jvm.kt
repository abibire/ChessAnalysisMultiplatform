package com.andrewbibire.chessanalysis

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date

actual class StockfishEngine actual constructor(context: Any?) {
    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null
    private var executableFile: File? = null

    companion object {
        private val logFile: File by lazy {
            val userHome = System.getProperty("user.home")
            File(userHome, "ChessAnalysis-Stockfish-Debug.log").also {
                it.writeText("=== Chess Analysis Stockfish Debug Log ===\n")
                it.appendText("Started: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())}\n\n")
            }
        }

        private fun log(message: String) {
            println(message) // Still print to console
            try {
                logFile.appendText("$message\n")
            } catch (e: Exception) {
                // Ignore logging errors
            }
        }
    }

    actual suspend fun evaluatePosition(fen: String, depth: Int): EngineResult = withContext(Dispatchers.IO) {
        try {
            log("JVM Stockfish: evaluatePosition called with fen=$fen, depth=$depth")
            initializeIfNeeded()

            writer?.write("position fen $fen\n")
            writer?.write("go depth $depth\n")
            writer?.flush()
            log("JVM Stockfish: Commands sent to engine")

            var lastScore: String? = null
            var bestMove: String? = null

            while (true) {
                val line = reader?.readLine()?.trim() ?: break
                log("JVM Stockfish: $line")

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

            log("JVM Stockfish: Evaluation complete - score=$lastScore, bestMove=$bestMove")
            EngineResult(lastScore ?: "0.00", bestMove)
        } catch (e: Exception) {
            log("JVM Stockfish ERROR: ${e.message}")
            log("Stack trace: ${e.stackTraceToString()}")
            throw e
        }
    }

    actual suspend fun evaluateWithMultiPV(fen: String, depth: Int, numLines: Int): EngineResult = withContext(Dispatchers.IO) {
        try {
            log("JVM Stockfish: evaluateWithMultiPV called with fen=$fen, depth=$depth, numLines=$numLines")
            initializeIfNeeded()

            writer?.write("setoption name MultiPV value $numLines\n")
            writer?.write("position fen $fen\n")
            writer?.write("go depth $depth\n")
            writer?.flush()
            log("JVM Stockfish: Multi-PV commands sent to engine")

            val pvLines = mutableMapOf<Int, PVLine>()
            var lastScore: String? = null
            var bestMove: String? = null

            while (true) {
                val line = reader?.readLine()?.trim() ?: break
                log("JVM Stockfish: $line")

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

            log("JVM Stockfish: Multi-PV evaluation complete - ${alternativeLines.size} lines")
            EngineResult(lastScore ?: "0.00", bestMove, alternativeLines)
        } catch (e: Exception) {
            log("JVM Stockfish ERROR: ${e.message}")
            log("Stack trace: ${e.stackTraceToString()}")
            throw e
        }
    }

    private fun initializeIfNeeded() {
        if (process == null) {
            log("JVM Stockfish: Initializing engine...")
            val stockfishBinary = extractStockfishBinary()
            log("JVM Stockfish: Binary extracted to ${stockfishBinary.absolutePath}")
            log("JVM Stockfish: Binary exists: ${stockfishBinary.exists()}, size: ${stockfishBinary.length()} bytes")

            // Make sure it's executable on Unix-like systems
            if (!System.getProperty("os.name").lowercase().contains("windows")) {
                stockfishBinary.setExecutable(true)
                log("JVM Stockfish: Made binary executable")
            }

            log("JVM Stockfish: Starting process...")
            process = ProcessBuilder(stockfishBinary.absolutePath)
                .redirectErrorStream(true)  // Merge stderr into stdout to prevent buffer deadlock
                .start()
            writer = BufferedWriter(OutputStreamWriter(process!!.outputStream, Charsets.UTF_8))
            reader = BufferedReader(InputStreamReader(process!!.inputStream, Charsets.UTF_8))
            log("JVM Stockfish: Process started, sending UCI command")

            writer?.write("uci\n")
            writer?.flush()

            while (true) {
                val line = reader?.readLine()?.trim() ?: break
                log("JVM Stockfish UCI: $line")
                if (line.contains("uciok")) {
                    log("JVM Stockfish: UCI initialization complete")
                    break
                }
            }
        }
    }

    private fun extractStockfishBinary(): File {
        val (resourcePath, fileName) = getStockfishResourcePath()
        log("JVM Stockfish: Looking for resource at: $resourcePath")

        // Create a temp directory for the executable
        val tempDir = File(System.getProperty("java.io.tmpdir"), "stockfish-jvm")
        tempDir.mkdirs()
        log("JVM Stockfish: Temp directory: ${tempDir.absolutePath}")

        val executableFile = File(tempDir, fileName)

        // Extract if not already present or if file size is different
        val resourceStream = this::class.java.getResourceAsStream("/$resourcePath")
            ?: throw IllegalStateException("Stockfish binary not found in resources: /$resourcePath")

        log("JVM Stockfish: Found resource, extracting to ${executableFile.absolutePath}")
        resourceStream.use { input ->
            executableFile.outputStream().use { output ->
                val bytes = input.copyTo(output)
                log("JVM Stockfish: Extracted $bytes bytes")
            }
        }

        this.executableFile = executableFile
        return executableFile
    }

    private fun getStockfishResourcePath(): Pair<String, String> {
        val osName = System.getProperty("os.name").lowercase()
        val osArch = System.getProperty("os.arch").lowercase()

        log("=== JVM Stockfish Platform Detection ===")
        log("os.name (raw): ${System.getProperty("os.name")}")
        log("os.arch (raw): ${System.getProperty("os.arch")}")
        log("os.name (lowercase): $osName")
        log("os.arch (lowercase): $osArch")

        // Check Windows processor architecture from environment
        val processorArch = System.getenv("PROCESSOR_ARCHITECTURE")?.lowercase()
        val processorArchW6432 = System.getenv("PROCESSOR_ARCHITEW6432")?.lowercase()
        log("PROCESSOR_ARCHITECTURE: $processorArch")
        log("PROCESSOR_ARCHITEW6432: $processorArchW6432")

        val result = when {
            osName.contains("mac") || osName.contains("darwin") -> {
                when {
                    osArch.contains("aarch64") || osArch.contains("arm") -> {
                        log("Detected: macOS ARM64")
                        Pair("stockfish/macos-aarch64/stockfish", "stockfish")
                    }
                    else -> {
                        log("Detected: macOS x86-64")
                        Pair("stockfish/macos-x86-64/stockfish", "stockfish")
                    }
                }
            }
            osName.contains("windows") -> {
                // Windows ARM detection: check PROCESSOR_ARCHITECTURE environment variable
                // On Windows ARM, this will be "arm64" even when running x64 emulated apps
                val isWindowsArm = processorArch?.contains("arm") == true ||
                                   processorArchW6432?.contains("arm") == true

                when {
                    isWindowsArm -> {
                        log("Detected: Windows ARM64 (via PROCESSOR_ARCHITECTURE=$processorArch)")
                        Pair("stockfish/windows-aarch64/stockfish.exe", "stockfish.exe")
                    }
                    osArch.contains("aarch64") || osArch.contains("arm") -> {
                        log("Detected: Windows ARM64 (via os.arch)")
                        Pair("stockfish/windows-aarch64/stockfish.exe", "stockfish.exe")
                    }
                    else -> {
                        log("Detected: Windows x86-64")
                        Pair("stockfish/windows-x86-64/stockfish.exe", "stockfish.exe")
                    }
                }
            }
            osName.contains("linux") -> {
                log("Detected: Linux x86-64")
                Pair("stockfish/linux-x86-64/stockfish", "stockfish")
            }
            else -> {
                log("ERROR: Unsupported operating system")
                throw IllegalStateException("Unsupported operating system: $osName")
            }
        }

        log("Selected resource path: ${result.first}")
        log("======================================")
        return result
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
            // Ignore
        }

        writer?.close()
        reader?.close()
        process?.destroy()
        process = null

        // Clean up temp file
        executableFile?.delete()
    }
}
