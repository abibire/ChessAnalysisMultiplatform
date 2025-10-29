package com.andrewbibire.chessanalysis

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.pgn.PgnHolder
import java.io.File

fun generateFensFromPgn(pgn: String): List<Position> {
    val board = Board()
    val positions = mutableListOf<Position>()
    val tempFile = File.createTempFile("chess_pgn", ".pgn")
    try {
        tempFile.writeText(pgn)
        val pgnHolder = PgnHolder(tempFile.absolutePath)
        pgnHolder.loadPgn()
        val game = pgnHolder.games.firstOrNull() ?: return positions
        positions.add(Position(fenString = board.fen))
        for (move in game.halfMoves) {
            val chessMove = Move(move.from, move.to, move.promotion)
            board.doMove(chessMove)
            positions.add(Position(fenString = board.fen))
        }
    } finally {
        tempFile.delete()
    }
    return positions
}