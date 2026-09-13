"""Reglas de negocio de MIFARMA: stock, FEFO, caja y respaldos locales."""

from __future__ import annotations

import json
import sqlite3
from datetime import date, datetime, timedelta
from pathlib import Path

from config import DIAS_ALERTA_CADUCIDAD, NOMBRE_MARCA
from db import dicts, uno
from letras import monto_en_letras

UNIDADES = ("caja", "blister", "unidad")
UNIDAD_TICKET = {
    "unidad": "Pieza",
    "blister": "Blíster",
    "caja": "Caja",
}
METODOS_PAGO = ("efectivo", "tarjeta", "transferencia", "mixto")
TABLAS_RESPALDO = (
    "config",
    "productos",
    "lotes",
    "clientes",
    "turnos_caja",
    "ventas",
    "venta_items",
    "movimientos_caja",
    "compras",
    "compra_items",
)


class ErrorNegocio(Exception):
    pass


def ahora() -> str:
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")


def hoy() -> str:
    return date.today().isoformat()


def leer_config(conexion: sqlite3.Connection) -> dict[str, str]:
    pares = {
        fila["clave"]: fila["valor"]
        for fila in conexion.execute("SELECT clave, valor FROM config")
    }
    pares["nombre_farmacia"] = NOMBRE_MARCA
    return pares


def guardar_config(conexion: sqlite3.Connection, valores: dict[str, str]) -> None:
    for clave, valor in valores.items():
        if clave == "nombre_farmacia":
            valor = NOMBRE_MARCA
        conexion.execute(
            "INSERT INTO config (clave, valor) VALUES (?, ?) "
            "ON CONFLICT(clave) DO UPDATE SET valor = excluded.valor",
            (clave, str(valor).strip()),
        )
    conexion.commit()


def piezas_de(producto: dict, unidad: str, cantidad: int) -> int:
    if cantidad <= 0:
        raise ErrorNegocio("La cantidad debe ser mayor a cero.")
    if unidad not in UNIDADES:
        raise ErrorNegocio("Unidad de venta no válida.")
    por_blister = max(int(producto["unidades_por_blister"] or 1), 1)
    blister_caja = max(int(producto["blister_por_caja"] or 1), 1)
    if unidad == "unidad":
        return cantidad
    if unidad == "blister":
        return cantidad * por_blister
    return cantidad * por_blister * blister_caja


def precio_de(producto: dict, unidad: str) -> float:
    if unidad == "caja":
        return float(producto["precio_caja"] or 0)
    if unidad == "blister":
        return float(producto["precio_blister"] or 0)
    return float(producto["precio_unidad"] or 0)


def stock_producto(conexion: sqlite3.Connection, producto_id: int) -> int:
    fila = conexion.execute(
        "SELECT COALESCE(SUM(cantidad), 0) AS stock FROM lotes WHERE producto_id = ?",
        (producto_id,),
    ).fetchone()
    return int(fila["stock"])


def lotes_fefo(conexion: sqlite3.Connection, producto_id: int) -> list[dict]:
    return dicts(
        conexion.execute(
            "SELECT * FROM lotes WHERE producto_id = ? AND cantidad > 0 "
            "ORDER BY caducidad ASC, id ASC",
            (producto_id,),
        )
    )


def descontar_fefo(
    conexion: sqlite3.Connection, producto_id: int, piezas: int
) -> list[dict]:
    """Descuenta piezas del lote que caduca primero. No permite stock negativo."""
    if piezas <= 0:
        raise ErrorNegocio("No hay piezas que descontar.")
    restantes = piezas
    consumos: list[dict] = []
    for lote in lotes_fefo(conexion, producto_id):
        if restantes <= 0:
            break
        tomar = min(restantes, int(lote["cantidad"]))
        conexion.execute(
            "UPDATE lotes SET cantidad = cantidad - ? WHERE id = ?",
            (tomar, lote["id"]),
        )
        restantes -= tomar
        consumos.append(
            {
                "lote_id": lote["id"],
                "numero": lote["numero"],
                "caducidad": lote["caducidad"],
                "piezas": tomar,
            }
        )
    if restantes > 0:
        raise ErrorNegocio("Stock insuficiente para completar la venta.")
    return consumos


