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
import chessanalysis.composeapp.generated.resources.Res
import chessanalysis.composeapp.generated.resources.bB
import chessanalysis.composeapp.generated.resources.bK
import chessanalysis.composeapp.generated.resources.bN
import chessanalysis.composeapp.generated.resources.bP
import chessanalysis.composeapp.generated.resources.bQ
import chessanalysis.composeapp.generated.resources.bR
import chessanalysis.composeapp.generated.resources.wB
import chessanalysis.composeapp.generated.resources.wK
import chessanalysis.composeapp.generated.resources.wN
import chessanalysis.composeapp.generated.resources.wP
import chessanalysis.composeapp.generated.resources.wQ
import chessanalysis.composeapp.generated.resources.wR
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun Chessboard(
    fen: String,
    arrowUci: String? = null,
    badgeUci: String? = null,
    badgeDrawable: DrawableResource? = null,
    modifier: Modifier = Modifier
) {
    val board = parseFenToBoard(fen)
    BoxWithConstraints(modifier = modifier) {
        val squareW: Dp = maxWidth / 8
        val squareH: Dp = maxHeight / 8
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0..7) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        for (col in 0..7) {
                            val isLight = (row + col) % 2 == 0
                            val piece = board[row][col]
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
                    val sx = (from.second + 0.5f) * cw
                    val sy = ((7 - from.first) + 0.5f) * ch
                    val ex = (to.second + 0.5f) * cw
                    val ey = ((7 - to.first) + 0.5f) * ch
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
                            color = Color(0xFF7FA650),
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
                        drawPath(path = path, color = Color(0xFF7FA650))
                    }
                }
            }
            if (badgeDrawable != null && badgeUci != null && badgeUci.length >= 4) {
                val to = uciToCoords(badgeUci.substring(2, 4))
                val badgeSize = (if (squareW < squareH) squareW else squareH) * 0.36f
                val squareLeft = squareW * to.second
                val squareTop = squareH * (7 - to.first)
                val offsetXDp = squareLeft + squareW - badgeSize
                val offsetYDp = squareTop
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

@Composable
fun ChessSquare(piece: String, isLight: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(if (isLight) Color(0xFFEEEED2) else Color(0xFF769656)),
        contentAlignment = Alignment.Center
    ) {
        if (piece.isNotEmpty()) {
            val res = getPieceResourcePath(piece)
            if (res != null) {
                Image(
                    painter = painterResource(res),
                    contentDescription = piece,
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

fun getPieceResourcePath(piece: String): org.jetbrains.compose.resources.DrawableResource? {
    return when (piece) {
        "K" -> Res.drawable.wK
        "Q" -> Res.drawable.wQ
        "R" -> Res.drawable.wR
        "B" -> Res.drawable.wB
        "N" -> Res.drawable.wN
        "P" -> Res.drawable.wP
        "k" -> Res.drawable.bK
        "q" -> Res.drawable.bQ
        "r" -> Res.drawable.bR
        "b" -> Res.drawable.bB
        "n" -> Res.drawable.bN
        "p" -> Res.drawable.bP
        else -> null
    }
}

fun uciToCoords(sq: String): Pair<Int, Int> {
    val file = sq[0] - 'a'
    val rank = sq[1] - '1'
    return rank to file
}
