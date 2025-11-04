package com.andrewbibire.chessanalysis

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
import org.jetbrains.compose.resources.painterResource

@Composable
fun Chessboard(fen: String, modifier: Modifier = Modifier) {
    val board = parseFenToBoard(fen)
    Column(modifier = modifier) {
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
}

@Composable
fun ChessSquare(piece: String, isLight: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(if (isLight) Color(0xFFEEEED2) else Color(0xFF769656)),
        contentAlignment = Alignment.Center
    ) {
        if (piece.isNotEmpty()) {
            val resourcePath = getPieceResourcePath(piece)
            if (resourcePath != null) {
                Image(
                    painter = painterResource(resourcePath),
                    contentDescription = piece,
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            }
        }
    }
}

fun parseFenToBoard(fen: String): Array<Array<String>> {
    val board = Array(8) { Array(8) { "" } }
    val fenPosition = fen.split(" ")[0]
    val ranks = fenPosition.split("/")
    for ((rankIndex, rank) in ranks.withIndex()) {
        var fileIndex = 0
        for (char in rank) {
            if (char.isDigit()) {
                fileIndex += char.digitToInt()
            } else {
                board[rankIndex][fileIndex] = char.toString()
                fileIndex++
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