#!/usr/bin/env bash
# Arranca MIFARMA si hace falta y abre el navegador (interfaz, no JSON).
set -u
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
URL="http://127.0.0.1:5000"

es_nuestra_interfaz() {
  local cuerpo
  cuerpo="$(curl -sf --max-time 1 "$URL/" 2>/dev/null || true)"
  echo "$cuerpo" | grep -q "Punto de venta"
}

if [[ ! -d .venv ]]; then
  python3 -m venv .venv
  .venv/bin/pip install -q -r requirements.txt
fi

if es_nuestra_interfaz; then
  :
else
  if curl -sf --max-time 1 -o /dev/null "$URL/" 2>/dev/null; then
    echo "El puerto 5000 lo está usando otro programa (solo JSON)." >&2
    echo "Ciérralo y vuelve a abrir MIFARMA desde esta carpeta." >&2
    echo "La interfaz gráfica es http://127.0.0.1:5000/ (no /api)." >&2
  fi
  nohup .venv/bin/python app.py >/tmp/mifarma.log 2>&1 &
  for _ in $(seq 1 40); do
    if es_nuestra_interfaz; then
      break
    fi
    sleep 0.25
  done
fi

if command -v xdg-open >/dev/null 2>&1; then
  xdg-open "$URL/" >/dev/null 2>&1 &
elif command -v google-chrome >/dev/null 2>&1; then
  google-chrome --new-window "$URL/" >/dev/null 2>&1 &
fi
