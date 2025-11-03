package com.andrewbibire.chessanalysis

data class Position(
    val fenString: String,
    var score: String? = null,
    var classification: String? = null
)
