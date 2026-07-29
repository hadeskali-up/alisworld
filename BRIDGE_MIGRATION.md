# Bridge Migration

**Date:** 2026-07-29

AlisWorld app migrated from custom backend to shared bridge infrastructure.

## Changes

### API Endpoints

Old backend (separate FastAPI + SQLite):
- `POST /api/ingest/account-summary`
- `POST /api/ingest/open-positions`
- `GET /api/positions`
- `POST /api/positions/{ticket}/close`
- `GET /api/history`

New bridge (bridge.alisuhari.top):
- `GET /api/mt5-account` - account info + open PnL
- `GET /api/mt5-positions` - current positions with TP/SL progress
- `POST /api/mt5-commands` - command queue (close/open/modify positions)
- `GET /api/mt5-history` - deal history

### Architecture

**Before:**
```
[Windows MT5] → pusher.py → custom backend → Android app
                              (SQLite DB)
```

**After:**
```
[Windows MT5] → pusher.py → bridge.alisuhari.top → Android app
                              (shared with web, KMP apps)
```

### Code Changes

1. **ApiClient.kt**
   - Removed `BuildConfig.BACKEND_URL` / `BuildConfig.BACKEND_API_KEY`
   - Hardcoded `bridge.alisuhari.top`
   - Updated all endpoints to match bridge API
   - Close position now uses command queue pattern

2. **Models.kt**
   - Added bridge response types: `MT5AccountResponse`, `MT5PositionsResponse`, `MT5HistoryResponse`
   - Updated `Position` model to match bridge field names (`price_open`, `price_current`, etc.)
   - Added `CommandResponse` for command queue results

3. **build.gradle.kts**
   - Removed `local.properties` backend config injection
   - No need for `backend.url` / `backend.apiKey` anymore

### Command Queue Pattern

Old (synchronous):
```kotlin
POST /api/positions/{ticket}/close
→ immediate response
```

New (asynchronous):
```kotlin
POST /api/mt5-commands
{
  "command_type": "close_position",
  "payload": {"ticket": 12345}
}
→ { "id": 4, "status": "pending" }
→ pusher polls, executes, reports back within 2s
```

### Removed Features

- FCM push notifications (backend handled this)
- Custom trade history filtering/sorting (bridge returns last 50 deals only)
- Symbol stats aggregation (now computed client-side from history)

### Benefits

1. **Single source of truth** - bridge serves web, Android, KMP apps
2. **No separate backend to maintain** - one less service to run
3. **Unified command queue** - all apps use same MT5 control mechanism
4. **Real-time data** - pusher updates every 30s, shared across all clients

### Migration Checklist

- [x] Update ApiClient endpoints
- [x] Add bridge response models
- [x] Update Position model field names
- [x] Implement command queue for close position
- [x] Remove BuildConfig backend fields
- [x] Remove local.properties dependency
- [ ] Test on device (GitHub Actions will build APK)
- [ ] Verify command queue works end-to-end

### Future Enhancements

1. **FCM notifications** - move to bridge or keep old backend just for this
2. **Open position command** - add UI to open trades from app
3. **Modify SL/TP command** - adjust existing positions
4. **Close all command** - panic button
5. **WebSocket updates** - replace polling with push notifications

### Testing

```bash
# Test bridge endpoints
curl https://bridge.alisuhari.top/api/mt5-account | jq
curl https://bridge.alisuhari.top/api/mt5-positions | jq
curl https://bridge.alisuhari.top/api/mt5-history?limit=10 | jq

# Submit close command
curl -X POST https://bridge.alisuhari.top/api/mt5-commands \
  -H "Content-Type: application/json" \
  -d '{"command_type": "close_position", "payload": {"ticket": 12345}}'

# Check command status
curl https://bridge.alisuhari.top/api/mt5-commands | jq
```

### Rollback

If bridge migration fails:
1. Revert commits on `master`
2. Point app back to old backend
3. Restore `BuildConfig.BACKEND_URL` in build.gradle.kts
4. Add back `local.properties` with backend config

Old backend code is preserved in `backend/` directory (not deleted, just unused).
