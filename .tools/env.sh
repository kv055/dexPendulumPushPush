#!/usr/bin/env bash
TOOLSDIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export JAVA_HOME="$TOOLSDIR/jdk"
export ANDROID_HOME="$TOOLSDIR/android-sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
