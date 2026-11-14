# pushPushRelay

Native Android (Kotlin + Compose) app that replaces MacroDroid for the
dexPendulum [pushpush](../dexPendulum/dexFiatController) phone pipeline.

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

## Install on the phone

1. Grab the latest signed APK from [GitHub Releases](../../releases)
   (or `adb install app/build/outputs/apk/debug/app-debug.apk` for dev builds).
2. Open the APK on the phone (allow *Install unknown apps* when prompted),
   grant camera permission, scan the pairing QR.
3. Grant notification access when prompted (Android settings).
4. Disable battery optimization for the app (checklist in-app).
5. Done. Services restart automatically after reboot.

Note: cleartext HTTP is currently only allowed to the emulator host
(10.0.2.2). Pairing from a real phone over LAN requires either HTTPS on the
relay endpoints or extending `networkSecurityConfig`.

## Releases

Releases are fully automated: pushing a tag `v*` triggers
`.github/workflows/release.yml`, which runs the tests, builds a signed release
APK and attaches it to a GitHub Release.

### Secrets flow (dotenvx)

Signing material lives in a **local plaintext `.env`** (gitignored) holding the
keystore base64, passwords, and alias. After any commit that changes `.env`, a
lefthook `post-commit` hook encrypts it into the **committed `.env.enc`** via
[dotenvx](https://dotenvx.com) and creates an automatic `.env values update`
commit. The decryption private key stays in the gitignored `.env.keys` and is
uploaded once as the GitHub secret `DOTENV_PRIVATE_KEY_PRODUCTION`; CI uses it
to reconstruct `.env` from `.env.enc`.

Local signed release build:

```bash
export PATH="$HOME/.local/bin:$PATH"
dotenvx run -- bash -c '. .tools/env.sh && ./gradlew assembleRelease'
```

The keystore `pushpushrelay-release.keystore` is generated once via
`./scripts/generate-keystore.sh` and never leaves your machine (gitignored).
Back up `.env`, `.env.keys`, and the keystore privately — losing them forces an
uninstall/reinstall of the app, which wipes the stored pairing secret and
device id.

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
