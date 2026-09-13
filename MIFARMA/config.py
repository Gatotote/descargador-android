from pathlib import Path

NOMBRE_MARCA = "MIFARMA"
RAIZ = Path(__file__).resolve().parent
CARPETA_DATOS = RAIZ / "data"
RUTA_BD = CARPETA_DATOS / "mifarma.db"
CARPETA_RESPALDOS = CARPETA_DATOS / "respaldos"
PUERTO = 5050
HOST = "127.0.0.1"
EXTENSIONES_LOGO = (".png", ".jpg", ".jpeg", ".webp", ".svg", ".gif")
TAMANO_MAX_LOGO = 4 * 1024 * 1024


def ruta_logo() -> Path | None:
    """Logo digital del cliente, si ya lo dejaron en data/."""
    CARPETA_DATOS.mkdir(parents=True, exist_ok=True)
    for ext in EXTENSIONES_LOGO:
        candidato = CARPETA_DATOS / f"logo{ext}"
        if candidato.is_file():
            return candidato
    return None


def borrar_logo() -> None:
    for ext in EXTENSIONES_LOGO:
        candidato = CARPETA_DATOS / f"logo{ext}"
        if candidato.exists():
            candidato.unlink()


def guardar_logo(nombre: str, contenido: bytes) -> Path:
    if len(contenido) > TAMANO_MAX_LOGO:
        raise ValueError("El logo pesa más de 4 MB.")
    ext = Path(nombre or "").suffix.lower()
    if ext not in EXTENSIONES_LOGO:
        raise ValueError("Usa PNG, JPG, WEBP, SVG o GIF.")
    borrar_logo()
    destino = CARPETA_DATOS / f"logo{ext}"
    destino.write_bytes(contenido)
    return destino

# Alertas de caducidad (días).
DIAS_ALERTA_CADUCIDAD = 90
DIAS_CADUCADO = 0
