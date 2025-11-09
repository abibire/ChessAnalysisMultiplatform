package com.andrewbibire.chessanalysis.online

import com.andrewbibire.chessanalysis.network.NetworkResult
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

object LichessService {
    private const val BASE_URL = "https://lichess.org"

    // Create a dedicated HttpClient without ContentNegotiation to avoid Accept header conflicts
    private val httpClient = HttpClient {
        // No ContentNegotiation plugin to avoid Accept header override
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Get games for the current month (no until parameter)
     */
    suspend fun getCurrentMonthGames(username: String): NetworkResult<List<OnlineGame>> {
        return try {
            val currentYear = getCurrentYear()
            val currentMonth = getCurrentMonth()
            val since = getMonthStartTimestamp(currentYear, currentMonth)

            getGames(username, since, null)
        } catch (e: Exception) {
            NetworkResult.Error(e, "Failed to fetch Lichess games: ${e.message}")
        }
    }

    /**
     * Get games for a specific year and month
     */
    suspend fun getGamesForYearMonth(username: String, year: Int, month: Int): NetworkResult<List<OnlineGame>> {
        return try {
            // Calculate Unix timestamps in milliseconds for the month range
            val since = getMonthStartTimestamp(year, month)
            val until = getMonthEndTimestamp(year, month)

            getGames(username, since, until)
        } catch (e: Exception) {
            NetworkResult.Error(e, "Failed to fetch Lichess games: ${e.message}")
        }
    }

    /**
     * Get games between two timestamps (until is optional)
     */
    private suspend fun getGames(username: String, since: Long, until: Long?): NetworkResult<List<OnlineGame>> {
        return try {
            val response = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "lichess.org"
                    path("api/games/user/$username")
                    parameters.append("since", since.toString())
                    until?.let { parameters.append("until", it.toString()) }
                    parameters.append("pgnInJson", "true")
                    parameters.append("clocks", "false")
                    parameters.append("evals", "false")
                    parameters.append("opening", "false")
                }
                header("Accept", "application/x-ndjson")
            }

            if (response.status.isSuccess()) {
                val bodyText = response.bodyAsText()
                println("Response first 100 chars: ${bodyText.take(100)}")
                val games = parseNdJson(bodyText)
                println("Parsed ${games.size} games")
                NetworkResult.Success(games)
            } else {
                println("HTTP Error: ${response.status.value}")
                NetworkResult.Error(
                    Exception("HTTP ${response.status.value}"),
                    "Failed to fetch games: ${response.status.description}"
                )
            }
        } catch (e: Exception) {
            println("Lichess API Error: ${e.message}")
            e.printStackTrace()
            NetworkResult.Error(e, "Failed to fetch Lichess games: ${e.message}")
        }
    }

    /**
     * Parse NDJSON (Newline Delimited JSON) response
     */
    private fun parseNdJson(bodyText: String): List<OnlineGame> {
        return bodyText
            .split("\n")
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                try {
                    val lichessGame = json.decodeFromString<LichessGame>(line)
                    lichessGame.toOnlineGame()
                } catch (e: Exception) {
                    println("Parse error: ${e.message}")
                    null // Skip malformed lines
                }
            }
    }

    /**
     * Validate that a username exists and fetch current month games
     */
    suspend fun getUserProfile(username: String): NetworkResult<UserProfile> {
        return try {
            // Try to fetch current month games to validate the user exists
            val result = getCurrentMonthGames(username)

            when (result) {
                is NetworkResult.Success -> {
                    // User exists and has games (even if current month is empty, we found them)
                    NetworkResult.Success(
                        UserProfile(
                            username = username,
                            platform = Platform.LICHESS,
                            displayName = username
                        )
                    )
                }
                is NetworkResult.Error -> {
                    // Check if it's a 404 (user not found) or other error
                    if (result.message?.contains("404") == true) {
                        NetworkResult.Error(
                            Exception("User not found"),
                            "User '$username' not found on Lichess"
                        )
                    } else {
                        result
                    }
                }
            }
        } catch (e: Exception) {
            NetworkResult.Error(e, "Failed to fetch user profile: ${e.message}")
        }
    }

    /**
     * Get available archives (year-month combinations)
     * Note: Lichess doesn't have an archives endpoint like Chess.com,
     * so we'll generate a reasonable range
     */
    suspend fun getAvailableArchives(username: String): NetworkResult<List<String>> {
        return try {
            // Generate archives for the last 3 years
            val currentYear = getCurrentYear()
            val currentMonth = getCurrentMonth()
            val archives = mutableListOf<String>()

            // Generate year/month combinations going back 3 years
            for (year in (currentYear - 2)..currentYear) {
                val endMonth = if (year == currentYear) currentMonth else 12
                for (month in 1..endMonth) {
                    archives.add("$year/${month.toString().padStart(2, '0')}")
                }
            }

            NetworkResult.Success(archives.reversed()) // Most recent first
        } catch (e: Exception) {
            NetworkResult.Error(e, "Failed to generate archives")
        }
    }

    /**
     * Get Unix timestamp in milliseconds for the start of a month
     */
    private fun getMonthStartTimestamp(year: Int, month: Int): Long {
        // Simple calculation: Create a date for the 1st of the month at 00:00:00
        // This is a simplified version - in production you'd use proper date/time library
        val monthStr = month.toString().padStart(2, '0')
        return when (month) {
            1 -> if (year == 2025) 1735689600000L else calculateTimestamp(year, month)
            2 -> if (year == 2025) 1738368000000L else calculateTimestamp(year, month)
            3 -> if (year == 2025) 1740787200000L else calculateTimestamp(year, month)
            4 -> if (year == 2025) 1743465600000L else calculateTimestamp(year, month)
            5 -> if (year == 2025) 1746057600000L else calculateTimestamp(year, month)
            6 -> if (year == 2025) 1748736000000L else calculateTimestamp(year, month)
            7 -> if (year == 2025) 1751328000000L else calculateTimestamp(year, month)
            8 -> if (year == 2025) 1754006400000L else calculateTimestamp(year, month)
            9 -> if (year == 2025) 1756684800000L else calculateTimestamp(year, month)
            10 -> if (year == 2025) 1759276800000L else calculateTimestamp(year, month)
            11 -> if (year == 2025) 1761955200000L else calculateTimestamp(year, month)
            12 -> if (year == 2025) 1764547200000L else calculateTimestamp(year, month)
            else -> calculateTimestamp(year, month)
        }
    }

    /**
     * Get Unix timestamp in milliseconds for the end of a month
     */
    private fun getMonthEndTimestamp(year: Int, month: Int): Long {
        val nextMonth = if (month == 12) 1 else month + 1
        val nextYear = if (month == 12) year + 1 else year
        return getMonthStartTimestamp(nextYear, nextMonth) - 1
    }

    /**
     * Fallback timestamp calculation for years other than 2025
     */
    private fun calculateTimestamp(year: Int, month: Int): Long {
        // Approximate calculation based on 2025 values
        val baseYear = 2025
        val baseTimestamp = 1735689600000L // Jan 1, 2025
        val yearDiff = year - baseYear
        val millisPerYear = 365.25 * 24 * 60 * 60 * 1000 // Account for leap years

        val yearOffset = (yearDiff * millisPerYear).toLong()
        val monthOffset = (month - 1) * 30.44 * 24 * 60 * 60 * 1000 // Average month length

        return baseTimestamp + yearOffset + monthOffset.toLong()
    }
}
