#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
[ -d jdk ] || tar xzf jdk.tar.gz && mv jdk-17* jdk
[ -d cmdtools ] || unzip -q cmdtools.zip
export JAVA_HOME="$PWD/jdk"
[ -d android-sdk ] || {
  mkdir -p android-sdk/cmdline-tools
  mv cmdtools android-sdk/cmdline-tools/latest
  yes | ./android-sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root="$PWD/android-sdk" --licenses >/dev/null
  ./android-sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root="$PWD/android-sdk" \
    "platform-tools" "platforms;android-35" "build-tools;35.0.0" >/dev/null
}
echo SDK_SETUP_OK
