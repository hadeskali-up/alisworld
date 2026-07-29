package com.alisworld.app.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import com.alisworld.app.BuildConfig

class ApiClient {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        install(Logging) {
            level = LogLevel.INFO
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 15000
        }

        defaultRequest {
            url(BuildConfig.BACKEND_URL)
            header("X-API-Key", BuildConfig.BACKEND_API_KEY)
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun getDashboardSummary(): Result<DashboardSummary> = runCatching {
        client.get("/api/dashboard/summary").body()
    }

    suspend fun getPositions(): Result<List<Position>> = runCatching {
        client.get("/api/positions").body()
    }

    suspend fun closePosition(ticket: Long): Result<ClosePositionResponse> = runCatching {
        client.post("/api/positions/$ticket/close").body()
    }

    suspend fun getHistory(
        sort: String = "close_time",
        order: String = "desc",
        limit: Int = 50,
        offset: Int = 0
    ): Result<HistoryResponse> = runCatching {
        client.get("/api/history") {
            parameter("sort", sort)
            parameter("order", order)
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()
    }

    suspend fun getSymbolStats(): Result<SymbolStatsResponse> = runCatching {
        client.get("/api/history/stats/by-symbol").body()
    }

    suspend fun registerFcmToken(token: String): Result<Unit> = runCatching {
        client.post("/api/fcm/register") {
            parameter("token", token)
        }
    }

    fun close() {
        client.close()
    }

    companion object {
        val instance: ApiClient by lazy { ApiClient() }
    }
}
