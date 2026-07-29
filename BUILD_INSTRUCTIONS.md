# AlisWorld Build Instructions

## Quick Start (Local Build)

Since you don't want to run production yet, here's how to build and test locally:

### 1. Backend Setup (Local Testing)

```bash
cd backend
python -m venv venv
venv\Scripts\activate  # Windows
pip install -r requirements.txt

# Create .env
copy .env.example .env
# Edit .env: set a random API_KEY, keep localhost

# Initialize DB
python init_db.py

# Run backend
python -m uvicorn main:app --host 0.0.0.0 --port 8000
```

Backend akan run di `http://localhost:8000`

### 2. Android Build (2 Options)

#### Option A: Android Studio (Recommended for Testing)

1. Open Android Studio
2. Open project: `android/` folder
3. Create `local.properties`:
   ```properties
   sdk.dir=C:\\Users\\User\\AppData\\Local\\Android\\Sdk
   backend.url=http://10.0.2.2:8000
   backend.apiKey=your-test-api-key
   ```
   Note: `10.0.2.2` is Android emulator's host loopback
4. Create `app/google-services.json`:
   - Go to https://console.firebase.google.com
   - Create project → Add Android app
   - Package name: `com.alisworld.app`
   - Download `google-services.json` → place in `android/app/`
5. Build → Run (connects to emulator or phone)

#### Option B: Command Line APK

```bash
cd android
./gradlew assembleDebug
# APK output: app/build/outputs/apk/debug/app-debug.apk
```

Install via:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. Pusher (When Ready to Connect MT5)

```bash
cd pusher
pip install -r requirements.txt
copy .env.example .env
# Edit .env with MT5 credentials + backend URL
python pusher.py
```

---

## Production Deployment (Later)

When ready for production:

1. **Backend**: Deploy to VPS/cloud with HTTPS
2. **Pusher**: Run on MT5 VPS
3. **Android APK**: Build release signed APK via GitHub Actions

### GitHub Actions Setup

1. Create GitHub repo: `gh repo create alisworld --public --source=.`
2. Push: `git remote add origin https://github.com/yourusername/alisworld.git && git push -u origin main`
3. Add GitHub Secrets:
   - `BACKEND_URL`: Your production backend URL
   - `BACKEND_API_KEY`: Production API key
   - `GOOGLE_SERVICES_JSON`: Content of your `google-services.json` (paste entire file as secret)
   - `ANDROID_KEYSTORE_BASE64`: Base64 of your release keystore
   - `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

Generate keystore:
```bash
keytool -genkey -v -keystore alisworld-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias alisworld
# Then base64 encode it for GitHub secret
```

GitHub Actions will build APK on every push to main.

---

## Testing Without MT5 (Mock Data)

If you want to test the app UI without connecting to MT5:

1. Backend has no seed data script, but you can manually insert via SQLite:

```bash
cd backend
sqlite3 alisworld.db

INSERT INTO account_snapshots (balance, equity, margin, free_margin, margin_level, currency, daily_pnl, daily_pnl_pct, server_time)
VALUES (10000, 10200, 300, 9900, 3400, 'USD', 150.50, 1.5, '2026-07-29T15:00:00Z');

INSERT INTO open_positions (ticket, symbol, type, volume, open_price, current_price, sl, tp, swap, commission, profit, open_time, comment)
VALUES 
(123001, 'EURUSD', 'buy', 0.5, 1.0850, 1.0870, 1.0800, 1.0950, -0.5, -2.0, 98.50, '2026-07-29T10:00:00Z', ''),
(123002, 'GBPUSD', 'sell', 0.3, 1.2750, 1.2730, 1.2800, 1.2650, -0.3, -1.5, 59.70, '2026-07-29T11:00:00Z', '');

INSERT INTO trade_history (ticket, symbol, type, volume, open_price, close_price, swap, commission, profit, result, close_reason, open_time, close_time)
VALUES
(123000, 'XAUUSD', 'buy', 0.1, 2350.50, 2358.20, -1.2, -3.0, 75.80, 'win', 'tp', '2026-07-28T08:00:00Z', '2026-07-28T14:30:00Z');
```

2. Start backend, open app, you'll see mock data.

---

## Firebase FCM Setup (Notifications)

1. Go to https://console.firebase.google.com
2. Create project "AlisWorld"
3. Add Android app:
   - Package: `com.alisworld.app`
   - Download `google-services.json` → `android/app/`
4. Project Settings → Service Accounts → Generate new private key
   - Save as `backend/firebase-service-account.json`
   - Add to backend `.env`: `FIREBASE_CREDENTIALS_PATH=firebase-service-account.json`

Without Firebase, app will work but TP notifications won't be sent.

---

## Repository Location

- Local: `C:\Users\User\alisworld`
- Backup: Check Telegram message for temp.sh link

---

## Troubleshooting

### Backend won't start
- Check `.env` exists with API_KEY set
- Run `python init_db.py` to create database

### Android build fails
- Ensure `local.properties` exists with sdk.dir and backend config
- Ensure `google-services.json` exists (or remove Firebase dependency temporarily)
- Sync Gradle: File → Sync Project with Gradle Files

### Pusher can't connect to MT5
- Ensure MT5 terminal is running on same machine
- Check MT5 credentials in `.env`
- Check backend URL is reachable from VPS

### App shows "Invalid API key"
- Ensure `backend.apiKey` in `local.properties` matches backend `.env` API_KEY

---

## Next Steps

1. Test backend locally: `cd backend && python -m uvicorn main:app`
2. Open Android Studio, load project, run on emulator
3. Verify dashboard loads (will be empty until pusher runs or you add mock data)
4. When ready, deploy backend to VPS and update pusher config
