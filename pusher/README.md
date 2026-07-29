# AlisWorld MT5 Pusher

Bridge script that runs on your MT5 Windows VPS. Pushes account/position data to AlisWorld backend and polls for remote close commands.

## Setup

1. Install Python dependencies:
   ```bash
   pip install -r requirements.txt
   ```

2. Create `.env` from template:
   ```bash
   cp .env.example .env
   ```

3. Edit `.env` with your MT5 credentials and backend URL:
   ```env
   MT5_LOGIN=67647448
   MT5_PASSWORD=your-password
   MT5_SERVER=ForexTimeFXTM-Demo02
   BACKEND_URL=https://your-backend-url.com
   API_KEY=your-api-key
   ```

4. Run the pusher:
   ```bash
   python pusher.py
   ```

## What it does

- **Every 2 seconds**: Pushes account summary + open positions to backend
- **Every 1 second**: Polls backend for pending close-position commands
- **Real-time**: Detects newly closed positions and reports them (with TP/SL/manual reason detection)

## Daily PnL Calculation

Daily PnL resets at midnight in your configured timezone (default: Asia/Kuala_Lumpur = UTC+8). The pusher tracks all deals since today's start and sums their profit/commission/swap.

## Close Reason Detection

When a position closes, the pusher attempts to detect if it was:
- `tp` — Take Profit hit
- `sl` — Stop Loss hit
- `manual` — Manually closed
- `other` — Unknown reason

Detection is based on MT5 deal comments. For more accurate detection, consider enhancing `detect_close_reason()` to compare close price with original TP/SL levels.

## Running as a Service

To keep the pusher running 24/7 on your VPS:

### Option A: Windows Task Scheduler
1. Open Task Scheduler
2. Create Basic Task → "AlisWorld Pusher"
3. Trigger: At startup
4. Action: Start a program → `python.exe C:\path\to\pusher.py`
5. Settings: "If task fails, restart every 1 minute"

### Option B: NSSM (Non-Sucking Service Manager)
```cmd
nssm install AlisWorldPusher "C:\Python311\python.exe" "C:\path\to\pusher.py"
nssm start AlisWorldPusher
```

## Troubleshooting

- **"MT5 initialize failed"**: Ensure MT5 terminal is running on the same machine
- **"MT5 login failed"**: Check credentials in `.env`
- **"Push failed"**: Check backend URL and API key, ensure backend is reachable
- **Position close fails with "requote"**: MT5 broker rejected the price — this is normal during high volatility, pusher will retry on next command poll

## Notes

- This script must run on the same Windows machine as your MT5 terminal
- It does NOT need to be the same VPS as your backend — it talks to the backend over HTTPS
- Multiple pushers can run against the same backend (e.g., multiple MT5 accounts)
