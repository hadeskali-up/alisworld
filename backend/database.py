import aiosqlite
from config import settings
from typing import List, Optional, Dict, Any
from datetime import datetime
import zoneinfo


def get_db_path():
    return settings.database_url.replace("sqlite:///", "").replace("./", "")


async def get_db():
    """Async context manager for database connections"""
    db = await aiosqlite.connect(get_db_path())
    db.row_factory = aiosqlite.Row
    try:
        yield db
    finally:
        await db.close()


async def insert_account_snapshot(snapshot: Dict[str, Any]):
    """Store account summary snapshot"""
    async with aiosqlite.connect(get_db_path()) as db:
        await db.execute("""
            INSERT INTO account_snapshots 
            (balance, equity, margin, free_margin, margin_level, currency, daily_pnl, daily_pnl_pct, server_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            snapshot["balance"],
            snapshot["equity"],
            snapshot["margin"],
            snapshot["free_margin"],
            snapshot["margin_level"],
            snapshot["currency"],
            snapshot["daily_pnl"],
            snapshot["daily_pnl_pct"],
            snapshot["server_time"]
        ))
        await db.commit()


async def replace_open_positions(positions: List[Dict[str, Any]]):
    """Full replace of open positions table"""
    async with aiosqlite.connect(get_db_path()) as db:
        # Clear existing
        await db.execute("DELETE FROM open_positions")
        
        # Insert new
        for pos in positions:
            await db.execute("""
                INSERT INTO open_positions 
                (ticket, symbol, type, volume, open_price, current_price, sl, tp, swap, commission, profit, open_time, comment)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                pos["ticket"],
                pos["symbol"],
                pos["type"],
                pos["volume"],
                pos["open_price"],
                pos["current_price"],
                pos["sl"],
                pos["tp"],
                pos["swap"],
                pos["commission"],
                pos["profit"],
                pos["open_time"],
                pos.get("comment", "")
            ))
        await db.commit()


async def insert_trade_history(trade: Dict[str, Any]):
    """Append closed trade to history"""
    async with aiosqlite.connect(get_db_path()) as db:
        await db.execute("""
            INSERT OR IGNORE INTO trade_history 
            (ticket, symbol, type, volume, open_price, close_price, swap, commission, profit, result, close_reason, open_time, close_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            trade["ticket"],
            trade["symbol"],
            trade["type"],
            trade["volume"],
            trade["open_price"],
            trade["close_price"],
            trade["swap"],
            trade["commission"],
            trade["profit"],
            trade["result"],
            trade["close_reason"],
            trade["open_time"],
            trade["close_time"]
        ))
        await db.commit()


async def get_latest_account_snapshot() -> Optional[Dict[str, Any]]:
    """Get most recent account snapshot"""
    async with aiosqlite.connect(get_db_path()) as db:
        db.row_factory = aiosqlite.Row
        async with db.execute(
            "SELECT * FROM account_snapshots ORDER BY created_at DESC LIMIT 1"
        ) as cursor:
            row = await cursor.fetchone()
            return dict(row) if row else None


async def get_open_positions() -> List[Dict[str, Any]]:
    """Get all current open positions"""
    async with aiosqlite.connect(get_db_path()) as db:
        db.row_factory = aiosqlite.Row
        async with db.execute(
            "SELECT * FROM open_positions ORDER BY open_time DESC"
        ) as cursor:
            rows = await cursor.fetchall()
            return [dict(row) for row in rows]


async def get_trade_history(
    limit: int = 50,
    offset: int = 0,
    sort_by: str = "close_time",
    order: str = "desc"
) -> tuple[List[Dict[str, Any]], int]:
    """Get paginated trade history with total count"""
    valid_sorts = ["close_time", "profit", "symbol", "result"]
    valid_orders = ["asc", "desc"]
    
    if sort_by not in valid_sorts:
        sort_by = "close_time"
    if order not in valid_orders:
        order = "desc"
    
    async with aiosqlite.connect(get_db_path()) as db:
        db.row_factory = aiosqlite.Row
        
        # Get total count
        async with db.execute("SELECT COUNT(*) as count FROM trade_history") as cursor:
            count_row = await cursor.fetchone()
            total = count_row["count"] if count_row else 0
        
        # Get paginated results
        query = f"SELECT * FROM trade_history ORDER BY {sort_by} {order} LIMIT ? OFFSET ?"
        async with db.execute(query, (limit, offset)) as cursor:
            rows = await cursor.fetchall()
            items = [dict(row) for row in rows]
    
    return items, total


async def get_symbol_stats() -> List[Dict[str, Any]]:
    """Aggregate profit by symbol"""
    async with aiosqlite.connect(get_db_path()) as db:
        db.row_factory = aiosqlite.Row
        async with db.execute("""
            SELECT 
                symbol,
                SUM(profit) as profit,
                COUNT(*) as trade_count
            FROM trade_history
            GROUP BY symbol
            HAVING profit > 0
            ORDER BY profit DESC
        """) as cursor:
            rows = await cursor.fetchall()
            return [dict(row) for row in rows]


async def create_command(command_id: str, ticket: int) -> None:
    """Queue a close-position command"""
    async with aiosqlite.connect(get_db_path()) as db:
        await db.execute("""
            INSERT INTO commands (command_id, action, ticket, status)
            VALUES (?, 'close_position', ?, 'pending')
        """, (command_id, ticket))
        await db.commit()


async def get_pending_commands() -> List[Dict[str, Any]]:
    """Get all pending commands for pusher to poll"""
    async with aiosqlite.connect(get_db_path()) as db:
        db.row_factory = aiosqlite.Row
        async with db.execute(
            "SELECT command_id, action, ticket FROM commands WHERE status = 'pending' ORDER BY created_at ASC"
        ) as cursor:
            rows = await cursor.fetchall()
            return [dict(row) for row in rows]


async def update_command_result(command_id: str, status: str, closed_price: Optional[float], message: str):
    """Update command status after pusher executes it"""
    async with aiosqlite.connect(get_db_path()) as db:
        await db.execute("""
            UPDATE commands 
            SET status = ?, closed_price = ?, message = ?, completed_at = CURRENT_TIMESTAMP
            WHERE command_id = ?
        """, (status, closed_price, message, command_id))
        await db.commit()


async def register_fcm_token(token: str):
    """Register device FCM token"""
    async with aiosqlite.connect(get_db_path()) as db:
        await db.execute(
            "INSERT OR IGNORE INTO fcm_tokens (token) VALUES (?)",
            (token,)
        )
        await db.commit()


async def get_all_fcm_tokens() -> List[str]:
    """Get all registered FCM tokens"""
    async with aiosqlite.connect(get_db_path()) as db:
        async with db.execute("SELECT token FROM fcm_tokens") as cursor:
            rows = await cursor.fetchall()
            return [row[0] for row in rows]
