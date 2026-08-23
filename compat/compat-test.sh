#!/usr/bin/env bash
# Compatibility test for pushPushRelay.
#
# Boots one headless emulator per Android API level, pairs the app via
# deep-link against a local mock pushpush server, and asserts:
#   1. heartbeat POST with valid secret + per-install device id
#   2. payment relay POST from an allowlisted notification
#
# Usage: ./compat-test.sh [--keep] [--skip-build] [levels...]
#   levels: API levels to test, default "21 26 34"
#   --keep: leave the last emulator and mock server running for debugging
#   --skip-build: reuse existing APKs instead of running gradle
set -uo pipefail

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPAT="$REPO/compat"
RESULTS="$COMPAT/results"
SECRET="test-secret-123"
BOOT_TIMEOUT=420
HEARTBEAT_TIMEOUT=60
RELAY_TIMEOUT=45
KEEP=0
SKIP_BUILD=0

declare -A ARCH=( [21]=x86 [22]=x86 [23]=x86 [24]=x86 [25]=x86 [26]=x86 [27]=x86 [28]=x86 [29]=x86 [30]=x86 [31]=x86 [32]=x86 [33]=x86_64 [34]=x86_64 [35]=x86_64 )
LEVELS=()
while [ $# -gt 0 ]; do
  case "$1" in
    --keep) KEEP=1 ;;
    --skip-build) SKIP_BUILD=1 ;;
    *) LEVELS+=("$1") ;;
  esac
  shift
