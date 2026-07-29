"""
MT5 Pusher for AlisWorld Backend
Adapted from pusher_bridge_v2.py to work with AlisWorld API contract
"""

import MetaTrader5 as mt5
import requests
import time
import os
from datetime import datetime, timezone, timedelta
from typing import List, Dict, Optional, Set
from dotenv import load_dotenv
import pytz

load_dotenv()

# Configuration
BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8000")
API_KEY = os.getenv("API_KEY")
PUSH_INTERVAL = int(os.getenv("PUSH_INTERVAL", "2"))  # seconds
POLL_INTERVAL = int(os.getenv("POLL_INTERVAL", "1"))  # seconds
TIMEZONE = os.getenv("TIMEZONE", "Asia/Kuala_Lumpur")

# MT5 credentials
MT5_LOGIN = int(os.getenv("MT5_LOGIN"))
MT5_PASSWORD = os.getenv("MT5_PASSWORD")
MT5_SERVER = os.getenv("MT5_SERVER")

# Track closed positions to detect new closes
seen_closed_tickets: Set[int] = set()


def init_mt5() -> bool:
    """Initialize MT5 connection"""
    if not mt5.initialize():
        print(f"MT5 initialize failed: {mt5.last_error()}")
        return False
    
    authorized = mt5.login(MT5_LOGIN, password=MT5_PASSWORD, server=MT5_SERVER)
    if not authorized:
        print(f"MT5 login failed: {mt5.last_error()}")
        mt5.shutdown()
        return False
    
    print(f"✓ Connected to MT5: {mt5.account_info().login} on {mt5.account_info().server}")
    return True


def get_daily_pnl_utc8() -> tuple[float, float]:
    """
    Calculate daily PnL based on UTC+8 midnight reset.
    Returns (daily_pnl, daily_pnl_pct)
    """
    tz = pytz.timezone(TIMEZONE)
    now = datetime.now(tz)
    today_start = now.replace(hour=0, minute=0, second=0, microsecond=0)
    
    # Get deals since today's start
    deals = mt5.history_deals_get(today_start, datetime.now(tz))
    if not deals:
        return 0.0, 0.0
    
    daily_profit = sum(
        deal.profit + deal.commission + deal.swap
        for deal in deals
        if deal.type in (mt5.DEAL_TYPE_BUY, mt5.DEAL_TYPE_SELL)
    )
    
    # Calculate percentage
    account_info = mt5.account_info()
    if account_info and account_info.balance > 0:
        daily_pct = (daily_profit / account_info.balance) * 100
    else:
        daily_pct = 0.0
    
    return daily_profit, daily_pct


def get_account_summary() -> Optional[Dict]:
    """Fetch account summary matching AlisWorld contract"""
    info = mt5.account_info()
    if info is None:
        return None
    
    daily_pnl, daily_pnl_pct = get_daily_pnl_utc8()
    
    return {
        "balance": info.balance,
        "equity": info.equity,
        "margin": info.margin,
        "free_margin": info.margin_free,
        "margin_level": info.margin_level if info.margin > 0 else 0.0,
        "currency": info.currency,
        "daily_pnl": daily_pnl,
        "daily_pnl_pct": daily_pnl_pct,
        "server_time": datetime.now(timezone.utc).isoformat()
    }


def get_open_positions() -> List[Dict]:
    """Get open positions matching AlisWorld contract"""
    positions = mt5.positions_get()
    if positions is None:
        return []
    
    result = []
    for pos in positions:
        pos_type = "buy" if pos.type == mt5.POSITION_TYPE_BUY else "sell"
        
        result.append({
            "ticket": pos.ticket,
            "symbol": pos.symbol,
            "type": pos_type,
            "volume": pos.volume,
            "open_price": pos.price_open,
            "current_price": pos.price_current,
            "sl": pos.sl,
            "tp": pos.tp,
            "swap": pos.swap,
            "commission": getattr(pos, 'commission', 0.0),
            "profit": pos.profit,
            "open_time": datetime.fromtimestamp(pos.time, tz=timezone.utc).isoformat(),
            "comment": pos.comment
        })
    
    return result


def detect_close_reason(deal) -> str:
    """
    Detect if a position closed via TP, SL, manual, or other.
    Uses MT5 deal entry type to determine reason.
    """
    # MT5 deal entry constants
    ENTRY_IN = 0    # Entry into market
    ENTRY_OUT = 1   # Exit from market
    
    # Check if this is an exit deal
    if deal.entry != ENTRY_OUT:
        return "other"
    
    # Check deal comment for hints
    comment = deal.comment.lower()
    if "tp" in comment or "take profit" in comment:
        return "tp"
    if "sl" in comment or "stop loss" in comment:
        return "sl"
    if "close" in comment or "manual" in comment:
        return "manual"
    
    # TODO: More sophisticated detection could check if close price == TP or SL
    # from the original position, but that requires tracking position history.
    # For now, default to "other"
    return "other"


