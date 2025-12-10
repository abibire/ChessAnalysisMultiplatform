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

            println("INFO: Starting stockfish process from: ${stockfishBinary.absolutePath}")
            try {
                process = ProcessBuilder(stockfishBinary.absolutePath)
                    .redirectErrorStream(true)
                    .start()
                println("INFO: Process started successfully")
            } catch (e: Exception) {
                println("ERROR: Failed to start stockfish process: ${e.javaClass.name}: ${e.message}")
                e.printStackTrace()
                throw IllegalStateException("Cannot start stockfish: ${e.message}", e)
            }
            writer = BufferedWriter(OutputStreamWriter(process!!.outputStream, Charsets.UTF_8))
            reader = BufferedReader(InputStreamReader(process!!.inputStream, Charsets.UTF_8))

            println("INFO: Sending 'uci' command to stockfish")
            writer?.write("uci\n")
            writer?.flush()

            while (true) {
                val line = reader?.readLine()?.trim() ?: break
                println("STOCKFISH: $line")
                if (line.contains("uciok")) {
                    println("INFO: Stockfish initialized successfully")
                    break
                }
            }
        }
    }

    private fun extractStockfishBinary(): File {
        // On macOS, try to find stockfish in the app bundle first (for App Store builds)
        val osName = System.getProperty("os.name").lowercase()
        if (osName.contains("mac") || osName.contains("darwin")) {
            val bundledBinary = findStockfishInAppBundle()
            if (bundledBinary != null && bundledBinary.exists()) {
                println("INFO: Using bundled stockfish binary at: ${bundledBinary.absolutePath}")
                this.executableFile = bundledBinary
                return bundledBinary
            } else {
                println("INFO: Bundled stockfish not found, falling back to JAR extraction")
            }
        }

        // Fallback: Extract from JAR resources to temp directory
        // (used by Windows, Linux, and macOS DMG builds)
        val (resourcePath, fileName) = getStockfishResourcePath()
        println("INFO: Extracting stockfish from JAR resource: $resourcePath")

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

    /**
     * Attempts to find the stockfish binary in the macOS app bundle.
     * This is used for App Store builds where the binary is bundled with the app.
     * @return The stockfish binary file if found in the app bundle, null otherwise
     */
    private fun findStockfishInAppBundle(): File? {
        return try {
            // Get the path to the currently running JAR/class file
            val jarPath = this::class.java.protectionDomain.codeSource.location.toURI().path
            val jarFile = File(jarPath)

            // Navigate up from the JAR to find the app bundle's Contents directory
            // Typical structure: Game Review.app/Contents/app/ComposeApp.jar
            var current: File? = jarFile.parentFile
            var contentsDir: File? = null

            // Search up the directory tree for the Contents directory (max 5 levels)
            for (i in 0..5) {
                if (current == null) break
                if (current.name == "Contents" && current.parentFile?.name?.endsWith(".app") == true) {
                    contentsDir = current
                    break
                }
                current = current.parentFile
            }

            if (contentsDir == null) return null

            // Determine which architecture binary to use
            val osArch = System.getProperty("os.arch").lowercase()
            val binaryName = when {
                osArch.contains("aarch64") || osArch.contains("arm") -> "stockfish-aarch64"
                else -> "stockfish-x86-64"
            }

            // Look for stockfish in Contents/MacOS/ (required for sandboxed apps)
            val macosDir = File(contentsDir, "MacOS")
            val stockfishBinary = File(macosDir, binaryName)

            if (stockfishBinary.exists()) {
                stockfishBinary
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
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
                when {
                    osArch.contains("aarch64") || osArch.contains("arm") ->
                        Pair("stockfish/linux-aarch64/stockfish", "stockfish")
                    else ->
                        Pair("stockfish/linux-x86-64/stockfish", "stockfish")
                }
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

        // Only delete temp files, NOT bundled binaries from app bundle
        executableFile?.let { file ->
            val tempDir = System.getProperty("java.io.tmpdir")
            if (file.absolutePath.startsWith(tempDir)) {
                file.delete()
            }
        }
    }
}
