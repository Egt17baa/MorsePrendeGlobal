#!/usr/bin/env bash
# Instala el Android SDK mínimo para compilar MorsePrende y escribe local.properties.
# Idempotente: si ya está instalado, no vuelve a descargar nada.
set -euo pipefail

SDK_DIR="${ANDROID_SDK_DIR:-$HOME/android-sdk}"
TOOLS_ZIP="commandlinetools-linux-9862592_latest.zip"
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if [ ! -x "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" ]; then
  tmp="$(mktemp -d)"
  curl -fsSL "https://dl.google.com/android/repository/$TOOLS_ZIP" -o "$tmp/tools.zip"
  mkdir -p "$SDK_DIR/cmdline-tools"
  unzip -q "$tmp/tools.zip" -d "$tmp"
  rm -rf "$SDK_DIR/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"
  rm -rf "$tmp"
fi

SDKMANAGER="$SDK_DIR/cmdline-tools/latest/bin/sdkmanager"
if [ ! -d "$SDK_DIR/platforms/android-36" ] || [ ! -d "$SDK_DIR/build-tools/36.0.0" ]; then
  yes | "$SDKMANAGER" --sdk_root="$SDK_DIR" --licenses >/dev/null || true
  "$SDKMANAGER" --sdk_root="$SDK_DIR" "platform-tools" "platforms;android-36" "build-tools;36.0.0"
fi

echo "sdk.dir=$SDK_DIR" > "$REPO_ROOT/local.properties"
echo "Android SDK listo en $SDK_DIR"
