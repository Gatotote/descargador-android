# MIFARMA

Sistema local de farmacia: punto de venta, inventario con lotes y
caducidades (FEFO), caja, clientes, compras y reportes.

Los datos **no salen de la computadora**. Viven en `data/mifarma.db`.

## Requisitos

Python 3.10 o superior.

## Arranque

```bash
cd MIFARMA
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/python app.py
```

Abre [http://127.0.0.1:5050](http://127.0.0.1:5050).

La primera vez se crea la base y se cargan unos productos de demostración
para poder probar la caja. Se pueden borrar desde Inventario.

## Pruebas

```bash
.venv/bin/pip install -r requirements.txt
.venv/bin/python -m pytest -q
```

## Respaldo

En **Ajustes** se descarga un JSON con todo el catálogo, lotes, ventas y
caja. El mismo archivo se puede restaurar. Copia también `data/mifarma.db`
si quieres un respaldo binario.

## Destino en el equipo de trabajo

Este árbol debe copiarse a `/home/gatotote/Clientes/MIFARMA` y no
mezclarse con otros repos.
