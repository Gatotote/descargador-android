"""Genera los PNG del icono (versiones de Android < 8) a partir del mismo
emblema que la app de escritorio: aro fino + flecha descarga / flecha
conversion + bandeja, en azul sobre fondo oscuro.

    python generar_iconos_android.py
"""

import os

from PIL import Image, ImageDraw

RAIZ = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(RAIZ, "app", "src", "main", "res")

AZUL = (47, 107, 223)
FONDO = (15, 19, 26)

DENSIDADES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}
SUPER = 4  # se dibuja grande y se reduce (antialiasing)


def _emblema(lienzo, color, grosor):
    """Dibuja el emblema centrado ocupando ~72 % del lienzo cuadrado."""
    d = ImageDraw.Draw(lienzo)
    s = lienzo.size[0]
    cx = cy = s / 2
    r = s * 0.34
    g = grosor

    # Aro
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=color, width=g)

    ancho_cab = s * 0.055
    cx_izq, cx_der = cx - s * 0.09, cx + s * 0.09
    y_top, y_bot = cy - s * 0.15, cy + s * 0.12

    # Flecha abajo
    d.line([(cx_izq, y_top), (cx_izq, y_bot)], fill=color, width=g)
    d.line([(cx_izq - ancho_cab, y_bot - ancho_cab), (cx_izq, y_bot)], fill=color, width=g)
    d.line([(cx_izq + ancho_cab, y_bot - ancho_cab), (cx_izq, y_bot)], fill=color, width=g)

    # Flecha arriba
    d.line([(cx_der, y_bot), (cx_der, y_top)], fill=color, width=g)
    d.line([(cx_der - ancho_cab, y_top + ancho_cab), (cx_der, y_top)], fill=color, width=g)
    d.line([(cx_der + ancho_cab, y_top + ancho_cab), (cx_der, y_top)], fill=color, width=g)

    # Bandeja
    by = cy + s * 0.19
    d.line([(cx - s * 0.12, by), (cx + s * 0.12, by)], fill=color, width=g)

    # Redondear extremos (Pillow no hace caps redondos en line)
    rr = g / 2
    for px, py in ((cx_izq, y_top), (cx_izq, y_bot), (cx_der, y_top), (cx_der, y_bot),
                   (cx - s * 0.12, by), (cx + s * 0.12, by)):
        d.ellipse([px - rr, py - rr, px + rr, py + rr], fill=color)


def _icono(tam, redondo):
    s = tam * SUPER
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    if redondo:
        ImageDraw.Draw(img).ellipse([0, 0, s - 1, s - 1], fill=FONDO)
    else:
        ImageDraw.Draw(img).rectangle([0, 0, s - 1, s - 1], fill=FONDO)
    _emblema(img, AZUL, max(2, int(s * 0.028)))
    return img.resize((tam, tam), Image.LANCZOS)


def main():
    for sufijo, tam in DENSIDADES.items():
        carpeta = os.path.join(RES, f"mipmap-{sufijo}")
        os.makedirs(carpeta, exist_ok=True)
        _icono(tam, redondo=False).save(os.path.join(carpeta, "ic_launcher.png"))
        _icono(tam, redondo=True).save(os.path.join(carpeta, "ic_launcher_round.png"))
        print(f"mipmap-{sufijo}: ic_launcher.png + ic_launcher_round.png ({tam}px)")


if __name__ == "__main__":
    main()
