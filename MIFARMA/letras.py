"""Importes en letras, como en el ticket de MIFARMA (pesos M.N.)."""

from __future__ import annotations

_UNIDADES = (
    "CERO",
    "UN",
    "DOS",
    "TRES",
    "CUATRO",
    "CINCO",
    "SEIS",
    "SIETE",
    "OCHO",
    "NUEVE",
    "DIEZ",
    "ONCE",
    "DOCE",
    "TRECE",
    "CATORCE",
    "QUINCE",
    "DIECISEIS",
    "DIECISIETE",
    "DIECIOCHO",
    "DIECINUEVE",
    "VEINTE",
)
_DECENAS = (
    "",
    "",
    "VEINTE",
    "TREINTA",
    "CUARENTA",
    "CINCUENTA",
    "SESENTA",
    "SETENTA",
    "OCHENTA",
    "NOVENTA",
)
_CENTENAS = (
    "",
    "CIENTO",
    "DOSCIENTOS",
    "TRESCIENTOS",
    "CUATROCIENTOS",
    "QUINIENTOS",
    "SEISCIENTOS",
    "SETECIENTOS",
    "OCHOCIENTOS",
    "NOVECIENTOS",
)


def _0_99(n: int) -> str:
    if n <= 20:
        return _UNIDADES[n]
    if n < 30:
        return "VEINTI" + _UNIDADES[n - 20]
    decenas, unidades = divmod(n, 10)
    if unidades == 0:
        return _DECENAS[decenas]
    return f"{_DECENAS[decenas]} Y {_UNIDADES[unidades]}"


def _0_999(n: int) -> str:
    if n == 100:
        return "CIEN"
    centenas, resto = divmod(n, 100)
    if centenas == 0:
        return _0_99(resto)
    if resto == 0:
        return _CENTENAS[centenas]
    return f"{_CENTENAS[centenas]} {_0_99(resto)}"


def entero_en_letras(n: int) -> str:
    if n < 0:
        raise ValueError("El importe no puede ser negativo.")
    if n < 1000:
        return _0_999(n)
    if n < 1_000_000:
        miles, resto = divmod(n, 1000)
        cabeza = "MIL" if miles == 1 else f"{_0_999(miles)} MIL"
        if resto == 0:
            return cabeza
        return f"{cabeza} {_0_999(resto)}"
    millones, resto = divmod(n, 1_000_000)
    cabeza = "UN MILLON" if millones == 1 else f"{entero_en_letras(millones)} MILLONES"
    if resto == 0:
        return cabeza
    return f"{cabeza} {entero_en_letras(resto)}"


def monto_en_letras(monto: float) -> str:
    """==(DOSCIENTOS SETENTA Y UN PESOS 00/100 M.N.)=="""
    total = round(float(monto), 2)
    pesos = int(total)
    centavos = int(round((total - pesos) * 100))
    if centavos == 100:
        pesos += 1
        centavos = 0
    if pesos == 1:
        cuerpo = "UN PESO"
    else:
        cuerpo = f"{entero_en_letras(pesos)} PESOS"
    return f"==({cuerpo} {centavos:02d}/100 M.N.)=="
