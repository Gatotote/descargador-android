"""Carga inicial de demostración. Solo corre si la base está vacía."""

from __future__ import annotations

import sqlite3
from datetime import date, timedelta

from servicios import registrar_lote, guardar_producto


def sembrar_si_vacia(conexion: sqlite3.Connection) -> None:
    n = conexion.execute("SELECT COUNT(*) AS n FROM productos").fetchone()["n"]
    if n:
        return
    muestras = [
        {
            "codigo_barras": "7501000000001",
            "nombre": "Paracetamol 500 mg 20 tabletas",
            "principio_activo": "Paracetamol",
            "laboratorio": "Genérico",
            "categoria": "Analgésicos",
            "unidades_por_blister": 10,
            "blister_por_caja": 2,
            "precio_caja": 38.00,
            "precio_blister": 22.00,
            "precio_unidad": 2.50,
            "costo": 18.00,
            "stock_minimo": 10,
            "requiere_receta": False,
        },
        {
            "codigo_barras": "7501000000002",
            "nombre": "Ibuprofeno 400 mg 10 tabletas",
            "principio_activo": "Ibuprofeno",
            "laboratorio": "Genérico",
            "categoria": "Analgésicos",
            "unidades_por_blister": 10,
            "blister_por_caja": 1,
            "precio_caja": 42.00,
            "precio_blister": 42.00,
            "precio_unidad": 5.00,
            "costo": 20.00,
            "stock_minimo": 8,
            "requiere_receta": False,
        },
        {
            "codigo_barras": "7501000000003",
            "nombre": "Loratadina 10 mg 10 tabletas",
            "principio_activo": "Loratadina",
            "laboratorio": "Genérico",
            "categoria": "Antihistamínicos",
            "unidades_por_blister": 10,
            "blister_por_caja": 1,
            "precio_caja": 35.00,
            "precio_blister": 35.00,
            "precio_unidad": 4.00,
            "costo": 14.00,
            "stock_minimo": 6,
            "requiere_receta": False,
        },
        {
            "codigo_barras": "7501000000004",
            "nombre": "Omeprazol 20 mg 14 cápsulas",
            "principio_activo": "Omeprazol",
            "laboratorio": "Genérico",
            "categoria": "Digestivo",
            "unidades_por_blister": 7,
            "blister_por_caja": 2,
            "precio_caja": 55.00,
            "precio_blister": 30.00,
            "precio_unidad": 5.00,
            "costo": 24.00,
            "stock_minimo": 6,
            "requiere_receta": False,
        },
        {
            "codigo_barras": "7501000000005",
            "nombre": "Alcohol etílico 70% 250 ml",
            "principio_activo": "Etanol",
            "laboratorio": "MIFARMA",
            "categoria": "Cuidado",
            "unidades_por_blister": 1,
            "blister_por_caja": 1,
            "precio_caja": 28.00,
            "precio_blister": 28.00,
            "precio_unidad": 28.00,
            "costo": 12.00,
            "stock_minimo": 12,
            "requiere_receta": False,
        },
        {
            "codigo_barras": "7501000000006",
            "nombre": "Amoxicilina 500 mg 12 cápsulas",
            "principio_activo": "Amoxicilina",
            "laboratorio": "Genérico",
            "categoria": "Antibióticos",
            "unidades_por_blister": 6,
            "blister_por_caja": 2,
            "precio_caja": 68.00,
            "precio_blister": 38.00,
            "precio_unidad": 7.00,
            "costo": 30.00,
            "stock_minimo": 4,
            "requiere_receta": True,
        },
    ]
    hoy = date.today()
    for datos in muestras:
        producto_id = guardar_producto(conexion, datos)
        conexion.execute(
            "UPDATE productos SET es_demostracion = 1 WHERE id = ?",
            (producto_id,),
        )
        registrar_lote(
            conexion,
            producto_id,
            numero=f"DEM-{producto_id}A",
            caducidad=(hoy + timedelta(days=400)).isoformat(),
            cantidad=40,
            costo_unitario=float(datos["costo"]),
        )
        registrar_lote(
            conexion,
            producto_id,
            numero=f"DEM-{producto_id}B",
            caducidad=(hoy + timedelta(days=25)).isoformat(),
            cantidad=8,
            costo_unitario=float(datos["costo"]),
        )
    conexion.execute(
        "INSERT INTO clientes (nombre, telefono, documento, notas) VALUES (?, ?, ?, ?)",
        ("PUBLICO EN GENERAL", "", "", "Cliente de mostrador"),
    )
    conexion.commit()
