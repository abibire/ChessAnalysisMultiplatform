package com.andrewbibire.chessanalysis.online

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChessComArchivesResponse(
    val archives: List<String>
)

@Serializable
data class ChessComPlayerProfile(
    val avatar: String? = null,
    val username: String,
    val name: String? = null
)

@Serializable
data class ChessComGamesResponse(
    val games: List<ChessComGame>
)

@Serializable
data class ChessComGame(
    val url: String,
    val pgn: String,
    @SerialName("time_control")
    val timeControl: String,
    @SerialName("end_time")
    val endTime: Long,
    val rated: Boolean,
    val white: ChessComPlayer,
    val black: ChessComPlayer,
    @SerialName("time_class")
    val timeClass: String? = null,
    val rules: String? = null
)

@Serializable
data class ChessComPlayer(
    val rating: Int,
    val result: String,
    @SerialName("@id")
    val id: String? = null,
    val username: String
)

fun ChessComGame.toOnlineGame(): OnlineGame {
    val resultString = when {
        white.result == "win" -> "1-0"
        black.result == "win" -> "0-1"
        else -> "1/2-1/2"
    }

    return OnlineGame(
        id = url,
        white = white.username,
        black = black.username,
        result = resultString,
        timeControl = timeControl,
        endTime = endTime * 1000,
        pgn = pgn,
        whiteRating = white.rating,
        blackRating = black.rating,
        url = url
    )
}