def turno_abierto(conexion: sqlite3.Connection) -> dict | None:
    return uno(
        conexion.execute(
            "SELECT * FROM turnos_caja WHERE estado = 'abierto' ORDER BY id DESC LIMIT 1"
        ).fetchone()
    )


def abrir_caja(conexion: sqlite3.Connection, monto_inicial: float, notas: str = "") -> dict:
    if turno_abierto(conexion):
        raise ErrorNegocio("Ya hay un turno de caja abierto.")
    if monto_inicial < 0:
        raise ErrorNegocio("El fondo inicial no puede ser negativo.")
    cursor = conexion.execute(
        "INSERT INTO turnos_caja (abierto_en, monto_inicial, notas, estado) "
        "VALUES (?, ?, ?, 'abierto')",
        (ahora(), float(monto_inicial), notas.strip()),
    )
    if monto_inicial:
        conexion.execute(
            "INSERT INTO movimientos_caja (turno_id, fecha, tipo, monto, concepto) "
            "VALUES (?, ?, 'fondo', ?, 'Fondo de apertura')",
            (cursor.lastrowid, ahora(), float(monto_inicial)),
        )
    conexion.commit()
    return dict(conexion.execute("SELECT * FROM turnos_caja WHERE id = ?", (cursor.lastrowid,)).fetchone())


def resumen_turno(conexion: sqlite3.Connection, turno_id: int) -> dict:
    turno = uno(conexion.execute("SELECT * FROM turnos_caja WHERE id = ?", (turno_id,)).fetchone())
    if not turno:
        raise ErrorNegocio("Turno no encontrado.")
    ventas = dicts(
        conexion.execute("SELECT * FROM ventas WHERE turno_id = ?", (turno_id,))
    )
    movimientos = dicts(
        conexion.execute(
            "SELECT * FROM movimientos_caja WHERE turno_id = ? ORDER BY id",
            (turno_id,),
        )
    )
    total_ventas = sum(float(v["total"]) for v in ventas)
    efectivo = sum(float(v["total"]) for v in ventas if v["metodo_pago"] == "efectivo")
    otros = total_ventas - efectivo
    egresos = sum(float(m["monto"]) for m in movimientos if m["tipo"] == "egreso")
    ingresos = sum(float(m["monto"]) for m in movimientos if m["tipo"] == "ingreso")
    esperado = float(turno["monto_inicial"]) + efectivo + ingresos - egresos
    return {
        "turno": turno,
        "ventas": ventas,
        "movimientos": movimientos,
        "total_ventas": total_ventas,
        "efectivo": efectivo,
        "otros": otros,
        "egresos": egresos,
        "ingresos": ingresos,
        "esperado": esperado,
    }


def cerrar_caja(conexion: sqlite3.Connection, monto_declarado: float, notas: str = "") -> dict:
    turno = turno_abierto(conexion)
    if not turno:
        raise ErrorNegocio("No hay un turno de caja abierto.")
    resumen = resumen_turno(conexion, turno["id"])
    conexion.execute(
        "UPDATE turnos_caja SET cerrado_en = ?, monto_declarado = ?, notas = ?, estado = 'cerrado' "
        "WHERE id = ?",
        (ahora(), float(monto_declarado), notas.strip(), turno["id"]),
    )
    conexion.commit()
    resumen = resumen_turno(conexion, turno["id"])
    resumen["diferencia"] = float(monto_declarado) - float(resumen["esperado"])
    return resumen


