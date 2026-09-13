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
    pos = cliente.get("/pos")
    assert pos.status_code == 200
    assert b"MIFARMA" in pos.data
    estado = cliente.get("/api/estado").get_json()
    assert estado["marca"] == "MIFARMA"
    assert estado["caja_abierta"] is False
