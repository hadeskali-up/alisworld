# AlisWorld Bridge History and Holdings Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Use one MT5 pusher process to push live data and safely process bridge close commands, persist complete MT5 deal history in SQLite for correct Malaysia-time P&L summaries, and connect the Android History/Holdings UI to that backend.

**Architecture:** Extend the currently running `C:\Users\User\Downloads\mt5_pusher.py` instead of starting `pusher_bridge_v2.py`. Retain its existing 2-second `/api/mt5-data` push, then add the compatible bridge poll → MT5 execute → bridge status-update cycle from `pusher_bridge_v2.py`. On the bridge backend, persist every unique deal received from the pusher in SQLite, exposing server-computed history summaries and pagination to Android.

**Tech Stack:** Python 3.11, MetaTrader5 Python API, requests, FastAPI bridge, SQLite, Kotlin/Jetpack Compose/Ktor, GitHub Actions.

---

## Verified current state

- Active process was started at 13:44 MYT from `C:\Users\User\Downloads` as `python -u mt5_pusher.py`.
- That active `mt5_pusher.py` pushes to `POST https://bridge.alisuhari.top/api/mt5-data` every 2 seconds but has no command polling code.
- `C:\Users\User\Downloads\pusher_bridge_v2.py` **does contain** a correct bridge command flow:
  - `GET /api/mt5-commands/poll`
  - `mt5.positions_get(ticket=...)` then `mt5.order_send(...)`
  - `PATCH /api/mt5-commands/{id}` with `completed`/`failed`, result/error
- `pusher_bridge_v2.py` is not the active process, which is why its poll flow currently does nothing.
- The live bridge is fresh and connected; `/api/mt5-status` reported two positions and a 2-second freshness age.
- Live bridge history caps at 200 deals even with `?limit=10000`; active pusher also constructs a capped list. This is a bridge/pusher data-retention constraint, not proof that MT5 has only 200 historical deals.
- The user selected Malaysia time (UTC+8) as the daily P&L boundary.
- Do not issue a real close command as a test without a fresh explicit authorization.

## Safety constraints

- There must be exactly one data/command pusher process after cutover.
- Preserve current 2-second data-push behaviour and existing MT5/scalper processes.
- Keep FXTM-compatible `ORDER_FILLING_FOK`; never copy the old `pusher.py` IOC implementation.
- A queued command must be marked failed—not retried indefinitely—when its ticket has already closed via TP/SL/manual action.
- Never expose MT5 credentials, bridge secrets, Telegram tokens, or database paths in Git.

---

### Task 1: Establish a testable one-process pusher contract

**Objective:** Capture the actual bridge schema and make command processing testable without MT5 order execution.

**Files:**
- Modify: `C:\Users\User\Downloads\mt5_pusher.py`
- Reference only: `C:\Users\User\Downloads\pusher_bridge_v2.py:172-242`
- Create: `C:\Users\User\Downloads\tests\test_mt5_pusher_commands.py`

**Steps:**
1. Read live `/openapi.json`, current pending-command response, and `pusher_bridge_v2.py` request/update formats.
2. Extract pure helpers in the active pusher for: command polling, command validation, converting an MT5 execution outcome to bridge PATCH payload, and status updates.
3. Write failing tests with fake `requests`/fake MT5 interfaces for:
   - empty poll response makes no MT5 call;
   - `close_position` command obtains `payload.ticket` and produces FOK close request;
   - absent ticket produces `failed` PATCH with a useful error;
   - unsupported command produces a `failed` PATCH;
   - completed close produces `completed` PATCH with result.
4. Run the tests red before implementing each helper, then green after minimal implementation.

**Verification:** No network POST close request and no MT5 `order_send` during unit tests.

### Task 2: Integrate polling into the active pusher

**Objective:** Add bridge polling to the active script so one Python process owns both data ingestion and command execution.

**Files:**
- Modify: `C:\Users\User\Downloads\mt5_pusher.py`

**Steps:**
1. Add `COMMAND_POLL_INTERVAL` (default 2 seconds) separately from data `PUSH_INTERVAL` (current 2 seconds).
2. In the existing loop, track `last_data_push` and `last_command_poll`; preserve the existing data push exactly.
3. When a command is pending, initialize MT5 only for execution if necessary, validate payload ticket/type, execute FOK close, and PATCH final status to bridge.
4. Ensure MT5 is shut down cleanly and errors in command polling cannot stop normal data pushing.
5. Add concise logs for command id, queue receipt, result status, and bridge-update failure—never log credentials.
6. Stop the old one-way pusher only after syntax/tests pass, then start the same `mt5_pusher.py` path as a single bidirectional process; verify only one `mt5_pusher.py` process remains.

**Verification:**
- Bridge stays `connected` and fresh after restart.
- `GET /api/mt5-commands/poll` remains reachable.
- No real close command is created during cutover.
- User can later explicitly approve one demo-position close for end-to-end validation, or we validate using a non-existent test ticket only if the user authorizes creating that harmless queued command.

### Task 3: Persist unrestricted deal history in bridge SQLite

**Objective:** Turn repeated recent-deal pushes into durable history by storing every distinct MT5 deal.

**Files:**
- Locate and modify deployed bridge FastAPI source for `https://bridge.alisuhari.top` (exact path to discover before editing)
- Modify its SQLite schema/migration module
- Create backend tests for ingestion/query/aggregation

