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
            url("https://bridge.alisuhari.top")
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun getDashboardSummary(): Result<DashboardSummary> = runCatching {
        val accountResp: MT5AccountResponse = client.get("/api/mt5-account").body()
        DashboardSummary(
            balance = accountResp.account.balance,
            equity = accountResp.account.equity,
            dailyPnl = accountResp.open_pnl,
            dailyPnlPct = if (accountResp.account.balance > 0) 
                (accountResp.open_pnl / accountResp.account.balance) * 100 else 0.0,
            openPositionsCount = accountResp.open_positions,
            openPositionsValue = accountResp.open_pnl,
            currency = accountResp.account.currency,
            lastUpdate = accountResp.last_updated
        )
    }

    suspend fun getPositions(): Result<List<Position>> = runCatching {
        val resp: MT5PositionsResponse = client.get("/api/mt5-positions").body()
        resp.positions
    }

    suspend fun closePosition(ticket: Long): Result<ClosePositionResponse> = runCatching {
        val resp: CommandResponse = client.post("/api/mt5-commands") {
            setBody(ClosePositionCommand(payload = ClosePositionPayload(ticket)))
        }.body()
        ClosePositionResponse(commandId = resp.id.toString(), status = resp.status)
    }

    suspend fun getHistory(
        limit: Int = 50,
        offset: Int = 0,
        symbol: String? = null
    ): Result<HistoryResponse> = runCatching {
        val resp: MT5HistoryResponse = client.get("/api/mt5-history") {
            parameter("limit", limit)
            parameter("offset", offset)
            symbol?.let { parameter("symbol", it) }
        }.body()
        HistoryResponse(
            items = resp.deals.map { deal ->
                HistoryItem(deal.ticket, deal.symbol, deal.type, deal.volume, deal.price, deal.price, deal.swap, deal.commission, deal.netPnl, if (deal.netPnl >= 0) "win" else "loss", deal.comment, deal.time, deal.time)
            },
            total = resp.total,
            hasMore = resp.hasMore,
            symbols = resp.symbols,
            dailyPnl = resp.summary?.dailyPnl ?: 0.0,
            allTimePnl = resp.summary?.allTimePnl ?: 0.0,
            filteredListPnl = resp.summary?.filteredListPnl ?: 0.0
        )
    }

    suspend fun getSymbolStats(): Result<SymbolStatsResponse> = runCatching {
        // Derive from history
        val history = getHistory(limit = 200).getOrThrow()
        val statsBySymbol = history.items
            .groupBy { it.symbol }
            .map { (symbol, trades) ->
                SymbolStats(
                    symbol = symbol,
                    profit = trades.sumOf { it.profit },
                    tradeCount = trades.size
                )
            }
            .sortedByDescending { it.profit }
        
        SymbolStatsResponse(stats = statsBySymbol)
    }

    suspend fun registerFcmToken(token: String): Result<Unit> = runCatching {
        // No-op for now (bridge doesn't handle FCM yet)
    }

    fun close() {
        client.close()
    }

    companion object {
        val instance: ApiClient by lazy { ApiClient() }
    }
}