def registrar_movimiento_caja(
    conexion: sqlite3.Connection, tipo: str, monto: float, concepto: str
) -> None:
    turno = turno_abierto(conexion)
    if not turno:
        raise ErrorNegocio("Abre caja antes de registrar un movimiento.")
    if tipo not in ("ingreso", "egreso"):
        raise ErrorNegocio("Tipo de movimiento no válido.")
    if monto <= 0:
        raise ErrorNegocio("El monto debe ser mayor a cero.")
    conexion.execute(
        "INSERT INTO movimientos_caja (turno_id, fecha, tipo, monto, concepto) "
        "VALUES (?, ?, ?, ?, ?)",
        (turno["id"], ahora(), tipo, float(monto), concepto.strip()),
    )
    conexion.commit()


def siguiente_folio(conexion: sqlite3.Connection) -> str:
    cfg = leer_config(conexion)
    inicial = int(cfg.get("folio_inicial") or 1)
    fila = conexion.execute(
        "SELECT folio FROM ventas ORDER BY id DESC LIMIT 1"
    ).fetchone()
    if not fila:
        return str(inicial)
    digitos = "".join(c for c in str(fila["folio"]) if c.isdigit())
    ultimo = int(digitos) if digitos else 0
    return str(max(ultimo + 1, inicial))


def fecha_ticket(valor: str) -> str:
    texto = str(valor or "")
    try:
        dt = datetime.strptime(texto[:19], "%Y-%m-%d %H:%M:%S")
    except ValueError:
        return texto
    return dt.strftime("%d/%m/%Y %I:%M:%S %p")


def lineas_para_ticket(items: list[dict]) -> list[dict]:
    visibles = []
    for item in items:
        cantidad = int(item.get("cantidad") or 0)
        if cantidad <= 0:
            continue
        visibles.append(
            {
                "descripcion": str(item.get("descripcion") or "").upper(),
                "cantidad": cantidad,
                "unidad": UNIDAD_TICKET.get(item.get("unidad"), item.get("unidad") or "Pieza"),
                "precio": float(item.get("precio_unitario") or 0),
                "total": float(item.get("total") or 0),
            }
        )
    return visibles


def registrar_venta(conexion: sqlite3.Connection, payload: dict) -> dict:
    turno = turno_abierto(conexion)
    if not turno:
        raise ErrorNegocio("Abre caja antes de vender.")
    lineas = payload.get("items") or []
    if not lineas:
        raise ErrorNegocio("La venta no tiene productos.")
    descuento = float(payload.get("descuento") or 0)
    if descuento < 0:
        raise ErrorNegocio("El descuento no puede ser negativo.")
    metodo = payload.get("metodo_pago") or "efectivo"
    if metodo not in METODOS_PAGO:
        raise ErrorNegocio("Método de pago no válido.")

    subtotal = 0.0
    preparadas: list[dict] = []
    for linea in lineas:
        producto = uno(
            conexion.execute(
                "SELECT * FROM productos WHERE id = ? AND activo = 1",
                (linea.get("producto_id"),),
            ).fetchone()
        )
        if not producto:
            raise ErrorNegocio("Hay un producto que ya no está activo.")
        unidad = linea.get("unidad") or "unidad"
        cantidad = int(linea.get("cantidad") or 0)
        piezas = piezas_de(producto, unidad, cantidad)
        precio = precio_de(producto, unidad)
        if precio <= 0:
            raise ErrorNegocio(f"{producto['nombre']} no tiene precio para {unidad}.")
        total_linea = round(precio * cantidad, 2)
        subtotal += total_linea
        preparadas.append(
            {
                "producto": producto,
                "unidad": unidad,
                "cantidad": cantidad,
                "piezas": piezas,
                "precio": precio,
                "total": total_linea,
            }
        )

    subtotal = round(subtotal, 2)
    if descuento > subtotal:
        raise ErrorNegocio("El descuento no puede ser mayor al subtotal.")
    total = round(subtotal - descuento, 2)
    recibido = payload.get("recibido")
    recibido_f = float(recibido) if recibido not in (None, "") else None
    cambio = None
    if metodo == "efectivo":
        if recibido_f is None:
            recibido_f = total
        if recibido_f < total:
            raise ErrorNegocio("El monto recibido es menor al total.")
        cambio = round(recibido_f - total, 2)

    folio = siguiente_folio(conexion)
    cursor = conexion.execute(
        "INSERT INTO ventas (folio, turno_id, cliente_id, fecha, subtotal, descuento, total, "
        "metodo_pago, recibido, cambio, notas) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        (
            folio,
            turno["id"],
            payload.get("cliente_id") or None,
            ahora(),
            subtotal,
            descuento,
            total,
            metodo,
            recibido_f,
            cambio,
            (payload.get("notas") or "").strip(),
        ),
    )
    venta_id = cursor.lastrowid

    for linea in preparadas:
        consumos = descontar_fefo(conexion, linea["producto"]["id"], linea["piezas"])
        for consumo in consumos:
            conexion.execute(
                "INSERT INTO venta_items (venta_id, producto_id, lote_id, descripcion, unidad, "
                "cantidad, piezas, precio_unitario, total) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                (
                    venta_id,
                    linea["producto"]["id"],
                    consumo["lote_id"],
                    linea["producto"]["nombre"],
                    linea["unidad"],
                    linea["cantidad"] if consumo is consumos[0] else 0,
                    consumo["piezas"],
                    linea["precio"],
                    linea["total"] if consumo is consumos[0] else 0,
                ),
            )

    conexion.execute(
        "INSERT INTO movimientos_caja (turno_id, fecha, tipo, monto, concepto, venta_id) "
        "VALUES (?, ?, 'venta', ?, ?, ?)",
        (turno["id"], ahora(), total, f"Venta {folio}", venta_id),
    )
    conexion.commit()
    return venta_completa(conexion, venta_id)


