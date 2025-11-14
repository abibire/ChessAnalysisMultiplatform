package com.andrewbibire.chessanalysis

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.pgn.PgnHolder

/**
 * Validates if a PGN can be successfully parsed
 * Returns true if at least 3 positions can be generated (start + at least 2 moves)
 */
fun isValidParsablePgn(pgn: String): Boolean {
    return try {
        val positions = generateFensFromPgn(pgn)
        // Require at least starting position + 2 moves
        positions.size >= 3
    } catch (e: Exception) {
        false
    }
}

fun generateFensFromPgn(pgn: String): List<Position> {
    // First try the standard PgnHolder approach
    val standardResult = tryStandardParsing(pgn)
    if (standardResult.isNotEmpty()) {
        return standardResult
    }

    // If that fails, try custom SAN parsing
    println("Standard parsing failed, trying custom SAN parser")
    return tryCustomSanParsing(pgn)
}

private fun tryStandardParsing(pgn: String): List<Position> {
    val board = Board()
    val positions = mutableListOf<Position>()
    val pgnHolder = PgnHolder("")

    try {
        // Clean the PGN by removing clock annotations and fixing misleading event names
        var cleanedPgn = pgn.replace(Regex("""\{[^}]*\}"""), "")

        // Replace "Chess960" in Event tag to prevent parser from rejecting standard games
        cleanedPgn = cleanedPgn.replace(Regex("""\[Event "Live Chess - Chess960"\]"""), """[Event "Live Chess"]""")
        pgnHolder.loadPgn(cleanedPgn)
        val game = pgnHolder.games.firstOrNull()
        if (game == null) {
            println("No game found after parsing PGN")
            return positions
        }
        positions.add(Position(fenString = board.fen))

        for (move in game.halfMoves) {
            try {
                // Skip moves with null coordinates - can't process them
                if (move.from == null || move.to == null) {
                    println("Move with null coordinates: ${move.san}")
                    // Can't continue without proper move parsing
                    return emptyList()
                }

                val chessMove = Move(move.from, move.to, move.promotion)

                board.doMove(chessMove)
                val playedMove = if (chessMove.promotion != null) {
                    // Convert promotion piece to single letter (e.g., "WHITE_QUEEN" -> "q")
                    val promotionLetter = when (chessMove.promotion.toString().lowercase()) {
                        "white_queen", "black_queen" -> "q"
                        "white_rook", "black_rook" -> "r"
                        "white_bishop", "black_bishop" -> "b"
                        "white_knight", "black_knight" -> "n"
                        else -> chessMove.promotion.toString().lowercase().last().toString()
                    }
                    "${chessMove.from.toString().lowercase()}${chessMove.to.toString().lowercase()}$promotionLetter"
                } else {
                    "${chessMove.from.toString().lowercase()}${chessMove.to.toString().lowercase()}"
                }

                // Store the SAN notation for sound detection
                val san = move.san ?: ""

                positions.add(Position(fenString = board.fen, playedMove = playedMove, sanNotation = san))
            } catch (e: Exception) {
                println("Failed to parse move ${move.san}: ${e.message}")
                // If a move fails, we can't continue reliably
                return emptyList()
            }
        }
    } catch (e: Exception) {
        println("Failed to parse PGN: ${e.message}")
        return emptyList()
    }
    return positions
}

private fun tryCustomSanParsing(pgn: String): List<Position> {
    val board = Board()
    val positions = mutableListOf<Position>()

    try {
        // Extract the moves section from PGN
        val movesText = extractMovesFromPgn(pgn)
        if (movesText.isEmpty()) {
            println("No moves found in PGN")
            return positions
        }

        // Parse move tokens (e.g., "1. e4 e5 2. Nf3 Nc6")
        val moveTokens = parseMoveTokens(movesText)
        println("Found ${moveTokens.size} move tokens")

        positions.add(Position(fenString = board.fen))

        for (sanMove in moveTokens) {
            try {
                val move = findMoveFromSan(board, sanMove)
                if (move == null) {
                    println("Could not find legal move for SAN: $sanMove")
                    break
                }

                board.doMove(move)
                val playedMove = if (move.promotion != null) {
                    // Convert promotion piece to single letter (e.g., "WHITE_QUEEN" -> "q")
                    val promotionLetter = when (move.promotion.toString().lowercase()) {
                        "white_queen", "black_queen" -> "q"
                        "white_rook", "black_rook" -> "r"
                        "white_bishop", "black_bishop" -> "b"
                        "white_knight", "black_knight" -> "n"
                        else -> move.promotion.toString().lowercase().last().toString()
                    }
                    "${move.from.toString().lowercase()}${move.to.toString().lowercase()}$promotionLetter"
                } else {
                    "${move.from.toString().lowercase()}${move.to.toString().lowercase()}"
                }

                positions.add(Position(fenString = board.fen, playedMove = playedMove, sanNotation = sanMove))
            } catch (e: Exception) {
                println("Error applying move $sanMove: ${e.message}")
                break
            }
        }
    } catch (e: Exception) {
        println("Custom parsing failed: ${e.message}")
    }
    return positions
}

