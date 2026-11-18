#!/usr/bin/env bash
TOOLSDIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Prefer the local toolchain from setup.sh; without it (e.g. CI) keep the environment's own.
if [ -d "$TOOLSDIR/jdk" ]; then export JAVA_HOME="$TOOLSDIR/jdk"; fi
if [ -d "$TOOLSDIR/android-sdk" ]; then export ANDROID_HOME="$TOOLSDIR/android-sdk"; fi
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