def venta_completa(conexion: sqlite3.Connection, venta_id: int) -> dict:
    venta = uno(conexion.execute("SELECT * FROM ventas WHERE id = ?", (venta_id,)).fetchone())
    if not venta:
        raise ErrorNegocio("Venta no encontrada.")
    items = dicts(
        conexion.execute(
            "SELECT vi.*, l.numero AS lote_numero, l.caducidad AS lote_caducidad "
            "FROM venta_items vi LEFT JOIN lotes l ON l.id = vi.lote_id "
            "WHERE vi.venta_id = ?",
            (venta_id,),
        )
    )
    cliente = None
    if venta["cliente_id"]:
        cliente = uno(
            conexion.execute("SELECT * FROM clientes WHERE id = ?", (venta["cliente_id"],)).fetchone()
        )
    cfg = leer_config(conexion)
    lineas_ticket = lineas_para_ticket(items)
    return {
        "venta": venta,
        "lineas": items,
        "lineas_ticket": lineas_ticket,
        "cliente": cliente,
        "cliente_nombre": (cliente["nombre"] if cliente else cfg.get("cliente_mostrador") or "PUBLICO EN GENERAL"),
        "marca": NOMBRE_MARCA,
        "total_letras": monto_en_letras(float(venta["total"])),
        "arts_vendidos": float(sum(int(i["cantidad"] or 0) for i in lineas_ticket)),
        "fecha_ticket": fecha_ticket(venta["fecha"]),
        "entregado": float(venta["recibido"] if venta["recibido"] is not None else venta["total"]),
        "cambio": float(venta["cambio"] or 0),
    }


def buscar_productos(conexion: sqlite3.Connection, q: str, solo_activos: bool = True) -> list[dict]:
    q = (q or "").strip()
    where = ["1=1"]
    params: list = []
    if solo_activos:
        where.append("p.activo = 1")
    if q:
        like = f"%{q}%"
        where.append(
            "(p.nombre LIKE ? OR p.principio_activo LIKE ? OR p.codigo_barras LIKE ? "
            "OR p.laboratorio LIKE ?)"
        )
        params.extend([like, like, like, like])
        # Coincidencia exacta de código de barras primero.
        if q.isdigit() or len(q) >= 6:
            exacto = dicts(
                conexion.execute(
                    f"SELECT p.*, COALESCE(SUM(l.cantidad), 0) AS stock "
                    f"FROM productos p LEFT JOIN lotes l ON l.producto_id = p.id "
                    f"WHERE {' AND '.join(where)} AND p.codigo_barras = ? "
                    f"GROUP BY p.id",
                    [*params, q],
                )
            )
            if exacto:
                return exacto
    sql = (
        "SELECT p.*, COALESCE(SUM(l.cantidad), 0) AS stock "
        "FROM productos p LEFT JOIN lotes l ON l.producto_id = p.id "
        f"WHERE {' AND '.join(where)} GROUP BY p.id "
        "ORDER BY p.nombre COLLATE NOCASE LIMIT 40"
    )
    return dicts(conexion.execute(sql, params))


