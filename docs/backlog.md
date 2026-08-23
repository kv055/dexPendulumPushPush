# Backlog — pushPushRelay

States: `OPEN` | `IN_PROGRESS(<agent>)` | `DONE` | `BLOCKED(reason)`.
One item per agent cycle. Gate: `. .tools/env.sh && ./gradlew build lint test`.

---

## P1 — Skeleton

### [OPEN] B01: Gradle project skeleton
- settings.gradle.kts, root + `:app` build.gradle.kts (Kotlin 2.x, Compose BOM,
  kotlinx-serialization, OkHttp, minSdk 26, targetSdk 35), version catalog.
- App class, MainActivity placeholder, manifest with INTERNET permission.
- Accept: gate green; `./gradlew :app:assembleDebug` produces an APK.

## P2 — Core logic (pure Kotlin, unit-testable)

### [OPEN] B02: Pairing payload parser
- `pairing/PairingPayload.kt`: parse JSON QR content
  `{"v":1,"type":"pushpush-pairing","notify_url","heartbeat_url","secret"}`.
- Validate type/version/URLs(https or dev http)/non-blank secret ≤256 chars.
- Accept: full unit test coverage incl. malformed inputs.

### [OPEN] B03: Device ID generator/store contract
- `storage/PairedConfig.kt`: interface + impl over EncryptedSharedPreferences;
  holds urls, secret, deviceId (`payment-phone-<8hex>` generated once).
- Accept: unit tests with fake prefs for id stability and payload round-trip.

### [OPEN] B04: Relay API client
- `net/RelayClient.kt`: OkHttp client, `postNotification(app,title,text)`,
  `postHeartbeat(deviceId)`; sets `X-Webhook-Secret`; never logs secret/body.
- Accept: MockWebServer tests incl. 4xx/5xx → retryable result types.

### [OPEN] B05: Notification filter
- `relay/NotificationFilter.kt`: allowlist matching + default banking packages.
- Accept: table-driven unit tests.

## P3 — Android components

### [OPEN] B06: NotificationListenerService relay service
- `relay/PushRelayService.kt`, manifest binding + intent filter, permission
  check helper, deep-link to notification access settings.
- Accept: Robolectric/unit-testable parts green; manual smoke checklist in item.

### [OPEN] B07: Heartbeat foreground service
- `heartbeat/HeartbeatService.kt`: foreground service, 5-min coroutine loop,
  posts heartbeat, exposes last-success state via StateFlow; runs on boot
  (RECEIVE_BOOT_COMPLETED) when paired.
- Accept: interval logic unit-tested (injected scheduler).

### [OPEN] B08: QR scan screen
- CameraX + ML Kit barcode scanner in `ui/ScanScreen.kt`; on valid payload:
  save config, start services, navigate to status.

## P4 — UI

### [OPEN] B09: Status screen + first-run checklist
- Endpoint, pairing state, last heartbeat/notification timestamps, connection
  test button, allowlist editor, battery-optimization exemption prompt.

## P5 — Server side

### [OPEN] B10: dexMonitor JSON pairing QR
- In dexArbTrading repo: extend `PhonePushSetupSection.tsx` to render a second
  QR (after Reveal) with the v1 pairing JSON; update
  `e2e/tests/communication-qr.spec.ts`; keep MacroDroid text QR untouched.
