package com.andrewbibire.chessanalysis

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.move.Move

fun uciToSan(uci: String, fen: String): String {
    val cleanUci = uci.trim()
    if (cleanUci.length < 4) return cleanUci

    return try {
        val board = Board()
        board.loadFromFen(fen)

        val from = Square.valueOf(cleanUci.substring(0, 2).uppercase())
        val to = Square.valueOf(cleanUci.substring(2, 4).uppercase())

        val piece = board.getPiece(from)

        // Detect castling from UCI notation (king moving to specific squares)
        val isCastling = piece.pieceType?.name == "KING" && (
            cleanUci == "e1g1" || cleanUci == "e1c1" || cleanUci == "e8g8" || cleanUci == "e8c8"
        )

        // Only treat as promotion if length is exactly 5 and 5th char is a valid promotion piece
        val isPromotion = cleanUci.length == 5 && cleanUci[4].lowercaseChar() in listOf('q', 'r', 'b', 'n')

        val move = if (isPromotion) {
            val promotionPiece = when (cleanUci[4].lowercaseChar()) {
                'q' -> Piece.WHITE_QUEEN
                'r' -> Piece.WHITE_ROOK
                'b' -> Piece.WHITE_BISHOP
                'n' -> Piece.WHITE_KNIGHT
                else -> return cleanUci
            }
            Move(from, to, promotionPiece)
        } else {
            Move(from, to)
        }

        // Check if it's a capture (includes en passant)
        // En passant: pawn moves diagonally to empty square
        val isPawnMovingDiagonally = piece.pieceType?.name == "PAWN" && from.file != to.file
        val isNormalCapture = board.getPiece(to) != Piece.NONE
        val captureNotation = if (isNormalCapture || isPawnMovingDiagonally) "x" else ""

        board.doMove(move, true)

        val notation = when {
            isCastling && (cleanUci.endsWith("c1") || cleanUci.endsWith("c8")) -> "O-O-O"
            isCastling -> "O-O"
            piece.pieceType?.name == "PAWN" -> {
                val file = from.toString()[0].lowercase()
                val promotionSuffix = if (isPromotion) "=${cleanUci[4].uppercaseChar()}" else ""
                if (captureNotation.isNotEmpty()) {
                    "$file$captureNotation${to.toString().lowercase()}$promotionSuffix"
                } else {
                    "${to.toString().lowercase()}$promotionSuffix"
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
        println("Error converting UCI $cleanUci to SAN: ${e.message}")
        e.printStackTrace()
        cleanUci
    }
}