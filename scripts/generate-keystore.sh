#!/usr/bin/env bash
# One-time: generate the release signing keystore and print the GitHub Secrets
# needed by .github/workflows/release.yml.
#
# The keystore is NEVER committed. Keep a private backup of the file and its
# passwords — losing it means you cannot update-install; users must uninstall
# and reinstall, which wipes the stored pairing secret and device id.
set -euo pipefail

KS_FILE="${1:-pushpushrelay-release.keystore}"
ALIAS="pushPushRelay"
VALIDITY_DAYS=10950 # 30 years

if [[ -e "$KS_FILE" ]]; then
  echo "ERROR: $KS_FILE already exists — refusing to overwrite." >&2
  exit 1
fi

keytool -genkeypair -v \
  -keystore "$KS_FILE" \
  -alias "$ALIAS" \
  -keyalg RSA -keysize 4096 \
  -validity "$VALIDITY_DAYS"

echo
echo "Keystore created: $KS_FILE"
echo "Add these GitHub repo secrets (Settings → Secrets and variables → Actions):"
echo
echo "  ANDROID_KEYSTORE_BASE64 = \$(base64 -w0 < $KS_FILE)"
echo "  ANDROID_KEYSTORE_PASSWORD = (the store password you entered)"
echo "  ANDROID_KEY_ALIAS = $ALIAS"
echo "  ANDROID_KEY_PASSWORD = (the key password you entered)"
echo
echo "To print the base64 value now:  base64 -w0 $KS_FILE"
