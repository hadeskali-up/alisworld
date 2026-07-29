from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime


# Ingest models (pusher → backend)
class AccountSummary(BaseModel):
    balance: float
    equity: float
    margin: float
    free_margin: float
    margin_level: float
    currency: str
    daily_pnl: float
    daily_pnl_pct: float
    server_time: str  # ISO 8601


class OpenPosition(BaseModel):
    ticket: int
    symbol: str
    type: str  # "buy" or "sell"
    volume: float
    open_price: float
    current_price: float
    sl: float
    tp: float
    swap: float
    commission: float
    profit: float
    open_time: str  # ISO 8601
    comment: str = ""


class OpenPositionsPayload(BaseModel):
    positions: List[OpenPosition]


class TradeHistory(BaseModel):
    ticket: int
    symbol: str
    type: str  # "buy" or "sell"
    volume: float
    open_price: float
    close_price: float
    swap: float
    commission: float
    profit: float
    result: str  # "win" or "loss"
    close_reason: str  # "tp", "sl", "manual", "other"
    open_time: str  # ISO 8601
    close_time: str  # ISO 8601


# Command models
class Command(BaseModel):
    command_id: str
    action: str  # "close_position"
    ticket: int


class CommandResult(BaseModel):
    status: str  # "success" or "failed"
    closed_price: Optional[float] = None
    message: str = ""


# App-facing models (backend → Android)
class DashboardSummary(BaseModel):
    balance: float
    equity: float
    daily_pnl: float
    daily_pnl_pct: float
    open_positions_count: int
    open_positions_value: float  # Total floating PnL
    currency: str
    last_update: str  # ISO 8601


class PositionResponse(BaseModel):
    ticket: int
    symbol: str
    type: str
    volume: float
    open_price: float
    current_price: float
    sl: float
    tp: float
    swap: float
    commission: float
    profit: float
    open_time: str
    duration_seconds: int
    comment: str


class HistoryItem(BaseModel):
    ticket: int
    symbol: str
    type: str
    volume: float
    open_price: float
    close_price: float
    swap: float
    commission: float
    profit: float
    result: str
    close_reason: str
    open_time: str
    close_time: str


class HistoryResponse(BaseModel):
    items: List[HistoryItem]
    total: int
    has_more: bool


class SymbolStats(BaseModel):
    symbol: str
    profit: float
    trade_count: int


class SymbolStatsResponse(BaseModel):
    stats: List[SymbolStats]
