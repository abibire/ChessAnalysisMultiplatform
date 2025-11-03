package com.andrewbibire.chessanalysis

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.bhlangonijr.chesslib.Board
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun App(context: Any? = null) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            ChessAnalysisApp(context)
        }
    }
}

@Composable
fun ChessAnalysisApp(context: Any?) {
    val pgn = """
        [Event "Live Chess"]
[Site "Chess.com"]
[Date "2025.11.02"]
[Round "-"]
[White "Tyler26-2011"]
[Black "MemeBlunders"]
[Result "1-0"]
[CurrentPosition "3k4/1Q2Q3/8/8/6PN/3K3P/P7/8 b - - 6 59"]
[Timezone "UTC"]
[ECO "D11"]
[ECOUrl "https://www.chess.com/openings/Slav-Defense-Modern-Line-3...Nf6"]
[UTCDate "2025.11.02"]
[UTCTime "18:22:48"]
[WhiteElo "1147"]
[BlackElo "1134"]
[TimeControl "60"]
[Termination "Tyler26-2011 won by checkmate"]
[StartTime "18:22:48"]
[EndDate "2025.11.02"]
[EndTime "18:25:06"]
[Link "https://www.chess.com/analysis/game/live/145039229878/analysis?move=116"]
[WhiteUrl "https://www.chess.com/bundles/web/images/noavatar_l.84a92436.gif"]
[WhiteCountry "129"]
[WhiteTitle ""]
[BlackUrl "https://images.chesscomfiles.com/uploads/v1/user/68083330.ca9db885.50x50o.e8061c0a14f0.jpeg"]
[BlackCountry "2"]
[BlackTitle ""]

1. d4 d5 2. c4 c6 3. Nf3 Nf6 4. Bf4 Bf5 $6 5. e3 e6 6. Nc3 Be7 7. cxd5 cxd5 8.
Bd3 $6 Bxd3 9. Qxd3 Nbd7 10. h3 O-O 11. O-O h6 12. Bh2 Kh8 13. Rfe1 Kg8 14. Nd2
Kh8 15. f3 Kg8 16. e4 Kh8 17. e5 Nh7 18. Rad1 a6 19. Rf1 b5 20. f4 b4 21. Ncb1 $6
Rb8 $6 22. b3 a5 23. Nf3 Kg8 24. Nbd2 Qc8 $6 25. Rc1 Qd8 $6 26. Rc2 Rc8 27. Rfc1
Rxc2 28. Rxc2 Qe8 $6 29. Kf2 $6 Qd8 $6 30. g4 $6 Qb8 $6 31. Nh4 $4 Rc8 $9 32. f5 $4
Rxc2 $1 33. Qxc2 Qb6 $9 34. fxe6 $4 Qxd4+ $9 35. Ke2 Nxe5 $4 36. exf7+ $9 Kxf7 $4 37.
Qf5+ $9 Kg8 $4 38. Qe6+ Kf8 39. Bxe5 Qe4+ $2 40. Nxe4 Nf6 41. Nxf6 $9 Bxf6 $6 42. Bxf6
$9 gxf6 43. Qxf6+ Ke8 44. Qxh6 Kd7 45. Qd2 Kc6 46. Kd3 Kc5 $6 47. Qg5 a4 48. bxa4
b3 49. Qe5 b2 50. Qxb2 Kd6 51. a5 Ke6 52. a6 Kd6 53. a7 Ke6 54. a8=Q Kd6 55.
Qbb7 Ke6 56. Qxd5+ Ke7 57. Qe5+ Kd7 58. Qb7+ Kd8 59. Qee7# 1-0
    """.trimIndent()

    val positions = remember { generateFensFromPgn(pgn) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isEvaluating by remember { mutableStateOf(false) }

    val stockfishEngine = remember(context) {
        if (context != null) createStockfishEngine(context) else null
    }

    LaunchedEffect(context) {
        OpeningBook.init()
        if (stockfishEngine != null && positions.isNotEmpty()) {
            isEvaluating = true
            delay(2000)
            withContext(Dispatchers.Default) {
                for (position in positions) {
                    val result = stockfishEngine.evaluatePosition(position.fenString, depth = 14)
                    position.score = result.score
                    position.bestMove = result.bestMove
                }
            }
            var lastOpeningName: String? = null
            for (i in 1 until positions.size) {
                val prev = positions[i - 1]
                val cur = positions[i]

                val boardFen = cur.fenString.substringBefore(' ')
                val openingName = OpeningBook.lookupBoardFen(boardFen)
                if (openingName != null) {
                    cur.isBook = true
                    cur.openingName = openingName
                    cur.classification = "Book"
                    lastOpeningName = openingName
                    continue
                } else {
                    cur.isBook = false
                    cur.openingName = lastOpeningName
                }

                val onlyMove = hasOnlyOneLegalMove(prev.fenString)
                cur.forced = onlyMove
                if (!onlyMove) {
                    val moveColour = if (isWhiteToMove(prev.fenString)) MoveColour.WHITE else MoveColour.BLACK
                    val prevEval = parseEvaluationWhiteCentric(prev.score, prev.fenString)
                    val curEval = parseEvaluationWhiteCentric(cur.score, cur.fenString)
                    cur.classification = if (prevEval != null && curEval != null) {
                        classifyPointLoss(prevEval, curEval, moveColour)
                    } else {
                        null
                    }
                } else {
                    cur.classification = null
                }
            }
            isEvaluating = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Move: $currentIndex", style = MaterialTheme.typography.headlineMedium)
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
                isLast
            )
            Text(text = "Score: $displayScore", style = MaterialTheme.typography.titleLarge)
            positions[currentIndex].forced?.let { f ->
                if (f) {
                    Text(
                        text = "Forced",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            positions[currentIndex].classification?.let { cls ->
                Text(text = "Move quality: $cls", style = MaterialTheme.typography.titleMedium)
            }
            positions[currentIndex].openingName?.let { name ->
                Text(text = "$name", style = MaterialTheme.typography.titleMedium)
            }

            if (currentIndex > 0) {
                val prevPosition = positions[currentIndex - 1]
                val currentPosition = positions[currentIndex]
                val classification = currentPosition.classification

                if (classification != "Book" && classification != "Best" && classification != "Forced") {
                    prevPosition.bestMove?.let { best ->
                        Text(
                            text = "Best: $best",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { if (currentIndex > 0) currentIndex-- }, enabled = currentIndex > 0) { Text("Back") }
            Button(onClick = { if (currentIndex < positions.size - 1) currentIndex++ }, enabled = currentIndex < positions.size - 1) { Text("Forward") }
        }
    }
}

fun normalizeScoreForDisplay(raw: String?, fen: String, isLast: Boolean): String {
    if (raw == null) return "N/A"
    val lower = raw.lowercase()
    return if (lower.startsWith("mate")) {
        val digits = Regex("""-?\d+""").find(lower)?.value
        if (digits != null) {
            val movesToMate = digits.toIntOrNull()
            if (movesToMate != null) "Mate in ${abs(movesToMate)}" else raw
        } else raw
    } else {
        val score = raw.toDoubleOrNull()
        if (score != null) {
            val whiteToMove = isWhiteToMove(fen)
            val adjustedScore = if (whiteToMove) score else -score
            val roundedScore = (adjustedScore * 10.0).roundToInt() / 10.0
            roundedScore.toString()
        } else raw
    }
}

fun hasOnlyOneLegalMove(fen: String): Boolean {
    val board = Board()
    board.loadFromFen(fen)
    val moves = board.legalMoves()
    return moves.size == 1
}

expect fun createStockfishEngine(context: Any?): StockfishEngine