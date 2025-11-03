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
[Round "-"]
[White "MemeBlunders"]
[Black "alimofatteh"]
[Result "1-0"]
[CurrentPosition "r1b3kr/p2p1Q1p/1p1Pp1p1/1Bp5/4P2n/8/2PNK1P1/5R2 b - - 2 25"]
[Timezone "UTC"]
[ECO "B10"]
[ECOUrl "https://www.chess.com/openings/Caro-Kann-Defense-2.Nf3"]
[UTCDate "2025.11.03"]
[UTCTime "01:11:08"]
[WhiteElo "1118"]
[BlackElo "1102"]
[TimeControl "60"]
[Termination "MemeBlunders won by checkmate"]
[StartTime "01:11:08"]
[EndDate "2025.11.03"]
[EndTime "01:12:32"]
[Link "https://www.chess.com/analysis/game/live/145049862930/analysis?move=45"]
[WhiteUrl "https://images.chesscomfiles.com/uploads/v1/user/68083330.ca9db885.50x50o.e8061c0a14f0.jpeg"]
[WhiteCountry "2"]
[WhiteTitle ""]
[BlackUrl "https://images.chesscomfiles.com/uploads/v1/user/187317603.77b24f15.50x50o.e91ccd42288c.png"]
[BlackCountry "5"]
[BlackTitle ""]

1. e4 c6 2. Nf3 Qb6 3. Nc3 g6 4. d4 Bg7 5. d5 c5 6. Bd2 Qxb2 7. Rc1 Bxc3 8. Bxc3
Qxc3+ 9. Nd2 Qa3 10. Qf3 Qxa2 11. Bc4 Qb2 12. d6 Qxc1+ 13. Ke2 e6 14. Rxc1 Nc6
15. Nb3 Ne5 16. Qc3 b6 17. Qxe5 f6 18. Qc3 Kf7 19. Bb5 Nh6 20. Nd2 Ng4 21. f3
Nxh2 22. Rh1 Nxf3 23. Rf1 Nh4 24. Qxf6+ Kg8 25. Qf7# 1-0
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
            println("STOCKFISH: All scores -> " +
                    positions.mapIndexed { i, p -> "Move $i: ${p.score}" }.joinToString(", ")
            )
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
            val rawScore = positions[currentIndex].score
            val isLast = currentIndex == positions.lastIndex
            val displayScore = if (isLast && (rawScore == null || rawScore == "0.00" || rawScore == "0")) {
                when (resultTag) {
                    "1-0" -> "White wins"
                    "0-1" -> "Black wins"
                    "1/2-1/2" -> "Draw"
                    else -> "Game over"
                }
            } else {
                rawScore ?: "N/A"
            }
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
