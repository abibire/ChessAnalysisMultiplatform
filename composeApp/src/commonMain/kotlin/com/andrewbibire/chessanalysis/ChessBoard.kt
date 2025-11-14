package com.andrewbibire.chessanalysis

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.layout.ContentScale
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import chessanalysis.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

// Data class to track square positions
data class SquareBounds(val position: androidx.compose.ui.geometry.Offset, val size: IntSize)

@Composable
fun Chessboard(
    fen: String,
    arrowUci: String? = null,
    badgeUci: String? = null,
    badgeDrawable: DrawableResource? = null,
    flipped: Boolean = false,
    modifier: Modifier = Modifier,
    selectedSquare: String? = null,
    legalMoves: List<String> = emptyList(),
    onSquareClick: ((String) -> Unit)? = null,
    canStartDrag: ((String) -> Boolean)? = null,
    onDragStart: ((String, String) -> Unit)? = null,
    onDragEnd: ((String?) -> Unit)? = null,
    draggedFromSquare: String? = null
) {
    val board = parseFenToBoard(fen)

    // Track all square positions
    val squarePositions = remember { mutableStateMapOf<String, SquareBounds>() }

    // Clear stale positions when board flips (UCI-to-position mapping changes)
    LaunchedEffect(flipped) {
        squarePositions.clear()
    }

    // Function to find which square contains a position
    fun findSquareAt(position: androidx.compose.ui.geometry.Offset): String? {
        println("FIND_SQUARE: Looking for square at position $position, flipped=$flipped")
        println("FIND_SQUARE: Available squares: ${squarePositions.keys.sorted()}")
        val result = squarePositions.entries.firstOrNull { (square, bounds) ->
            val contains = position.x >= bounds.position.x &&
                position.x < bounds.position.x + bounds.size.width &&
                position.y >= bounds.position.y &&
                position.y < bounds.position.y + bounds.size.height
            if (contains) {
                println("FIND_SQUARE: Found match - square=$square, bounds=[${bounds.position.x},${bounds.position.x + bounds.size.width}) x [${bounds.position.y},${bounds.position.y + bounds.size.height})")
            }
            contains
        }?.key
        println("FIND_SQUARE: Result = $result")
        return result
    }

    BoxWithConstraints(modifier = modifier) {
        val squareW: Dp = maxWidth / 8
        val squareH: Dp = maxHeight / 8
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0..7) {
                    val displayRow = if (flipped) 7 - row else row
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        for (col in 0..7) {
                            val displayCol = if (flipped) 7 - col else col
                            val isLight = (row + col) % 2 == 0
                            val piece = board[displayRow][displayCol]
                            val squareUci = coordsToUci(displayRow, displayCol)
                            val isSelected = squareUci == selectedSquare
                            val isLegalMove = legalMoves.contains(squareUci)
                            val isDraggedFrom = squareUci == draggedFromSquare

                            ChessSquare(
                                piece = piece,
                                isLight = isLight,
                                isSelected = isSelected,
                                isLegalMove = isLegalMove,
                                isDraggedFrom = isDraggedFrom,
                                squareUci = squareUci,
                                onClick = { onSquareClick?.invoke(squareUci) },
                                canStartDrag = { canStartDrag?.invoke(squareUci) ?: true },
                                onDragStart = { onDragStart?.invoke(squareUci, piece) },
                                onDragEnd = { dropPosition ->
                                    val targetSquare = findSquareAt(dropPosition)
                                    onDragEnd?.invoke(targetSquare)
                                },
                                onPositionChanged = { position, size ->
                                    squarePositions[squareUci] = SquareBounds(position, size)
                                },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }
                }
            }
            if (arrowUci != null && arrowUci.length >= 4) {
                Canvas(modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Ensure the canvas doesn't block touch events
                        compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
                    }
                ) {
                    val cw = size.width / 8f
                    val ch = size.height / 8f
                    val from = uciToCoords(arrowUci.substring(0, 2))
                    val to = uciToCoords(arrowUci.substring(2, 4))
                    val sx = ((if (flipped) 7 - from.second else from.second) + 0.5f) * cw
                    val sy = ((if (flipped) from.first else 7 - from.first) + 0.5f) * ch
                    val ex = ((if (flipped) 7 - to.second else to.second) + 0.5f) * cw
                    val ey = ((if (flipped) to.first else 7 - to.first) + 0.5f) * ch
                    val dx = ex - sx
                    val dy = ey - sy
                    val len = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (len > 0f) {
                        val ux = dx / len
                        val uy = dy / len
                        val ah = cw * 0.5f
                        val aw = cw * 0.35f
                        val shaftEndX = ex - ux * ah
                        val shaftEndY = ey - uy * ah
                        drawLine(
                            color = Color(0xCC81C14B),
                            start = androidx.compose.ui.geometry.Offset(sx, sy),
                            end = androidx.compose.ui.geometry.Offset(shaftEndX, shaftEndY),
                            strokeWidth = cw * 0.22f,
                            cap = StrokeCap.Butt
                        )
                        val nx = -uy
                        val ny = ux
                        val p1x = shaftEndX + nx * aw
                        val p1y = shaftEndY + ny * aw
                        val p2x = shaftEndX - nx * aw
                        val p2y = shaftEndY - ny * aw
                        val path = Path()
                        path.moveTo(ex, ey)
                        path.lineTo(p1x, p1y)
                        path.lineTo(p2x, p2y)
                        path.close()
                        drawPath(path = path, color = Color(0xCC81C14B))
                    }
                }
            }
            if (badgeDrawable != null && badgeUci != null && badgeUci.length >= 4) {
                val to = uciToCoords(badgeUci.substring(2, 4))
                val badgeSize = (if (squareW < squareH) squareW else squareH) * 0.38f
                val displayCol = if (flipped) 7 - to.second else to.second
                val displayRow = if (flipped) to.first else 7 - to.first
                val squareLeft = squareW * displayCol
                val squareTop = squareH * displayRow
                val adjustment = 3.dp
                // Adjust X position for rightmost file to prevent cutoff (H file normally, A file when flipped)
                val isRightmostFile = if (flipped) to.second == 0 else to.second == 7
                val offsetXDp = if (isRightmostFile) {
                    squareLeft + squareW - badgeSize - adjustment
                } else {
                    squareLeft + squareW - (badgeSize / 2) - adjustment
                }
                val offsetYDp = squareTop - (badgeSize / 2) + adjustment
                Image(
                    painter = painterResource(badgeDrawable),
                    contentDescription = "classification",
                    modifier = Modifier
                        .offset(x = offsetXDp, y = offsetYDp)
                        .size(badgeSize)
                )
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun ChessSquare(
    piece: String,
    isLight: Boolean,
    isSelected: Boolean = false,
    isLegalMove: Boolean = false,
    isDraggedFrom: Boolean = false,
    squareUci: String = "",
    onClick: () -> Unit = {},
    canStartDrag: () -> Boolean = { true },
    onDragStart: () -> Unit = {},
    onDragEnd: (androidx.compose.ui.geometry.Offset) -> Unit = {},
    onPositionChanged: (androidx.compose.ui.geometry.Offset, IntSize) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> Color(0xFFBBCA44)  // Highlight selected square
        isLight -> Color(0xFFEEEED2)
        else -> Color(0xFF769656)
    }

    var dragStartOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var currentDragOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var squarePosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Box(
        modifier = modifier
            .background(backgroundColor)
            .onGloballyPositioned { coordinates ->
                val newPos = coordinates.positionInRoot()
                val newSize = coordinates.size
                squarePosition = newPos
                onPositionChanged(newPos, newSize)
            }
            .pointerInput(squareUci, piece, canStartDrag, onDragStart, onDragEnd) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        println("DRAG_START: square=$squareUci, piece='$piece', isEmpty=${piece.isEmpty()}")
                        if (piece.isNotEmpty() && canStartDrag()) {
                            dragStartOffset = startOffset
                            currentDragOffset = androidx.compose.ui.geometry.Offset.Zero
                            onDragStart()
                            println("DRAG_START: Called onDragStart for $squareUci")
                        } else {
                            println("DRAG_START: Piece is empty or cannot start drag, not calling onDragStart")
                        }
                    },
                    onDragEnd = {
                        println("DRAG_END_CALLBACK: square=$squareUci")
                        if (piece.isNotEmpty()) {
                            // Calculate absolute drop position
                            val dropPosition = squarePosition + dragStartOffset + currentDragOffset
                            println("DRAG_END_CALC: square=$squareUci, squarePosition=$squarePosition, dragStartOffset=$dragStartOffset, currentDragOffset=$currentDragOffset, dropPosition=$dropPosition")
                            onDragEnd(dropPosition)
                        } else {
                            // Even if piece is empty now, still clear state
                            onDragEnd(androidx.compose.ui.geometry.Offset.Zero)
                        }
                        currentDragOffset = androidx.compose.ui.geometry.Offset.Zero
                        dragStartOffset = androidx.compose.ui.geometry.Offset.Zero
                    },
                    onDragCancel = {
                        println("DRAG_CANCEL: square=$squareUci")
                        // Always call onDragEnd to clear state, even on cancel
                        onDragEnd(androidx.compose.ui.geometry.Offset.Zero)
                        currentDragOffset = androidx.compose.ui.geometry.Offset.Zero
                        dragStartOffset = androidx.compose.ui.geometry.Offset.Zero
                    },
                    onDrag = { _, dragAmount ->
                        currentDragOffset += dragAmount
                    }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (piece.isNotEmpty()) {
            val pieceFileName = getPieceSvgFileName(piece)
            if (pieceFileName != null) {
                val context = LocalPlatformContext.current
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(Res.getUri("drawable/$pieceFileName"))
                        .build(),
                    contentDescription = piece,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .graphicsLayer {
                            alpha = if (isDraggedFrom) 0.3f else 1f
                        }
                )
            }
        }

        // Draw indicator for legal move squares (chess.com style)
        if (isLegalMove) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (piece.isNotEmpty()) {
                    // Capture: draw hollow ring around the edge
                    val strokeWidth = size.width * 0.08f
                    val radius = (size.width / 2) - (strokeWidth / 2)
                    drawCircle(
                        color = Color(0x60000000),  // Semi-transparent black
                        radius = radius,
                        center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                    )
                } else {
                    // Empty square: draw small filled dot in center
                    val radius = size.width * 0.15f
                    drawCircle(
                        color = Color(0x60000000),  // Semi-transparent black
                        radius = radius,
                        center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                    )
                }
            }
        }
    }
}

