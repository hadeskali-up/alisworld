# AlisWorld

**Android app for monitoring and managing live MT5 forex trades.**

Replace Jarvis with a modern, self-hosted solution: dashboard, current trades, history, TP-hit notifications, and remote trade closing from your phone.

---

## Architecture

```
[Windows VPS: MT5 Terminal]
        │
        ▼
   pusher.py  ──(HTTPS)──►  Backend API  ◄──(REST)──  Android App
        ▲                       │
        └──(polls commands)──── SQLite DB
```

- **pusher.py**: Runs on your MT5 VPS, pushes account/position snapshots every 2-5s, polls for close-trade commands
- **Backend**: FastAPI + SQLite, stores data and queues commands
- **Android app**: Jetpack Compose, native notifications via FCM

---

## Security Note

**This app exposes trade-closing control.** Keep your API key secret. Do not commit real credentials, account numbers, VPS IPs, or Firebase config to this public repo.

---

## Setup

### 1. Backend

```bash
cd backend
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt

# Create .env from template
cp .env.example .env
# Edit .env: set API_KEY, TIMEZONE=Asia/Kuala_Lumpur

# Run migrations (creates DB schema)
python init_db.py

# Start server
uvicorn main:app --host 0.0.0.0 --port 8000
```

Backend runs at `http://localhost:8000`. API docs at `/docs`.

### 2. Pusher (on MT5 VPS)

```bash
cd pusher
pip install -r requirements.txt

# Create .env from template
cp .env.example .env
# Edit .env: set MT5 login/password/server, BACKEND_URL, API_KEY (must match backend)

# Run pusher
python pusher.py
```

Pusher connects to MT5, pushes data to backend every 2s, polls for commands every 1s.

### 3. Android App

#### Option A: Build locally (Android Studio)

1. Open `android/` in Android Studio
2. Create `local.properties`:
   ```properties
   sdk.dir=/path/to/Android/sdk
   backend.url=https://your-backend-url.com
   backend.apiKey=your-api-key-here
   ```
3. Add `google-services.json` (from Firebase Console → Project Settings → Download) to `android/app/`
4. Build → Run

#### Option B: Download pre-built APK

GitHub Actions builds APKs on every push to `main`. Go to **Releases** and download the latest APK.

**First-run setup:**
1. Install APK
2. Open app → Settings
3. Paste backend URL and API key
4. Grant notification permission when prompted

---

## Firebase Setup (for TP notifications)

1. Create a Firebase project at https://console.firebase.google.com
2. Add an Android app to the project:
   - Package name: `com.alisworld.app` (or whatever you set in `build.gradle`)
3. Download `google-services.json`
4. Place it in `android/app/` (gitignored, never commit it)
5. Backend FCM setup:
   - Firebase Console → Project Settings → Service Accounts → Generate new private key
   - Save as `backend/firebase-service-account.json` (gitignored)
   - Add to backend `.env`: `FIREBASE_CREDENTIALS_PATH=firebase-service-account.json`

---

## Environment Variables

### Backend `.env`
```env
API_KEY=your-secret-key-here
TIMEZONE=Asia/Kuala_Lumpur
DATABASE_URL=sqlite:///./alisworld.db
FIREBASE_CREDENTIALS_PATH=firebase-service-account.json
```

### Pusher `.env`
```env
MT5_LOGIN=67647448
MT5_PASSWORD=your-mt5-password
MT5_SERVER=ForexTimeFXTM-Demo02
BACKEND_URL=https://your-backend-url.com
API_KEY=your-secret-key-here
PUSH_INTERVAL=2
POLL_INTERVAL=1
```

### Android `local.properties`
```properties
sdk.dir=/path/to/Android/sdk
backend.url=https://your-backend-url.com
backend.apiKey=your-api-key-here
```

---

## API Endpoints

### Pusher → Backend (X-API-Key auth)
- `POST /api/ingest/account-summary` — account snapshot
- `POST /api/ingest/open-positions` — current positions (full replace)
- `POST /api/ingest/trade-history` — closed trade record
- `GET /api/commands/pending` — poll for close commands
- `POST /api/commands/{command_id}/result` — report command result

### Android → Backend (X-API-Key auth)
- `GET /api/dashboard/summary` — daily PnL + open position count
- `GET /api/positions` — current open trades
- `POST /api/positions/{ticket}/close` — queue a close command
- `GET /api/history?sort=pnl|date|symbol&order=asc|desc&limit=50&offset=0` — trade history
- `GET /api/history/stats/by-symbol` — profit by symbol (pie chart data)

---

## Deployment

### Backend (VPS/cloud)
- Run behind nginx/Caddy with HTTPS
- Set up systemd service or supervisor to keep it running
- Firewall: allow only pusher VPS IP + your phone IP range (optional but recommended)

### Pusher (MT5 VPS)
- Run as a Windows service or task scheduler job
- Ensure it auto-restarts on failure

### Android APK
- GitHub Actions builds and signs APKs on every push to `main`
- See `.github/workflows/android-build.yml`
- Upload keystore to GitHub Secrets (see CI/CD Setup below)

---

## CI/CD Setup (GitHub Actions)

1. Generate a release keystore:
   ```bash
   keytool -genkey -v -keystore alisworld-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias alisworld
   ```
2. Base64-encode it:
   ```bash
   base64 -w 0 alisworld-release.jks > keystore.base64.txt
   ```
3. Go to GitHub repo → Settings → Secrets and variables → Actions → New repository secret:
   - `ANDROID_KEYSTORE_BASE64` = contents of `keystore.base64.txt`
   - `KEYSTORE_PASSWORD` = keystore password
   - `KEY_ALIAS` = `alisworld`
   - `KEY_PASSWORD` = key password
   - `BACKEND_URL` = your backend URL
   - `BACKEND_API_KEY` = your API key

GitHub Actions will build, sign, and upload APK as a release artifact on every push to `main`.

---

## License

MIT
