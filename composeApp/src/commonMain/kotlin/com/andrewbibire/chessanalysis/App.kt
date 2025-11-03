package com.andrewbibire.chessanalysis

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlin.math.abs

@Composable
fun App(context: Any? = null) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ChessAnalysisApp(context)
        }
    }
}

@Composable
fun ChessAnalysisApp(context: Any?) {
    val samplePgn = """
        [Event "WCC-25"]
[Site "Moscow"]
[Date "1985.10.05"]
[Round "15"]
[White "Karpov, Anatoly"]
[Black "Kasparov, Garry"]
[Result "1/2-1/2"]
[ECO "D44"]

1. d4 Nf6 2. c4 e6 3. Nf3 d5 4. Nc3 c6 5. Bg5 dxc4 6. e4 b5 7. e5 h6 8. Bh4 g5 9. Nxg5 hxg5 10. Bxg5 Nbd7 11. g3 Qa5 12. exf6 Bb7 13. Bg2 O-O-O 14. O-O c5 15. d5 b4 16. a3 bxc3 17. bxc3 exd5 18. Qg4 Qc7 19. Rab1 Bd6 20. Rfd1 Kb8 21. Bxd5 Ne5 22. Qe2 Rhe8 23. Rxb7+ Qxb7 24. Bxb7 Kxb7 25. Rb1+ Kc7 26. Qe4 Rb8 27. Qd5 Rxb1+ 28. Kg2 Rb2 29. Kh3 Rb1 30. Kg2 Rb2 31. Kh3 Rb1 32. Kg2 1/2-1/2
    """.trimIndent()

    val positions = remember { generateFensFromPgn(samplePgn) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isEvaluating by remember { mutableStateOf(false) }

    val resultTag = remember(samplePgn) {
        val match = Regex("""\[Result\s+"([^"]+)"""").find(samplePgn)
        match?.groupValues?.getOrNull(1) ?: "*"
    }

    val stockfishEngine = remember(context) {
        if (context != null) createStockfishEngine(context) else null
    }

    LaunchedEffect(context) {
        if (stockfishEngine != null && positions.isNotEmpty()) {
            isEvaluating = true
            delay(2000)
            withContext(Dispatchers.Default) {
                for ((index, position) in positions.withIndex()) {
                    val eval = stockfishEngine.evaluatePosition(position.fenString, depth = 5)
                    position.score = eval
                    println("STOCKFISH: Move #$index -> FEN: ${position.fenString}")
                    println("STOCKFISH: Eval: $eval")
                }
            }
            println("STOCKFISH: All evaluations complete")
            println(
                "STOCKFISH: All scores -> " +
                        positions.mapIndexed { i, p -> "Move $i: ${p.score}" }.joinToString(", ")
            )
            isEvaluating = false
        }
    }

    fun normalizeScoreForDisplay(raw: String?, isLast: Boolean): String {
        val gameResultOverride: String? = when (resultTag) {
            "1-0" -> "White wins"
            "0-1" -> "Black wins"
            "1/2-1/2" -> "Draw"
            else -> null
        }

        if (isLast) {
            if (gameResultOverride != null) {
                return gameResultOverride
            }
        }

        if (raw == null) return "N/A"

        val lower = raw.lowercase()

        return if (lower.startsWith("mate")) {
            val digits = Regex("""-?\d+""").find(lower)?.value
            if (digits != null) {
                val n = digits.toIntOrNull()
                if (n != null) {
                    "mate ${abs(n)}"
                } else {
                    raw
                }
            } else {
                raw
            }
        } else {
            raw
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Move: $currentIndex",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Chessboard(fen = positions[currentIndex].fenString)
        Spacer(modifier = Modifier.height(16.dp))
        if (isEvaluating) {
            CircularProgressIndicator()
        } else {
            val isLast = currentIndex == positions.lastIndex
            val displayScore = normalizeScoreForDisplay(
                positions[currentIndex].score,
                isLast
            )
            Text(
                text = "Score: $displayScore",
                style = MaterialTheme.typography.titleLarge
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { if (currentIndex > 0) currentIndex-- },
                enabled = currentIndex > 0
            ) {
                Text("Back")
            }
            Button(
                onClick = { if (currentIndex < positions.size - 1) currentIndex++ },
                enabled = currentIndex < positions.size - 1
            ) {
                Text("Forward")
            }
        }
    }
}

expect fun createStockfishEngine(context: Any?): StockfishEngine
