#!/usr/bin/env bash
# Arranca MIFARMA si hace falta y abre el navegador.
set -u
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
URL="http://127.0.0.1:5050"

if [[ ! -d .venv ]]; then
  python3 -m venv .venv
  .venv/bin/pip install -q -r requirements.txt
fi

ya_esta=0
if command -v curl >/dev/null 2>&1; then
  if curl -sf -o /dev/null --max-time 1 "$URL/" ; then
    ya_esta=1
  fi
fi

if [[ "$ya_esta" -eq 0 ]]; then
  nohup .venv/bin/python app.py >/tmp/mifarma.log 2>&1 &
  for _ in $(seq 1 40); do
    if curl -sf -o /dev/null --max-time 1 "$URL/" ; then
      ya_esta=1
      break
    fi
    sleep 0.25
  done
fi

if command -v xdg-open >/dev/null 2>&1; then
  xdg-open "$URL" >/dev/null 2>&1 &
elif command -v google-chrome >/dev/null 2>&1; then
  google-chrome --new-window "$URL" >/dev/null 2>&1 &
fi
