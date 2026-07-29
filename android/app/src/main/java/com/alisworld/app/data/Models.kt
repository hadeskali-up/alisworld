package com.alisworld.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Dashboard
@Serializable
data class DashboardSummary(
    val balance: Double,
    val equity: Double,
    @SerialName("daily_pnl") val dailyPnl: Double,
    @SerialName("daily_pnl_pct") val dailyPnlPct: Double,
    @SerialName("open_positions_count") val openPositionsCount: Int,
    @SerialName("open_positions_value") val openPositionsValue: Double,
    val currency: String,
    @SerialName("last_update") val lastUpdate: String
)

// Positions
@Serializable
data class Position(
    val ticket: Long,
    val symbol: String,
    val type: String,
    val volume: Double,
    @SerialName("open_price") val openPrice: Double,
    @SerialName("current_price") val currentPrice: Double,
    val sl: Double,
    val tp: Double,
    val swap: Double,
    val commission: Double,
    val profit: Double,
    @SerialName("open_time") val openTime: String,
    @SerialName("duration_seconds") val durationSeconds: Int,
    val comment: String
)

// Trade History
@Serializable
data class HistoryItem(
    val ticket: Long,
    val symbol: String,
    val type: String,
    val volume: Double,
    @SerialName("open_price") val openPrice: Double,
    @SerialName("close_price") val closePrice: Double,
    val swap: Double,
    val commission: Double,
    val profit: Double,
    val result: String,
    @SerialName("close_reason") val closeReason: String,
    @SerialName("open_time") val openTime: String,
    @SerialName("close_time") val closeTime: String
)

@Serializable
data class HistoryResponse(
    val items: List<HistoryItem>,
    val total: Int,
    @SerialName("has_more") val hasMore: Boolean
)

// Symbol Stats
@Serializable
data class SymbolStats(
    val symbol: String,
    val profit: Double,
    @SerialName("trade_count") val tradeCount: Int
)

@Serializable
data class SymbolStatsResponse(
    val stats: List<SymbolStats>
)

// Commands
@Serializable
data class ClosePositionResponse(
    @SerialName("command_id") val commandId: String,
    val status: String
)