fun parseFenToBoard(fen: String): Array<Array<String>> {
    val board = Array(8) { Array(8) { "" } }
    val pos = fen.split(" ")[0]
    val ranks = pos.split("/")
    for ((r, rank) in ranks.withIndex()) {
        var c = 0
        for (ch in rank) {
            if (ch.isDigit()) {
                c += ch.digitToInt()
            } else {
                board[r][c] = ch.toString()
                c++
            }
        }
    }
    return board
}

fun getPieceSvgFileName(piece: String): String? {
    return when (piece) {
        "K" -> "wK.svg"
        "Q" -> "wQ.svg"
        "R" -> "wR.svg"
        "B" -> "wB.svg"
        "N" -> "wN.svg"
        "P" -> "wP.svg"
        "k" -> "bK.svg"
        "q" -> "bQ.svg"
        "r" -> "bR.svg"
        "b" -> "bB.svg"
        "n" -> "bN.svg"
        "p" -> "bP.svg"
        else -> null
    }
}

fun uciToCoords(sq: String): Pair<Int, Int> {
    val file = sq[0] - 'a'
    val rank = sq[1] - '1'
    return rank to file
}

fun coordsToUci(row: Int, col: Int): String {
    // In FEN board array: row 0 = rank 8, row 7 = rank 1
    val file = ('a' + col)
    val rank = ('8' - row)
    return "$file$rank"
}