**Steps:**
1. Locate authoritative deployed bridge repository/source and database; do not modify the unused AlisWorld FastAPI backend unless it is confirmed to serve `bridge.alisuhari.top`.
2. Create a `mt5_deals` table keyed by MT5 deal `ticket` (not position/order ID) with order, position id if supplied, symbol, type, entry, volume, price, profit, commission, swap, fee, time, comment, magic, and ingestion timestamp.
3. Use idempotent upsert (`ticket` unique) during `/api/mt5-data` ingestion so the repeated 2-second snapshots cannot duplicate history.
4. Add indexes for `(time DESC)`, `(symbol, time DESC)`, and `entry` to keep history/filter/summary queries fast.
5. Preserve only market deal rows relevant to realised P&L; define realised deals as MT5 `entry == 1` consistently with the current Android behaviour.
6. Use Malaysia time (`Asia/Kuala_Lumpur`) to calculate the date boundary in backend, not handset local time.

**Backfill:**
1. Add a one-shot pusher mode/flag that asks MT5 for a configurable date range and submits batches to bridge (e.g. prior 1–5 years, depending on terminal history availability).
2. First run a read-only MT5 history count/date-range diagnostic.
3. Run backfill only after user confirmation of the discovered available range; uploads are idempotent.
4. Return to normal recent-window push after successful backfill.

**Verification:**
- Re-ingesting the same deal batch leaves row count unchanged.
- A history page spans more than 200 deals after backfill where MT5 provides them.
- Daily/all-time totals match a SQL fixture calculation under UTC+8.

### Task 4: Add server-side History API

**Objective:** Avoid misleading client-side partial totals and expose correct server-calculated P&L.

**Files:**
- Modify deployed bridge API routes/models
- Add backend tests

**Endpoints/shape:**
- `GET /api/mt5-history?limit=&offset=&symbol=` → paginated realised deals, `total`, `has_more`, and available symbols.
- Summary fields: `daily_pnl`, `all_time_pnl`, `filtered_list_pnl`, `currency`, `timezone: "Asia/Kuala_Lumpur"`.

**Steps:**
1. Implement parameterized symbol filtering; reject/normalize invalid pagination limits.
2. Compute Daily P&L as every realised close on the UTC+8 calendar day.
3. Compute All-time P&L over all persisted realised deals.
4. Compute filtered-list P&L over all persisted matching deals (not merely the visual page) and label it clearly in the UI.
5. Decide and document P&L basis: net P&L = `profit + commission + swap + fee`, because MT5 commission/swap may be separate fields. Apply the same basis in daily, all-time, and filtered totals.

**Verification:** Fixtures cover multiple symbols, dates around UTC+8 midnight, negative commission/swap, pagination, and empty symbol results.

### Task 5: Update Android History and Holdings UI

**Objective:** Deliver requested summaries/filter and the confirmed navigation/close-command UX fixes.

**Files:**
- `C:\Users\User\alisworld\android\app\src\main\java\com\alisworld\app\data\Models.kt`
- `...\data\ApiClient.kt`
- `...\ui\viewmodels\HistoryViewModel.kt`
- `...\ui\screens\HistoryScreen.kt`
- `...\ui\Navigation.kt`
- `...\ui\viewmodels\PositionsViewModel.kt`
- `...\ui\screens\PositionsScreen.kt`
- Add appropriate unit/UI tests

**Steps:**
1. Write failing tests for History summary parsing, symbol selection, and Malaysia-time totals supplied by server.
2. Render top cards: Daily P&L, All-time P&L, and This list P&L; include selected-symbol context.
3. Render `All symbols` plus server-returned symbols in a Material 3 filter dropdown; reset pagination when selection changes.
4. Use a common top-level navigation function for both Dashboard banner and NavigationBar so Dashboard → Holdings → any tab works without Back.
5. After command submission, show a ticket-specific queued state; disable duplicate close presses for that ticket and accurately show request rejection. Do not call it closed until bridge reports completed (a later enhancement can poll status).

**Verification:** Compose/navigation tests cover the exact reported flow and symbol filter; close UI tests cover queued and failure messages without live MT5 execution.

### Task 6: Review, release, and evidence

**Objective:** Commit, build, and deliver a verified APK only after all changes pass review.

**Steps:**
1. Check pusher and bridge tests, static checks, and Android tests.
2. Independent code review for secrets, command execution safety, command status handling, SQL parameterization, and correct P&L/timezone logic.
3. Commit/push Android repo only after review. Commit/push bridge repo separately if it is distinct.
4. Trigger/monitor matching GitHub Actions Android build on `master`; do not build APK locally.
5. Download exact APK artifact, verify non-zero size and SHA-256, perform startup/navigation audit, mirror to temp.sh and verify URL, send APK + separate report on Telegram, then run the mandatory delivery gate.
6. Update the AlisWorld troubleshooting archive with only verified root causes/outcomes.

## Key risks and mitigations

- **Active pusher cutover interruption:** keep existing one-way process alive until the updated script passes tests; record PID/command and verify the replacement immediately reports bridge freshness.
- **Duplicate pusher instances:** verify exactly one process after cutover to prevent duplicate data pushes and competing MT5 API sessions.
- **Stale closing command:** re-read current MT5 position by ticket at execution time; report failure if absent.
- **MT5 history availability:** SQLite can only backfill what MT5 terminal has downloaded/returns. Discovery precedes any backfill.
- **Financial side effect:** no real position is closed during implementation/test without explicit per-test approval.
- **Bridge source location unknown:** discovery is mandatory before designing migrations; do not edit local `alisworld/backend` if bridge deployment does not use it.
