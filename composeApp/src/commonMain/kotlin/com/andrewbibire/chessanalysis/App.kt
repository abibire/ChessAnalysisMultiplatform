package com.andrewbibire.chessanalysis

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import coil3.compose.rememberAsyncImagePainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.DrawableResource
import chessanalysis.composeapp.generated.resources.Res
import chessanalysis.composeapp.generated.resources.best
import chessanalysis.composeapp.generated.resources.excellent
import chessanalysis.composeapp.generated.resources.inaccuracy
import chessanalysis.composeapp.generated.resources.mistake
import chessanalysis.composeapp.generated.resources.good
import chessanalysis.composeapp.generated.resources.blunder
import chessanalysis.composeapp.generated.resources.book
import chessanalysis.composeapp.generated.resources.forced
import com.andrewbibire.chessanalysis.online.getCountryCode
import dev.carlsen.flagkit.FlagKit
import com.andrewbibire.chessanalysis.audio.ChessSoundManager
import com.andrewbibire.chessanalysis.audio.MoveAnalyzer

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
    var showUsernameInput by remember { mutableStateOf(false) }
    var usernameTextFieldValue by remember { mutableStateOf(TextFieldValue()) }
    var isPrefilledUsername by remember { mutableStateOf(false) }
    val sheetSnackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Sound manager for move sounds
    val soundManager = remember { ChessSoundManager() }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isEvaluating by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBoardFlipped by remember { mutableStateOf(false) }
    var analysisCompleted by remember { mutableStateOf(0) }

    var selectedPlatform by remember { mutableStateOf<com.andrewbibire.chessanalysis.online.Platform?>(null) }
    var userProfile by remember { mutableStateOf<com.andrewbibire.chessanalysis.online.UserProfile?>(null) }
    var isLoadingProfile by remember { mutableStateOf(false) }

    // Load last username when entering input mode
    LaunchedEffect(showUsernameInput) {
        if (showUsernameInput && usernameTextFieldValue.text.isEmpty()) {
            val lastUsername = com.andrewbibire.chessanalysis.online.UserPreferences.getLastUsername()
            if (lastUsername != null) {
                usernameTextFieldValue = TextFieldValue(
                    text = lastUsername,
                    selection = TextRange(lastUsername.length)
                )
                isPrefilledUsername = true
            }
        }
    }

    // Reset input state when sheet is dismissed
    LaunchedEffect(showBottomSheet) {
        if (!showBottomSheet) {
            showUsernameInput = false
            usernameTextFieldValue = TextFieldValue()
            isPrefilledUsername = false
            selectedPlatform = null
        }
    }

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

    // Parse player information from PGN
    val whitePlayer = remember(pgn) {
        pgn?.let { Regex("""\[White\s+"([^"]+)"\]""").find(it)?.groupValues?.get(1) }
    }
    val blackPlayer = remember(pgn) {
        pgn?.let { Regex("""\[Black\s+"([^"]+)"\]""").find(it)?.groupValues?.get(1) }
    }
    val whiteElo = remember(pgn) {
        pgn?.let { Regex("""\[WhiteElo\s+"([^"]+)"\]""").find(it)?.groupValues?.get(1) }
    }
    val blackElo = remember(pgn) {
        pgn?.let { Regex("""\[BlackElo\s+"([^"]+)"\]""").find(it)?.groupValues?.get(1) }
    }

    // Avatar URLs and country codes state
    var whiteAvatar by remember { mutableStateOf<String?>(null) }
    var blackAvatar by remember { mutableStateOf<String?>(null) }
    var whiteCountryCode by remember { mutableStateOf<String?>(null) }
    var blackCountryCode by remember { mutableStateOf<String?>(null) }

    // Focus requester for keyboard navigation
    val focusRequester = remember { FocusRequester() }

    // Request focus when a game is loaded
    LaunchedEffect(pgn) {
        if (pgn != null) {
            focusRequester.requestFocus()
        }
    }

    // Detect platform from PGN headers
    val isChessComGame = remember(pgn) {
        pgn?.contains("[Site \"Chess.com\"]", ignoreCase = true) == true
    }

    val isLichessGame = remember(pgn) {
        pgn?.contains("[Site \"https://lichess.org", ignoreCase = true) == true
    }

    // Fetch Chess.com avatars and country codes when players change
    LaunchedEffect(whitePlayer, blackPlayer, isChessComGame) {
        whiteAvatar = null
        blackAvatar = null
        whiteCountryCode = null
        blackCountryCode = null

        // Only fetch for Chess.com games
        if (isChessComGame) {
            whitePlayer?.let { username ->
                val result = com.andrewbibire.chessanalysis.online.ChessComService.getPlayerProfile(username)
                if (result is com.andrewbibire.chessanalysis.network.NetworkResult.Success) {
                    whiteAvatar = result.data.avatar
                    whiteCountryCode = result.data.getCountryCode()
                    println("DEBUG: Fetched white avatar for $username: ${result.data.avatar}, country: ${whiteCountryCode}")
                }
            }
            blackPlayer?.let { username ->
                val result = com.andrewbibire.chessanalysis.online.ChessComService.getPlayerProfile(username)
                if (result is com.andrewbibire.chessanalysis.network.NetworkResult.Success) {
                    blackAvatar = result.data.avatar
                    blackCountryCode = result.data.getCountryCode()
                    println("DEBUG: Fetched black avatar for $username: ${result.data.avatar}, country: ${blackCountryCode}")
                }
            }
        }
    }

    // Determine display order - searched player always on left
    // Use the saved username from preferences (gets saved every time user searches)
    val searchedUsername = remember(pgn) {
        com.andrewbibire.chessanalysis.online.UserPreferences.getLastUsername()?.lowercase()
    }

    val isSearchedPlayerWhite = remember(searchedUsername, whitePlayer, blackPlayer) {
        val whiteLower = whitePlayer?.lowercase()
        val blackLower = blackPlayer?.lowercase()
        val result = whiteLower == searchedUsername
        println("DEBUG: searchedUsername=$searchedUsername, whiteLower=$whiteLower, blackLower=$blackLower, isSearchedPlayerWhite=$result")
        result
    }

    val leftPlayer = remember(isSearchedPlayerWhite, whitePlayer, blackPlayer) {
        val player = if (isSearchedPlayerWhite) whitePlayer else blackPlayer
        println("DEBUG: leftPlayer=$player (isSearchedPlayerWhite=$isSearchedPlayerWhite)")
        player
    }
    val leftElo = remember(isSearchedPlayerWhite, whiteElo, blackElo) {
        if (isSearchedPlayerWhite) whiteElo else blackElo
    }
    val leftColor = remember(isSearchedPlayerWhite) {
        if (isSearchedPlayerWhite) "white" else "black"
    }
    val leftAvatar = remember(isSearchedPlayerWhite, whiteAvatar, blackAvatar) {
        if (isSearchedPlayerWhite) whiteAvatar else blackAvatar
    }
    val leftCountryCode = remember(isSearchedPlayerWhite, whiteCountryCode, blackCountryCode) {
        if (isSearchedPlayerWhite) whiteCountryCode else blackCountryCode
    }

    val rightPlayer = remember(isSearchedPlayerWhite, whitePlayer, blackPlayer) {
        val player = if (isSearchedPlayerWhite) blackPlayer else whitePlayer
        println("DEBUG: rightPlayer=$player (isSearchedPlayerWhite=$isSearchedPlayerWhite)")
        player
    }
    val rightElo = remember(isSearchedPlayerWhite, whiteElo, blackElo) {
        if (isSearchedPlayerWhite) blackElo else whiteElo
    }
    val rightColor = remember(isSearchedPlayerWhite) {
        if (isSearchedPlayerWhite) "black" else "white"
    }
    val rightAvatar = remember(isSearchedPlayerWhite, whiteAvatar, blackAvatar) {
        if (isSearchedPlayerWhite) blackAvatar else whiteAvatar
    }
    val rightCountryCode = remember(isSearchedPlayerWhite, whiteCountryCode, blackCountryCode) {
        if (isSearchedPlayerWhite) blackCountryCode else whiteCountryCode
    }

    val positions = remember(pgn) {
        pgn?.let { generateFensFromPgn(it) } ?: emptyList()
    }

    // Safe index that's always within bounds - use this instead of currentIndex directly
    val safeCurrentIndex = if (positions.isEmpty()) 0 else currentIndex.coerceIn(0, positions.lastIndex)

    // Clamp currentIndex to valid range when positions changes to prevent crashes
    LaunchedEffect(positions.size) {
        if (positions.isNotEmpty()) {
            currentIndex = currentIndex.coerceIn(0, positions.lastIndex)
        } else {
            currentIndex = 0
        }
        isPlaying = false
    }

    // Auto-flip board based on searched player's color
    // If they're playing Black, flip the board so Black is at the bottom
    LaunchedEffect(isSearchedPlayerWhite, pgn, isChessComGame, isLichessGame) {
        if ((isChessComGame || isLichessGame) && pgn != null) {
            isBoardFlipped = !isSearchedPlayerWhite
        }
    }

    // Count move classifications for each player
    data class ClassificationStats(
        val best: Int = 0,
        val excellent: Int = 0,
        val good: Int = 0,
        val inaccuracy: Int = 0,
        val mistake: Int = 0,
        val blunder: Int = 0,
        val book: Int = 0
    )

    // Calculate stats when analysis is completed
    val (whiteStats, blackStats) = remember(analysisCompleted) {
        val white = mutableMapOf<String, Int>()
        val black = mutableMapOf<String, Int>()

        positions.forEachIndexed { index, position ->
            if (index > 0) { // Skip initial position
                position.classification?.let { classification ->
                    // Determine who made the move: odd indices = white moves, even indices = black moves
                    val isWhiteMove = index % 2 == 1
                    val stats = if (isWhiteMove) white else black
                    stats[classification] = (stats[classification] ?: 0) + 1
                }
            }
        }

        val whiteStats = ClassificationStats(
            best = white["Best"] ?: 0,
            excellent = white["Excellent"] ?: 0,
            good = white["Good"] ?: 0,
            inaccuracy = white["Inaccuracy"] ?: 0,
            mistake = white["Mistake"] ?: 0,
            blunder = white["Blunder"] ?: 0
        )

        val blackStats = ClassificationStats(
            best = black["Best"] ?: 0,
            excellent = black["Excellent"] ?: 0,
            good = black["Good"] ?: 0,
            inaccuracy = black["Inaccuracy"] ?: 0,
            mistake = black["Mistake"] ?: 0,
            blunder = black["Blunder"] ?: 0
        )

        println("DEBUG: Analysis completed=$analysisCompleted, White stats: $whiteStats")
        println("DEBUG: Analysis completed=$analysisCompleted, Black stats: $blackStats")

        Pair(whiteStats, blackStats)
    }

    // Show error if PGN failed to parse
    if (pgn != null && positions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Failed to parse game",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This game format is not supported (e.g., Chess960 variants)",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { pgn = null }) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    val stockfishEngine = remember(context) {
        createStockfishEngine(context)
    }

    LaunchedEffect(context) {
        OpeningBook.init()
    }

    LaunchedEffect(pgn, stockfishEngine) {
        if (pgn != null && positions.isNotEmpty()) {
            isEvaluating = true
            currentIndex = 0
            analysisCompleted = 0
            delay(500)
            try {
                withContext(Dispatchers.Default) {
                    for (position in positions) {
                        // Check if coroutine is still active (allows cancellation)
                        if (!isActive) break

                        val result = stockfishEngine.evaluatePosition(position.fenString, depth = 14)
                        position.score = result.score
                        position.bestMove = result.bestMove
                    }
                }

                // Only classify if we completed the full analysis (not cancelled)
                if (isActive) {
                    classifyPositions(positions)
                    analysisCompleted++ // Trigger stats recalculation
                }
            } catch (e: Exception) {
                println("KOTLIN: Analysis error: ${e.message}")
            } finally {
                // Always reset evaluating state, even if cancelled or error
                isEvaluating = false
            }
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

    // Play move sounds when position changes
    LaunchedEffect(currentIndex, positions, gameTermination) {
        if (currentIndex > 0 && positions.isNotEmpty()) {
            val currentPosition = positions.getOrNull(currentIndex)
            val previousPosition = positions.getOrNull(currentIndex - 1)

            if (currentPosition != null && previousPosition != null) {
                val uciMove = currentPosition.playedMove
                val currentFen = currentPosition.fenString
                val previousFen = previousPosition.fenString

                // Parse SAN notation to detect move types
                val san = currentPosition.sanNotation ?: ""
                val isCapture = san.contains('x')
                val isPromotion = san.contains('=')
                val isCheck = san.contains('+') && !san.contains('#')
                val isCastling = san.startsWith("O-O")

                // Detect if we're at the last move
                val isLastMove = currentIndex == positions.lastIndex

                // Detect checkmate - either from SAN notation or game termination
                val isCheckmate = (san.contains('#') || (isLastMove && (
                    gameTermination.contains("checkmate", ignoreCase = true) ||
                    gameTermination.contains("mate", ignoreCase = true)
                )))

                // Detect game end without checkmate (resignation, timeout, draw, etc.)
                val isGameEndNoCheckmate = isLastMove && !isCheckmate && (
                    gameTermination.contains("resignation", ignoreCase = true) ||
                    gameTermination.contains("timeout", ignoreCase = true) ||
                    gameTermination.contains("draw", ignoreCase = true) ||
                    gameTermination.contains("abandoned", ignoreCase = true) ||
                    gameResult != "*"
                )

                // For non-checkmate game endings, only play game end sound (not the move sound)
                if (isGameEndNoCheckmate) {
                    soundManager.playGameEndSound()
                } else {
                    // Play normal move sound (which handles checkmate internally)
                    soundManager.playMoveSound(
                        isCapture = isCapture,
                        isCheck = isCheck,
                        isCheckmate = isCheckmate,
                        isPromotion = isPromotion,
                        isCastling = isCastling
                    )
                }
            }
        }
    }

    val badgeUci = remember(currentIndex, positions) {
        positions.getOrNull(currentIndex)?.playedMove
    }
    val badgeDrawable = remember(currentIndex, positions) {
        classificationBadge(positions.getOrNull(currentIndex)?.classification)
    }

    if (userProfile != null) {
        com.andrewbibire.chessanalysis.online.GamesListScreen(
            userProfile = userProfile!!,
            onGameSelected = { game ->
                pgn = game.pgn
                userProfile = null
            },
            onBackPressed = {
                userProfile = null
            }
        )
    } else {
        Box(modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusTarget()
            .onPreviewKeyEvent { keyEvent ->
                // Handle keyboard navigation on desktop
                if (keyEvent.type == KeyEventType.KeyDown && !isEvaluating && positions.isNotEmpty()) {
                    when (keyEvent.key) {
                        Key.DirectionRight -> {
                            // Right arrow: move forward
                            if (currentIndex < positions.lastIndex) {
                                currentIndex++
                                true
                            } else {
                                false
                            }
                        }
                        Key.DirectionLeft -> {
                            // Left arrow: move backward
                            if (currentIndex > 0) {
                                currentIndex--
                                true
                            } else {
                                false
                            }
                        }
                        Key.DirectionUp -> {
                            // Up arrow: go to last move
                            if (currentIndex < positions.lastIndex) {
                                currentIndex = positions.lastIndex
                                true
                            } else {
                                false
                            }
                        }
                        Key.DirectionDown -> {
                            // Down arrow: go to first move
                            if (currentIndex > 0) {
                                currentIndex = 0
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // Click to gain focus for keyboard navigation
                focusRequester.requestFocus()
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    // Dismiss any active snackbars on tap
                    sheetSnackbarHostState.currentSnackbarData?.dismiss()
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (pgn != null && positions.isNotEmpty()) {
                    EvaluationBar(
                        score = if (isEvaluating) null else positions[safeCurrentIndex].score,
                        fen = positions[safeCurrentIndex].fenString,
                        gameResult = gameResult,
                        isLastMove = safeCurrentIndex == positions.lastIndex,
                        modifier = Modifier.fillMaxWidth().height(24.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                }

                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val screenWidth = maxWidth
                    val screenHeight = maxHeight

                    // Detect device type based on screen width
                    val isTinyPhone = screenWidth < 380.dp
                    val isPhone = screenWidth < 600.dp
                    val isTablet = screenWidth >= 600.dp && screenWidth < 900.dp
                    val isLargeTablet = screenWidth >= 900.dp

                    // Space needed for buttons and controls
                    // Tiny phones (like iPhone SE 2) need more space to prevent button crushing
                    val minSpaceForButtons = when {
                        isTinyPhone -> 300.dp  // Extra space for tiny phones
                        isPhone -> 180.dp      // Regular phones
                        isTablet -> 200.dp
                        else -> 220.dp
                    }

                    val availableHeight = screenHeight - 24.dp
                    val maxBoardFromHeight = availableHeight - minSpaceForButtons

                    // Scale board size based on device type
                    val maxBoardSize = when {
                        isPhone -> screenWidth  // Full width on phones
                        isTablet -> 700.dp      // Larger on tablets
                        isLargeTablet -> 900.dp // Even larger on big tablets
                        else -> 550.dp
                    }

                    val boardSize = minOf(
                        maxBoardSize,
                        maxBoardFromHeight.coerceAtLeast(200.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(boardSize)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (pgn != null && positions.isNotEmpty()) {
                            val cls = positions[safeCurrentIndex].classification ?: ""
                            val suppressArrow = cls == "Best" || cls == "Book" || cls == "Forced"
                            val arrow = if (!suppressArrow && safeCurrentIndex > 0)
                                positions[safeCurrentIndex - 1].bestMove?.takeIf { it.length >= 4 }?.substring(0, 4)
                            else null
                            Chessboard(
                                fen = positions[safeCurrentIndex].fenString,
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
                }

                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    val screenWidth = maxWidth

                    // Detect device type based on screen width
                    val isTinyPhone = screenWidth < 380.dp
                    val isPhone = screenWidth < 600.dp
                    val isTablet = screenWidth >= 600.dp && screenWidth < 900.dp
                    val isLargeTablet = screenWidth >= 900.dp

                    // Responsive sizing for UI elements
                    val avatarSize = when {
                        isTinyPhone -> 28.dp
                        isPhone -> 32.dp
                        isTablet -> 48.dp
                        isLargeTablet -> 56.dp
                        else -> 32.dp
                    }

                    val flagSize = when {
                        isTinyPhone -> 14.dp
                        isPhone -> 16.dp
                        isTablet -> 20.dp
                        isLargeTablet -> 24.dp
                        else -> 16.dp
                    }

                    val profileFontSize = when {
                        isTinyPhone -> 12.sp
                        isPhone -> 14.sp
                        isTablet -> 18.sp
                        isLargeTablet -> 22.sp
                        else -> 14.sp
                    }

                    val moveFontSize = when {
                        isTinyPhone -> 20.sp
                        isPhone -> 22.sp
                        isTablet -> 32.sp
                        isLargeTablet -> 40.sp
                        else -> 22.sp
                    }

                    val bodyFontSize = when {
                        isTinyPhone -> 14.sp
                        isPhone -> 16.sp
                        isTablet -> 20.sp
                        isLargeTablet -> 24.sp
                        else -> 16.sp
                    }

                    val smallFontSize = when {
                        isTinyPhone -> 12.sp
                        isPhone -> 14.sp
                        isTablet -> 16.sp
                        isLargeTablet -> 18.sp
                        else -> 14.sp
                    }

                    val statIconSize = when {
                        isTinyPhone -> 20.dp
                        isPhone -> 24.dp
                        isTablet -> 32.dp
                        isLargeTablet -> 40.dp
                        else -> 24.dp
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Load Game",
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Load Game",
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
                            // Box with Move text and player profiles at same level
                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Searched player profile (top left)
                                PlayerProfile(
                                    playerName = leftPlayer,
                                    rating = leftElo,
                                    color = leftColor,
                                    avatarUrl = leftAvatar,
                                    otherPlayerHasAvatar = rightAvatar != null,
                                    isLeftSide = true,
                                    countryCode = leftCountryCode,
                                    avatarSize = avatarSize,
                                    flagSize = flagSize,
                                    fontSize = profileFontSize,
                                    modifier = Modifier.align(Alignment.TopStart)
                                )

                                // Move text (center)
                                Text(
                                    text = "Move: ${currentIndex}",
                                    fontSize = moveFontSize,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.align(Alignment.TopCenter)
                                )

                                // Opponent profile (top right)
                                PlayerProfile(
                                    playerName = rightPlayer,
                                    rating = rightElo,
                                    color = rightColor,
                                    avatarUrl = rightAvatar,
                                    otherPlayerHasAvatar = leftAvatar != null,
                                    isLeftSide = false,
                                    countryCode = rightCountryCode,
                                    avatarSize = avatarSize,
                                    flagSize = flagSize,
                                    fontSize = profileFontSize,
                                    modifier = Modifier.align(Alignment.TopEnd)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            if (isEvaluating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = BoardDark
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Analyzing...",
                                    fontSize = bodyFontSize,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                val isLast = safeCurrentIndex == positions.lastIndex
                                if (isLast) {
                                    Text(
                                        text = gameTermination,
                                        fontSize = bodyFontSize,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                } else if (safeCurrentIndex == 0) {
                                    // Show classification statistics at starting position
                                    // Determine left and right stats based on player colors
                                    val leftStats = if (isSearchedPlayerWhite) whiteStats else blackStats
                                    val rightStats = if (isSearchedPlayerWhite) blackStats else whiteStats

                                    // Check if there are any stats to show
                                    val hasStats = (leftStats.best + leftStats.excellent + leftStats.good +
                                            leftStats.inaccuracy + leftStats.mistake + leftStats.blunder +
                                            rightStats.best + rightStats.excellent + rightStats.good +
                                            rightStats.inaccuracy + rightStats.mistake + rightStats.blunder) > 0

                                    if (hasStats) {
                                        Text(
                                            text = "Game Statistics",
                                            fontSize = bodyFontSize,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Display in a 3-column grid with 2 items each
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                ClassificationStatItem("Best", leftStats.best, rightStats.best, statIconSize, smallFontSize)
                                                ClassificationStatItem("Excellent", leftStats.excellent, rightStats.excellent, statIconSize, smallFontSize)
                                            }
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                ClassificationStatItem("Good", leftStats.good, rightStats.good, statIconSize, smallFontSize)
                                                ClassificationStatItem("Inaccuracy", leftStats.inaccuracy, rightStats.inaccuracy, statIconSize, smallFontSize)
                                            }
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                ClassificationStatItem("Mistake", leftStats.mistake, rightStats.mistake, statIconSize, smallFontSize)
                                                ClassificationStatItem("Blunder", leftStats.blunder, rightStats.blunder, statIconSize, smallFontSize)
                                            }
                                        }
                                    } else {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(32.dp),
                                            color = BoardDark
                                        )
                                    }
                                } else {
                                    val displayScore = normalizeScoreForDisplay(
                                        positions[safeCurrentIndex].score,
                                        positions[safeCurrentIndex].fenString,
                                        isLast
                                    )

                                    Text(
                                        text = "Evaluation: $displayScore",
                                        fontSize = bodyFontSize,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                positions[safeCurrentIndex].classification?.let { c ->
                                    val playedMoveNotation = if (safeCurrentIndex > 0) {
                                        positions[safeCurrentIndex].playedMove?.let { uci ->
                                            uciToSan(uci, positions[safeCurrentIndex - 1].fenString)
                                        } ?: "This move"
                                    } else {
                                        "This move"
                                    }

                                    val classificationText = when (c) {
                                        "Best" -> "$playedMoveNotation is $c."
                                        "Excellent" -> "$playedMoveNotation is $c"
                                        "Good" -> "$playedMoveNotation is $c"
                                        "Inaccuracy" -> "$playedMoveNotation is an $c"
                                        "Mistake" -> "$playedMoveNotation is a $c"
                                        "Blunder" -> "$playedMoveNotation is a $c"
                                        "Book" -> "$playedMoveNotation is a $c Move."
                                        "Forced" -> "$playedMoveNotation is $c."
                                        else -> "$playedMoveNotation is $c."
                                    }

                                    val bestMoveText = if (safeCurrentIndex > 0 && c != "Best" && c != "Book" && c != "Forced") {
                                        positions[safeCurrentIndex - 1].bestMove?.let { bm ->
                                            val notation = uciToSan(bm, positions[safeCurrentIndex - 1].fenString)
                                            ", $notation is Best."
                                        }
                                    } else null

                                    Text(
                                        text = classificationText + (bestMoveText ?: ""),
                                        fontSize = smallFontSize,
                                        color = when (c) {
                                            "Best" -> BestColor
                                            "Excellent" -> EvalGreen
                                            "Good" -> GoodColor
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

                                positions[safeCurrentIndex].openingName?.let { name ->
                                    Text(
                                        text = name,
                                        fontSize = smallFontSize * 0.9f,
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
                }
            }

            // Snackbar overlay at top of app
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                SnackbarHost(
                    hostState = sheetSnackbarHostState,
                    modifier = Modifier
                        .padding(16.dp)
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            if (showBottomSheet) {
                val sheetState = rememberModalBottomSheetState(
                    skipPartiallyExpanded = true
                )
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrimColor = Color.Black.copy(alpha = 0.32f)  // Semi-transparent overlay for better click detection
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.Start
                        ) {
                            // Header with back arrow when in input mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (showUsernameInput) {
                                    IconButton(
                                        onClick = {
                                            showUsernameInput = false
                                            usernameTextFieldValue = TextFieldValue()
                                            isPrefilledUsername = false
                                            selectedPlatform = null
                                            isLoadingProfile = false
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = if (showUsernameInput) {
                                        when (selectedPlatform) {
                                            com.andrewbibire.chessanalysis.online.Platform.CHESS_COM -> "Chess.com Username"
                                            com.andrewbibire.chessanalysis.online.Platform.LICHESS -> "Lichess Username"
                                            else -> "Enter Username"
                                        }
                                    } else "Import Games",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (showUsernameInput) {
                                // Username input mode
                                val loadGamesAction: () -> Unit = {
                                    coroutineScope.launch {
                                        try {
                                            isLoadingProfile = true

                                            val result = when (selectedPlatform) {
                                                com.andrewbibire.chessanalysis.online.Platform.CHESS_COM -> {
                                                    com.andrewbibire.chessanalysis.online.ChessComService.getUserProfile(usernameTextFieldValue.text)
                                                }
                                                com.andrewbibire.chessanalysis.online.Platform.LICHESS -> {
                                                    com.andrewbibire.chessanalysis.online.LichessService.getUserProfile(usernameTextFieldValue.text)
                                                }
                                                else -> com.andrewbibire.chessanalysis.network.NetworkResult.Error(
                                                    Exception("Unknown platform"),
                                                    "Unknown platform"
                                                )
                                            }

                                            when (result) {
                                                is com.andrewbibire.chessanalysis.network.NetworkResult.Success -> {
                                                    isLoadingProfile = false
                                                    // Save username for next time
                                                    com.andrewbibire.chessanalysis.online.UserPreferences.saveLastUsername(usernameTextFieldValue.text)
                                                    userProfile = result.data
                                                    showBottomSheet = false
                                                }
                                                is com.andrewbibire.chessanalysis.network.NetworkResult.Error -> {
                                                    isLoadingProfile = false
                                                    // Determine error message based on exception type and message
                                                    val errorMessage = when {
                                                        // Check if it's a network connectivity error (no internet)
                                                        result.exception.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                                                                result.exception.message?.contains("timeout", ignoreCase = true) == true ||
                                                                result.exception.message?.contains("Failed to connect", ignoreCase = true) == true ||
                                                                result.exception.message?.contains("Connection refused", ignoreCase = true) == true -> {
                                                            "No internet connection. Please check your network and try again."
                                                        }
                                                        // For all other API failures, show user-friendly message
                                                        else -> "Unable to find an account with that username"
                                                    }
                                                    sheetSnackbarHostState.showSnackbar(
                                                        message = errorMessage,
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            }
                                        } catch (e: Exception) {
                                            isLoadingProfile = false
                                            // Handle any unexpected errors
                                            sheetSnackbarHostState.showSnackbar(
                                                message = "An error occurred: ${e.message}",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = usernameTextFieldValue,
                                    onValueChange = { newValue ->
                                        // If text is prefilled and user makes ANY change
                                        if (isPrefilledUsername) {
                                            // Check if text was deleted (backspace) - clear the field completely
                                            if (newValue.text.length < usernameTextFieldValue.text.length) {
                                                usernameTextFieldValue = TextFieldValue(text = "", selection = TextRange(0))
                                                isPrefilledUsername = false
                                            } else {
                                                // Text was added - extract only the newly typed characters
                                                // by removing the old prefilled text and keeping what was added
                                                val oldText = usernameTextFieldValue.text
                                                val newText = newValue.text

                                                // Find what was actually typed (the difference)
                                                val addedText = if (newText.startsWith(oldText)) {
                                                    newText.removePrefix(oldText)
                                                } else if (newText.endsWith(oldText)) {
                                                    newText.removeSuffix(oldText)
                                                } else {
                                                    // Character inserted in middle or text replaced - use new text
                                                    newText
                                                }

                                                val filteredText = addedText.replace(" ", "")
                                                usernameTextFieldValue = TextFieldValue(text = filteredText, selection = TextRange(filteredText.length))
                                                isPrefilledUsername = false
                                            }
                                        } else {
                                            // Filter out spaces
                                            val filteredText = newValue.text.replace(" ", "")
                                            usernameTextFieldValue = newValue.copy(text = filteredText)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Username") },
                                    enabled = !isLoadingProfile,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (usernameTextFieldValue.text.isNotBlank() && !isLoadingProfile) {
                                                loadGamesAction()
                                            }
                                        }
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        focusedTextColor = if (isPrefilledUsername) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = if (isPrefilledUsername) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = loadGamesAction,
                                    enabled = usernameTextFieldValue.text.isNotBlank() && !isLoadingProfile,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BoardDark,
                                        contentColor = Color.White,
                                        disabledContainerColor = Color(0xFF3d3d3d),
                                        disabledContentColor = Color(0xFF888888)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (isLoadingProfile) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = Color.White
                                        )
                                    } else {
                                        Text(
                                            text = "Load Games",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            } else {
                                // Button mode
                                ImportOption(
                                    iconContent = {
                                        MaterialSymbol(name = "chess_pawn_2", tint = Color(0xFF80b64d), fill = 1f)
                                    },
                                    title = "Chess.com",
                                    description = "Import from Chess.com",
                                    onClick = {
                                        selectedPlatform = com.andrewbibire.chessanalysis.online.Platform.CHESS_COM
                                        showUsernameInput = true
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                ImportOption(
                                    iconContent = {
                                        MaterialSymbol(name = "chess_knight", tint = Color.White, fill = 0f, flipHorizontally = true)
                                    },
                                    title = "Lichess",
                                    description = "Import from Lichess",
                                    onClick = {
                                        selectedPlatform = com.andrewbibire.chessanalysis.online.Platform.LICHESS
                                        showUsernameInput = true
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                ImportOption(
                                    icon = Icons.Filled.ContentPaste,
                                    title = "Paste from Clipboard",
                                    description = "Import a PGN from your clipboard",
                                    iconTint = BookColor,
                                    onClick = {
                                        coroutineScope.launch {
                                            val clipboardText = readClipboard()
                                            if (isValidPgn(clipboardText)) {
                                                pgn = clipboardText
                                                showBottomSheet = false
                                            } else {
                                                sheetSnackbarHostState.showSnackbar(
                                                    message = "Invalid or empty PGN in clipboard",
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
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
        "good" -> Res.drawable.good
        "inaccuracy" -> Res.drawable.inaccuracy
        "mistake" -> Res.drawable.mistake
        "blunder" -> Res.drawable.blunder
        "book" -> Res.drawable.book
        "forced" -> Res.drawable.forced
        else -> null
    }
}

@Composable
fun ClassificationStatItem(
    classification: String,
    leftCount: Int,
    rightCount: Int,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    modifier: Modifier = Modifier
) {
    val icon = classificationBadge(classification)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left player count - fixed width to prevent wrapping
        Text(
            text = leftCount.toString(),
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(min = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Classification icon
        if (icon != null) {
            androidx.compose.foundation.Image(
                painter = org.jetbrains.compose.resources.painterResource(icon),
                contentDescription = classification,
                modifier = Modifier.size(iconSize)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Right player count - fixed width to prevent wrapping
        Text(
            text = rightCount.toString(),
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(min = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Start
        )
    }
}

@Composable
fun ImportOption(
    icon: Any? = null,
    iconContent: (@Composable () -> Unit)? = null,
    title: String,
    description: String,
    iconTint: Color = Color.White,
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
            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                when {
                    iconContent != null -> iconContent()
                    icon is ImageVector -> {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            modifier = Modifier.size(32.dp),
                            tint = iconTint
                        )
                    }
                    icon is Painter -> {
                        Icon(
                            painter = icon,
                            contentDescription = title,
                            modifier = Modifier.size(32.dp),
                            tint = iconTint
                        )
                    }
                }
            }
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
    return isValidParsablePgn(text)
}

@Composable
fun PlayerProfile(
    playerName: String?,
    rating: String?,
    color: String,
    avatarUrl: String?,
    otherPlayerHasAvatar: Boolean,
    isLeftSide: Boolean,
    countryCode: String? = null,
    avatarSize: androidx.compose.ui.unit.Dp = 32.dp,
    flagSize: androidx.compose.ui.unit.Dp = 16.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = if (isLeftSide) Alignment.Start else Alignment.End
    ) {
        // Only show avatar section if this player has an avatar OR the other player has one (for uniformity)
        val shouldShowAvatar = avatarUrl != null || otherPlayerHasAvatar

        if (shouldShowAvatar) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl != null) {
                    androidx.compose.foundation.Image(
                        painter = rememberAsyncImagePainter(avatarUrl),
                        contentDescription = "$playerName avatar",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "No avatar",
                        modifier = Modifier.size(avatarSize * 0.6f),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Player name with flag and color indicator
        if (playerName != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isLeftSide) Arrangement.Start else Arrangement.End
            ) {
                // Flag icon (if available)
                if (countryCode != null) {
                    FlagKit.getFlag(countryCode = countryCode)?.let { flagIcon ->
                        Icon(
                            imageVector = flagIcon,
                            contentDescription = "$countryCode flag",
                            modifier = Modifier.size(flagSize),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }

                Text(
                    text = playerName.take(15),
                    fontSize = fontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(4.dp))
                // Color indicator - same as GamesListScreen
                Box(
                    modifier = Modifier
                        .size(fontSize.value.dp * 0.6f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (color == "white") Color.White else Color(0xFF2C2C2C))
                )
            }
        }

        // Rating - only show if available
        if (rating != null && rating != "?" && rating.isNotBlank()) {
            Text(
                text = rating,
                fontSize = fontSize * 0.9f,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

expect fun createStockfishEngine(context: Any?): StockfishEngine

expect suspend fun readClipboard(): String?