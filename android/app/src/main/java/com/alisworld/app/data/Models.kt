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

// Bridge MT5 Account Response
@Serializable
data class MT5Account(
    val login: Int,
    val balance: Double,
    val equity: Double,
    val margin: Double,
    @SerialName("margin_free") val marginFree: Double,
    @SerialName("margin_level") val marginLevel: Double? = null,
    val profit: Double,
    val currency: String,
    val server: String,
    val leverage: Int,
    val name: String? = null,
    val company: String? = null
)

@Serializable
data class MT5AccountResponse(
    val account: MT5Account,
    @SerialName("open_positions") val open_positions: Int,
    @SerialName("open_pnl") val open_pnl: Double,
    @SerialName("last_updated") val last_updated: String
)

// Positions
@Serializable
data class Position(
    val ticket: Long,
    val symbol: String,
    val type: String,
    val volume: Double,
    @SerialName("price_open") val openPrice: Double,
    @SerialName("price_current") val currentPrice: Double,
    val sl: Double,
    val tp: Double,
    val swap: Double,
    val commission: Double,
    val profit: Double,
    @SerialName("pnl_pct") val pnlPct: Double? = null,
    @SerialName("tp_progress") val tpProgress: Double? = null,
    @SerialName("sl_progress") val slProgress: Double? = null,
    val time: String,
    val comment: String,
    val magic: Int? = null
) {
    val openTime: String get() = time
    val durationSeconds: Int get() = 0 // Bridge doesn't compute this
}

@Serializable
data class MT5PositionsResponse(
    val positions: List<Position>,
    val count: Int,
    @SerialName("total_pnl") val totalPnl: Double,
    @SerialName("last_updated") val lastUpdated: String,
    val account: MT5Account? = null
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

// Bridge MT5 History Response
@Serializable
data class MT5Deal(
    val ticket: Long,
    val order: Long,
    val symbol: String,
    val type: String,
    @SerialName("type_raw") val typeRaw: Int,
    val entry: Int,
    val volume: Double,
    val price: Double,
    val profit: Double,
    val commission: Double,
    val swap: Double,
    val fee: Double,
    val time: String,
    val comment: String,
    val magic: Int
)

@Serializable
data class MT5HistoryResponse(
    val deals: List<MT5Deal>,
    val count: Int,
    @SerialName("total_pnl") val totalPnl: Double,
    @SerialName("last_updated") val lastUpdated: String
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

@Serializable
data class CommandResponse(
    val id: Int,
    @SerialName("command_type") val commandType: String,
    val status: String,
    val message: String? = null
)
