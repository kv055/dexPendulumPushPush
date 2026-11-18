# AGENTS.md — pushPushRelay

Native Android (Kotlin + Compose) relay app for the dexPendulum pushpush
endpoint. It replaces MacroDroid: it scans a pairing QR from the dexMonitor
Communication tab, then relays payment notifications and periodic heartbeats.

## Project facts

- Endpoints (from pairing QR):
  - `POST {notify_url}` body `{"app","title","text"}`, header `X-Webhook-Secret`
  - `POST {heartbeat_url}` body `{"device_id"}` (≤128 chars), same header
- Heartbeat interval: **5 minutes** via foreground service coroutine timer
  (WorkManager minimum period is 15 min — do NOT use it for heartbeat).
- Device ID: `payment-phone-<8 hex>` generated once per install, persisted.
- Secret storage must use EncryptedSharedPreferences / encrypted DataStore.
- Notification reading uses `NotificationListenerService` (requires
  "Notification access" grant; deep-link the user to settings on first run).
- Relay only notifications whose package is on the user-selected allowlist (all-installed-apps picker, empty default).
  The picker enumerates ALL installed apps (like MacroDroid's trigger config);
  default is empty — nothing relays until the user selects apps.

## Multi-agent loop

Work is driven by `docs/backlog.md`. Each item has a state:
`OPEN | IN_PROGRESS(<agent>) | DONE | BLOCKED(reason)`.

Loop roles (defined in `.opencode/agents/*.md`):

1. **Planner** — refines/backfills backlog items with acceptance criteria;
   never writes app code.
2. **Implementer** — picks the top OPEN item, marks it IN_PROGRESS, writes
   code following this file's conventions, moves it to DONE only after the
   Tester passes it.
3. **Tester** — writes/runs unit tests (`./gradlew :app:testDebugUnitTest`)
   and the full gate (`./gradlew build lint test`) for each item; fails items
   back to Implementer with notes in the backlog entry.
4. **Reviewer** — reviews the diff of each DONE item against this file and
   security rules; reopens with findings if needed.

Rules:

- Never mark an item DONE without a green gate run.
- No comments in code unless explaining a non-obvious invariant.
- No secrets committed; the shared secret lives only on-device (encrypted).
- Keep responses/diffs minimal; one backlog item per cycle.

## Build environment

- Local toolchain lives in `.tools/` (JDK 17 + Android cmdline-tools), see
  `.tools/setup.sh` and `.tools/env.sh`. Use `. .tools/env.sh` before gradle.
- Gate command: `. .tools/env.sh && ./gradlew build lint test`

## Conventions

- Package root: `de.dexarb.pushpushrelay`
- Modules: single `:app` module, layered as
  `pairing/`, `relay/`, `heartbeat/`, `net/`, `ui/`, `storage/`.
- DI by hand (constructor injection) — no Hilt/Koin unless size demands it.
- HTTP: OkHttp + kotlinx.serialization.
- UI: Jetpack Compose, Material 3.

## Compatibility test

`compat/compat-test.sh` boots headless emulators (default: API 34–37, Android 14–17),
pairs the app via deep-link against a local mock pushpush server, and asserts
heartbeat + payment relay on each level. Run before releases:

```
. .tools/env.sh && ./compat/compat-test.sh            # full matrix
./compat/compat-test.sh --skip-build 34               # single level, reuse APKs
```

Exit code is non-zero on any failure; per-level logs and screenshots land in
`compat/results/` (gitignored). `--keep` leaves the emulator running for
debugging.
