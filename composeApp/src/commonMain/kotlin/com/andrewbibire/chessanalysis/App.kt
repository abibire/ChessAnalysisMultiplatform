package com.andrewbibire.chessanalysis

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.DrawableResource
import chessanalysis.composeapp.generated.resources.Res
import chessanalysis.composeapp.generated.resources.best
import chessanalysis.composeapp.generated.resources.excellent
import chessanalysis.composeapp.generated.resources.forced
import chessanalysis.composeapp.generated.resources.inaccuracy
import chessanalysis.composeapp.generated.resources.mistake
import chessanalysis.composeapp.generated.resources.okay
import chessanalysis.composeapp.generated.resources.theory
import chessanalysis.composeapp.generated.resources.blunder

@Composable
fun App(context: Any? = null) {
    MaterialTheme(
        colorScheme = ChessAnalysisDarkColorScheme
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
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
$9 gxf6 43. Qxf6+ Ke8 44. Qxh6 Kd7 45. Qd2 Kc6 $6 46. Kd3 Kc5 $6 47. Qg5 a4 48. bxa4
b3 49. Qe5 b2 50. Qxb2 Kd6 51. a5 Ke6 52. a6 Kd6 53. a7 Ke6 54. a8=Q Kd6 55. Qbb7 Ke6 56. Qxd5+ Ke7 57. Qe5+ Kd7 58. Qb7+ Kd8 59. Qee7# 1-0
    """.trimIndent()

    val gameResult = remember {
        Regex("""\[Result\s+"([^"]+)"\]""").find(pgn)?.groupValues?.get(1) ?: "*"
    }

    val gameTermination = remember {
        val termination = Regex("""\[Termination\s+"([^"]+)"\]""").find(pgn)?.groupValues?.get(1)
        termination ?: when (gameResult) {
            "1-0" -> "White wins"
            "0-1" -> "Black wins"
            "1/2-1/2" -> "Draw"
            else -> "Game over"
        }
    }

    val positions = remember { generateFensFromPgn(pgn) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isEvaluating by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBoardFlipped by remember { mutableStateOf(false) }

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
            classifyPositions(positions)
            isEvaluating = false
        }
    }

    LaunchedEffect(isPlaying, currentIndex) {
        if (isPlaying && currentIndex < positions.lastIndex) {
            delay(1000)
            currentIndex++
        } else if (currentIndex >= positions.lastIndex) {
            isPlaying = false
        }
    }

    val badgeUci = remember(currentIndex) {
        positions[currentIndex].playedMove
    }
    val badgeDrawable = remember(currentIndex) { classificationBadge(positions[currentIndex].classification) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EvaluationBar(
            score = if (isEvaluating) null else positions[currentIndex].score,
            fen = positions[currentIndex].fenString,
            gameResult = gameResult,
            isLastMove = currentIndex == positions.lastIndex,
            modifier = Modifier.fillMaxWidth().height(24.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            val cls = positions[currentIndex].classification ?: ""
            val suppressArrow = cls == "Best" || cls == "Book" || cls == "Forced"
            val arrow = if (!suppressArrow && currentIndex > 0)
                positions[currentIndex - 1].bestMove?.takeIf { it.length >= 4 }?.substring(0, 4)
            else null
            Chessboard(
                fen = positions[currentIndex].fenString,
                arrowUci = arrow,
                badgeUci = badgeUci,
                badgeDrawable = badgeDrawable,
                flipped = isBoardFlipped,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Move: ${currentIndex}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (isEvaluating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = BoardDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Analyzing...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val isLast = currentIndex == positions.lastIndex
                    if (isLast) {
                        Text(
                            text = gameTermination,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        val displayScore = normalizeScoreForDisplay(
                            positions[currentIndex].score,
                            positions[currentIndex].fenString,
                            isLast
                        )
                        Text(
                            text = "Evaluation: $displayScore",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    positions[currentIndex].classification?.let { c ->
                        Text(
                            text = "Classification: $c",
                            style = MaterialTheme.typography.bodyMedium,
                            color = when (c) {
                                "Best" -> EvalGreen
                                "Excellent" -> EvalGreen
                                "Okay" -> EvalYellow
                                "Inaccuracy" -> EvalYellow
                                "Mistake" -> EvalRed
                                "Blunder" -> EvalRed
                                "Book" -> PrimaryBlue
                                "Forced" -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    positions[currentIndex].openingName?.let { name ->
                        Text(
                            text = "Opening: $name",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (currentIndex > 0) {
                        val cls = positions[currentIndex].classification
                        if (cls != null && cls != "Best" && cls != "Book" && cls != "Forced") {
                            positions[currentIndex - 1].bestMove?.let { bm ->
                                val notation = uciToSan(bm, positions[currentIndex - 1].fenString)
                                Text(
                                    text = "Best: $notation",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EvalGreen
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                EvaluationButton(
                    onClick = { isBoardFlipped = !isBoardFlipped },
                    enabled = true,
                    modifier = Modifier
                        .height(32.dp)
                        .width(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SwapVert,
                        contentDescription = "Flip board",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EvaluationButton(
                    onClick = { currentIndex = 0 },
                    enabled = currentIndex > 0 && !isEvaluating,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "First move",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                EvaluationButton(
                    onClick = { if (currentIndex > 0) currentIndex-- },
                    enabled = currentIndex > 0 && !isEvaluating,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NavigateBefore,
                        contentDescription = "Previous move",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                EvaluationButton(
                    onClick = { isPlaying = !isPlaying },
                    enabled = currentIndex < positions.lastIndex && !isEvaluating,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                EvaluationButton(
                    onClick = { if (currentIndex < positions.lastIndex) currentIndex++ },
                    enabled = currentIndex < positions.lastIndex && !isEvaluating,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NavigateNext,
                        contentDescription = "Next move",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                EvaluationButton(
                    onClick = { currentIndex = positions.lastIndex },
                    enabled = currentIndex < positions.lastIndex && !isEvaluating,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Last move",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EvaluationButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(48.dp)
            .shadow(
                elevation = if (enabled) 4.dp else 0.dp,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF3d3d3d),
            contentColor = Color.White,
            disabledContainerColor = Color(0xFF2a2a2a),
            disabledContentColor = Color(0xFF555555)
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        )
    ) { content() }
}

fun normalizeScoreForDisplay(raw: String?, fen: String, isLast: Boolean): String {
    if (raw == null) return "N/A"
    val lower = raw.lowercase()
    return if (lower.startsWith("mate")) {
        val digits = Regex("""[+-]?\d+""").find(lower)?.value
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

@Composable
fun EvaluationBar(
    score: String?,
    fen: String,
    gameResult: String,
    isLastMove: Boolean,
    modifier: Modifier = Modifier
) {
    val evaluation = parseEvaluationWhiteCentric(score, fen)

    val pgnAdvantage: Float? = when (gameResult) {
        "1-0" -> 1f
        "0-1" -> 0f
        "1/2-1/2" -> 0.5f
        else -> null
    }

    val whiteAdvantage = when {
        isLastMove && pgnAdvantage != null -> pgnAdvantage
        evaluation == null -> 0.5f
        evaluation.type == EvalType.Mate -> {
            val absValue = kotlin.math.abs(evaluation.value)
            if (absValue > 900) {
                if (pgnAdvantage != null) pgnAdvantage else if (evaluation.value > 0) 1f else 0f
            } else {
                if (evaluation.value > 0) 1f else 0f
            }
        }
        else -> {
            val normalized = (evaluation.value / 100.0).coerceIn(-10.0, 10.0)
            ((normalized + 10.0) / 20.0).toFloat().coerceIn(0.0f, 1.0f)
        }
    }

    val animatedWhiteAdvantage by animateFloatAsState(
        targetValue = whiteAdvantage,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
    )

    val fraction = animatedWhiteAdvantage.coerceIn(0f, 1f)

    val leftTextColor = if (fraction <= 0f) Color.White else Color(0xFF3a3a3a)
    val rightTextColor = if (fraction >= 1f) Color(0xFF3a3a3a) else Color.White

    val isGameOver = isLastMove || (evaluation?.type == EvalType.Mate && kotlin.math.abs(evaluation.value) > 900)

    val (whiteScore, blackScore) = when {
        isGameOver -> {
            when (gameResult) {
                "1-0" -> "1" to "0"
                "0-1" -> "0" to "1"
                "1/2-1/2" -> "½" to "½"
                else -> null to null
            }
        }
        evaluation?.type == EvalType.Mate && evaluation.value > 0 -> {
            val mateIn = kotlin.math.abs(evaluation.value.toInt())
            "M$mateIn" to null
        }
        evaluation?.type == EvalType.Mate && evaluation.value < 0 -> {
            val mateIn = kotlin.math.abs(evaluation.value.toInt())
            null to "M$mateIn"
        }
        evaluation?.type == EvalType.Centipawn -> {
            val scoreAbs = kotlin.math.abs(evaluation.value / 100.0)
            val rounded = (scoreAbs * 10).roundToInt() / 10.0
            val scoreStr = rounded.toString()
            if (rounded == 0.0) "0" to "0"
            else if (evaluation.value > 0) scoreStr to null
            else null to scoreStr
        }
        else -> null to null
    }

    Box(modifier = modifier.background(Color(0xFF3a3a3a))) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(Color.White)
                .align(Alignment.CenterStart)
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (whiteScore != null) {
                Text(
                    text = whiteScore,
                    style = MaterialTheme.typography.labelSmall,
                    color = leftTextColor,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            if (blackScore != null) {
                Text(
                    text = blackScore,
                    style = MaterialTheme.typography.labelSmall,
                    color = rightTextColor,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }
        }
    }
}

fun classificationBadge(cls: String?): DrawableResource? {
    return when (cls?.lowercase()) {
        "best" -> Res.drawable.best
        "excellent" -> Res.drawable.excellent
        "okay" -> Res.drawable.okay
        "inaccuracy" -> Res.drawable.inaccuracy
        "mistake" -> Res.drawable.mistake
        "blunder" -> Res.drawable.blunder
        "book", "theory" -> Res.drawable.theory
        "forced" -> Res.drawable.forced
        else -> null
    }
}

expect fun createStockfishEngine(context: Any?): StockfishEngine