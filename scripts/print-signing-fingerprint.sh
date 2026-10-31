#!/usr/bin/env bash
# Imprime la huella SHA-256 del certificado con que está firmado un APK y verifica la firma.
set -euo pipefail
APK="${1:?Uso: $0 ruta/al.apk}"
SDK_DIR="${ANDROID_SDK_DIR:-$HOME/android-sdk}"
APKSIGNER="$(ls -d "$SDK_DIR"/build-tools/*/ | sort -V | tail -1)apksigner"
"$APKSIGNER" verify --print-certs "$APK" | grep -E "certificate SHA-256 digest"
