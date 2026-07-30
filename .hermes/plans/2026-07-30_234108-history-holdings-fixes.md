# AlisWorld History Metrics and Holdings Fixes Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Add useful realised-P&L summaries and symbol filtering to History, fix Holdings close-command reliability, and make bottom navigation work after opening Holdings from the dashboard banner.

**Architecture:** Keep History calculations client-side only after confirming the bridge history endpoint can return the complete closed-deal dataset. Store the selected symbol in `HistoryViewModel` state and derive the displayed list/metrics from the loaded closed deals. For Holdings issues, first create reproducible checks and trace the UI → bridge command contract and dashboard → NavController route stack; only then make minimal targeted fixes.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android Navigation Compose, Kotlin StateFlow/ViewModel, Ktor, GitHub Actions.

---

## Current context and evidence

- Repo: `C:\Users\User\alisworld`, current commit `19e3869` (`Fix close position command serialization`). Working tree was clean during planning.
- History currently displays one `This list` total at `android/app/src/main/java/com/alisworld/app/ui/screens/HistoryScreen.kt:66-73`.
- `ApiClient.getHistory()` currently calls `/api/mt5-history` with `limit` only; supplied `sort`, `order`, and `offset` arguments are unused. It filters `entry == 1` locally.
- Therefore daily and all-time realised P&L cannot be claimed correct until the actual bridge endpoint’s response/pagination semantics are verified. The current first page is not necessarily “all time”.
- Holdings navigation from the dashboard uses a direct `navigate("positions")` at `ui/Navigation.kt:63-68`, unlike the bottom navigation’s start-destination `popUpTo` behaviour at lines 42-50.
- The close button confirms and calls `PositionsViewModel.closePosition()`; the view model currently reloads immediately after the API accepts a command, but provides no pending/success/error feedback per ticket.
- A serialization fix was just committed for the Android command request, so persistent failure must be verified against the live bridge/pusher command contract rather than guessed.

## Acceptance criteria

1. History top section shows **Daily P&L**, **All-time P&L**, and **This list P&L**.
2. User can select **All symbols** or one returned symbol; list and This list P&L update to that selection.
3. Daily and all-time P&L are calculated from the full relevant closed-deal dataset, not a partial page. Their definitions use realised `profit` as returned by MT5 (including/excluding commission and swap consistently with existing app semantics).
4. Close Position sends exactly one valid close command for the selected ticket, shows pending/result feedback, and accurately reflects actual bridge/pusher response rather than assuming the MT5 trade immediately disappeared.
5. Opening Holdings from the dashboard and then tapping any bottom-nav destination works immediately; no Android Back workaround.
6. No existing open trade is closed during investigation or verification unless the user explicitly authorizes a controlled test.
7. APK is built only by GitHub Actions; after success, artifact validation, temp.sh verification, Telegram APK/report delivery, and the delivery gate all pass.

---

### Task 1: Confirm live bridge contracts and create a safe feedback loop

**Objective:** Determine whether missing UI data or an API/bridge mismatch causes the two reported Holding bugs, without executing a trade close.

**Files:**
- Inspect: `android/app/src/main/java/com/alisworld/app/data/ApiClient.kt:39-129`
- Inspect: `android/app/src/main/java/com/alisworld/app/data/Models.kt:104-171`
- Inspect: bridge API/OpenAPI or deployed source, if available
- Create (if no existing test setup): `android/app/src/test/java/com/alisworld/app/data/BridgeContractTest.kt`

**Step 1: Capture safe, read-only endpoint evidence.**

- Request the bridge’s `/openapi.json` or API documentation and call only GET endpoints (`/api/mt5-account`, `/api/mt5-positions`, `/api/mt5-history`).
- Record actual history response shape, date format/time zone, max limit, pagination parameters, total semantics, and whether a server summary exists.
- Inspect command endpoint schema via OpenAPI or deployed source. Do **not** POST a close command to a real ticket.

**Step 2: Build red-capable tests/harnesses.**

- Add pure Kotlin tests for parsing the known history/command response fixtures.
- Add a navigation UI/instrumentation test or a small deterministic NavController harness for the exact sequence: Dashboard → banner → Positions → tap History and Dashboard.
- Add a fake-`ApiClient`/repository seam if needed so `closePosition` can be tested against a simulated `pending`, rejected, and malformed response without touching MT5.

**Step 3: Run the tests before any fix.**

Run appropriate JVM/instrumented tests through the CI workflow after implementation; locally, only non-Android test tooling may be used for diagnosis, never local APK compilation.

**Success signal:** Evidence identifies exact request/response shape and a regression test can fail for each code-level defect.

---

### Task 2: Design the History data model and metric definitions

**Objective:** Make metric calculations correct, explicit, and testable before changing the visual page.

