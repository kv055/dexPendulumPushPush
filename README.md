# pushPushRelay

Native Android (Kotlin + Compose) app that replaces MacroDroid for the
dexArbTrading [pushpush](../dexArbTrading/dexFiatController) phone pipeline.

## What it does

1. **Pair** — scan the "Relay app pairing" QR in DexArb → Trading Accounts →
   Communication → Phone payment monitoring (click *Reveal header secrets*
   first so the QR contains the shared secret).
2. **Relay payments** — a `NotificationListenerService` forwards notifications
   from allowlisted banking apps to `POST {notify_url}` with body
   `{"app","title","text"}` and header `X-Webhook-Secret`.
3. **Heartbeat** — a foreground service POSTs `{"device_id":"payment-phone-<8hex>"}`
   every 5 minutes to `{heartbeat_url}` with the same header, keeping the
   dashboard's *Phone online* state green.

## Setup on the phone

1. Install `app/build/outputs/apk/debug/app-debug.apk` (or a release build).
2. Open the app, grant camera permission, scan the pairing QR.
3. Grant notification access when prompted (Android settings).
4. Disable battery optimization for the app (checklist in-app).
5. Done. Services restart automatically after reboot.

## Development

See AGENTS.md for architecture and the multi-agent loop. Toolchain lives in
`.tools/` (run `. .tools/setup.sh` once, then `. .tools/env.sh`). Gate:

```
. .tools/env.sh && ./gradlew build lint test
```

The pairing QR payload contract: `{"v":1,"type":"pushpush-pairing",
"notify_url":...,"heartbeat_url":...,"secret":...}` — produced by
dexMonitor's `PhonePushSetupSection.tsx`, parsed by
`pairing/PairingPayload.kt`.
