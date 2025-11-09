package com.andrewbibire.chessanalysis.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class NetworkClient(
    val baseUrl: String = "",
    val defaultHeaders: Map<String, String> = emptyMap()
) {
    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = false
            })
        }
        if (defaultHeaders.isNotEmpty()) {
            install(DefaultRequest) {
                defaultHeaders.forEach { (key, value) ->
                    header(key, value)
                }
            }
        }
    }

    fun close() {
        client.close()
    }
}

suspend inline fun <reified T> NetworkClient.get(
    path: String,
    queryParams: Map<String, String> = emptyMap(),
    additionalHeaders: Map<String, String> = emptyMap()
): NetworkResult<T> {
    return try {
        val fullPath = if (baseUrl.isNotEmpty()) "$baseUrl/$path" else path
        val urlWithParams = if (queryParams.isEmpty()) {
            fullPath
        } else {
            val params = queryParams.entries.joinToString("&") { "${it.key}=${it.value}" }
            "$fullPath?$params"
        }
        NetworkResult.Success(
            client.get {
                url(urlWithParams)
                defaultHeaders.forEach { (key, value) -> header(key, value) }
                additionalHeaders.forEach { (key, value) -> header(key, value) }
            }.body()
        )
    } catch (e: Exception) {
        NetworkResult.Error(e, e.message)
    }
}

suspend inline fun <reified T, reified R> NetworkClient.post(
    path: String,
    body: T,
    queryParams: Map<String, String> = emptyMap(),
    additionalHeaders: Map<String, String> = emptyMap()
): NetworkResult<R> {
    return try {
        val fullPath = if (baseUrl.isNotEmpty()) "$baseUrl/$path" else path
        val urlWithParams = if (queryParams.isEmpty()) {
            fullPath
        } else {
            val params = queryParams.entries.joinToString("&") { "${it.key}=${it.value}" }
            "$fullPath?$params"
        }
        NetworkResult.Success(
            client.post {
                url(urlWithParams)
                defaultHeaders.forEach { (key, value) -> header(key, value) }
                additionalHeaders.forEach { (key, value) -> header(key, value) }
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body()
        )
    } catch (e: Exception) {
        NetworkResult.Error(e, e.message)
    }
}

suspend inline fun <reified R> NetworkClient.post(
    path: String,
    queryParams: Map<String, String> = emptyMap(),
    additionalHeaders: Map<String, String> = emptyMap()
): NetworkResult<R> {
    return try {
        val fullPath = if (baseUrl.isNotEmpty()) "$baseUrl/$path" else path
        val urlWithParams = if (queryParams.isEmpty()) {
            fullPath
        } else {
            val params = queryParams.entries.joinToString("&") { "${it.key}=${it.value}" }
            "$fullPath?$params"
        }
        NetworkResult.Success(
            client.post {
                url(urlWithParams)
                defaultHeaders.forEach { (key, value) -> header(key, value) }
                additionalHeaders.forEach { (key, value) -> header(key, value) }
            }.body()
        )
    } catch (e: Exception) {
        NetworkResult.Error(e, e.message)
    }
}