def producto_con_stock(conexion: sqlite3.Connection, producto_id: int) -> dict | None:
    producto = uno(
        conexion.execute(
            "SELECT p.*, COALESCE(SUM(l.cantidad), 0) AS stock "
            "FROM productos p LEFT JOIN lotes l ON l.producto_id = p.id "
            "WHERE p.id = ? GROUP BY p.id",
            (producto_id,),
        ).fetchone()
    )
    if not producto:
        return None
    producto["lotes"] = dicts(
        conexion.execute(
            "SELECT * FROM lotes WHERE producto_id = ? ORDER BY caducidad, id",
            (producto_id,),
        )
    )
    return producto


def guardar_producto(conexion: sqlite3.Connection, datos: dict, producto_id: int | None = None) -> int:
    nombre = (datos.get("nombre") or "").strip()
    if not nombre:
        raise ErrorNegocio("El nombre del producto es obligatorio.")
    codigo = (datos.get("codigo_barras") or "").strip() or None
    campos = {
        "codigo_barras": codigo,
        "nombre": nombre,
        "principio_activo": (datos.get("principio_activo") or "").strip(),
        "laboratorio": (datos.get("laboratorio") or "").strip(),
        "categoria": (datos.get("categoria") or "").strip(),
        "unidades_por_blister": int(datos.get("unidades_por_blister") or 1),
        "blister_por_caja": int(datos.get("blister_por_caja") or 1),
        "precio_caja": float(datos.get("precio_caja") or 0),
        "precio_blister": float(datos.get("precio_blister") or 0),
        "precio_unidad": float(datos.get("precio_unidad") or 0),
        "costo": float(datos.get("costo") or 0),
        "stock_minimo": int(datos.get("stock_minimo") or 0),
        "requiere_receta": 1 if datos.get("requiere_receta") else 0,
        "activo": 1 if datos.get("activo", True) else 0,
    }
    columnas = ", ".join(campos)
    placeholders = ", ".join("?" for _ in campos)
    valores = list(campos.values())
    try:
        if producto_id:
            asignaciones = ", ".join(f"{c} = ?" for c in campos)
            conexion.execute(
                f"UPDATE productos SET {asignaciones} WHERE id = ?",
                [*valores, producto_id],
            )
            identificador = producto_id
        else:
            cursor = conexion.execute(
                f"INSERT INTO productos ({columnas}) VALUES ({placeholders})",
                valores,
            )
            identificador = cursor.lastrowid
        conexion.commit()
        return identificador
    except sqlite3.IntegrityError as exc:
        raise ErrorNegocio("Ese código de barras ya está registrado.") from exc


