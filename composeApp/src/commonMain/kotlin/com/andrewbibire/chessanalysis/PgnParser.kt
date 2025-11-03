package com.andrewbibire.chessanalysis

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.pgn.PgnHolder

fun generateFensFromPgn(pgn: String): List<Position> {
    val board = Board()
    val positions = mutableListOf<Position>()
    val pgnHolder = PgnHolder("")
    pgnHolder.loadPgn(pgn)
    val game = pgnHolder.games.firstOrNull() ?: return positions
    positions.add(Position(fenString = board.fen))
    for (move in game.halfMoves) {
        val chessMove = Move(move.from, move.to, move.promotion)
        board.doMove(chessMove)
        val playedMove = "${move.from.toString().lowercase()}${move.to.toString().lowercase()}"
        positions.add(Position(fenString = board.fen, playedMove = playedMove))
    }
    return positions
}