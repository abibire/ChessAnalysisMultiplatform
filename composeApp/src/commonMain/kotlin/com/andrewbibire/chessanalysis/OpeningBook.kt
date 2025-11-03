package com.andrewbibire.chessanalysis

import chessanalysis.composeapp.generated.resources.Res
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object OpeningBook {
    private var loaded = false
    private var map: Map<String, String> = emptyMap()

    suspend fun init() {
        if (loaded) return
        val json = try {
            Res.readBytes("files/openings.json").decodeToString()
        } catch (e: Exception) {
            println("Error loading openings.json: ${e.message}")
            null
        } ?: return

        map = parseOpenings(json)
        loaded = true
    }

    fun lookupBoardFen(boardFen: String): String? {
        if (!loaded) return null
        return map[boardFen]
    }
}

fun parseOpenings(json: String): Map<String, String> {
    return try {
        val jsonElement = Json.parseToJsonElement(json)
        jsonElement.jsonObject.mapValues { it.value.jsonPrimitive.content }
    } catch (e: Exception) {
        println("Error parsing openings.json: ${e.message}")
        emptyMap()
    }
}