package com.andrewbibire.chessanalysis.online

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LichessGame(
    val id: String,
    val rated: Boolean,
    val variant: String,
    val speed: String,
    val perf: String,
    val createdAt: Long,
    val lastMoveAt: Long,
    val status: String,
    val players: LichessPlayers,
    @SerialName("pgn")
    val pgnData: String? = null,
    val clock: LichessClock? = null,
    val winner: String? = null
)

@Serializable
data class LichessPlayers(
    val white: LichessPlayer,
    val black: LichessPlayer
)

@Serializable
data class LichessPlayer(
    val user: LichessUser? = null,
    val rating: Int? = null,
    val ratingDiff: Int? = null
)

@Serializable
data class LichessUser(
    val name: String,
    val id: String
)

@Serializable
data class LichessClock(
    val initial: Int,
    val increment: Int
)

fun LichessGame.toOnlineGame(): OnlineGame {
    val resultString = when (winner) {
        "white" -> "1-0"
        "black" -> "0-1"
        else -> when (status) {
            "draw", "stalemate" -> "1/2-1/2"
            else -> "*"
        }
    }

    val timeControl = clock?.let {
        val initialSeconds = it.initial
        val incrementSeconds = it.increment
        "$initialSeconds+$incrementSeconds"
    } ?: "-"

    return OnlineGame(
        id = id,
        white = players.white.user?.name ?: "Unknown",
        black = players.black.user?.name ?: "Unknown",
        result = resultString,
        timeControl = timeControl,
        endTime = lastMoveAt,
        pgn = pgnData ?: "",
        whiteRating = players.white.rating,
        blackRating = players.black.rating,
        url = "https://lichess.org/$id"
    )
}
