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
import kotlin.math.roundToInt

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
        [Site "Chess.com"]
        [Date "2025.11.03"]
        [Round "?"]
        [White "SuperJJ04"]
        [Black "MemeBlunders"]
        [Result "1-0"]
        [TimeControl "60"]
        [WhiteElo "1123"]
        [BlackElo "1118"]
        [Termination "SuperJJ04 won by checkmate"]
        [Link "https://www.chess.com/game/145050889662"]

        1. e4 d5 2. exd5 Nf6 3. Qf3 Nxd5 4. Bc4 Nb6 5. Qxf7+ Kd7 6. Qe6+ Ke8 7. Qf7+ Kd7
        8. Be6+ Kc6 9. Bd5+ Nxd5 10. d3 Qd6 11. Qf3 Be6 12. c4 Qd7 13. cxd5+ Bxd5 14.
        Qg3 Qe6+ 15. Be3 g6 16. Nc3 Bh6 17. Nxd5 Qxd5 18. Bxh6 Qxd3 19. Qxd3 Rf8 20.
        Rc1+ Kb6 21. Qb3+ Ka6 22. Rxc7 Nc6 23. Qxb7+ Ka5 24. Rxc6 Ka4 25. Ra6# 1-0
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
                    val eval = stockfishEngine.evaluatePosition(position.fenString, depth = 14)
                    position.score = eval
                }
            }
            for (i in 1 until positions.size) {
                val prev = positions[i - 1]
                val cur = positions[i]
                val moveColour = if (isWhiteToMove(prev.fenString)) MoveColour.WHITE else MoveColour.BLACK
                val prevEval = parseEvaluationWhiteCentric(prev.score, prev.fenString)
                val curEval = parseEvaluationWhiteCentric(cur.score, cur.fenString)
                if (prevEval != null && curEval != null) {
                    cur.classification = classifyPointLoss(prevEval, curEval, moveColour)
                } else {
                    cur.classification = null
                }
            }
            isEvaluating = false
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
                positions[currentIndex].fenString,
                isLast,
                resultTag
            )
            Text(
                text = "Score: $displayScore",
                style = MaterialTheme.typography.titleLarge
            )
            val cls = positions[currentIndex].classification
            if (cls != null) {
                Text(
                    text = "Move quality: $cls",
                    style = MaterialTheme.typography.titleMedium
                )
            }
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

fun normalizeScoreForDisplay(raw: String?, fen: String, isLast: Boolean, resultTag: String): String {
    val gameResultOverride: String? = when (resultTag) {
        "1-0" -> "White wins"
        "0-1" -> "Black wins"
        "1/2-1/2" -> "Draw"
        else -> null
    }
    if (isLast && gameResultOverride != null) {
        return gameResultOverride
    }
    if (raw == null) return "N/A"
    val lower = raw.lowercase()
    return if (lower.startsWith("mate")) {
        val digits = Regex("""-?\d+""").find(lower)?.value
        if (digits != null) {
            val movesToMate = digits.toIntOrNull()
            if (movesToMate != null) {
                "Mate in ${abs(movesToMate)}"
            } else {
                raw
            }
        } else {
            raw
        }
    } else {
        val score = raw.toDoubleOrNull()
        if (score != null) {
            val whiteToMove = isWhiteToMove(fen)
            val adjustedScore = if (whiteToMove) score else -score
            val roundedScore = (adjustedScore * 10.0).roundToInt() / 10.0
            roundedScore.toString()
        } else {
            raw
        }
    }
}

expect fun createStockfishEngine(context: Any?): StockfishEngine
