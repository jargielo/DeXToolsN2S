#!/usr/bin/env bash
set -euo pipefail
# GitHub's Ubuntu runner includes JDK 17 and the Android command-line tools.
# Local builds use the caller's JAVA_HOME and ANDROID_HOME.
if [ -n "${JAVA_HOME_17_X64:-}" ]; then
  export JAVA_HOME="$JAVA_HOME_17_X64"
  export PATH="$JAVA_HOME/bin:$PATH"
fi
: "${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK directory}"
java -version
if [ -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
  sdkmanager_bin="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
else
  sdkmanager_bin=$(command -v sdkmanager)
fi
sdk_args=(--sdk_root="$ANDROID_HOME")
# Java SDK tools do not automatically use the standard HTTPS_PROXY environment variable.
if [ -n "${HTTPS_PROXY:-}" ]; then
  read -r proxy_host proxy_port < <(python3 -c 'import os,urllib.parse; p=urllib.parse.urlparse(os.environ["HTTPS_PROXY"]); print(p.hostname,p.port or 80)')
  sdk_args+=(--proxy=http --proxy_host="$proxy_host" --proxy_port="$proxy_port")
fi
answers=$(mktemp)
trap 'rm -f "$answers"' EXIT
printf 'y\n%.0s' {1..200} > "$answers"
"$sdkmanager_bin" "${sdk_args[@]}" --licenses < "$answers"
"$sdkmanager_bin" "${sdk_args[@]}" 'platforms;android-36' 'build-tools;36.0.0'
./gradlew --no-daemon --stacktrace assembleDebug testDebugUnitTest lintDebug
"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --verbose app/build/outputs/apk/debug/app-debug.apk
"$ANDROID_HOME/build-tools/36.0.0/aapt" dump badging app/build/outputs/apk/debug/app-debug.apk
sha256sum app/build/outputs/apk/debug/app-debug.apk > app/build/outputs/apk/debug/app-debug.apk.sha256