private fun extractMovesFromPgn(pgn: String): String {
    // Remove headers (lines starting with [)
    val lines = pgn.lines().filter { !it.trim().startsWith("[") }
    return lines.joinToString(" ")
}

private fun parseMoveTokens(movesText: String): List<String> {
    // Remove comments in braces
    var cleaned = movesText.replace(Regex("""\{[^}]*\}"""), "")
    // Remove move numbers (e.g., "1.", "2.", "10.")
    cleaned = cleaned.replace(Regex("""\d+\."""), "")
    // Remove result (1-0, 0-1, 1/2-1/2)
    cleaned = cleaned.replace(Regex("""[01]/[012]-[01]/[012]|[01]-[01]"""), "")
    // Split on whitespace and filter empty
    return cleaned.split(Regex("""\s+""")).filter { it.isNotBlank() }
}

private fun findMoveFromSan(board: Board, san: String): Move? {
    val legalMoves = board.legalMoves()

    // Clean the SAN (remove check/checkmate symbols)
    val cleanSan = san.replace("+", "").replace("#", "").replace("!", "").replace("?", "")

    // Handle castling
    if (cleanSan == "O-O" || cleanSan == "0-0") {
        return legalMoves.firstOrNull { it.toString().contains("g1") || it.toString().contains("g8") }
    }
    if (cleanSan == "O-O-O" || cleanSan == "0-0-0") {
        return legalMoves.firstOrNull { it.toString().contains("c1") || it.toString().contains("c8") }
    }

    // Try to match the move
    for (move in legalMoves) {
        if (matchesSan(move, cleanSan, board)) {
            return move
        }
    }
    return null
}

private fun matchesSan(move: Move, san: String, board: Board): Boolean {
    val from = move.from
    val to = move.to
    val piece = board.getPiece(from)
    val pieceType = piece.pieceType ?: return false

    // Pawn moves
    if (pieceType.name.startsWith("PAWN")) {
        // Pawn capture (e.g., "exd4")
        if (san.length >= 3 && san[1] == 'x') {
            val fromFile = san[0].toString()
            val toSquare = san.substring(2, 4)
            return from.toString().lowercase().startsWith(fromFile.lowercase()) &&
                   to.toString().lowercase() == toSquare.lowercase()
        }
        // Regular pawn move (e.g., "e4")
        if (san.length == 2) {
            return to.toString().lowercase() == san.lowercase()
        }
        // Pawn promotion (e.g., "e8=Q")
        if (san.contains("=")) {
            val toSquare = san.substring(0, 2)
            return to.toString().lowercase() == toSquare.lowercase()
        }
    }

    // Piece moves (e.g., "Nf3", "Bxe5", "Rad1")
    val pieceChar = when (pieceType.name) {
        "KNIGHT" -> 'N'
        "BISHOP" -> 'B'
        "ROOK" -> 'R'
        "QUEEN" -> 'Q'
        "KING" -> 'K'
        else -> return false
    }

    if (san.isEmpty() || san[0] != pieceChar) {
        return false
    }

    // Extract destination square from SAN
    val destMatch = Regex("""([a-h][1-8])""").findAll(san).lastOrNull()
    if (destMatch != null) {
        val dest = destMatch.value
        if (to.toString().lowercase() != dest.lowercase()) {
            return false
        }

        // Check disambiguation (e.g., "Rad1" means rook from a-file)
        if (san.length > 3 && san[1].isLowerCase()) {
            val disambig = san[1]
            if (!from.toString().lowercase().contains(disambig.lowercase())) {
                return false
            }
        }
        return true
    }
    return false
}