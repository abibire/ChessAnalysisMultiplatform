package com.andrewbibire.chessanalysis

data class MaterialInfo(
    val whiteCaptured: Map<Char, Int>,  // lowercase piece -> count
    val blackCaptured: Map<Char, Int>,  // uppercase piece -> count
    val whiteAdvantage: Int  // positive if white ahead, negative if black ahead
)

fun calculateMaterial(fen: String): MaterialInfo {
    val board = fen.substringBefore(' ')

    // Count current pieces on board
    // In FEN: uppercase = white pieces, lowercase = black pieces
    val whitePieces = mutableMapOf<Char, Int>()
    val blackPieces = mutableMapOf<Char, Int>()

    board.forEach { char ->
        when {
            char in 'A'..'Z' -> whitePieces[char] = (whitePieces[char] ?: 0) + 1
            char in 'a'..'z' -> blackPieces[char] = (blackPieces[char] ?: 0) + 1
        }
    }

    // Starting pieces for each side
    val startingPieces = mapOf(
        'p' to 8, 'P' to 8,
        'r' to 2, 'R' to 2,
        'n' to 2, 'N' to 2,
        'b' to 2, 'B' to 2,
        'q' to 1, 'Q' to 1
    )

    // Calculate captured pieces (what's missing from the board)
    val whiteCaptured = mutableMapOf<Char, Int>()
    val blackCaptured = mutableMapOf<Char, Int>()

    // White captured black pieces (black's missing pieces)
    listOf('p', 'r', 'n', 'b', 'q').forEach { piece ->
        val starting = startingPieces[piece] ?: 0
        val current = blackPieces[piece] ?: 0
        val captured = starting - current
        if (captured > 0) {
            whiteCaptured[piece] = captured
        }
    }

    // Black captured white pieces (white's missing pieces)
    listOf('P', 'R', 'N', 'B', 'Q').forEach { piece ->
        val starting = startingPieces[piece] ?: 0
        val current = whitePieces[piece] ?: 0
        val captured = starting - current
        if (captured > 0) {
            blackCaptured[piece.lowercaseChar()] = captured
        }
    }

    // Calculate material values
    val pieceValues = mapOf('p' to 1, 'n' to 3, 'b' to 3, 'r' to 5, 'q' to 9)

    val whiteValue = whitePieces.entries.sumOf { (piece, count) ->
        if (piece == 'K') 0 else (pieceValues[piece.lowercaseChar()] ?: 0) * count
    }

    val blackValue = blackPieces.entries.sumOf { (piece, count) ->
        if (piece == 'k') 0 else (pieceValues[piece] ?: 0) * count
    }

    val advantage = whiteValue - blackValue

    return MaterialInfo(whiteCaptured, blackCaptured, advantage)
}
