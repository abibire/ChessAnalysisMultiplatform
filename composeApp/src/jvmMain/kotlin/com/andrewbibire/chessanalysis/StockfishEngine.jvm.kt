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
            println("JVM Stockfish: evaluatePosition called with fen=$fen, depth=$depth")
            initializeIfNeeded()

            writer?.write("position fen $fen\n")
            writer?.write("go depth $depth\n")
            writer?.flush()
            println("JVM Stockfish: Commands sent to engine")

            var lastScore: String? = null
            var bestMove: String? = null

            while (true) {
                val line = reader?.readLine() ?: break
                println("JVM Stockfish: $line")

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

            println("JVM Stockfish: Evaluation complete - score=$lastScore, bestMove=$bestMove")
            EngineResult(lastScore ?: "0.00", bestMove)
        } catch (e: Exception) {
            println("JVM Stockfish ERROR: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private fun initializeIfNeeded() {
        if (process == null) {
            println("JVM Stockfish: Initializing engine...")
            val stockfishBinary = extractStockfishBinary()
            println("JVM Stockfish: Binary extracted to ${stockfishBinary.absolutePath}")

            // Make sure it's executable on Unix-like systems
            if (!System.getProperty("os.name").lowercase().contains("windows")) {
                stockfishBinary.setExecutable(true)
                println("JVM Stockfish: Made binary executable")
            }

            process = ProcessBuilder(stockfishBinary.absolutePath)
                .redirectErrorStream(true)  // Merge stderr into stdout to prevent buffer deadlock
                .start()
            writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))
            reader = BufferedReader(InputStreamReader(process!!.inputStream))
            println("JVM Stockfish: Process started, sending UCI command")

            writer?.write("uci\n")
            writer?.flush()

            while (true) {
                val line = reader?.readLine() ?: break
                println("JVM Stockfish UCI: $line")
                if (line.contains("uciok")) {
                    println("JVM Stockfish: UCI initialization complete")
                    break
                }
            }
        }
    }

    private fun extractStockfishBinary(): File {
        val (resourcePath, fileName) = getStockfishResourcePath()
        println("JVM Stockfish: Looking for resource at: $resourcePath")

        // Create a temp directory for the executable
        val tempDir = File(System.getProperty("java.io.tmpdir"), "stockfish-jvm")
        tempDir.mkdirs()
        println("JVM Stockfish: Temp directory: ${tempDir.absolutePath}")

        val executableFile = File(tempDir, fileName)

        // Extract if not already present or if file size is different
        val resourceStream = this::class.java.classLoader.getResourceAsStream(resourcePath)
            ?: throw IllegalStateException("Stockfish binary not found in resources: $resourcePath")

        println("JVM Stockfish: Found resource, extracting to ${executableFile.absolutePath}")
        resourceStream.use { input ->
            executableFile.outputStream().use { output ->
                val bytes = input.copyTo(output)
                println("JVM Stockfish: Extracted $bytes bytes")
            }
        }

        this.executableFile = executableFile
        return executableFile
    }

    private fun getStockfishResourcePath(): Pair<String, String> {
        val osName = System.getProperty("os.name").lowercase()
        val osArch = System.getProperty("os.arch").lowercase()
        println("JVM Stockfish: Detecting platform - OS: $osName, Arch: $osArch")

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
                Pair("stockfish/windows-x86-64/stockfish.exe", "stockfish.exe")
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
