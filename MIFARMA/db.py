"""Conexión SQLite y esquema de MIFARMA. Todo queda en un archivo local."""

from __future__ import annotations

import sqlite3
from pathlib import Path

from config import CARPETA_DATOS, NOMBRE_MARCA, RUTA_BD

ESQUEMA = """
PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS config (
    clave TEXT PRIMARY KEY,
    valor TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS productos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo_barras TEXT UNIQUE,
    nombre TEXT NOT NULL,
    principio_activo TEXT DEFAULT '',
    laboratorio TEXT DEFAULT '',
    categoria TEXT DEFAULT '',
    unidades_por_blister INTEGER NOT NULL DEFAULT 10,
    blister_por_caja INTEGER NOT NULL DEFAULT 1,
    precio_caja REAL NOT NULL DEFAULT 0,
    precio_blister REAL NOT NULL DEFAULT 0,
    precio_unidad REAL NOT NULL DEFAULT 0,
    costo REAL NOT NULL DEFAULT 0,
    stock_minimo INTEGER NOT NULL DEFAULT 5,
    requiere_receta INTEGER NOT NULL DEFAULT 0,
    activo INTEGER NOT NULL DEFAULT 1,
    es_demostracion INTEGER NOT NULL DEFAULT 0,
    creado_en TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE TABLE IF NOT EXISTS lotes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    producto_id INTEGER NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    numero TEXT NOT NULL,
    caducidad TEXT NOT NULL,
    cantidad INTEGER NOT NULL DEFAULT 0,
    costo_unitario REAL NOT NULL DEFAULT 0,
    creado_en TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE TABLE IF NOT EXISTS clientes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    telefono TEXT DEFAULT '',
    documento TEXT DEFAULT '',
    notas TEXT DEFAULT '',
    creado_en TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE TABLE IF NOT EXISTS turnos_caja (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    abierto_en TEXT NOT NULL,
    cerrado_en TEXT,
    monto_inicial REAL NOT NULL DEFAULT 0,
    monto_declarado REAL,
    notas TEXT DEFAULT '',
    estado TEXT NOT NULL DEFAULT 'abierto'
);

CREATE TABLE IF NOT EXISTS ventas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    folio TEXT NOT NULL UNIQUE,
    turno_id INTEGER REFERENCES turnos_caja(id),
    cliente_id INTEGER REFERENCES clientes(id),
    fecha TEXT NOT NULL,
    subtotal REAL NOT NULL,
    descuento REAL NOT NULL DEFAULT 0,
    total REAL NOT NULL,
    metodo_pago TEXT NOT NULL,
    recibido REAL,
    cambio REAL,
    notas TEXT DEFAULT ''
);

CREATE TABLE IF NOT EXISTS venta_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    venta_id INTEGER NOT NULL REFERENCES ventas(id) ON DELETE CASCADE,
    producto_id INTEGER NOT NULL REFERENCES productos(id),
    lote_id INTEGER REFERENCES lotes(id),
    descripcion TEXT NOT NULL,
    unidad TEXT NOT NULL,
    cantidad INTEGER NOT NULL,
    piezas INTEGER NOT NULL,
    precio_unitario REAL NOT NULL,
    total REAL NOT NULL
);

CREATE TABLE IF NOT EXISTS movimientos_caja (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    turno_id INTEGER NOT NULL REFERENCES turnos_caja(id),
    fecha TEXT NOT NULL,
    tipo TEXT NOT NULL,
    monto REAL NOT NULL,
    concepto TEXT DEFAULT '',
    venta_id INTEGER REFERENCES ventas(id)
);

CREATE TABLE IF NOT EXISTS compras (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    fecha TEXT NOT NULL,
    proveedor TEXT DEFAULT '',
    folio TEXT DEFAULT '',
    total REAL NOT NULL DEFAULT 0,
    notas TEXT DEFAULT ''
);

CREATE TABLE IF NOT EXISTS compra_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    compra_id INTEGER NOT NULL REFERENCES compras(id) ON DELETE CASCADE,
    producto_id INTEGER NOT NULL REFERENCES productos(id),
    lote_id INTEGER REFERENCES lotes(id),
    cantidad INTEGER NOT NULL,
    costo_unitario REAL NOT NULL,
    total REAL NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_lotes_producto_cad ON lotes(producto_id, caducidad);
CREATE INDEX IF NOT EXISTS idx_productos_nombre ON productos(nombre);
CREATE INDEX IF NOT EXISTS idx_ventas_fecha ON ventas(fecha);
"""


def conectar(ruta: Path | None = None) -> sqlite3.Connection:
    destino = Path(ruta) if ruta else RUTA_BD
    destino.parent.mkdir(parents=True, exist_ok=True)
    conexion = sqlite3.connect(destino)
    conexion.row_factory = sqlite3.Row
    conexion.execute("PRAGMA foreign_keys = ON")
    conexion.execute("PRAGMA journal_mode = WAL")
    return conexion


def dicts(filas) -> list[dict]:
    return [dict(fila) for fila in filas]


def uno(fila) -> dict | None:
    return dict(fila) if fila else None


def iniciar_esquema(conexion: sqlite3.Connection) -> None:
    conexion.executescript(ESQUEMA)
    _sembrar_config(conexion)
    conexion.commit()


def _sembrar_config(conexion: sqlite3.Connection) -> None:
    actuales = {
        fila["clave"]: fila["valor"]
        for fila in conexion.execute("SELECT clave, valor FROM config")
    }
    por_defecto = {
        "nombre_farmacia": NOMBRE_MARCA,
        "moneda": "$",
        "dias_alerta_caducidad": "90",
        "ticket_pie": "Gracias por su compra · MIFARMA",
        "direccion": "",
        "telefono": "",
    }
    for clave, valor in por_defecto.items():
        if clave not in actuales:
            conexion.execute(
                "INSERT INTO config (clave, valor) VALUES (?, ?)",
                (clave, valor),
            )
    # La marca no se diluye aunque alguien la haya escrito distinto.
    conexion.execute(
        "UPDATE config SET valor = ? WHERE clave = 'nombre_farmacia'",
        (NOMBRE_MARCA,),
    )


def preparar_bd(ruta: Path | None = None) -> sqlite3.Connection:
    CARPETA_DATOS.mkdir(parents=True, exist_ok=True)
    conexion = conectar(ruta)
    iniciar_esquema(conexion)
    return conexion
