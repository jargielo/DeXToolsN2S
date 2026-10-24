#!/usr/bin/env bash
set -euo pipefail
cd /workspace/DeXToolsN2S
mkdir -p /workspace/toolchains/downloads /workspace/.android/cache /workspace/.gradle
if [ ! -x /workspace/toolchains/jdk-17/bin/javac ]; then
  curl -fsSL 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.21%2B9/OpenJDK17U-jdk_x64_linux_hotspot_17.0.21_9.tar.gz' -o /workspace/toolchains/downloads/jdk17.tar.gz
  printf '%s  %s\n' 8b9b611d917e3dfd8419d3566b5949226f69b052fed7e109c1368add1dfe8711 /workspace/toolchains/downloads/jdk17.tar.gz | sha256sum -c -
  mkdir -p /workspace/toolchains/jdk-17
  tar -xzf /workspace/toolchains/downloads/jdk17.tar.gz --strip-components=1 -C /workspace/toolchains/jdk-17
fi
if [ ! -x /workspace/toolchains/android-sdk/cmdline-tools/19.0/bin/sdkmanager ]; then
  curl -fsSL https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip -o /workspace/toolchains/downloads/cmdline.zip
  printf '%s  %s\n' 7ec965280a073311c339e571cd5de778b9975026cfcbe79f2b1cdcb1e15317ee /workspace/toolchains/downloads/cmdline.zip | sha256sum -c -
  unpack_dir=$(mktemp -d /tmp/dextools-sdk.XXXXXX)
  unzip -q /workspace/toolchains/downloads/cmdline.zip -d "$unpack_dir"
  mkdir -p /workspace/toolchains/android-sdk/cmdline-tools
  mv "$unpack_dir/cmdline-tools" /workspace/toolchains/android-sdk/cmdline-tools/19.0
  rmdir "$unpack_dir"
fi
cat > /workspace/toolchains/activate.sh <<'ACTIVATE'
export JAVA_HOME=/workspace/toolchains/jdk-17
export ANDROID_HOME=/workspace/toolchains/android-sdk
export ANDROID_SDK_ROOT=/workspace/toolchains/android-sdk
export ANDROID_USER_HOME=/workspace/.android
export ANDROID_PREFS_ROOT=/workspace
export GRADLE_USER_HOME=/workspace/.gradle
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/19.0/bin:$PATH"
export JAVA_OPTS="${JAVA_OPTS:-} -Duser.home=/workspace -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Duser.home=/workspace -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
ACTIVATE
source /workspace/toolchains/activate.sh
# Local Gradle configuration for the managed proxy; never store proxy credentials.
python3 - <<'PY'
import os,pathlib,urllib.parse
p=pathlib.Path('/workspace/.gradle/gradle.properties')
updates={
 'org.gradle.java.home':'/workspace/toolchains/jdk-17',
 'org.gradle.jvmargs':'-Xmx2048m -Dfile.encoding=UTF-8 -Duser.home=/workspace -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts'
}
proxy=urllib.parse.urlparse(os.environ.get('HTTPS_PROXY',''))
if proxy.hostname:
 for scheme in ['http','https']:
  updates['systemProp.'+scheme+'.proxyHost']=proxy.hostname
  updates['systemProp.'+scheme+'.proxyPort']=str(proxy.port or 80)
 updates['systemProp.http.nonProxyHosts']='localhost|127.*'
old=p.read_text().splitlines() if p.exists() else []
keep=[line for line in old if line.partition('=')[0] not in updates]
p.write_text('\n'.join(keep+[k+'='+v for k,v in updates.items()])+'\n')
PY
sdk_args=(--sdk_root="$ANDROID_HOME")
if [ -n "${HTTPS_PROXY:-}" ]; then
  read -r proxy_host proxy_port < <(python3 -c 'import os,urllib.parse; p=urllib.parse.urlparse(os.environ["HTTPS_PROXY"]); print(p.hostname,p.port or 80)')
  sdk_args+=(--proxy=http --proxy_host="$proxy_host" --proxy_port="$proxy_port")
fi
answers=$(mktemp /tmp/dextools-licenses.XXXXXX)
printf 'y\n%.0s' {1..200} > "$answers"
sdkmanager "${sdk_args[@]}" --licenses < "$answers"
rm "$answers"
sdkmanager "${sdk_args[@]}" 'platforms;android-36' 'build-tools;36.0.0' 'platform-tools'
# ANDROID_HOME supplies SDK discovery; leave tracked files and local.properties intact.
./gradlew --no-daemon assembleDebug testDebugUnitTest lintDebug
"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --verbose app/build/outputs/apk/debug/app-debug.apk
