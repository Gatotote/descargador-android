from pathlib import Path

import config
from config import borrar_logo, guardar_logo, ruta_logo

PNG_1PX = (
    b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01"
    b"\x08\x06\x00\x00\x00\x1f\x15\xc4\x89\x00\x00\x00\nIDATx\x9cc\x00\x01"
    b"\x00\x00\x05\x00\x01\r\n-\xb4\x00\x00\x00\x00IEND\xaeB`\x82"
)


def test_logo_se_puede_agregar_despues(tmp_path: Path, monkeypatch):
    datos = tmp_path / "data"
    datos.mkdir()
    monkeypatch.setattr(config, "CARPETA_DATOS", datos)
    assert ruta_logo() is None
    destino = guardar_logo("mifarma.png", PNG_1PX)
    assert destino.name == "logo.png"
    assert ruta_logo() == destino
    guardar_logo("marca.svg", b"<svg xmlns='http://www.w3.org/2000/svg'></svg>")
    assert ruta_logo().suffix == ".svg"
    assert not (datos / "logo.png").exists()
    borrar_logo()
    assert ruta_logo() is None
