package com.andrewbibire.chessanalysis

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform