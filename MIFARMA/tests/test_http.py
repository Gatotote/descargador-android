from pathlib import Path

import app as mifarma


def test_paginas_muestran_mifarma(monkeypatch, tmp_path: Path):
    ruta = tmp_path / "http.db"
    monkeypatch.setattr(mifarma, "RUTA_BD", ruta)
    import db as modulo_bd

    monkeypatch.setattr(modulo_bd, "RUTA_BD", ruta)
    mifarma.crear_app()
    cliente = mifarma.app.test_client()
    inicio = cliente.get("/")
    assert inicio.status_code == 200
    assert b"MIFARMA" in inicio.data
    assert b"Mifarma" not in inicio.data
    assert b"Punto de venta" in inicio.data
    salud = cliente.get("/api").get_json()
    assert salud == {"app": "MIFARMA", "status": "ok"}
    pos = cliente.get("/pos")
    assert pos.status_code == 200
    assert b"MIFARMA" in pos.data
    estado = cliente.get("/api/estado").get_json()
    assert estado["marca"] == "MIFARMA"
    assert estado["caja_abierta"] is False

    cliente.post("/caja", data={"accion": "abrir", "monto_inicial": "50"})
    productos = cliente.get("/api/productos?q=Paracetamol").get_json()["productos"]
    cobro = cliente.post(
        "/api/venta",
        json={
            "items": [{"producto_id": productos[0]["id"], "unidad": "unidad", "cantidad": 1}],
            "metodo_pago": "efectivo",
            "recibido": 10,
        },
    ).get_json()
    assert cobro["ok"] is True
    ticket = cliente.get(f"/ventas/{cobro['detalle']['venta']['id']}")
    assert ticket.status_code == 200
    assert b"MIFARMA" in ticket.data
    assert b"FOLIO:" in ticket.data
    assert b"HERA530330MQ4" in ticket.data
    assert b"PUBLICO EN GENERAL" in ticket.data
    assert b"M.N." in ticket.data
    assert b"TypeError" not in ticket.data
    assert b"lote " not in ticket.data
    assert b"trebol" in ticket.data
    ajustes = cliente.get("/ajustes")
    assert ajustes.status_code == 200
    assert "Usar este logo".encode() in ajustes.data
