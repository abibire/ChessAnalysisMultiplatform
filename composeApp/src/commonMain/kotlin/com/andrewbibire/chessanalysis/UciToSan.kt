package com.andrewbibire.chessanalysis

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.move.Move

fun uciToSan(uci: String, fen: String): String {
    if (uci.length < 4) return uci

    return try {
        val board = Board()
        board.loadFromFen(fen)

        val from = Square.valueOf(uci.substring(0, 2).uppercase())
        val to = Square.valueOf(uci.substring(2, 4).uppercase())

        val move = if (uci.length > 4) {
            val promotionPiece = when (uci[4].lowercaseChar()) {
                'q' -> Piece.WHITE_QUEEN
                'r' -> Piece.WHITE_ROOK
                'b' -> Piece.WHITE_BISHOP
                'n' -> Piece.WHITE_KNIGHT
                else -> return uci
            }
            Move(from, to, promotionPiece)
        } else {
            Move(from, to)
        }

        val piece = board.getPiece(from)
        val captureNotation = if (board.getPiece(to) != Piece.NONE) "x" else ""

        board.doMove(move, true)

        val notation = when {
            move.toString().contains("O-O-O") -> "O-O-O"
            move.toString().contains("O-O") -> "O-O"
            piece.pieceType?.name == "PAWN" -> {
                val file = from.toString()[0].lowercase()
                if (captureNotation.isNotEmpty()) {
                    "$file$captureNotation${to.toString().lowercase()}" +
                            if (uci.length > 4) "=${uci[4].uppercaseChar()}" else ""
                } else {
                    to.toString().lowercase() +
                            if (uci.length > 4) "=${uci[4].uppercaseChar()}" else ""
                }
            }
            else -> {
                val pieceSymbol = when (piece.pieceType?.name) {
                    "KNIGHT" -> "N"
                    "BISHOP" -> "B"
                    "ROOK" -> "R"
                    "QUEEN" -> "Q"
                    "KING" -> "K"
                    else -> ""
                }
                "$pieceSymbol$captureNotation${to.toString().lowercase()}"
            }
        }

        val check = if (board.isMated) "#" else if (board.isKingAttacked) "+" else ""
        notation + check
    } catch (e: Exception) {
        println("Error converting UCI $uci to SAN: ${e.message}")
        e.printStackTrace()
        uci
    }
}