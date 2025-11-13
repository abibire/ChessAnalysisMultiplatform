package com.andrewbibire.chessanalysis.audio

/**
 * Analyzes chess positions to determine move properties for sound selection
 */
object MoveAnalyzer {

    /**
     * Detect if a position has check based on FEN notation
     * This is a simplified approach - we check if the current side to move is in check
     */
    fun isCheck(fen: String): Boolean {
        // In FEN, check is typically indicated by the position itself
        // A more robust solution would use the chess library
        // For now, we'll use a simple heuristic or rely on the + symbol in SAN
        return false // We'll detect this from the move notation instead
    }

    /**
     * Detect if a move is a capture based on UCI notation and FEN
     */
    fun isCapture(uciMove: String?, fromFen: String, toFen: String): Boolean {
        if (uciMove == null || uciMove.length < 4) return false

        // Compare piece counts before and after
        val piecesFrom = countPieces(fromFen)
        val piecesTo = countPieces(toFen)

        // Also check for en passant capture
        val isEnPassant = isEnPassantMove(uciMove, fromFen)

        return piecesTo < piecesFrom || isEnPassant
    }

    /**
     * Detect if a move is a promotion based on UCI notation
     */
    fun isPromotion(uciMove: String?): Boolean {
        if (uciMove == null || uciMove.length != 5) return false

        // Promotion moves have 5 characters: e7e8q
        // The 5th character must be a promotion piece (q, r, b, n)
        val lastChar = uciMove.last().lowercaseChar()
        val isValidPromotionPiece = lastChar in setOf('q', 'r', 'b', 'n')

        // Also check that it's from rank 7 or 2 (promotion ranks)
        val fromRank = uciMove[1]
        val toRank = uciMove[3]
        val isPromotionRank = (fromRank == '7' && toRank == '8') || (fromRank == '2' && toRank == '1')

        val result = isValidPromotionPiece && isPromotionRank
        println("MoveAnalyzer.isPromotion: uciMove=$uciMove, length=${uciMove.length}, lastChar=$lastChar, isValidPiece=$isValidPromotionPiece, isPromotionRank=$isPromotionRank, result=$result")
        return result
    }

    /**
     * Count total pieces on the board (excluding kings)
     */
    private fun countPieces(fen: String): Int {
        val position = fen.split(" ")[0]
        return position.count { it.isLetter() && it.lowercaseChar() != 'k' }
    }

    /**
     * Detect en passant capture
     */
    private fun isEnPassantMove(uciMove: String, fen: String): Boolean {
        if (uciMove.length < 4) return false

        val from = uciMove.substring(0, 2)
        val to = uciMove.substring(2, 4)

        // En passant: pawn moves diagonally to empty square
        val fromFile = from[0]
        val toFile = to[0]
        val fromRank = from[1]
        val toRank = to[1]

        // Must be diagonal pawn move
        if (fromFile == toFile) return false

        // Must be from rank 5->6 (white) or 4->3 (black)
        if (!((fromRank == '5' && toRank == '6') || (fromRank == '4' && toRank == '3'))) {
            return false
        }

        // Check if en passant target square matches in FEN
        val fenParts = fen.split(" ")
        if (fenParts.size < 4) return false
        val epTarget = fenParts[3]

        return epTarget == to
    }

    /**
     * Detect checkmate by checking if the game is over
     */
    fun isCheckmate(fen: String): Boolean {
        // We can check the move count or game state
        // This is simplified - ideally would use the chess library
        return false // Will be enhanced if needed
    }
}
