package com.andrewbibire.chessanalysis.online

import kotlinx.serialization.Serializable

enum class Platform {
    CHESS_COM,
    LICHESS
}

@Serializable
data class UserProfile(
    val username: String,
    val platform: Platform,
    val displayName: String? = null,
    val avatar: String? = null
)

@Serializable
data class OnlineGame(
    val id: String,
    val white: String,
    val black: String,
    val result: String,
    val timeControl: String,
    val endTime: Long,
    val pgn: String,
    val whiteRating: Int? = null,
    val blackRating: Int? = null,
    val url: String? = null
)

data class MonthGames(
    val year: Int,
    val month: Int,
    val games: List<OnlineGame>
)
