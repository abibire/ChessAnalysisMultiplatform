package com.andrewbibire.chessanalysis

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessAnalysisApp(context: Any?) {
    var pgn by remember { mutableStateOf<String?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var currentIndex by remember { mutableIntStateOf(0) }
    var isEvaluating by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBoardFlipped by remember { mutableStateOf(false) }

    val gameResult = remember(pgn) {
        pgn?.let { Regex("""\[Result\s+"([^"]+)"\]""").find(it)?.groupValues?.get(1) } ?: "*"
    }

    val gameTermination = remember(pgn, gameResult) {
        val termination = pgn?.let { Regex("""\[Termination\s+"([^"]+)"\]""").find(it)?.groupValues?.get(1) }
        termination ?: when (gameResult) {
            "1-0" -> "White wins"
            "0-1" -> "Black wins"
            "1/2-1/2" -> "Draw"
            else -> "Game over"
        }
    }

    val positions = remember(pgn) {
        pgn?.let { generateFensFromPgn(it) } ?: emptyList()
    }

    val stockfishEngine = remember(context) {
        if (context != null) createStockfishEngine(context) else null
    }

    LaunchedEffect(context) {
        OpeningBook.init()
    }

    LaunchedEffect(pgn, stockfishEngine) {
        if (pgn != null && stockfishEngine != null && positions.isNotEmpty()) {
            isEvaluating = true
            currentIndex = 0
            delay(500)
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

    val badgeUci = remember(currentIndex, positions) {
        positions.getOrNull(currentIndex)?.playedMove
    }
    val badgeDrawable = remember(currentIndex, positions) {
        classificationBadge(positions.getOrNull(currentIndex)?.classification)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        if (pgn != null && positions.isNotEmpty()) {
            EvaluationBar(
                score = if (isEvaluating) null else positions[currentIndex].score,
                fen = positions[currentIndex].fenString,
                gameResult = gameResult,
                isLastMove = currentIndex == positions.lastIndex,
                modifier = Modifier.fillMaxWidth().height(24.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            if (pgn != null && positions.isNotEmpty()) {
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
            } else {
                Chessboard(
                    fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                    arrowUci = null,
                    badgeUci = null,
                    badgeDrawable = null,
                    flipped = false,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (pgn == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { showBottomSheet = true },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(64.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3d3d3d),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add PGN",
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Add PGN",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
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
                        val playedMoveNotation = if (currentIndex > 0) {
                            positions[currentIndex].playedMove?.let { uci ->
                                uciToSan(uci, positions[currentIndex - 1].fenString)
                            } ?: "This move"
                        } else {
                            "This move"
                        }

                        val classificationText = when (c) {
                            "Best" -> "$playedMoveNotation is $c."
                            "Excellent" -> "$playedMoveNotation is $c"
                            "Okay" -> "$playedMoveNotation is $c"
                            "Inaccuracy" -> "$playedMoveNotation is an $c"
                            "Mistake" -> "$playedMoveNotation is a $c"
                            "Blunder" -> "$playedMoveNotation is a $c"
                            "Book" -> "$playedMoveNotation is a $c Move."
                            "Forced" -> "$playedMoveNotation is $c."
                            else -> "$playedMoveNotation is $c."
                        }

                        val bestMoveText = if (currentIndex > 0 && c != "Best" && c != "Book" && c != "Forced") {
                            positions[currentIndex - 1].bestMove?.let { bm ->
                                val notation = uciToSan(bm, positions[currentIndex - 1].fenString)
                                ", $notation is Best."
                            }
                        } else null

                        Text(
                            text = classificationText + (bestMoveText ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = when (c) {
                                "Best" -> BestColor
                                "Excellent" -> EvalGreen
                                "Okay" -> OkayColor
                                "Inaccuracy" -> InaccuracyColor
                                "Mistake" -> MistakeColor
                                "Blunder" -> BlunderColor
                                "Book" -> BookColor
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
                }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EvaluationButton(
                        onClick = { showBottomSheet = true },
                        enabled = true,
                        modifier = Modifier
                            .height(32.dp)
                            .width(64.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Load PGN",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

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
                    modifier = Modifier.fillMaxWidth(),
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

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 48.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Import PGN",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                ImportOption(
                    icon = Icons.Filled.ContentPaste,
                    title = "Paste from Clipboard",
                    description = "Import a PGN from your clipboard",
                    onClick = {
                        coroutineScope.launch {
                            val clipboardText = readClipboard()
                            if (isValidPgn(clipboardText)) {
                                pgn = clipboardText
                                showBottomSheet = false
                            } else {
                                showBottomSheet = false
                                snackbarHostState.showSnackbar(
                                    message = "Invalid or empty PGN in clipboard",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ImportOption(
                    icon = Icons.Filled.Language,
                    title = "Chess.com",
                    description = "Import from Chess.com",
                    onClick = {
                        coroutineScope.launch {
                            showBottomSheet = false
                            snackbarHostState.showSnackbar(
                                message = "Coming soon",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ImportOption(
                    icon = Icons.Filled.Language,
                    title = "Lichess",
                    description = "Import from Lichess",
                    onClick = {
                        coroutineScope.launch {
                            showBottomSheet = false
                            snackbarHostState.showSnackbar(
                                message = "Coming soon",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )
            }
        }
    }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .padding(16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
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

@Composable
fun ImportOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun isValidPgn(text: String?): Boolean {
    if (text.isNullOrBlank()) return false

    val trimmed = text.trim()

    val movePattern = Regex("""\d+\.\s*([NBRQK]?[a-h]?[1-8]?x?[a-h][1-8](=[NBRQ])?[+#]?|O-O(-O)?[+#]?)""")
    val matches = movePattern.findAll(trimmed).toList()

    return matches.size >= 2
}

expect fun createStockfishEngine(context: Any?): StockfishEngine

expect suspend fun readClipboard(): String?