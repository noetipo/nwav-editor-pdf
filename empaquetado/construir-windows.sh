#!/usr/bin/env bash
# Construye, desde macOS o Linux, la version PORTABLE para Windows (x64):
#   target/NWAV-Editor-PDF-<version>-windows-x64.zip
# Incluye su propio Java (Eclipse Temurin JRE 21, GPLv2 + Classpath Exception), asi que
# en Windows basta con descomprimir y abrir "NWAV Editor PDF.cmd".
#
# Uso:  empaquetado/construir-windows.sh [--sin-pruebas]
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
cd "$RAIZ"
VERSION="$(./mvnw -q -B help:evaluate -Dexpression=project.version -DforceStdout)"
NOMBRE="NWAV-Editor-PDF-${VERSION}-windows-x64"
CACHE="$RAIZ/empaquetado/.cache"
SALIDA="$RAIZ/target/windows"
CARPETA="$SALIDA/NWAV Editor PDF"
mkdir -p "$CACHE"

if [[ "${1:-}" != "--sin-pruebas" ]]; then
  echo "==> Pruebas"
  ./mvnw -q -B test
fi

echo "==> Compilando con JavaFX para Windows"
./mvnw -q -B -DskipTests -Djavafx.platform=win package
rm -rf "$SALIDA"
mkdir -p "$CARPETA/app/lib"
./mvnw -q -B -Djavafx.platform=win dependency:copy-dependencies \
  -DincludeScope=runtime -DexcludeArtifactIds=lombok -DoutputDirectory="$CARPETA/app/lib"
cp "target/nwav-editor-pdf-${VERSION}.jar" "$CARPETA/app/lib/"
# Solo JavaFX de Windows (nada de macOS/Linux)
if ls "$CARPETA/app/lib" | grep -qE 'javafx-.*-(mac|linux)'; then
  echo "ERROR: se colaron librerias de JavaFX que no son de Windows" >&2; exit 1
fi

echo "==> Java para Windows (Temurin JRE 21)"
INFO="$(curl -fsSL 'https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jre&os=windows&vendor=eclipse')"
read -r ZIP_URL ZIP_SHA ZIP_NOMBRE < <(python3 -c '
import json,sys
a=[x for x in json.load(sys.stdin) if x["binary"]["package"]["name"].endswith(".zip")][0]["binary"]["package"]
print(a["link"], a["checksum"], a["name"])' <<<"$INFO")
if [[ ! -f "$CACHE/$ZIP_NOMBRE" ]]; then
  curl -fsSL --retry 3 -o "$CACHE/$ZIP_NOMBRE.part" "$ZIP_URL"
  mv "$CACHE/$ZIP_NOMBRE.part" "$CACHE/$ZIP_NOMBRE"
fi
CALCULADO="$(shasum -a 256 "$CACHE/$ZIP_NOMBRE" | cut -d' ' -f1)"
if [[ "$CALCULADO" != "$ZIP_SHA" ]]; then
  rm -f "$CACHE/$ZIP_NOMBRE"
  echo "ERROR: la suma SHA-256 del JRE no coincide; descarga corrupta o manipulada" >&2; exit 1
fi
TMPJRE="$(mktemp -d)"
unzip -q "$CACHE/$ZIP_NOMBRE" -d "$TMPJRE"
mv "$TMPJRE"/*/ "$CARPETA/runtime"
rm -rf "$TMPJRE"

echo "==> Lanzadores"
cp "$RAIZ/empaquetado/windows/"*.cmd "$CARPETA/"
cp "$RAIZ/empaquetado/windows/LEEME.txt" "$CARPETA/"
cp "$RAIZ/empaquetado/nwav-icono.ico" "$CARPETA/app/"

echo "==> Comprimiendo"
rm -f "$RAIZ/target/$NOMBRE.zip"
(cd "$SALIDA" && zip -qr -X "$RAIZ/target/$NOMBRE.zip" "NWAV Editor PDF")
echo "Listo: target/$NOMBRE.zip ($(du -h "$RAIZ/target/$NOMBRE.zip" | cut -f1))"