def check_new_closed_positions():
    """Check for newly closed positions and report to backend"""
    global seen_closed_tickets
    
    # Get recent deal history (last hour)
    utc_to = datetime.utcnow()
    utc_from = utc_to - timedelta(hours=1)
    deals = mt5.history_deals_get(utc_from, utc_to)
    
    if not deals:
        return
    
    # Group deals by position to reconstruct full trades
    position_deals = {}
    for deal in deals:
        if deal.type not in (mt5.DEAL_TYPE_BUY, mt5.DEAL_TYPE_SELL):
            continue
        
        pos_id = deal.position_id
        if pos_id not in position_deals:
            position_deals[pos_id] = []
        position_deals[pos_id].append(deal)
    
    # Find completed positions (have both entry and exit)
    for pos_id, deals_list in position_deals.items():
        # Skip if already reported
        if pos_id in seen_closed_tickets:
            continue
        
        # Sort by time
        deals_list.sort(key=lambda d: d.time)
        
        # Find entry and exit
        entry_deal = None
        exit_deal = None
        
        for deal in deals_list:
            if deal.entry == 0:  # ENTRY_IN
                entry_deal = deal
            elif deal.entry == 1:  # ENTRY_OUT
                exit_deal = deal
        
        # If we have both, this is a closed position
        if entry_deal and exit_deal:
            close_reason = detect_close_reason(exit_deal)
            
            # Calculate total profit (including swap and commission)
            total_profit = sum(d.profit + d.commission + d.swap for d in deals_list)
            result = "win" if total_profit > 0 else "loss"
            
            trade_history = {
                "ticket": pos_id,
                "symbol": exit_deal.symbol,
                "type": "buy" if entry_deal.type == mt5.DEAL_TYPE_BUY else "sell",
                "volume": exit_deal.volume,
                "open_price": entry_deal.price,
                "close_price": exit_deal.price,
                "swap": sum(d.swap for d in deals_list),
                "commission": sum(d.commission for d in deals_list),
                "profit": total_profit,
                "result": result,
                "close_reason": close_reason,
                "open_time": datetime.fromtimestamp(entry_deal.time, tz=timezone.utc).isoformat(),
                "close_time": datetime.fromtimestamp(exit_deal.time, tz=timezone.utc).isoformat()
            }
            
            # Send to backend
            if push_trade_history(trade_history):
                seen_closed_tickets.add(pos_id)
                print(f"  ✓ Reported closed position #{pos_id}: {result} ({close_reason})")


def push_account_summary(payload: Dict) -> bool:
    """POST account summary to AlisWorld backend"""
    url = f"{BACKEND_URL}/api/ingest/account-summary"
    headers = {"X-API-Key": API_KEY}
    try:
        resp = requests.post(url, json=payload, headers=headers, timeout=10)
        resp.raise_for_status()
        return True
    except Exception as e:
        print(f"  ✗ Account summary push failed: {e}")
        return False


def push_open_positions(positions: List[Dict]) -> bool:
    """POST open positions to AlisWorld backend"""
    url = f"{BACKEND_URL}/api/ingest/open-positions"
    headers = {"X-API-Key": API_KEY}
    payload = {"positions": positions}
    try:
        resp = requests.post(url, json=payload, headers=headers, timeout=10)
        resp.raise_for_status()
        return True
    except Exception as e:
        print(f"  ✗ Open positions push failed: {e}")
        return False


def push_trade_history(trade: Dict) -> bool:
    """POST closed trade to AlisWorld backend"""
    url = f"{BACKEND_URL}/api/ingest/trade-history"
    headers = {"X-API-Key": API_KEY}
    try:
        resp = requests.post(url, json=trade, headers=headers, timeout=10)
        resp.raise_for_status()
        return True
    except Exception as e:
        print(f"  ✗ Trade history push failed: {e}")
        return False


def poll_commands() -> List[Dict]:
    """Poll AlisWorld backend for pending commands"""
    url = f"{BACKEND_URL}/api/commands/pending"
    headers = {"X-API-Key": API_KEY}
    try:
        resp = requests.get(url, headers=headers, timeout=10)
        resp.raise_for_status()
        data = resp.json()
        return data.get("commands", [])
    except Exception as e:
        print(f"  ✗ Poll commands failed: {e}")
        return []


