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
        sort: String = "close_time",
        order: String = "desc",
        limit: Int = 50,
        offset: Int = 0
    ): Result<HistoryResponse> = runCatching {
        val resp: MT5HistoryResponse = client.get("/api/mt5-history") {
            parameter("limit", limit)
        }.body()
        
        // MT5 deal entry=1 means a closing deal. Entry deals (entry=0) are excluded:
        // the History tab is intentionally a realised-P&L view only.
        val closedDeals = resp.deals.filter { it.entry == 1 }
        HistoryResponse(
            items = closedDeals.map { deal ->
                HistoryItem(
                    ticket = deal.ticket,
                    symbol = deal.symbol,
                    type = deal.type,
                    volume = deal.volume,
                    openPrice = deal.price,
                    closePrice = deal.price,
                    swap = deal.swap,
                    commission = deal.commission,
                    profit = deal.profit,
                    result = if (deal.profit >= 0) "win" else "loss",
                    closeReason = deal.comment,
                    openTime = deal.time,
                    closeTime = deal.time
                )
            },
            total = closedDeals.size,
            hasMore = false
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
