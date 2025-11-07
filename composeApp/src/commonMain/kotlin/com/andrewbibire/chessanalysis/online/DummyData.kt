package com.andrewbibire.chessanalysis.online

import kotlin.random.Random

object DummyData {
    private val samplePgn = """
        [Event "Live Chess"]
        [Site "Chess.com"]
        [Date "2025.11.07"]
        [Round "-"]
        [White "Player1"]
        [Black "Player2"]
        [Result "1-0"]
        [WhiteElo "1500"]
        [BlackElo "1480"]
        [TimeControl "600"]
        [EndTime "14:30:45 PST"]
        [Termination "Player1 won by checkmate"]

        1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6 5. O-O Be7 6. Re1 b5 7. Bb3 d6
        8. c3 O-O 9. h3 Nb8 10. d4 Nbd7 1-0
    """.trimIndent()

    fun validateUsername(username: String, platform: Platform): Boolean {
        return username.length >= 3 && username.all { it.isLetterOrDigit() || it == '_' || it == '-' }
    }

    fun fetchUserProfile(username: String, platform: Platform): UserProfile? {
        return if (validateUsername(username, platform)) {
            UserProfile(
                username = username,
                platform = platform,
                displayName = username.replaceFirstChar { it.uppercase() }
            )
        } else {
            null
        }
    }

    fun fetchGamesForMonth(username: String, platform: Platform, year: Int, month: Int): MonthGames {
        val games = buildList {
            val gamesCount = Random.nextInt(5, 15)
            repeat(gamesCount) { index ->
                val isWhite = Random.nextBoolean()
                val result = listOf("1-0", "0-1", "1/2-1/2").random()
                val timeControl = listOf("600", "300", "180+2", "900+10").random()

                add(
                    OnlineGame(
                        id = "game_${year}_${month}_$index",
                        white = if (isWhite) username else "Opponent${Random.nextInt(100, 999)}",
                        black = if (isWhite) "Opponent${Random.nextInt(100, 999)}" else username,
                        result = result,
                        timeControl = timeControl,
                        endTime = currentTimeMillis() - Random.nextLong(0, 30L * 24 * 60 * 60 * 1000),
                        pgn = samplePgn,
                        whiteRating = Random.nextInt(1200, 2000),
                        blackRating = Random.nextInt(1200, 2000),
                        url = "https://${if (platform == Platform.CHESS_COM) "chess.com" else "lichess.org"}/game/$index"
                    )
                )
            }
        }.sortedByDescending { it.endTime }

        return MonthGames(year = year, month = month, games = games)
    }
}
