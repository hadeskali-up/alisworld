from fastapi import FastAPI, HTTPException, Header, Depends
from fastapi.middleware.cors import CORSMiddleware
from typing import Optional
import uuid
from datetime import datetime

from config import settings
from models import *
import database as db
from notifications import init_firebase, send_tp_notification, firebase_enabled


app = FastAPI(title="AlisWorld API", version="1.0.0")

# CORS for Android app
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Restrict in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize Firebase on startup
@app.on_event("startup")
async def startup():
    global firebase_enabled
    firebase_enabled = init_firebase()


# Auth dependency
async def verify_api_key(x_api_key: str = Header(...)):
    if x_api_key != settings.api_key:
        raise HTTPException(status_code=403, detail="Invalid API key")


# Health check
@app.get("/health")
async def health():
    return {"status": "ok", "timezone": settings.timezone}


# ============================================================================
# INGEST ENDPOINTS (pusher → backend)
# ============================================================================

@app.post("/api/ingest/account-summary", dependencies=[Depends(verify_api_key)])
async def ingest_account_summary(payload: AccountSummary):
    await db.insert_account_snapshot(payload.model_dump())
    return {"status": "ok"}


@app.post("/api/ingest/open-positions", dependencies=[Depends(verify_api_key)])
async def ingest_open_positions(payload: OpenPositionsPayload):
    positions = [p.model_dump() for p in payload.positions]
    await db.replace_open_positions(positions)
    return {"status": "ok"}


@app.post("/api/ingest/trade-history", dependencies=[Depends(verify_api_key)])
async def ingest_trade_history(payload: TradeHistory):
    trade = payload.model_dump()
    await db.insert_trade_history(trade)
    
    # Send notification if TP-hit
    if payload.close_reason == "tp":
        tokens = await db.get_all_fcm_tokens()
        if tokens:
            await send_tp_notification(
                tokens,
                payload.symbol,
                payload.profit,
                payload.close_price
            )
    
    return {"status": "ok"}


@app.get("/api/commands/pending", dependencies=[Depends(verify_api_key)])
async def get_pending_commands():
    commands = await db.get_pending_commands()
    return {"commands": commands}


@app.post("/api/commands/{command_id}/result", dependencies=[Depends(verify_api_key)])
async def report_command_result(command_id: str, payload: CommandResult):
    await db.update_command_result(
        command_id,
        payload.status,
        payload.closed_price,
        payload.message
    )
    return {"status": "ok"}


# ============================================================================
# APP ENDPOINTS (Android → backend)
# ============================================================================

@app.get("/api/dashboard/summary", response_model=DashboardSummary, dependencies=[Depends(verify_api_key)])
async def get_dashboard_summary():
    snapshot = await db.get_latest_account_snapshot()
    if not snapshot:
        raise HTTPException(status_code=404, detail="No account data available")
    
    positions = await db.get_open_positions()
    open_count = len(positions)
    open_value = sum(p["profit"] for p in positions)
    
    return DashboardSummary(
        balance=snapshot["balance"],
        equity=snapshot["equity"],
        daily_pnl=snapshot["daily_pnl"],
        daily_pnl_pct=snapshot["daily_pnl_pct"],
        open_positions_count=open_count,
        open_positions_value=open_value,
        currency=snapshot["currency"],
        last_update=snapshot["server_time"]
    )


@app.get("/api/positions", response_model=List[PositionResponse], dependencies=[Depends(verify_api_key)])
async def get_positions():
    positions = await db.get_open_positions()
    
    result = []
    for pos in positions:
        # Calculate duration
        open_time = datetime.fromisoformat(pos["open_time"].replace("Z", "+00:00"))
        now = datetime.now(open_time.tzinfo)
        duration = int((now - open_time).total_seconds())
        
        result.append(PositionResponse(
            ticket=pos["ticket"],
            symbol=pos["symbol"],
            type=pos["type"],
            volume=pos["volume"],
            open_price=pos["open_price"],
            current_price=pos["current_price"],
            sl=pos["sl"],
            tp=pos["tp"],
            swap=pos["swap"],
            commission=pos["commission"],
            profit=pos["profit"],
            open_time=pos["open_time"],
            duration_seconds=duration,
            comment=pos["comment"]
        ))
    
    return result


@app.post("/api/positions/{ticket}/close", dependencies=[Depends(verify_api_key)])
async def close_position(ticket: int):
    command_id = str(uuid.uuid4())
    await db.create_command(command_id, ticket)
    return {"command_id": command_id, "status": "pending"}


@app.get("/api/history", response_model=HistoryResponse, dependencies=[Depends(verify_api_key)])
async def get_history(
    sort: str = "close_time",
    order: str = "desc",
    limit: int = 50,
    offset: int = 0
):
    items, total = await db.get_trade_history(limit, offset, sort, order)
    
    history_items = [
        HistoryItem(
            ticket=item["ticket"],
            symbol=item["symbol"],
            type=item["type"],
            volume=item["volume"],
            open_price=item["open_price"],
            close_price=item["close_price"],
            swap=item["swap"],
            commission=item["commission"],
            profit=item["profit"],
            result=item["result"],
            close_reason=item["close_reason"],
            open_time=item["open_time"],
            close_time=item["close_time"]
        )
        for item in items
    ]
    
    return HistoryResponse(
        items=history_items,
        total=total,
        has_more=(offset + limit) < total
    )


@app.get("/api/history/stats/by-symbol", response_model=SymbolStatsResponse, dependencies=[Depends(verify_api_key)])
async def get_symbol_stats():
    stats = await db.get_symbol_stats()
    return SymbolStatsResponse(
        stats=[
            SymbolStats(
                symbol=s["symbol"],
                profit=s["profit"],
                trade_count=s["trade_count"]
            )
            for s in stats
        ]
    )


@app.post("/api/fcm/register", dependencies=[Depends(verify_api_key)])
async def register_fcm_token(token: str):
    await db.register_fcm_token(token)
    return {"status": "ok"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
