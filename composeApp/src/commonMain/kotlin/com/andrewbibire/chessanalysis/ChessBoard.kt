package com.andrewbibire.chessanalysis

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.layout.ContentScale
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import chessanalysis.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@Composable
fun Chessboard(
    fen: String,
    arrowUci: String? = null,
    badgeUci: String? = null,
    badgeDrawable: DrawableResource? = null,
    flipped: Boolean = false,
    modifier: Modifier = Modifier
) {
    val board = parseFenToBoard(fen)
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
                            ChessSquare(
                                piece = piece,
                                isLight = isLight,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }
                }
            }
            if (arrowUci != null && arrowUci.length >= 4) {
                Canvas(modifier = Modifier.fillMaxSize()) {
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
fun ChessSquare(piece: String, isLight: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(if (isLight) Color(0xFFEEEED2) else Color(0xFF769656)),
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
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
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