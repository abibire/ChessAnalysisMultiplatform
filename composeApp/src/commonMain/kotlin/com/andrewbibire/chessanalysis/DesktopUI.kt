package com.andrewbibire.chessanalysis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrewbibire.chessanalysis.audio.ChessSoundManager
import com.andrewbibire.chessanalysis.online.UserPreferences
import com.andrewbibire.chessanalysis.online.getCountryCode
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Desktop-optimized UI with persistent side panels
 * No modal dialogs or bottom sheets - everything is visible at once
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopChessAnalysisApp(context: Any?) {
    var pgn by remember { mutableStateOf<String?>(null) }
    var usernameTextFieldValue by remember { mutableStateOf(TextFieldValue()) }
    var isPrefilledUsername by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Sound manager for move sounds
    val soundManager = remember { ChessSoundManager() }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isEvaluating by remember { mutableStateOf(false) }
    var analysisProgress by remember { mutableIntStateOf(0) }
    var analysisTotalMoves by remember { mutableIntStateOf(0) }
    var analysisRevision by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBoardFlipped by remember { mutableStateOf(false) }
    var analysisCompleted by remember { mutableStateOf(0) }

    var selectedPlatform by remember { mutableStateOf<com.andrewbibire.chessanalysis.online.Platform?>(null) }
    var userProfile by remember { mutableStateOf<com.andrewbibire.chessanalysis.online.UserProfile?>(null) }
    var isLoadingProfile by remember { mutableStateOf(false) }

    // Promotion dialog state
    var showPromotionDialog by remember { mutableStateOf(false) }
    var promotionIsWhite by remember { mutableStateOf(true) }
    var promotionFromSquare by remember { mutableStateOf<String?>(null) }
    var promotionToSquare by remember { mutableStateOf<String?>(null) }

    // Depth settings
    var analysisDepth by remember { mutableIntStateOf(UserPreferences.getAnalysisDepth()) }

    // Alternative lines state
    var alternativeLines by remember { mutableStateOf<List<PVLine>>(emptyList()) }
    var alternativeLinesFen by remember { mutableStateOf<String?>(null) }
    var isLoadingAlternatives by remember { mutableStateOf(false) }

    // Alternative line exploration state
    var isExploringAlternativeLine by remember { mutableStateOf(false) }
    var alternativeLineReturnIndex by remember { mutableIntStateOf(0) }
    var alternativeLineReturnPositionCount by remember { mutableIntStateOf(0) }

    // Load last username when starting
    LaunchedEffect(Unit) {
        val lastUsername = UserPreferences.getLastUsername()
        if (lastUsername != null) {
            usernameTextFieldValue = TextFieldValue(
                text = lastUsername,
                selection = TextRange(lastUsername.length)
            )
            isPrefilledUsername = true
        }
    }

    // Clear alternative lines when game changes
    LaunchedEffect(pgn) {
        alternativeLines = emptyList()
        alternativeLinesFen = null
    }

    // Clear alternative lines when user navigates
    LaunchedEffect(currentIndex) {
        alternativeLines = emptyList()
        alternativeLinesFen = null
    }

    // Delay showing board slightly to allow images to render
    var imagesLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200) // Small delay to allow image rendering
        imagesLoaded = true
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
                }
            }
            blackPlayer?.let { username ->
                val result = com.andrewbibire.chessanalysis.online.ChessComService.getPlayerProfile(username)
                if (result is com.andrewbibire.chessanalysis.network.NetworkResult.Success) {
                    blackAvatar = result.data.avatar
                    blackCountryCode = result.data.getCountryCode()
                }
            }
        }
    }

    // Determine display order - searched player always on left
    val searchedUsername = remember(pgn) {
        UserPreferences.getLastUsername()?.lowercase()
    }

    val isSearchedPlayerWhite = remember(searchedUsername, whitePlayer, blackPlayer) {
        val whiteLower = whitePlayer?.lowercase()
        whiteLower == searchedUsername
    }

    val leftPlayer = remember(isSearchedPlayerWhite, whitePlayer, blackPlayer) {
        if (isSearchedPlayerWhite) whitePlayer else blackPlayer
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
        if (isSearchedPlayerWhite) blackPlayer else whitePlayer
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
        pgn?.let { generateFensFromPgn(it) }?.toMutableList() ?: mutableListOf()
    }

    // Store original analyzed positions to preserve analysis when user makes moves
    var originalPositions by remember { mutableStateOf<List<Position>>(emptyList()) }

    // Track where user branched off from the original analysis (null if no branching)
    var branchPointIndex by remember { mutableIntStateOf(-1) }

    // Derived state: are we currently on an alternate path?
    val isOnAlternatePath = branchPointIndex >= 0

    // Cache for analyzed alternate path positions (key = FEN, value = analyzed Position)
    val alternatePathCache = remember { mutableMapOf<String, Position>() }

    // Track which positions (by FEN) are currently being analyzed in alternate path
    val analyzingFens = remember { mutableStateOf(setOf<String>()) }

    // Revision counter to trigger re-analysis when positions are modified
    var positionsRevision by remember { mutableIntStateOf(0) }

    // State for click-to-move functionality
    var selectedSquare by remember { mutableStateOf<String?>(null) }
    var legalMovesForSelected by remember { mutableStateOf<List<String>>(emptyList()) }

    // State for drag-and-drop functionality
    data class DragState(val fromSquare: String, val piece: String)
    var draggedPiece by remember { mutableStateOf<DragState?>(null) }
    var dragPosition by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }

    // Safe index that's always within bounds - use this instead of currentIndex directly
    val safeCurrentIndex = if (positions.isEmpty()) 0 else currentIndex.coerceIn(0, positions.lastIndex)

    // Helper function to check game ending state
    data class GameEndState(val isEnded: Boolean, val result: String, val description: String)

    fun checkGameEnd(fen: String): GameEndState {
        return try {
            val board = Board()
            board.loadFromFen(fen)

            when {
                board.isMated -> {
                    val whiteToMove = isWhiteToMove(fen)
                    val result = if (whiteToMove) "0-1" else "1-0"
                    val description = if (whiteToMove) "Black wins by checkmate" else "White wins by checkmate"
                    GameEndState(true, result, description)
                }
                board.isStaleMate -> GameEndState(true, "1/2-1/2", "Draw by stalemate")
                board.isInsufficientMaterial -> GameEndState(true, "1/2-1/2", "Draw by insufficient material")
                board.isRepetition -> GameEndState(true, "1/2-1/2", "Draw by repetition")
                else -> GameEndState(false, "*", "")
            }
        } catch (e: Exception) {
            GameEndState(false, "*", "")
        }
    }

    // Helper function to get legal moves for a piece at a square
    fun getLegalMovesForSquare(fen: String, fromSquare: String): List<String> {
        return try {
            val board = Board()
            board.loadFromFen(fen)
            val square = Square.valueOf(fromSquare.uppercase())
            val piece = board.getPiece(square)

            if (piece == Piece.NONE) return emptyList()

            val whiteToMove = isWhiteToMove(fen)
            val pieceIsWhite = piece.pieceSide == com.github.bhlangonijr.chesslib.Side.WHITE

            if (whiteToMove != pieceIsWhite) return emptyList()

            board.legalMoves()
                .filter { it.from.toString().lowercase() == fromSquare }
                .map { it.to.toString().lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Helper function to make a move
    fun completeMoveWithPromotion(fromSquare: String, toSquare: String, promotionPiece: String?) {
        if (positions.isEmpty()) return

        val currentPosition = positions[safeCurrentIndex]
        val board = Board()
        board.loadFromFen(currentPosition.fenString)

        try {
            val move = if (promotionPiece != null) {
                val targetPromotion = when (promotionPiece.uppercase()) {
                    "Q" -> if (promotionPiece == "Q") com.github.bhlangonijr.chesslib.Piece.WHITE_QUEEN else com.github.bhlangonijr.chesslib.Piece.BLACK_QUEEN
                    "R" -> if (promotionPiece == "R") com.github.bhlangonijr.chesslib.Piece.WHITE_ROOK else com.github.bhlangonijr.chesslib.Piece.BLACK_ROOK
                    "B" -> if (promotionPiece == "B") com.github.bhlangonijr.chesslib.Piece.WHITE_BISHOP else com.github.bhlangonijr.chesslib.Piece.BLACK_BISHOP
                    "N" -> if (promotionPiece == "N") com.github.bhlangonijr.chesslib.Piece.WHITE_KNIGHT else com.github.bhlangonijr.chesslib.Piece.BLACK_KNIGHT
                    else -> null
                }
                board.legalMoves().find {
                    it.from.toString().lowercase() == fromSquare &&
                            it.to.toString().lowercase() == toSquare &&
                            it.promotion == targetPromotion
                }
            } else {
                board.legalMoves().find {
                    it.from.toString().lowercase() == fromSquare &&
                            it.to.toString().lowercase() == toSquare &&
                            it.promotion == com.github.bhlangonijr.chesslib.Piece.NONE
                }
            }

            if (move != null) {
                val isPromotionMove = move.promotion != com.github.bhlangonijr.chesslib.Piece.NONE

                board.doMove(move)
                val newFen = board.fen

                val uciMove = if (isPromotionMove) {
                    "$fromSquare$toSquare${move.promotion.fenSymbol.lowercase()}"
                } else {
                    "$fromSquare$toSquare"
                }

                val newPosition = Position(
                    fenString = newFen,
                    playedMove = uciMove,
                    sanNotation = uciToSan(uciMove, currentPosition.fenString)
                )

                if (!isOnAlternatePath && originalPositions.isNotEmpty()) {
                    branchPointIndex = safeCurrentIndex
                }

                while (positions.size > safeCurrentIndex + 1) {
                    positions.removeAt(positions.size - 1)
                }

                positions.add(newPosition)
                positionsRevision++
                currentIndex++
            }
        } catch (e: Exception) {
            // Silently handle errors
        }

        selectedSquare = null
        legalMovesForSelected = emptyList()
    }

    fun makeMove(fromSquare: String, toSquare: String) {
        if (positions.isEmpty()) return

        val currentPosition = positions[safeCurrentIndex]
        val board = Board()
        board.loadFromFen(currentPosition.fenString)

        try {
            val hasPromotionMove = board.legalMoves().any {
                it.from.toString().lowercase() == fromSquare &&
                        it.to.toString().lowercase() == toSquare &&
                        it.promotion != com.github.bhlangonijr.chesslib.Piece.NONE
            }

            if (hasPromotionMove) {
                val isWhiteTurn = isWhiteToMove(currentPosition.fenString)
                promotionIsWhite = isWhiteTurn
                promotionFromSquare = fromSquare
                promotionToSquare = toSquare
                showPromotionDialog = true
                return
            }

            completeMoveWithPromotion(fromSquare, toSquare, null)
        } catch (e: Exception) {
            selectedSquare = null
            legalMovesForSelected = emptyList()
        }
    }

    // Helper function to convert a list of UCI PV moves to SAN notation for display
    fun convertPvToSan(uciMoves: List<String>, startingFen: String): List<String> {
        if (uciMoves.isEmpty()) return emptyList()

        val sanMoves = mutableListOf<String>()
        var currentFen = startingFen
        val board = Board()

        for (uciMove in uciMoves) {
            try {
                val san = uciToSan(uciMove, currentFen)
                sanMoves.add(san)

                board.loadFromFen(currentFen)
                val fromSquare = uciMove.substring(0, 2)
                val toSquare = uciMove.substring(2, 4)
                val promotionPiece = if (uciMove.length > 4) uciMove[4].toString() else null

                val move = board.legalMoves().find {
                    it.from.toString().lowercase() == fromSquare &&
                            it.to.toString().lowercase() == toSquare &&
                            (promotionPiece == null || it.promotion.fenSymbol.lowercase() == promotionPiece)
                }

                if (move != null) {
                    board.doMove(move)
                    currentFen = board.fen
                } else {
                    sanMoves.addAll(uciMoves.subList(sanMoves.size, uciMoves.size))
                    break
                }
            } catch (e: Exception) {
                sanMoves.addAll(uciMoves.subList(sanMoves.size, uciMoves.size))
                break
            }
        }

        return sanMoves
    }

    fun exploreAlternativeLine(pvMoves: List<String>, moveIndex: Int) {
        if (positions.isEmpty() || alternativeLinesFen == null) return

        alternativeLineReturnIndex = currentIndex
        alternativeLineReturnPositionCount = positions.size

        while (positions.size > safeCurrentIndex + 1) {
            positions.removeAt(positions.size - 1)
        }

        if (branchPointIndex < 0) {
            branchPointIndex = safeCurrentIndex
        }

        var currentFen = alternativeLinesFen!!
        val board = Board()

        for (i in 0..moveIndex) {
            val uciMove = pvMoves[i]
            board.loadFromFen(currentFen)

            try {
                val fromSquare = uciMove.substring(0, 2)
                val toSquare = uciMove.substring(2, 4)
                val promotionPiece = if (uciMove.length > 4) uciMove[4].toString() else null

                val move = board.legalMoves().find {
                    it.from.toString().lowercase() == fromSquare &&
                            it.to.toString().lowercase() == toSquare &&
                            (promotionPiece == null || it.promotion.fenSymbol.lowercase() == promotionPiece)
                }

                if (move != null) {
                    val fenBeforeMove = currentFen

                    board.doMove(move)
                    currentFen = board.fen

                    val newPosition = Position(
                        fenString = currentFen,
                        playedMove = uciMove,
                        sanNotation = uciToSan(uciMove, fenBeforeMove)
                    )

                    positions.add(newPosition)
                } else {
                    break
                }
            } catch (e: Exception) {
                break
            }
        }

        currentIndex = positions.size - 1
        positionsRevision++
        isExploringAlternativeLine = true

        // Request focus back to main keyboard handler
        focusRequester.requestFocus()
    }

    // Click handler for board squares
    val onSquareClick: (String) -> Unit = onSquareClick@{ clickedSquare ->
        if (isEvaluating || positions.isEmpty()) return@onSquareClick

        val currentPosition = positions[safeCurrentIndex]

        if (selectedSquare == null) {
            val moves = getLegalMovesForSquare(currentPosition.fenString, clickedSquare)
            if (moves.isNotEmpty()) {
                selectedSquare = clickedSquare
                legalMovesForSelected = moves
            }
        } else {
            if (legalMovesForSelected.contains(clickedSquare)) {
                makeMove(selectedSquare!!, clickedSquare)
            } else {
                val moves = getLegalMovesForSquare(currentPosition.fenString, clickedSquare)
                if (moves.isNotEmpty()) {
                    selectedSquare = clickedSquare
                    legalMovesForSelected = moves
                } else {
                    selectedSquare = null
                    legalMovesForSelected = emptyList()
                }
            }
        }
    }

    // Drag handlers
    val canStartDrag: (String) -> Boolean = { fromSquare ->
        if (isEvaluating || positions.isEmpty()) {
            false
        } else {
            val currentPosition = positions[safeCurrentIndex]
            val moves = getLegalMovesForSquare(currentPosition.fenString, fromSquare)
            moves.isNotEmpty()
        }
    }

    val onDragStart: (String, String) -> Unit = { fromSquare, piece ->
        if (positions.isNotEmpty()) {
            val currentPosition = positions[safeCurrentIndex]
            val moves = getLegalMovesForSquare(currentPosition.fenString, fromSquare)
            if (moves.isNotEmpty()) {
                draggedPiece = DragState(fromSquare, piece)
                legalMovesForSelected = moves
                selectedSquare = null
            }
        }
    }

    val onDrag: (androidx.compose.ui.geometry.Offset) -> Unit = { position ->
        dragPosition = position
    }

    val onDragEnd: (String?) -> Unit = onDragEnd@{ toSquare ->
        try {
            draggedPiece?.let { drag ->
                if (toSquare != null && legalMovesForSelected.contains(toSquare)) {
                    makeMove(drag.fromSquare, toSquare)
                }
            }
        } finally {
            draggedPiece = null
            dragPosition = null
            legalMovesForSelected = emptyList()
            selectedSquare = null
        }
    }

    // Clear selection and drag state when position changes
    LaunchedEffect(safeCurrentIndex) {
        selectedSquare = null
        legalMovesForSelected = emptyList()
        draggedPiece = null
        dragPosition = null
    }

    // Clear selection and drag state when board flips
    LaunchedEffect(isBoardFlipped) {
        selectedSquare = null
        legalMovesForSelected = emptyList()
        draggedPiece = null
        dragPosition = null
    }

    // Clamp currentIndex to valid range when positions changes
    LaunchedEffect(positions.size) {
        if (positions.isNotEmpty()) {
            currentIndex = currentIndex.coerceIn(0, positions.lastIndex)
        } else {
            currentIndex = 0
        }
        isPlaying = false
    }

    // Automatically restore original analysis when navigating back to or before branch point
    LaunchedEffect(currentIndex, branchPointIndex) {
        if (isOnAlternatePath && currentIndex <= branchPointIndex && originalPositions.isNotEmpty()) {
            positions.clear()
            positions.addAll(originalPositions)
            branchPointIndex = -1
            alternatePathCache.clear()
        }
    }

    // Auto-flip board based on searched player's color
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

    // Calculate stats and accuracy when analysis is completed
    val (whiteStats, blackStats) = remember(analysisCompleted) {
        val white = mutableMapOf<String, Int>()
        val black = mutableMapOf<String, Int>()

        val positionsToAnalyze = if (originalPositions.isNotEmpty()) originalPositions else positions

        positionsToAnalyze.forEachIndexed { index, position ->
            if (index > 0) {
                position.classification?.let { classification ->
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

        Pair(whiteStats, blackStats)
    }

    // Calculate game accuracy
    val gameAccuracy = remember(analysisCompleted) {
        val positionsToAnalyze = if (originalPositions.isNotEmpty()) originalPositions else positions
        getGameAccuracy(positionsToAnalyze)
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
            analysisRevision++
            val currentRevision = analysisRevision

            isEvaluating = true
            currentIndex = 0
            analysisCompleted = 0
            analysisProgress = 0
            analysisTotalMoves = positions.size
            branchPointIndex = -1
            originalPositions = emptyList()
            alternatePathCache.clear()
            delay(500)
            try {
                withContext(Dispatchers.Default) {
                    for ((index, position) in positions.withIndex()) {
                        if (!isActive) break

                        val result = stockfishEngine.evaluatePosition(position.fenString, depth = analysisDepth)
                        position.score = result.score
                        position.bestMove = result.bestMove
                        analysisProgress = index + 1
                    }
                }

                if (isActive) {
                    classifyPositions(positions)
                    originalPositions = positions.toList()
                    analysisCompleted++
                }
            } catch (e: Exception) {
                println("KOTLIN: Analysis error: ${e.message}")
            } finally {
                if (currentRevision == analysisRevision) {
                    isEvaluating = false
                    analysisProgress = 0
                    analysisTotalMoves = 0
                }
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

    // Analyze alternate path positions in the background
    LaunchedEffect(positions.size, isOnAlternatePath, positionsRevision, stockfishEngine) {
        if (isOnAlternatePath && positions.isNotEmpty()) {
            for (i in (branchPointIndex + 1) until positions.size) {
                val position = positions[i]

                val cached = alternatePathCache[position.fenString]
                if (cached != null) {
                    positions[i] = cached.copy()
                } else {
                    analyzingFens.value = analyzingFens.value + position.fenString

                    if (isActive) {
                        val result = stockfishEngine.evaluatePosition(position.fenString, depth = analysisDepth)

                        var analyzedPosition = position.copy(
                            score = result.score,
                            bestMove = result.bestMove
                        )

                        if (i > 0) {
                            var prevPosition = positions[i - 1]

                            if (prevPosition.score == null || prevPosition.bestMove == null) {
                                val prevResult = stockfishEngine.evaluatePosition(prevPosition.fenString, depth = analysisDepth)
                                prevPosition = prevPosition.copy(
                                    score = prevResult.score,
                                    bestMove = prevResult.bestMove
                                )
                                positions[i - 1] = prevPosition
                                alternatePathCache[prevPosition.fenString] = prevPosition
                            }

                            val boardFen = analyzedPosition.fenString.substringBefore(' ')
                            val openingName = OpeningBook.lookupBoardFen(boardFen)
                            if (openingName != null) {
                                analyzedPosition = analyzedPosition.copy(
                                    isBook = true,
                                    openingName = openingName,
                                    classification = "Book"
                                )
                            } else {
                                val onlyMove = hasOnlyOneLegalMove(prevPosition.fenString)
                                if (onlyMove) {
                                    analyzedPosition = analyzedPosition.copy(
                                        forced = true,
                                        classification = "Forced"
                                    )
                                } else {
                                    val moveColour = if (isWhiteToMove(prevPosition.fenString)) MoveColour.WHITE else MoveColour.BLACK
                                    val prevEval = parseEvaluationWhiteCentric(prevPosition.score, prevPosition.fenString)
                                    val curEval = parseEvaluationWhiteCentric(analyzedPosition.score, analyzedPosition.fenString)
                                    if (prevEval != null && curEval != null) {
                                        val classification = classifyPointLoss(prevEval, curEval, moveColour, analyzedPosition.playedMove, prevPosition.bestMove)
                                        val accuracy = getMoveAccuracy(prevEval, curEval, moveColour)
                                        analyzedPosition = analyzedPosition.copy(
                                            forced = false,
                                            classification = classification,
                                            accuracy = accuracy
                                        )
                                    } else {
                                        analyzedPosition = analyzedPosition.copy(
                                            forced = false,
                                            classification = null,
                                            accuracy = null
                                        )
                                    }
                                }
                            }
                        }

                        alternatePathCache[analyzedPosition.fenString] = analyzedPosition
                        positions[i] = analyzedPosition
                        analyzingFens.value = analyzingFens.value - position.fenString
                    }
                }
            }
        }
    }

    // Play move sounds when position changes
    LaunchedEffect(currentIndex, positions, gameTermination) {
        if (currentIndex > 0 && positions.isNotEmpty()) {
            val currentPosition = positions.getOrNull(currentIndex)
            val previousPosition = positions.getOrNull(currentIndex - 1)

            if (currentPosition != null && previousPosition != null) {
                val san = currentPosition.sanNotation ?: ""
                val isCapture = san.contains('x')
                val isPromotion = san.contains('=')
                val isCheck = san.contains('+') && !san.contains('#')
                val isCastling = san.startsWith("O-O")

                val isLastMoveOfOriginalGame = if (originalPositions.isNotEmpty()) {
                    currentIndex == originalPositions.lastIndex && !isOnAlternatePath
                } else {
                    currentIndex == positions.lastIndex
                }

                val isCheckmate = (san.contains('#') || (isLastMoveOfOriginalGame && (
                        gameTermination.contains("checkmate", ignoreCase = true) ||
                                gameTermination.contains("mate", ignoreCase = true)
                        )))

                val isGameEndNoCheckmate = isLastMoveOfOriginalGame && !isCheckmate && (
                        gameTermination.contains("resignation", ignoreCase = true) ||
                                gameTermination.contains("timeout", ignoreCase = true) ||
                                gameTermination.contains("draw", ignoreCase = true) ||
                                gameTermination.contains("abandoned", ignoreCase = true) ||
                                gameResult != "*"
                        )

                if (isGameEndNoCheckmate) {
                    soundManager.playGameEndSound()
                } else {
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

    // Determine if the current position is being analyzed
    val isCurrentPositionAnalyzing = isEvaluating ||
            (isOnAlternatePath && positions.getOrNull(safeCurrentIndex)?.let {
                it.fenString in analyzingFens.value || it.score == null
            } == true)

    // Get current position and extract relevant data
    val currentPosition = positions.getOrNull(safeCurrentIndex)
    val currentScore = currentPosition?.score
    val currentClassification = currentPosition?.classification

    // Check if current position is a game-ending state (for free moves)
    val freeMoveGameEnd = remember(currentPosition?.fenString, isOnAlternatePath) {
        if (isOnAlternatePath && currentPosition != null) {
            checkGameEnd(currentPosition.fenString)
        } else {
            GameEndState(false, "*", "")
        }
    }

    val badgeUci = remember(currentIndex, currentPosition?.playedMove) {
        currentPosition?.playedMove
    }
    val badgeDrawable = remember(currentIndex, currentClassification, isCurrentPositionAnalyzing) {
        if (isCurrentPositionAnalyzing) {
            null
        } else {
            classificationBadge(currentClassification)
        }
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
        // DESKTOP LAYOUT - Three column layout with persistent panels
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // Enforce minimum width to prevent UI breakage
            val effectiveWidth = maxWidth.coerceAtLeast(900.dp)

            Box(
                modifier = Modifier
                    .width(effectiveWidth)
                    .fillMaxHeight()
                    .align(if (maxWidth < 900.dp) Alignment.TopStart else Alignment.Center)
            ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .focusRequester(focusRequester)
                .focusTarget()
                .onPreviewKeyEvent { keyEvent ->
                    // Handle keyboard navigation
                    if (keyEvent.type == KeyEventType.KeyDown && !isEvaluating && positions.isNotEmpty()) {
                        when (keyEvent.key) {
                            Key.DirectionRight -> {
                                if (currentIndex < positions.lastIndex) {
                                    currentIndex++
                                    true
                                } else false
                            }
                            Key.DirectionLeft -> {
                                if (currentIndex > 0) {
                                    if (isExploringAlternativeLine && currentIndex == alternativeLineReturnIndex) {
                                        while (positions.size > alternativeLineReturnPositionCount) {
                                            positions.removeAt(positions.size - 1)
                                        }
                                        currentIndex = alternativeLineReturnIndex - 1
                                        isExploringAlternativeLine = false
                                        positionsRevision++
                                    } else {
                                        currentIndex--
                                    }
                                    true
                                } else false
                            }
                            Key.DirectionUp -> {
                                if (currentIndex < positions.lastIndex) {
                                    currentIndex = positions.lastIndex
                                    true
                                } else false
                            }
                            Key.DirectionDown -> {
                                if (currentIndex > 0) {
                                    currentIndex = 0
                                    true
                                } else false
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
                    focusRequester.requestFocus()
                }
        ) {
            // LEFT PANEL - Game controls and import
            Surface(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Chess Analysis",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Depth settings - Desktop-optimized compact design
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = DarkSurfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Header row with label and value
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Analysis Depth",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "$analysisDepth",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = BoardDark
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Compact slider with icons
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Quick preset button
                                IconButton(
                                    onClick = {
                                        analysisDepth = 5
                                        UserPreferences.saveAnalysisDepth(analysisDepth)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Bolt,
                                        contentDescription = "Quick (5)",
                                        modifier = Modifier.size(16.dp),
                                        tint = TextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Slider - more compact, no end tick
                                Slider(
                                    value = analysisDepth.toFloat(),
                                    onValueChange = { newValue ->
                                        analysisDepth = newValue.toInt()
                                    },
                                    onValueChangeFinished = {
                                        UserPreferences.saveAnalysisDepth(analysisDepth)
                                    },
                                    valueRange = 5f..20f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = BoardDark,
                                        activeTrackColor = BoardDark,
                                        inactiveTrackColor = BoardDark.copy(alpha = 0.3f)
                                    ),
                                    track = { sliderState ->
                                        SliderDefaults.Track(
                                            colors = SliderDefaults.colors(
                                                thumbColor = BoardDark,
                                                activeTrackColor = BoardDark,
                                                inactiveTrackColor = BoardDark.copy(alpha = 0.3f)
                                            ),
                                            sliderState = sliderState,
                                            drawStopIndicator = null
                                        )
                                    }
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                // Deep preset button
                                IconButton(
                                    onClick = {
                                        analysisDepth = 20
                                        UserPreferences.saveAnalysisDepth(analysisDepth)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    MaterialSymbol(
                                        name = "network_intelligence_history",
                                        tint = TextSecondary,
                                        sizeSp = 18f
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Flip board button (only show when game is loaded)
                    if (pgn != null) {
                        OutlinedButton(
                            onClick = { isBoardFlipped = !isBoardFlipped },
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Icon(Icons.Filled.SwapVert, "Flip board", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Flip Board")
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Import game section
                    Text(
                        text = "Import Game",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Platform selection (if no platform selected) - with icons
                    AnimatedVisibility(visible = selectedPlatform == null) {
                        Column {
                            Button(
                                onClick = { selectedPlatform = com.andrewbibire.chessanalysis.online.Platform.CHESS_COM },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DarkSurfaceVariant,
                                    contentColor = TextPrimary
                                )
                            ) {
                                // Chess pawn icon for Chess.com (green, matching mobile)
                                Box(modifier = Modifier.size(20.dp)) {
                                    MaterialSymbol(
                                        name = "chess_pawn_2",
                                        tint = Color(0xFF80b64d),
                                        fill = 1f,
                                        sizeSp = 20f
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Chess.com", fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { selectedPlatform = com.andrewbibire.chessanalysis.online.Platform.LICHESS },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                            ) {
                                // Chess knight icon for Lichess
                                Box(modifier = Modifier.size(20.dp)) {
                                    MaterialSymbol(
                                        name = "chess_knight",
                                        tint = Color.Black,
                                        fill = 0f,
                                        flipHorizontally = true,
                                        sizeSp = 20f
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Lichess", fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val clipboardText = readClipboard()
                                        if (isValidPgn(clipboardText)) {
                                            pgn = clipboardText
                                        } else {
                                            snackbarHostState.showSnackbar(
                                                message = "Invalid or empty PGN in clipboard",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Filled.ContentPaste, "Paste", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Paste PGN", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Username input (if platform selected)
                    AnimatedVisibility(visible = selectedPlatform != null) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        selectedPlatform = null
                                        isLoadingProfile = false
                                        userProfile = null
                                    }
                                ) {
                                    Icon(Icons.Filled.ArrowBack, "Back")
                                }
                                Text(
                                    text = when (selectedPlatform) {
                                        com.andrewbibire.chessanalysis.online.Platform.CHESS_COM -> "Chess.com"
                                        com.andrewbibire.chessanalysis.online.Platform.LICHESS -> "Lichess"
                                        else -> "Platform"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

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
                                                UserPreferences.saveLastUsername(usernameTextFieldValue.text)
                                                userProfile = result.data
                                            }
                                            is com.andrewbibire.chessanalysis.network.NetworkResult.Error -> {
                                                isLoadingProfile = false
                                                val errorMessage = when {
                                                    result.exception.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                                                            result.exception.message?.contains("timeout", ignoreCase = true) == true ||
                                                            result.exception.message?.contains("Failed to connect", ignoreCase = true) == true ||
                                                            result.exception.message?.contains("Connection refused", ignoreCase = true) == true -> {
                                                        "No internet connection. Please check your network and try again."
                                                    }
                                                    else -> "Unable to find an account with that username"
                                                }
                                                snackbarHostState.showSnackbar(
                                                    message = errorMessage,
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        }
                                    } catch (e: Exception) {
                                        isLoadingProfile = false
                                        snackbarHostState.showSnackbar(
                                            message = "An error occurred: ${e.message}",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = usernameTextFieldValue,
                                onValueChange = { newValue ->
                                    if (isPrefilledUsername) {
                                        if (newValue.text.length < usernameTextFieldValue.text.length) {
                                            usernameTextFieldValue = TextFieldValue(text = "", selection = TextRange(0))
                                            isPrefilledUsername = false
                                        } else {
                                            val oldText = usernameTextFieldValue.text
                                            val newText = newValue.text

                                            val addedText = if (newText.startsWith(oldText)) {
                                                newText.removePrefix(oldText)
                                            } else if (newText.endsWith(oldText)) {
                                                newText.removeSuffix(oldText)
                                            } else {
                                                newText
                                            }

                                            val filteredText = addedText.replace(" ", "")
                                            usernameTextFieldValue = TextFieldValue(text = filteredText, selection = TextRange(filteredText.length))
                                            isPrefilledUsername = false
                                        }
                                    } else {
                                        val filteredText = newValue.text.replace(" ", "")
                                        usernameTextFieldValue = newValue.copy(text = filteredText)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Username") },
                                enabled = !isLoadingProfile,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
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
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = loadGamesAction,
                                enabled = usernameTextFieldValue.text.isNotBlank() && !isLoadingProfile,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BoardDark,
                                    contentColor = Color.White
                                )
                            ) {
                                if (isLoadingProfile) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Load Games", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // Game loaded indicator
                    if (pgn != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Current Game",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = DarkSurfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                leftPlayer?.let {
                                    Text(
                                        text = "$it ${leftElo?.let { "($it)" } ?: ""}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "vs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                rightPlayer?.let {
                                    Text(
                                        text = "$it ${rightElo?.let { "($it)" } ?: ""}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        pgn = null
                                        currentIndex = 0
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DarkSurfaceVariant,
                                        contentColor = TextPrimary
                                    )
                                ) {
                                    Icon(Icons.Filled.Close, "Close game", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Close Game")
                                }
                            }
                        }
                    }
                }
            }

            // CENTER PANEL - Chess board and controls
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.background),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val currentPosition = if (pgn != null && positions.isNotEmpty()) {
                    positions[safeCurrentIndex]
                } else {
                    null
                }

                val isLastOfOriginalGame = if (originalPositions.isNotEmpty()) {
                    safeCurrentIndex == originalPositions.lastIndex && !isOnAlternatePath
                } else {
                    safeCurrentIndex == positions.lastIndex
                }

                val effectiveGameResult = if (freeMoveGameEnd.isEnded) {
                    freeMoveGameEnd.result
                } else {
                    gameResult
                }

                // Chess board with vertical evaluation bar - centered and scaled appropriately
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val maxBoardSize = minOf(maxWidth * 0.75f, maxHeight * 0.9f, 700.dp)

                    if (imagesLoaded || pgn != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.wrapContentSize()
                        ) {
                            // Row with eval bar and board
                            Row(
                                modifier = Modifier.wrapContentSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Vertical evaluation bar to the left (only show when game loaded)
                                if (currentPosition != null) {
                                    VerticalEvaluationBar(
                                        score = if (isCurrentPositionAnalyzing) null else currentPosition.score,
                                        fen = currentPosition.fenString,
                                        gameResult = effectiveGameResult,
                                        isLastMove = isLastOfOriginalGame || freeMoveGameEnd.isEnded,
                                        flipped = isBoardFlipped,
                                        modifier = Modifier.width(40.dp).height(maxBoardSize)
                                    )
                                }

                                Box(
                                    modifier = Modifier.size(maxBoardSize),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val suppressArrow = isCurrentPositionAnalyzing ||
                                            currentClassification == "Best" ||
                                            currentClassification == "Book" ||
                                            currentClassification == "Forced" ||
                                            (isOnAlternatePath && currentClassification == null)

                                    val arrow = if (!suppressArrow && safeCurrentIndex > 0 && positions.isNotEmpty())
                                        positions[safeCurrentIndex - 1].bestMove?.takeIf { it.length >= 4 }?.substring(0, 4)
                                    else null

                                    Chessboard(
                                        fen = currentPosition?.fenString ?: "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                                        arrowUci = arrow,
                                        badgeUci = badgeUci,
                                        badgeDrawable = badgeDrawable,
                                        flipped = isBoardFlipped,
                                        selectedSquare = if (currentPosition != null) selectedSquare else null,
                                        legalMoves = if (currentPosition != null) legalMovesForSelected else emptyList(),
                                        onSquareClick = if (currentPosition != null) onSquareClick else null,
                                        canStartDrag = if (currentPosition != null) canStartDrag else null,
                                        onDragStart = if (currentPosition != null) onDragStart else null,
                                        onDrag = if (currentPosition != null) onDrag else null,
                                        onDragEnd = if (currentPosition != null) onDragEnd else null,
                                        draggedFromSquare = draggedPiece?.fromSquare,
                                        draggedPiece = draggedPiece?.piece,
                                        dragPosition = dragPosition,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            // Message when no game loaded (directly below board)
                            if (pgn == null) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Import a game to start analysis",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Player info (only show when game loaded)
                if (pgn != null && positions.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerProfile(
                            playerName = leftPlayer,
                            rating = leftElo,
                            color = leftColor,
                            avatarUrl = leftAvatar,
                            otherPlayerHasAvatar = rightAvatar != null,
                            isLeftSide = true,
                            fen = currentPosition?.fenString,
                            countryCode = leftCountryCode,
                            avatarSize = 40.dp,
                            flagSize = 20.dp,
                            fontSize = 16.sp,
                            modifier = Modifier
                        )

                        PlayerProfile(
                            playerName = rightPlayer,
                            rating = rightElo,
                            color = rightColor,
                            avatarUrl = rightAvatar,
                            otherPlayerHasAvatar = leftAvatar != null,
                            isLeftSide = false,
                            fen = currentPosition?.fenString,
                            countryCode = rightCountryCode,
                            avatarSize = 40.dp,
                            flagSize = 20.dp,
                            fontSize = 16.sp,
                            modifier = Modifier
                        )
                    }

                    // Control buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { currentIndex = 0 },
                            enabled = currentIndex > 0 && !isEvaluating,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.SkipPrevious, "First move", modifier = Modifier.size(28.dp))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (currentIndex > 0) {
                                    if (isExploringAlternativeLine && currentIndex == alternativeLineReturnIndex) {
                                        while (positions.size > alternativeLineReturnPositionCount) {
                                            positions.removeAt(positions.size - 1)
                                        }
                                        currentIndex = alternativeLineReturnIndex - 1
                                        isExploringAlternativeLine = false
                                        positionsRevision++
                                    } else {
                                        currentIndex--
                                    }
                                }
                            },
                            enabled = currentIndex > 0 && !isEvaluating,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.NavigateBefore, "Previous move", modifier = Modifier.size(28.dp))
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            enabled = currentIndex < positions.lastIndex && !isEvaluating,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(
                            onClick = { if (currentIndex < positions.lastIndex) currentIndex++ },
                            enabled = currentIndex < positions.lastIndex && !isEvaluating,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.NavigateNext, "Next move", modifier = Modifier.size(28.dp))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { currentIndex = positions.lastIndex },
                            enabled = currentIndex < positions.lastIndex && !isEvaluating,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.SkipNext, "Last move", modifier = Modifier.size(28.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // RIGHT PANEL - Analysis details and statistics
            Surface(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Analysis",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (pgn != null && positions.isNotEmpty()) {
                        val currentPosition = positions[safeCurrentIndex]

                        // Analysis progress
                        if (isEvaluating && analysisTotalMoves > 0) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = DarkSurfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Analyzing...", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                                        Text("$analysisProgress / $analysisTotalMoves", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { analysisProgress.toFloat() / analysisTotalMoves.toFloat() },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = BoardDark,
                                        trackColor = BoardDark.copy(alpha = 0.3f),
                                        drawStopIndicator = {}
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Move counter
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = DarkSurfaceVariant
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Move: $currentIndex",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Current position evaluation (hide when analyzing)
                        if (!isCurrentPositionAnalyzing) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = DarkSurfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Position Evaluation",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    val isLastOfOriginalGame = if (originalPositions.isNotEmpty()) {
                                        safeCurrentIndex == originalPositions.lastIndex && !isOnAlternatePath
                                    } else {
                                        safeCurrentIndex == positions.lastIndex
                                    }

                                    if (freeMoveGameEnd.isEnded) {
                                        Text(
                                            text = freeMoveGameEnd.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    } else if (isLastOfOriginalGame) {
                                        Text(
                                            text = gameTermination,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    } else {
                                        val displayScore = normalizeScoreForDisplay(
                                            currentScore,
                                            currentPosition?.fenString ?: "",
                                            isLastOfOriginalGame
                                        )
                                        Text(
                                            text = displayScore,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Compute alternatives button
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    currentPosition?.fenString?.let { fen ->
                                        isLoadingAlternatives = true
                                        try {
                                            val result = stockfishEngine.evaluateWithMultiPV(fen, analysisDepth, 3)
                                            alternativeLines = result.alternativeLines
                                            alternativeLinesFen = fen
                                        } catch (e: Exception) {
                                            println("Error evaluating alternatives: ${e.message}")
                                        } finally {
                                            isLoadingAlternatives = false
                                        }
                                    }
                                }
                            },
                            enabled = !isLoadingAlternatives && currentPosition != null,
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            if (isLoadingAlternatives) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = BoardDark
                                )
                            } else {
                                Icon(Icons.Filled.List, "Alternative Lines", modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Compute Alternatives")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Move classification
                        if (!isCurrentPositionAnalyzing && currentClassification != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = when (currentClassification) {
                                        "Best" -> BestColor.copy(alpha = 0.2f)
                                        "Excellent" -> EvalGreen.copy(alpha = 0.2f)
                                        "Good" -> GoodColor.copy(alpha = 0.2f)
                                        "Inaccuracy" -> InaccuracyColor.copy(alpha = 0.2f)
                                        "Mistake" -> MistakeColor.copy(alpha = 0.2f)
                                        "Blunder" -> BlunderColor.copy(alpha = 0.2f)
                                        "Book" -> BookColor.copy(alpha = 0.2f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Move Quality",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    val playedMoveNotation = if (safeCurrentIndex > 0) {
                                        currentPosition.playedMove?.let { uci ->
                                            uciToSan(uci, positions[safeCurrentIndex - 1].fenString)
                                        } ?: "This move"
                                    } else {
                                        "This move"
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        classificationBadge(currentClassification)?.let { badge ->
                                            androidx.compose.foundation.Image(
                                                painter = org.jetbrains.compose.resources.painterResource(badge),
                                                contentDescription = currentClassification,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Column {
                                            Text(
                                                text = currentClassification,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = playedMoveNotation,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (safeCurrentIndex > 0 && currentClassification != "Best" && currentClassification != "Book" && currentClassification != "Forced") {
                                        positions[safeCurrentIndex - 1].bestMove?.let { bm ->
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "Best move:",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            val notation = uciToSan(bm, positions[safeCurrentIndex - 1].fenString)
                                            Text(
                                                text = notation,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = BestColor
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Opening name
                        currentPosition.openingName?.let { name ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = BookColor.copy(alpha = 0.2f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Opening",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Alternative lines (if loaded)
                        if (alternativeLines.isNotEmpty()) {
                            Text(
                                text = "Alternative Lines",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            alternativeLines.forEachIndexed { index, line ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (index == 0) DarkSurfaceVariant.copy(alpha = 1.2f)
                                        else DarkSurfaceVariant
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Line ${index + 1}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            alternativeLinesFen?.let { fen ->
                                                val displayScore = normalizeScoreForDisplay(
                                                    line.score,
                                                    fen,
                                                    false
                                                )
                                                Text(
                                                    text = displayScore,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Display moves as clickable chips
                                        val sanMoves = alternativeLinesFen?.let { fen ->
                                            convertPvToSan(line.pv.take(5), fen)
                                        } ?: emptyList()

                                        androidx.compose.foundation.layout.FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            sanMoves.forEachIndexed { moveIdx, move ->
                                                Surface(
                                                    modifier = Modifier.clickable {
                                                        exploreAlternativeLine(line.pv, moveIdx)
                                                    },
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                                ) {
                                                    Text(
                                                        text = move,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Game statistics (at starting position)
                        if (safeCurrentIndex == 0) {
                            val leftStats = if (isSearchedPlayerWhite) whiteStats else blackStats
                            val rightStats = if (isSearchedPlayerWhite) blackStats else whiteStats
                            val leftAccuracy = if (isSearchedPlayerWhite) gameAccuracy.white else gameAccuracy.black
                            val rightAccuracy = if (isSearchedPlayerWhite) gameAccuracy.black else gameAccuracy.white

                            val hasStats = (leftStats.best + leftStats.excellent + leftStats.good +
                                    leftStats.inaccuracy + leftStats.mistake + leftStats.blunder +
                                    rightStats.best + rightStats.excellent + rightStats.good +
                                    rightStats.inaccuracy + rightStats.mistake + rightStats.blunder) > 0

                            if (hasStats) {
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Game Statistics",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Accuracy
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text(
                                            text = leftPlayer ?: "Player 1",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        leftAccuracy?.let { acc ->
                                            val rounded = (acc * 10.0).roundToInt() / 10.0
                                            Text(
                                                text = "$rounded%",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = rightPlayer ?: "Player 2",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        rightAccuracy?.let { acc ->
                                            val rounded = (acc * 10.0).roundToInt() / 10.0
                                            Text(
                                                text = "$rounded%",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Move breakdown
                                listOf(
                                    "Best" to Pair(leftStats.best, rightStats.best),
                                    "Excellent" to Pair(leftStats.excellent, rightStats.excellent),
                                    "Good" to Pair(leftStats.good, rightStats.good),
                                    "Inaccuracy" to Pair(leftStats.inaccuracy, rightStats.inaccuracy),
                                    "Mistake" to Pair(leftStats.mistake, rightStats.mistake),
                                    "Blunder" to Pair(leftStats.blunder, rightStats.blunder)
                                ).forEach { (classification, counts) ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = when (classification) {
                                                "Best" -> BestColor.copy(alpha = 0.15f)
                                                "Excellent" -> EvalGreen.copy(alpha = 0.15f)
                                                "Good" -> GoodColor.copy(alpha = 0.15f)
                                                "Inaccuracy" -> InaccuracyColor.copy(alpha = 0.15f)
                                                "Mistake" -> MistakeColor.copy(alpha = 0.15f)
                                                "Blunder" -> BlunderColor.copy(alpha = 0.15f)
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                classificationBadge(classification)?.let { badge ->
                                                    androidx.compose.foundation.Image(
                                                        painter = org.jetbrains.compose.resources.painterResource(badge),
                                                        contentDescription = classification,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                }
                                                Text(
                                                    text = classification,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Row {
                                                Text(
                                                    text = "${counts.first}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.width(30.dp)
                                                )
                                                Text(
                                                    text = "${counts.second}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.width(30.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No game loaded",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Import a game from the left panel to begin analysis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
            }
        }

        // Snackbar overlay
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(16.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Promotion dialog
        if (showPromotionDialog) {
            PromotionDialog(
                isWhite = promotionIsWhite,
                onPieceSelected = { piece ->
                    showPromotionDialog = false
                    promotionFromSquare?.let { from ->
                        promotionToSquare?.let { to ->
                            completeMoveWithPromotion(from, to, piece)
                        }
                    }
                },
                onDismiss = {
                    showPromotionDialog = false
                    selectedSquare = null
                    legalMovesForSelected = emptyList()
                }
            )
        }
    }
}

/**
 * Vertical evaluation bar for desktop layout
 */
@Composable
fun VerticalEvaluationBar(
    score: String?,
    fen: String,
    gameResult: String,
    isLastMove: Boolean,
    flipped: Boolean,
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
        if (flipped) {
            // When flipped: White pieces at top, black pieces at bottom
            // White portion (top, grows downward from top)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    .background(Color.White)
                    .align(Alignment.TopCenter)
            )
        } else {
            // Normal orientation: Black pieces at top, white pieces at bottom
            // White portion (bottom, grows upward from bottom)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    .background(Color.White)
                    .align(Alignment.BottomCenter)
            )
        }

        // Scores column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (flipped) {
                // When flipped: White bar at top, black bar at bottom
                // White score at top (on white background when white is winning)
                if (whiteScore != null) {
                    Text(
                        text = whiteScore,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (fraction <= 0f) Color.White else Color(0xFF3a3a3a),
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Spacer(modifier = Modifier.height(1.dp))
                }

                // Black score at bottom (on black background when black is winning)
                if (blackScore != null) {
                    Text(
                        text = blackScore,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (fraction >= 1f) Color(0xFF3a3a3a) else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Spacer(modifier = Modifier.height(1.dp))
                }
            } else {
                // Normal: White bar at bottom, black bar at top
                // Black score at top (on black background when black is winning)
                if (blackScore != null) {
                    Text(
                        text = blackScore,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (fraction >= 1f) Color(0xFF3a3a3a) else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Spacer(modifier = Modifier.height(1.dp))
                }

                // White score at bottom (on white background when white is winning)
                if (whiteScore != null) {
                    Text(
                        text = whiteScore,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (fraction <= 0f) Color.White else Color(0xFF3a3a3a),
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Spacer(modifier = Modifier.height(1.dp))
                }
            }
        }
    }
}