**Files:**
- Modify: `android/app/src/main/java/com/alisworld/app/ui/viewmodels/HistoryViewModel.kt`
- Modify: `android/app/src/main/java/com/alisworld/app/data/ApiClient.kt:66-100`
- Possibly modify: `android/app/src/main/java/com/alisworld/app/data/Models.kt:97-130`
- Test: `android/app/src/test/java/com/alisworld/app/ui/viewmodels/HistoryViewModelTest.kt`

**Step 1: Write failing calculation tests.**

Fixture should include multiple symbols and dates. Assert:
- Daily P&L = sum of closed deals on the current MT5/bridge local calendar day.
- All-time P&L = sum of every loaded closed deal available from the bridge.
- This list P&L = sum after the selected symbol filter.
- `All symbols` shows every item.
- Selecting e.g. `XAUUSD` shows only XAUUSD and recalculates This list P&L.
- Empty filtered results show zero P&L and a clear no-results state.

**Step 2: Implement the smallest correct retrieval strategy based on Task 1.**

- If bridge supports pagination: honor `offset` and repeatedly fetch all pages (or a server-provided aggregate) before claiming all-time values.
- If bridge supports a large safe `limit` but not pagination: request the documented maximum and label/limit the metric appropriately only if it is demonstrably complete.
- If bridge provides explicit daily/all-time aggregates: deserialize and use those, while `This list` remains derived from the current selected list.
- Centralize closed-deal filtering and date parsing in testable helper logic; use the bridge’s documented timezone, not handset timezone by accident.

**Step 3: Run tests and verify green.**

**Success signal:** Tests prove daily/all-time/list totals and selected-symbol filtering from deterministic fixtures.

---

### Task 3: Add the History summary and symbol filter UI

**Objective:** Present the three requested summaries and an accessible symbol filter without disrupting refresh/load-more states.

**Files:**
- Modify: `android/app/src/main/java/com/alisworld/app/ui/screens/HistoryScreen.kt`
- Modify: `android/app/src/main/java/com/alisworld/app/ui/viewmodels/HistoryViewModel.kt`
- Test: Compose UI tests under `android/app/src/androidTest/...` if project supports them

**Step 1: Update state API.**

- Add `selectedSymbol: String?` (`null` = All symbols), derived available symbols, and explicit summary fields/derived values.
- Add `selectSymbol(symbol: String?)` without unnecessarily re-fetching history for client-side filtering.

**Step 2: Implement top layout.**

- Place a compact summary area at the top, before trades:
  - **Daily P&L**
  - **All-time P&L**
  - **This list P&L** (clearly indicates active selected symbol / all symbols)
- Keep positive/negative colors consistent with existing `GreenProfit`/`RedLoss` theme values.
- Add an Material 3 exposed dropdown or filter chip/dropdown with `All symbols` plus unique symbols from history.
- Maintain readable wrapping/scrolling for narrow phones.

**Step 3: Add UI assertions.**

- Verify all three metric labels render.
- Verify selecting a symbol updates visible cards and This list P&L.
- Verify refresh retains/reconciles selection safely when that symbol is absent from new data.

**Success signal:** UI test/harness demonstrates filtering and summaries, including empty filtered state.

---

### Task 4: Diagnose and repair close-position flow

**Objective:** Fix the confirmed root cause and give safe user feedback for command lifecycle.

**Files:**
- Modify as evidence requires: `android/app/src/main/java/com/alisworld/app/data/ApiClient.kt:59-64`
- Modify: `android/app/src/main/java/com/alisworld/app/data/Models.kt:145-171`
- Modify: `android/app/src/main/java/com/alisworld/app/ui/viewmodels/PositionsViewModel.kt:43-56`
- Modify: `android/app/src/main/java/com/alisworld/app/ui/screens/PositionsScreen.kt:77-239`
- Test: `android/app/src/test/java/com/alisworld/app/ui/viewmodels/PositionsViewModelTest.kt`

**Step 1: Write failing tests from Task 1 evidence.**

Cover the exact bridge response schema and at least:
- valid queued command response;
- HTTP/serialization rejection shown to the user;
- a command queued but not yet executed;
- no duplicate command while a ticket is already closing.

**Step 2: Implement only the proven contract fix.**

Possibilities must be selected solely from observed contract evidence, e.g. correct response field names/type, correct request envelope, required auth header, or bridge endpoint mismatch. Do not stack speculative changes.

**Step 3: Improve lifecycle feedback.**

- Track `closingTicket`/command state in `PositionsUiState`.
- Disable only that ticket’s close button while its request is pending.
- Show a clear queued/success/failure message (Snackbar or inline status).
- Do not imply “closed” merely because the command was queued; refresh/poll only according to confirmed bridge semantics.

**Step 4: Verify against a non-destructive fake/staging contract.**

No live position closure without explicit approval.

**Success signal:** Regression tests pass and UI exposes accurate command status.

---

### Task 5: Fix Dashboard-to-Holdings navigation stack