def registrar_lote(
    conexion: sqlite3.Connection,
    producto_id: int,
    numero: str,
    caducidad: str,
    cantidad: int,
    costo_unitario: float = 0,
) -> int:
    numero = (numero or "").strip()
    if not numero:
        raise ErrorNegocio("El número de lote es obligatorio.")
    if not caducidad:
        raise ErrorNegocio("La fecha de caducidad es obligatoria.")
    if cantidad <= 0:
        raise ErrorNegocio("La cantidad del lote debe ser mayor a cero.")
    producto = uno(
        conexion.execute("SELECT id FROM productos WHERE id = ?", (producto_id,)).fetchone()
    )
    if not producto:
        raise ErrorNegocio("Producto no encontrado.")
    existente = uno(
        conexion.execute(
            "SELECT id FROM lotes WHERE producto_id = ? AND numero = ? AND caducidad = ?",
            (producto_id, numero, caducidad),
        ).fetchone()
    )
    if existente:
        conexion.execute(
            "UPDATE lotes SET cantidad = cantidad + ?, costo_unitario = ? WHERE id = ?",
            (cantidad, float(costo_unitario), existente["id"]),
        )
        lote_id = existente["id"]
    else:
        cursor = conexion.execute(
            "INSERT INTO lotes (producto_id, numero, caducidad, cantidad, costo_unitario) "
            "VALUES (?, ?, ?, ?, ?)",
            (producto_id, numero, caducidad, cantidad, float(costo_unitario)),
        )
        lote_id = cursor.lastrowid
    conexion.commit()
    return lote_id


def registrar_compra(conexion: sqlite3.Connection, datos: dict) -> int:
    items = datos.get("items") or []
    if not items:
        raise ErrorNegocio("La compra no tiene productos.")
    total = 0.0
    cursor = conexion.execute(
        "INSERT INTO compras (fecha, proveedor, folio, total, notas) VALUES (?, ?, ?, 0, ?)",
        (
            datos.get("fecha") or hoy(),
            (datos.get("proveedor") or "").strip(),
            (datos.get("folio") or "").strip(),
            (datos.get("notas") or "").strip(),
        ),
    )
    compra_id = cursor.lastrowid
    for item in items:
        producto_id = int(item["producto_id"])
        cantidad = int(item["cantidad"])
        costo = float(item.get("costo_unitario") or 0)
        lote_id = registrar_lote(
            conexion,
            producto_id,
            item.get("lote") or "S/L",
            item.get("caducidad") or hoy(),
            cantidad,
            costo,
        )
        linea_total = round(costo * cantidad, 2)
        total += linea_total
        conexion.execute(
            "INSERT INTO compra_items (compra_id, producto_id, lote_id, cantidad, costo_unitario, total) "
            "VALUES (?, ?, ?, ?, ?, ?)",
            (compra_id, producto_id, lote_id, cantidad, costo, linea_total),
        )
    conexion.execute("UPDATE compras SET total = ? WHERE id = ?", (round(total, 2), compra_id))
    conexion.commit()
    return compra_id


def guardar_cliente(conexion: sqlite3.Connection, datos: dict, cliente_id: int | None = None) -> int:
    nombre = (datos.get("nombre") or "").strip()
    if not nombre:
        raise ErrorNegocio("El nombre del cliente es obligatorio.")
    valores = (
        nombre,
        (datos.get("telefono") or "").strip(),
        (datos.get("documento") or "").strip(),
        (datos.get("notas") or "").strip(),
    )
    if cliente_id:
        conexion.execute(
            "UPDATE clientes SET nombre = ?, telefono = ?, documento = ?, notas = ? WHERE id = ?",
            (*valores, cliente_id),
        )
        identificador = cliente_id
    else:
        cursor = conexion.execute(
            "INSERT INTO clientes (nombre, telefono, documento, notas) VALUES (?, ?, ?, ?)",
            valores,
        )
        identificador = cursor.lastrowid
    conexion.commit()
    return identificador


def alertas(conexion: sqlite3.Connection, dias: int | None = None) -> dict:
    limite = int(dias if dias is not None else DIAS_ALERTA_CADUCIDAD)
    hoy_iso = hoy()
    umbral = (date.today() + timedelta(days=limite)).isoformat()
    caducados = dicts(
        conexion.execute(
            "SELECT l.*, p.nombre FROM lotes l JOIN productos p ON p.id = l.producto_id "
            "WHERE l.cantidad > 0 AND l.caducidad < ? ORDER BY l.caducidad",
            (hoy_iso,),
        )
    )
    por_caducar = dicts(
        conexion.execute(
            "SELECT l.*, p.nombre FROM lotes l JOIN productos p ON p.id = l.producto_id "
            "WHERE l.cantidad > 0 AND l.caducidad >= ? AND l.caducidad <= ? "
            "ORDER BY l.caducidad",
            (hoy_iso, umbral),
        )
    )
    stock_bajo = dicts(
        conexion.execute(
            "SELECT p.*, COALESCE(SUM(l.cantidad), 0) AS stock "
            "FROM productos p LEFT JOIN lotes l ON l.producto_id = p.id "
            "WHERE p.activo = 1 GROUP BY p.id "
            "HAVING stock <= p.stock_minimo ORDER BY stock, p.nombre"
        )
    )
    return {
        "caducados": caducados,
        "por_caducar": por_caducar,
        "stock_bajo": stock_bajo,
    }


