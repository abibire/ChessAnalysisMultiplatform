package com.andrewbibire.chessanalysis.online

import com.andrewbibire.chessanalysis.network.NetworkClient
import com.andrewbibire.chessanalysis.network.NetworkResult
import com.andrewbibire.chessanalysis.network.get

object ChessComService {
    private const val BASE_URL = "https://api.chess.com/pub"

    private val client = NetworkClient(
        baseUrl = BASE_URL,
        defaultHeaders = mapOf(
            "User-Agent" to "ChessAnalysisApp/1.0"
        )
    )

    suspend fun getAvailableArchives(username: String): NetworkResult<List<String>> {
        val result = client.get<ChessComArchivesResponse>("player/$username/games/archives")
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.archives)
            is NetworkResult.Error -> result
        }
    }

    suspend fun getGamesForMonth(archiveUrl: String): NetworkResult<List<OnlineGame>> {
        val cleanUrl = archiveUrl.removePrefix("$BASE_URL/")
        val result = client.get<ChessComGamesResponse>(cleanUrl)
        return when (result) {
            is NetworkResult.Success -> {
                // Filter out variant games by rules field only (fast and reliable)
                val standardGames = result.data.games.filter { it.rules == "chess" }
                val variantCount = result.data.games.size - standardGames.size
                if (variantCount > 0) {
                    println("Chess.com: Filtered out $variantCount variant games")
                }

                val games = standardGames.map { it.toOnlineGame() }.reversed()
                println("Chess.com: Fetched ${games.size} standard chess games")
                NetworkResult.Success(games)
            }
            is NetworkResult.Error -> result
        }
    }

    suspend fun getGamesForYearMonth(username: String, year: Int, month: Int): NetworkResult<List<OnlineGame>> {
        val monthStr = month.toString().padStart(2, '0')
        return getGamesForMonth("$BASE_URL/player/$username/games/$year/$monthStr")
    }

    suspend fun getUserProfile(username: String): NetworkResult<UserProfile> {
        return try {
            val archives = getAvailableArchives(username)
            when (archives) {
                is NetworkResult.Success -> {
                    if (archives.data.isEmpty()) {
                        NetworkResult.Error(
                            Exception("No games found"),
                            "This user has no games available"
                        )
                    } else {
                        NetworkResult.Success(
                            UserProfile(
                                username = username,
                                platform = Platform.CHESS_COM,
                                displayName = username.replaceFirstChar { it.uppercase() }
                            )
                        )
                    }
                }
                is NetworkResult.Error -> archives
            }
        } catch (e: Exception) {
            NetworkResult.Error(e, "Failed to fetch user profile")
        }
    }
}