def execute_close_position(ticket: int) -> Dict:
    """Execute close position command"""
    # Get position info
    positions = mt5.positions_get(ticket=ticket)
    if not positions or len(positions) == 0:
        return {
            "status": "failed",
            "closed_price": None,
            "message": "Position not found or already closed"
        }
    
    position = positions[0]
    
    # Get current price
    tick = mt5.symbol_info_tick(position.symbol)
    if not tick:
        return {
            "status": "failed",
            "closed_price": None,
            "message": f"Failed to get current price for {position.symbol}"
        }
    
    # Prepare close request
    close_request = {
        "action": mt5.TRADE_ACTION_DEAL,
        "position": ticket,
        "symbol": position.symbol,
        "volume": position.volume,
        "type": mt5.ORDER_TYPE_SELL if position.type == mt5.POSITION_TYPE_BUY else mt5.ORDER_TYPE_BUY,
        "price": tick.bid if position.type == mt5.POSITION_TYPE_BUY else tick.ask,
        "deviation": 20,
        "magic": 0,
        "comment": "AlisWorld remote close",
        "type_time": mt5.ORDER_TIME_GTC,
        "type_filling": mt5.ORDER_FILLING_FOK,
    }
    
    # Execute close
    result = mt5.order_send(close_request)
    
    if result.retcode != mt5.TRADE_RETCODE_DONE:
        return {
            "status": "failed",
            "closed_price": None,
            "message": f"MT5 error: {result.comment}"
        }
    
    return {
        "status": "success",
        "closed_price": result.price,
        "message": ""
    }


def report_command_result(command_id: str, result: Dict) -> bool:
    """Report command execution result to AlisWorld backend"""
    url = f"{BACKEND_URL}/api/commands/{command_id}/result"
    headers = {"X-API-Key": API_KEY}
    try:
        resp = requests.post(url, json=result, headers=headers, timeout=10)
        resp.raise_for_status()
        return True
    except Exception as e:
        print(f"  ✗ Report command result failed: {e}")
        return False


def main_loop():
    """Main bidirectional pusher loop"""
    print(f"AlisWorld MT5 Pusher")
    print(f"  Backend: {BACKEND_URL}")
    print(f"  Push interval: {PUSH_INTERVAL}s")
    print(f"  Poll interval: {POLL_INTERVAL}s")
    print(f"  Timezone: {TIMEZONE}")
    print(f"  Press Ctrl+C to stop")
    print()
    
    if not init_mt5():
        print("Failed to initialize MT5. Exiting.")
        return
    
    mt5.shutdown()  # Close initial connection
    
    last_data_push = 0
    last_command_poll = 0
    
    try:
        while True:
            current_time = time.time()
            
            # Push account data and positions at regular interval
            if current_time - last_data_push >= PUSH_INTERVAL:
                if not init_mt5():
                    time.sleep(5)
                    continue
                
                try:
                    # Push account summary
                    account_summary = get_account_summary()
                    if account_summary:
                        push_account_summary(account_summary)
                    
                    # Push open positions
                    positions = get_open_positions()
                    push_open_positions(positions)
                    
                    # Check for new closed positions
                    check_new_closed_positions()
                    
                    # Log status
                    total_pnl = sum(p["profit"] for p in positions)
                    ts = datetime.now().strftime("%H:%M:%S")
                    print(f"[{ts}] ✓ Pushed: {len(positions)} positions, PnL: {total_pnl:+.2f}, Daily: {account_summary['daily_pnl']:+.2f}")
                    
                    last_data_push = current_time
                finally:
                    mt5.shutdown()
            
            # Poll for commands at faster interval
            if current_time - last_command_poll >= POLL_INTERVAL:
                commands = poll_commands()
                
                if commands:
                    # Re-init MT5 for command execution
                    if not init_mt5():
                        time.sleep(2)
                        continue
                    
                    try:
                        for cmd in commands:
                            cmd_id = cmd.get("command_id")
                            action = cmd.get("action")
                            ticket = cmd.get("ticket")
                            
                            print(f"  → Executing command {cmd_id}: {action} ticket {ticket}")
                            
                            if action == "close_position":
                                result = execute_close_position(ticket)
                                report_command_result(cmd_id, result)
                                print(f"    ✓ Command {cmd_id} {result['status']}")
                            else:
                                report_command_result(cmd_id, {
                                    "status": "failed",
                                    "closed_price": None,
                                    "message": f"Unknown action: {action}"
                                })
                    finally:
                        mt5.shutdown()
                
                last_command_poll = current_time
            
            time.sleep(0.5)  # Small sleep to avoid tight loop
            
    except KeyboardInterrupt:
        print("\nStopped by user.")
    finally:
        mt5.shutdown()


if __name__ == "__main__":
    main_loop()
