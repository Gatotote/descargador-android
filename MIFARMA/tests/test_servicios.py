from __future__ import annotations

import sqlite3
from datetime import date, timedelta
from pathlib import Path

import pytest

import db
import seed
import servicios


@pytest.fixture
def conexion(tmp_path: Path):
    ruta = tmp_path / "prueba.db"
    conexion = db.preparar_bd(ruta)
    yield conexion
    conexion.close()


def _paracetamol(conexion: sqlite3.Connection) -> int:
    return servicios.guardar_producto(
        conexion,
        {
            "nombre": "Paracetamol 500 mg",
            "codigo_barras": "111",
            "unidades_por_blister": 10,
            "blister_por_caja": 2,
            "precio_caja": 40,
            "precio_blister": 22,
            "precio_unidad": 2.5,
            "stock_minimo": 5,
        },
    )


def test_nombre_siempre_mifarma(conexion):
    servicios.guardar_config(conexion, {"nombre_farmacia": "mifarma"})
    assert servicios.leer_config(conexion)["nombre_farmacia"] == "MIFARMA"


def test_piezas_por_unidad(conexion):
    producto_id = _paracetamol(conexion)
    producto = servicios.producto_con_stock(conexion, producto_id)
    assert servicios.piezas_de(producto, "unidad", 3) == 3
    assert servicios.piezas_de(producto, "blister", 1) == 10
    assert servicios.piezas_de(producto, "caja", 1) == 20


def test_fefo_descuenta_el_lote_mas_proximo(conexion):
    producto_id = _paracetamol(conexion)
    hoy = date.today()
    servicios.registrar_lote(conexion, producto_id, "LEJANO", (hoy + timedelta(days=400)).isoformat(), 30)
    servicios.registrar_lote(conexion, producto_id, "PROXIMO", (hoy + timedelta(days=20)).isoformat(), 8)
    consumos = servicios.descontar_fefo(conexion, producto_id, 10)
    assert consumos[0]["numero"] == "PROXIMO"
    assert consumos[0]["piezas"] == 8
    assert consumos[1]["numero"] == "LEJANO"
    assert consumos[1]["piezas"] == 2
    assert servicios.stock_producto(conexion, producto_id) == 28


def test_no_permite_stock_negativo(conexion):
    producto_id = _paracetamol(conexion)
    servicios.registrar_lote(conexion, producto_id, "A", date.today().isoformat(), 2)
    with pytest.raises(servicios.ErrorNegocio):
        servicios.descontar_fefo(conexion, producto_id, 3)


def test_venta_requiere_caja_y_descuenta_stock(conexion):
    producto_id = _paracetamol(conexion)
    servicios.registrar_lote(conexion, producto_id, "A", (date.today() + timedelta(days=90)).isoformat(), 50)
    with pytest.raises(servicios.ErrorNegocio):
        servicios.registrar_venta(
            conexion,
            {"items": [{"producto_id": producto_id, "unidad": "unidad", "cantidad": 1}]},
        )
    servicios.abrir_caja(conexion, 100)
    detalle = servicios.registrar_venta(
        conexion,
        {
            "items": [{"producto_id": producto_id, "unidad": "blister", "cantidad": 1}],
            "metodo_pago": "efectivo",
            "recibido": 50,
        },
    )
    assert detalle["venta"]["folio"].startswith("MIF-")
    assert detalle["venta"]["total"] == 22
    assert servicios.stock_producto(conexion, producto_id) == 40


def test_respaldo_redondea_marca(conexion):
    producto_id = _paracetamol(conexion)
    dump = servicios.exportar_respaldo(conexion)
    assert dump["marca"] == "MIFARMA"
    conexion.execute("DELETE FROM productos")
    conexion.commit()
    servicios.restaurar_respaldo(conexion, dump)
    assert servicios.producto_con_stock(conexion, producto_id)["nombre"] == "Paracetamol 500 mg"
    assert servicios.leer_config(conexion)["nombre_farmacia"] == "MIFARMA"


def test_semilla_solo_si_vacia(conexion):
    seed.sembrar_si_vacia(conexion)
    n = conexion.execute("SELECT COUNT(*) AS n FROM productos").fetchone()["n"]
    seed.sembrar_si_vacia(conexion)
    n2 = conexion.execute("SELECT COUNT(*) AS n FROM productos").fetchone()["n"]
    assert n == n2
    assert n >= 6
