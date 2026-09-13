from pathlib import Path

NOMBRE_MARCA = "MIFARMA"
RAIZ = Path(__file__).resolve().parent
CARPETA_DATOS = RAIZ / "data"
RUTA_BD = CARPETA_DATOS / "mifarma.db"
CARPETA_RESPALDOS = CARPETA_DATOS / "respaldos"
PUERTO = 5050
HOST = "127.0.0.1"

# Alertas de caducidad (días).
DIAS_ALERTA_CADUCIDAD = 90
DIAS_CADUCADO = 0
