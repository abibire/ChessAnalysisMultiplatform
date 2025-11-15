package com.andrewbibire.chessanalysis

data class Position(
    val fenString: String,
    var score: String? = null,
    var classification: String? = null,
    var forced: Boolean = false,
    var isBook: Boolean = false,
    var openingName: String? = null,
    var bestMove: String? = null,
    var playedMove: String? = null,
    var sanNotation: String? = null,
    var accuracy: Double? = null
)