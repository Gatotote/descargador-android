#!/usr/bin/env bash
# Pone el acceso MIFARMA en el escritorio de esta cuenta.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
ICONO="$ROOT/static/img/mifarma-icono.png"
ABRIR="$ROOT/abrir.sh"
chmod +x "$ABRIR"

escribir() {
  local destino="$1"
  mkdir -p "$destino"
  local archivo="$destino/MIFARMA.desktop"
  cat > "$archivo" <<EOF
[Desktop Entry]
Type=Application
Version=1.0
Name=MIFARMA
Comment=Farmacia local para pruebas
Exec=$ABRIR
Icon=$ICONO
Terminal=false
Categories=Office;Finance;
StartupNotify=true
EOF
  chmod +x "$archivo"
  if command -v gio >/dev/null 2>&1; then
    gio set "$archivo" metadata::trusted true 2>/dev/null || true
  fi
  echo "$archivo"
}

DESTINOS=()
if [[ -n "${XDG_DESKTOP_DIR:-}" ]]; then
  DESTINOS+=("$XDG_DESKTOP_DIR")
fi
DESTINOS+=("$HOME/Desktop" "$HOME/Escritorio")
APPS="$HOME/.local/share/applications"
mkdir -p "$APPS"
escribir "$APPS" >/dev/null

hechos=()
for dest in "${DESTINOS[@]}"; do
  [[ -z "$dest" ]] && continue
  mkdir -p "$dest"
  hechos+=("$(escribir "$dest")")
done

printf '%s\n' "${hechos[@]}" | awk 'NF && !seen[$0]++'