def resumen_dashboard(conexion: sqlite3.Connection) -> dict:
    avisos = alertas(conexion)
    hoy_iso = hoy()
    ventas_hoy = uno(
        conexion.execute(
            "SELECT COUNT(*) AS n, COALESCE(SUM(total), 0) AS total "
            "FROM ventas WHERE fecha LIKE ?",
            (f"{hoy_iso}%",),
        ).fetchone()
    ) or {"n": 0, "total": 0}
    productos = conexion.execute("SELECT COUNT(*) AS n FROM productos WHERE activo = 1").fetchone()["n"]
    top = dicts(
        conexion.execute(
            "SELECT p.nombre, SUM(vi.piezas) AS piezas, SUM(vi.total) AS total "
            "FROM venta_items vi JOIN productos p ON p.id = vi.producto_id "
            "JOIN ventas v ON v.id = vi.venta_id "
            "WHERE v.fecha LIKE ? "
            "GROUP BY p.id ORDER BY piezas DESC LIMIT 5",
            (f"{hoy_iso}%",),
        )
    )
    return {
        "alertas": avisos,
        "ventas_hoy": ventas_hoy,
        "productos": productos,
        "top": top,
        "turno": turno_abierto(conexion),
        "marca": NOMBRE_MARCA,
    }


def ventas_rango(conexion: sqlite3.Connection, desde: str, hasta: str) -> list[dict]:
    return dicts(
        conexion.execute(
            "SELECT * FROM ventas WHERE fecha >= ? AND fecha <= ? ORDER BY fecha DESC",
            (f"{desde} 00:00:00", f"{hasta} 23:59:59"),
        )
    )


def exportar_respaldo(conexion: sqlite3.Connection) -> dict:
    dump = {
        "marca": NOMBRE_MARCA,
        "generado_en": ahora(),
        "tablas": {},
    }
    for tabla in TABLAS_RESPALDO:
        dump["tablas"][tabla] = dicts(conexion.execute(f"SELECT * FROM {tabla}"))
    return dump


def restaurar_respaldo(conexion: sqlite3.Connection, dump: dict) -> None:
    if not isinstance(dump, dict) or "tablas" not in dump:
        raise ErrorNegocio("El archivo de respaldo no es válido.")
    tablas = dump["tablas"]
    conexion.execute("PRAGMA foreign_keys = OFF")
    try:
        for tabla in reversed(TABLAS_RESPALDO):
            conexion.execute(f"DELETE FROM {tabla}")
        for tabla in TABLAS_RESPALDO:
            filas = tablas.get(tabla) or []
            for fila in filas:
                columnas = ", ".join(fila.keys())
                placeholders = ", ".join("?" for _ in fila)
                conexion.execute(
                    f"INSERT INTO {tabla} ({columnas}) VALUES ({placeholders})",
                    list(fila.values()),
                )
        conexion.execute(
            "UPDATE config SET valor = ? WHERE clave = 'nombre_farmacia'",
            (NOMBRE_MARCA,),
        )
        conexion.commit()
    finally:
        conexion.execute("PRAGMA foreign_keys = ON")


def guardar_respaldo_archivo(dump: dict, destino: Path) -> None:
    destino.parent.mkdir(parents=True, exist_ok=True)
    destino.write_text(json.dumps(dump, ensure_ascii=False, indent=2), encoding="utf-8")
