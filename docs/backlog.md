# Backlog — pushPushRelay

States: `OPEN` | `IN_PROGRESS(<agent>)` | `DONE` | `BLOCKED(reason)`.
One item per agent cycle. Gate: `. .tools/env.sh && ./gradlew build lint test`.

---

## P1 — Skeleton

### [DONE] B01: Gradle project skeleton
- settings.gradle.kts, root + `:app` build.gradle.kts (Kotlin 2.x, Compose BOM,
  kotlinx-serialization, OkHttp, minSdk 26, targetSdk 35), version catalog.
- App class, MainActivity placeholder, manifest with INTERNET permission.
- Accept: gate green; `./gradlew :app:assembleDebug` produces an APK.

## P2 — Core logic (pure Kotlin, unit-testable)

### [DONE] B02: Pairing payload parser
- `pairing/PairingPayload.kt`: parse JSON QR content
  `{"v":1,"type":"pushpush-pairing","notify_url","heartbeat_url","secret"}`.
- Validate type/version/URLs(https or dev http)/non-blank secret ≤256 chars.
- Accept: full unit test coverage incl. malformed inputs.
- Impl notes: kotlinx.serialization `Json { ignoreUnknownKeys = true }` into a
  private wire DTO; `PairingPayloadParser.parse()` returns sealed
  `PairingParseResult.Parsed/Invalid(reason)` with specific reasons. URL check
  via `java.net.URI` (scheme http/https + non-blank host). 11 unit tests green.

### [DONE] B03: Device ID generator/store contract
- `storage/PairedConfig.kt`: interface + impl over EncryptedSharedPreferences;
  holds urls, secret, deviceId (`payment-phone-<8hex>` generated once).
- Accept: unit tests with fake prefs for id stability and payload round-trip.
- Impl notes: `PairedConfig` data class + `PairedConfigStore` interface +
  `DeviceIdGenerator` (SecureRandom, 8 lowercase hex) are pure Kotlin in
  PairedConfig.kt; Android-backed `EncryptedPairedConfigStore(context)` lives
  in separate storage/EncryptedPairedConfigStore.kt (androidx security-crypto,
  AES256_GCM master key) and is not referenced from JVM unit tests. Tests use
  an in-memory fake store; id regex + uniqueness covered. 4 unit tests green.

### [DONE] B04: Relay API client
- `net/RelayClient.kt`: OkHttp client, `postNotification(app,title,text)`,
  `postHeartbeat(deviceId)`; sets `X-Webhook-Secret`; never logs secret/body.
- Accept: MockWebServer tests incl. 4xx/5xx → retryable result types.
- Impl notes: pure Kotlin; ctor `(OkHttpClient = default(10s connect/15s read),
  notifyUrl, heartbeatUrl, secretProvider)`. kotlinx.serialization JSON with
  `explicitNulls=false` so null app/title/text fields are omitted. Device id
  truncated to 128 chars. Synchronous calls; IOException → NetworkError(message),
  non-2xx → HttpError(code), 2xx → Success. Secret never in results' toString.

### [DONE] B05: Notification filter
- `relay/NotificationFilter.kt`: allowlist matching + default banking packages.
- Accept: table-driven unit tests.
- Impl notes: case-insensitive exact-package matching; blank package never
  relayed; empty allowlist relays nothing. Defaults (8): PayPal, Revolut,
  Wise, N26, Sparkasse, DKB, Commerzbank, Deutsche Bank. Table-driven tests.
  5 unit tests green.

## P3 — Android components

### [DONE] B06: NotificationListenerService relay service
- `relay/PushRelayService.kt`, manifest binding + intent filter, permission
  check helper, deep-link to notification access settings.
- Accept: Robolectric/unit-testable parts green; manual smoke checklist in item.

### [DONE] B07: Heartbeat foreground service
- `heartbeat/HeartbeatService.kt`: foreground service, 5-min coroutine loop,
  posts heartbeat, exposes last-success state via StateFlow; runs on boot
  (RECEIVE_BOOT_COMPLETED) when paired.
- Accept: interval logic unit-tested (injected scheduler).

### [DONE] B08: QR scan screen
- CameraX + ML Kit barcode scanner in `ui/ScanScreen.kt`; on valid payload:
  save config, start services, navigate to status.

## P4 — UI

### [DONE] B09: Status screen + first-run checklist
- Endpoint, pairing state, last heartbeat/notification timestamps, connection
  test button, allowlist editor, battery-optimization exemption prompt.

## P5 — Server side

### [DONE] B10: dexMonitor JSON pairing QR
- In dexArbTrading repo: extend `PhonePushSetupSection.tsx` to render a second
  QR (after Reveal) with the v1 pairing JSON; update
  `e2e/tests/communication-qr.spec.ts`; keep MacroDroid text QR untouched.

---

## Review cycle 1 (reviewer verdict: REOPEN)

### [DONE] R1: service correctness (B06/B07 findings)
- Guard `startForegroundService` against ForegroundServiceStartNotAllowedException (PushRelayService).
- Use NotificationFilter.shouldRelay (case-insensitive) instead of raw Set.contains.
- HeartbeatService must cancel the prior loop job on onStartCommand instead of stacking loops; hoist RelayClient out of the loop.
- Redact secret from PairedConfig/PairingPayload toString.

### [DONE] R2: UI robustness (B08/B09 findings)
- Invalid QR scan must show feedback and re-enable scanning (callback returns accepted flag; reset handled).
- SnackbarHost actually used; executor shut down via DisposableEffect.
- Allowlist editor: list all current entries (not just defaults), support adding/removing arbitrary packages.
- Runtime POST_NOTIFICATIONS request on API 33+; battery-optimization exemption button; re-check notification access on ON_RESUME.

R1/R2 fixed in commit "Fix reviewer findings R1/R2" — gate green, 31 tests.
Remaining accepted-as-minor: service-level Robolectric tests not yet added
(B06/B07 partial coverage), RelayClient constructed per notification instead
of shared instance.

Follow-up cycle 2: shared OkHttp client, lazy config store, Robolectric tests
(38 total), R8 rules + assembleRelease green.

Emulator verification (API 34 x86_64): pairing deep-link, heartbeat POST with
secret + per-install device id, payment relay from allowlisted package — all
green against a local mock pushpush server.

## Compatibility test cycle (compat/)

### [DONE] C01: Standardized emulator compatibility suite
- `compat/compat-test.sh` + `compat/mock_pushpush.py` + `compat/notifier-helper/`.
- Boots headless emulators (default 21/26/34), pairs via deep-link against the
  mock server, asserts heartbeat (secret + device-id format) and payment relay
  (allowlist injection + posted notification) per level; screenshots + logs in
  `compat/results/` (gitignored). Exit code non-zero on failure.
- First full run found and fixed: pipefail SIGPIPE false-failure, Android 5
  `settings put ""` limitation, listener-rebind allowlist overwrite race,
  helper `getSystemService(Class)` crash on API < 23, `cmd notification post`
  unavailability below API 28 (helper covers 21-27).
- Verified: PASS 21 / 26 / 34, exit 0 (single-command run).