done
[ ${#LEVELS[@]} -eq 0 ] && LEVELS=(21 26 34)

log()  { echo "[compat] $*"; }
fail() { echo "[compat] FAIL: $*" >&2; }

cd "$REPO"
. .tools/env.sh
export PATH="$ANDROID_HOME/platform-tools:$PATH"

mkdir -p "$RESULTS"

if [ "$SKIP_BUILD" -eq 0 ]; then
  log "Building app APK..."
  ./gradlew :app:assembleDebug -q || { fail "app build failed"; exit 1; }
  log "Building notifier-helper APK..."
  ./gradlew -p compat/notifier-helper assembleDebug -q || { fail "notifier-helper build failed"; exit 1; }
fi
APP_APK="$REPO/app/build/outputs/apk/debug/app-debug.apk"
HELPER_APK="$COMPAT/notifier-helper/app/build/outputs/apk/debug/app-debug.apk"
[ -f "$APP_APK" ] || { fail "app APK missing: $APP_APK"; exit 1; }
[ -f "$HELPER_APK" ] || { fail "helper APK missing: $HELPER_APK"; exit 1; }

adb_wait_boot() {
  local deadline=$((SECONDS + BOOT_TIMEOUT))
  adb wait-for-device
  while [ $SECONDS -lt $deadline ]; do
    [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] && return 0
    sleep 5
  done
  return 1
}

wait_for_hit() {
  local log="$1" path="$2" timeout="$3"
  local deadline=$((SECONDS + timeout))
  while [ $SECONDS -lt $deadline ]; do
    if [ -f "$log" ] && grep -q "\"path\": \"$path\"" "$log" && grep -q '"secret_ok": true' "$log"; then
      return 0
    fi
    sleep 2
  done
  return 1
}

inject_allowlist() {
  local pkg="$1" tmp
  tmp=$(mktemp)
  cat > "$tmp" <<EOF
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <set name="allowlist">
        <string>$pkg</string>
    </set>
</map>
EOF
  adb push "$tmp" /data/local/tmp/compat_settings.xml >/dev/null
  adb shell run-as de.dexarb.pushpushrelay cp /data/local/tmp/compat_settings.xml shared_prefs/settings.xml
  rm -f "$tmp"
}

base64url() { base64 -w0 | tr '+/' '-_' | tr -d '='; }

run_level() {
  local api="$1"
  local arch="${ARCH[$api]:-x86_64}"
  local avd="compat-api$api"
  local image="system-images;android-$api;default;$arch"
  local console_port=$((5600 + api % 100))
  local mock_port=$((8900 + api % 100))
  local dir="$RESULTS/api$api"
  local hits="$dir/hits.log"
  local serial="emulator-$console_port"

  mkdir -p "$dir"
  echo "=================================================================="
  log "API $api: starting"

  if ! "$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager" list avd 2>/dev/null | grep -q "Name: $avd\$"; then
    log "API $api: ensuring system image $image"
    yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$ANDROID_HOME" "$image" >"$dir/sdkmanager.log" 2>&1 \
      || { fail "API $api: sdkmanager failed (see $dir/sdkmanager.log)"; return 1; }
    log "API $api: creating AVD $avd"
    echo no | "$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager" create avd -n "$avd" -k "$image" --force >"$dir/avdmanager.log" 2>&1 \
      || { fail "API $api: avdmanager failed"; return 1; }
  fi

  rm -f "$hits"
  python3 "$COMPAT/mock_pushpush.py" --port "$mock_port" --log "$hits" --secret "$SECRET" \
    >"$dir/mock.log" 2>&1 &
  local mock_pid=$!

  ANDROID_AVD_HOME="${ANDROID_AVD_HOME:-$HOME/.android/avd}" \
    "$ANDROID_HOME/emulator/emulator" -avd "$avd" -port "$console_port" \
    -no-window -gpu swiftshader_indirect -no-audio -no-boot-anim -no-snapshot \
    -memory 1536 >"$dir/emulator.log" 2>&1 &
  local emu_pid=$!

  local ok=1
  adb -s "$serial" wait-for-device
  if ! adb_wait_boot; then
    fail "API $api: boot timeout"
    ok=0
  fi

  if [ $ok -eq 1 ]; then
    log "API $api (Android $(adb -s "$serial" shell getprop ro.build.version.release | tr -d '\r')): booted"

    adb -s "$serial" install -r "$APP_APK" >/dev/null 2>&1 || { fail "API $api: app install failed"; ok=0; }
  fi

  if [ $ok -eq 1 ]; then
    # Grant notification listener access (version-aware).
    local comp="de.dexarb.pushpushrelay/de.dexarb.pushpushrelay.relay.PushRelayService"
    if [ "$api" -ge 24 ]; then
      adb -s "$serial" shell cmd notification allow_listener "$comp" >/dev/null 2>&1
    fi
    local cur new
    cur=$(adb -s "$serial" shell settings get secure enabled_notification_listeners | tr -d '\r')
    if [ "$cur" = "null" ] || [ -z "$cur" ]; then new="$comp"; else case "$cur" in *"$comp"*) new="$cur";; *) new="$cur:$comp";; esac; fi
    adb -s "$serial" shell settings put secure enabled_notification_listeners "$new"

    if [ "$api" -ge 33 ]; then
      adb -s "$serial" shell pm grant de.dexarb.pushpushrelay android.permission.POST_NOTIFICATIONS >/dev/null 2>&1
    fi

    # Pair via deep-link.
    local payload
    payload=$(printf '{"v":1,"type":"pushpush-pairing","notify_url":"http://10.0.2.2:%s/pushpush/notify","heartbeat_url":"http://10.0.2.2:%s/pushpush/phone-heartbeat","secret":"%s"}' "$mock_port" "$mock_port" "$SECRET" | base64url)
    adb -s "$serial" shell am start -a android.intent.action.VIEW -d "pushpushrelay://pair?data=$payload" de.dexarb.pushpushrelay >/dev/null

    if wait_for_hit "$hits" "/pushpush/phone-heartbeat" $HEARTBEAT_TIMEOUT; then
      if grep -q '"body": "{\"device_id\":\"payment-phone-[0-9a-f]\{8\}\"}"' "$hits" || \
         grep -Eq '"device_id":"payment-phone-[0-9a-f]{8}"' "$hits"; then
        log "API $api: heartbeat OK (valid secret, device id format)"
      else
        fail "API $api: heartbeat arrived but device id malformed"
        ok=0
      fi
    else
      fail "API $api: no valid heartbeat within ${HEARTBEAT_TIMEOUT}s"
      ok=0
    fi
  fi

  if [ $ok -eq 1 ]; then
    # Relay check: allowlist a notifier package, post a notification, expect /notify hit.
    local relay_pkg notify_title notify_text
    if [ "$api" -ge 24 ]; then
      relay_pkg="com.android.shell"
      notify_title="Payment received"
      notify_text="You received 25,00 EUR from Compat Test"
    else
      relay_pkg="de.dexarb.notifierhelper"
      notify_title="Payment received"
      notify_text="You received 25,00 EUR from Compat Test"
      adb -s "$serial" install -r "$HELPER_APK" >/dev/null 2>&1 || { fail "API $api: helper install failed"; ok=0; }
    fi

    adb -s "$serial" shell am force-stop de.dexarb.pushpushrelay
    inject_allowlist "$relay_pkg"
    adb -s "$serial" shell am start -n de.dexarb.pushpushrelay/.MainActivity >/dev/null
    sleep 4
    local hits_before
    hits_before=$(grep -c '"/pushpush/notify"' "$hits" 2>/dev/null || echo 0)

    if [ "$api" -ge 24 ]; then
      adb -s "$serial" shell cmd notification post -S bigtext -t "$notify_title" compat_test "$notify_text" >/dev/null 2>&1
    else
      adb -s "$serial" shell am start -n de.dexarb.notifierhelper/.MainActivity >/dev/null
    fi

    if wait_for_hit "$hits" "/pushpush/notify" $RELAY_TIMEOUT; then
      log "API $api: payment relay OK"
    else
      fail "API $api: no payment relay within ${RELAY_TIMEOUT}s"
      ok=0
    fi
  fi

  adb -s "$serial" exec-out screencap -p > "$dir/screen.png" 2>/dev/null

  if [ "$KEEP" -eq 1 ]; then
    log "API $api: --keep set, leaving emulator ($serial) and mock (pid $mock_pid) running"
  else
    adb -s "$serial" emu kill >/dev/null 2>&1
    kill "$mock_pid" >/dev/null 2>&1
    sleep 3
    kill -9 "$emu_pid" "$mock_pid" >/dev/null 2>&1
  fi

  if [ $ok -eq 1 ]; then echo "PASS $api" >> "$RESULTS/summary.txt"; log "API $api: PASS"
  else echo "FAIL $api" >> "$RESULTS/summary.txt"; log "API $api: FAIL"; fi
  return $((1 - ok))
}

rm -f "$RESULTS/summary.txt"
OVERALL=0
for level in "${LEVELS[@]}"; do
  run_level "$level" || OVERALL=1
done

echo "=================================================================="
log "Summary:"
cat "$RESULTS/summary.txt"
if [ "$OVERALL" -eq 0 ]; then log "ALL PASS"; else fail "one or more levels failed"; fi
exit $OVERALL