**Objective:** Make direct dashboard banner navigation behave consistently with bottom nav selection.

**Files:**
- Modify: `android/app/src/main/java/com/alisworld/app/ui/Navigation.kt:42-72`
- Test: navigation Compose/instrumented test file, e.g. `android/app/src/androidTest/java/com/alisworld/app/ui/NavigationTest.kt`

**Step 1: Write failing navigation sequence test.**

Sequence:
1. Launch Dashboard.
2. Tap `LIVE FLOATING P&L` banner.
3. Assert Positions route is current.
4. Tap History bottom icon and assert History route is current.
5. Tap Dashboard bottom icon and assert Dashboard route is current.
6. Repeat dashboard → banner → Positions to ensure no duplicate/blocked route stack.

**Step 2: Consolidate navigation behavior.**

- Create one shared function for top-level routes that uses `popUpTo(findStartDestination())`, `launchSingleTop`, and `restoreState`.
- Use it from both bottom NavigationBar and the dashboard callback, ensuring direct navigation cannot create an incompatible nested/doubled stack.

**Step 3: Run regression test.**

**Success signal:** Exact user flow passes with no Back press needed.

---

### Task 6: Review, commit, and CI-only release verification

**Objective:** Keep scope controlled and deliver a verified Android APK through the required pipeline.

**Files:**
- Inspect/possibly modify: `.github/workflows/android-build.yml`
- Update: AlisWorld troubleshooting archive at `C:\Users\User\AppData\Local\hermes\skills\software-development\alisworld-apk-release\references\build-troubleshooting-archive.md` with verified evidence only.

**Step 1: Pre-commit review.**

- Check diff is limited to History summaries/filter, close command feedback/root-cause fix, navigation fix, and regression tests.
- Verify no credentials, API keys, MT5 account details, or Firebase config entered source control.
- Correct known workflow branch mismatch: workflow triggers on `master` but release condition currently checks `main`; confirm intended behavior and align if required for delivery.

**Step 2: Commit and push to `master`.**

Use concise commit message, e.g. `feat: improve history insights and holdings controls`.

**Step 3: Monitor the exact GitHub Actions run matching the pushed commit.**

- Do not use local Gradle/APK compilation.
- If CI fails, retrieve exact logs, fix first root cause, commit/push, and repeat.

**Step 4: Artifact and delivery verification.**

- Download APK from the successful matching run.
- Verify non-zero file, filename, byte size, SHA-256, commit, run URL, and duration.
- Run a startup audit because ViewModel/navigation initialization changes affect app startup/navigation graph; describe evidence accurately as audit, not physical-device testing.
- Upload exact APK to temp.sh and verify returned URL is reachable.
- Send APK and separate build report to Telegram.
- Produce evidence JSON and run the mandatory `verify_delivery_gate.py`; only report complete after `DELIVERY_GATE_PASSED`.

---

## Risks and decisions to confirm during investigation

- **All-time correctness:** If the bridge only returns a capped deal list and offers neither pagination nor aggregate totals, the Android app cannot calculate true all-time P&L. In that case, add/extend a bridge endpoint first or label it honestly as available-history P&L—do not present a misleading all-time number.
- **Daily timezone:** Confirm whether daily P&L uses broker server time, bridge server time, or Malaysia time (recommended given user context). This choice changes trades near midnight.
- **Profit definition:** Confirm whether “PnL” should use MT5’s `profit` field alone (current behaviour) or net result (`profit + swap + commission + fee`). Use one definition consistently in all three cards.
- **Live close test:** A real end-to-end closure is a financial side effect and requires explicit user authorization; otherwise contract tests/staging evidence are the verification boundary.
- **GitHub CLI unavailable locally:** `gh` is not installed in current environment. Before CI operations, use the established GitHub auth/discovery method or GitHub REST with an authenticated token; do not claim Actions status without real API/web evidence.

## Likely files to change

- `android/app/src/main/java/com/alisworld/app/ui/screens/HistoryScreen.kt`
- `android/app/src/main/java/com/alisworld/app/ui/viewmodels/HistoryViewModel.kt`
- `android/app/src/main/java/com/alisworld/app/data/ApiClient.kt`
- `android/app/src/main/java/com/alisworld/app/data/Models.kt`
- `android/app/src/main/java/com/alisworld/app/ui/viewmodels/PositionsViewModel.kt`
- `android/app/src/main/java/com/alisworld/app/ui/screens/PositionsScreen.kt`
- `android/app/src/main/java/com/alisworld/app/ui/Navigation.kt`
- Relevant unit/UI test files
- Potentially `.github/workflows/android-build.yml` (branch condition only)

## Non-goals

- No change to strategy, MT5 order management, auto-closing logic, or existing open trades.
- No backend rewrite unless bridge evidence proves aggregates/pagination are unavailable.
- No local Android build; APK validation remains GitHub Actions-only.
