"""Servidor local de MIFARMA. No expone la base fuera de esta computadora."""

from __future__ import annotations

import json
from datetime import date, datetime
from functools import wraps

from flask import (
    Flask,
    flash,
    jsonify,
    redirect,
    render_template,
    request,
    send_file,
    url_for,
)

from config import CARPETA_RESPALDOS, HOST, NOMBRE_MARCA, PUERTO, RUTA_BD
from db import conectar, dicts, preparar_bd
from seed import sembrar_si_vacia
from servicios import (
    ErrorNegocio,
    alertas,
    abrir_caja,
    buscar_productos,
    cerrar_caja,
    exportar_respaldo,
    guardar_cliente,
    guardar_config,
    guardar_producto,
    guardar_respaldo_archivo,
    leer_config,
    producto_con_stock,
    registrar_compra,
    registrar_lote,
    registrar_movimiento_caja,
    registrar_venta,
    restaurar_respaldo,
    resumen_dashboard,
    resumen_turno,
    turno_abierto,
    venta_completa,
    ventas_rango,
)

app = Flask(__name__)
app.secret_key = "mifarma-local-no-es-un-secreto-de-red"
app.config["TEMPLATES_AUTO_RELOAD"] = True


def bd():
    return conectar()


def pagina(vista):
    @wraps(vista)
    def envuelta(*args, **kwargs):
        conexion = bd()
        try:
            return vista(conexion, *args, **kwargs)
        except ErrorNegocio as exc:
            if request.path.startswith("/api/"):
                return jsonify({"ok": False, "error": str(exc)}), 400
            flash(str(exc), "error")
            return redirect(request.referrer or url_for("inicio"))
        finally:
            conexion.close()

    return envuelta


@app.context_processor
def comunes():
    conexion = bd()
    try:
        cfg = leer_config(conexion)
        avisos = alertas(conexion)
        return {
            "MARCA": NOMBRE_MARCA,
            "config": cfg,
            "turno": turno_abierto(conexion),
            "n_caducados": len(avisos["caducados"]),
            "n_por_caducar": len(avisos["por_caducar"]),
            "n_stock_bajo": len(avisos["stock_bajo"]),
            "hoy": date.today().isoformat(),
        }
    finally:
        conexion.close()


@app.template_filter("dinero")
def filtro_dinero(valor):
    try:
        return f"{float(valor):,.2f}"
    except (TypeError, ValueError):
        return "0.00"


@app.template_filter("fecha_corta")
def filtro_fecha(valor):
    if not valor:
        return "—"
    texto = str(valor)
    try:
        if " " in texto:
            dt = datetime.strptime(texto[:19], "%Y-%m-%d %H:%M:%S")
            return dt.strftime("%d/%m/%Y %H:%M")
        dt = datetime.strptime(texto[:10], "%Y-%m-%d")
        return dt.strftime("%d/%m/%Y")
    except ValueError:
        return texto


@app.route("/")
@pagina
def inicio(conexion):
    return render_template("inicio.html", resumen=resumen_dashboard(conexion))


@app.route("/pos")
@pagina
def pos(conexion):
    clientes = dicts(conexion.execute("SELECT * FROM clientes ORDER BY nombre COLLATE NOCASE"))
    return render_template("pos.html", clientes=clientes)


@app.route("/inventario")
@pagina
def inventario(conexion):
    q = request.args.get("q", "").strip()
    productos = buscar_productos(conexion, q, solo_activos=False)
    return render_template("inventario.html", productos=productos, q=q)


@app.route("/inventario/nuevo", methods=["GET", "POST"])
@pagina
def inventario_nuevo(conexion):
    if request.method == "POST":
        datos = _formulario_producto()
        producto_id = guardar_producto(conexion, datos)
        if request.form.get("lote_numero") and request.form.get("lote_cantidad"):
            registrar_lote(
                conexion,
                producto_id,
                request.form.get("lote_numero"),
                request.form.get("lote_caducidad") or date.today().isoformat(),
                int(request.form.get("lote_cantidad") or 0),
                float(request.form.get("lote_costo") or datos.get("costo") or 0),
            )
        flash("Producto guardado.", "ok")
        return redirect(url_for("inventario_editar", producto_id=producto_id))
    return render_template("producto_form.html", producto=None)


@app.route("/inventario/<int:producto_id>", methods=["GET", "POST"])
@pagina
def inventario_editar(conexion, producto_id):
    producto = producto_con_stock(conexion, producto_id)
    if not producto:
        flash("Producto no encontrado.", "error")
        return redirect(url_for("inventario"))
    if request.method == "POST":
        guardar_producto(conexion, _formulario_producto(), producto_id)
        flash("Producto actualizado.", "ok")
        return redirect(url_for("inventario_editar", producto_id=producto_id))
    return render_template("producto_form.html", producto=producto)


@app.route("/inventario/<int:producto_id>/lote", methods=["POST"])
@pagina
def inventario_lote(conexion, producto_id):
    registrar_lote(
        conexion,
        producto_id,
        request.form.get("numero"),
        request.form.get("caducidad"),
        int(request.form.get("cantidad") or 0),
        float(request.form.get("costo_unitario") or 0),
    )
    flash("Lote registrado.", "ok")
    return redirect(url_for("inventario_editar", producto_id=producto_id))


@app.route("/caducidades")
@pagina
def caducidades(conexion):
    return render_template("caducidades.html", avisos=alertas(conexion))


@app.route("/clientes")
@pagina
def clientes(conexion):
    lista = dicts(conexion.execute("SELECT * FROM clientes ORDER BY nombre COLLATE NOCASE"))
    return render_template("clientes.html", clientes=lista)


@app.route("/clientes/nuevo", methods=["POST"])
@pagina
def clientes_nuevo(conexion):
    guardar_cliente(conexion, request.form)
    flash("Cliente guardado.", "ok")
    return redirect(url_for("clientes"))


@app.route("/clientes/<int:cliente_id>", methods=["POST"])
@pagina
def clientes_editar(conexion, cliente_id):
    guardar_cliente(conexion, request.form, cliente_id)
    flash("Cliente actualizado.", "ok")
    return redirect(url_for("clientes"))


@app.route("/compras", methods=["GET", "POST"])
@pagina
def compras(conexion):
    if request.method == "POST":
        raw = request.form.get("items_json") or "[]"
        items = json.loads(raw)
        registrar_compra(
            conexion,
            {
                "fecha": request.form.get("fecha") or date.today().isoformat(),
                "proveedor": request.form.get("proveedor"),
                "folio": request.form.get("folio"),
                "notas": request.form.get("notas"),
                "items": items,
            },
        )
        flash("Compra registrada y stock actualizado.", "ok")
        return redirect(url_for("compras"))
    historial = dicts(
        conexion.execute("SELECT * FROM compras ORDER BY fecha DESC, id DESC LIMIT 30")
    )
    productos = buscar_productos(conexion, "", solo_activos=True)
    return render_template("compras.html", compras=historial, productos=productos)


@app.route("/caja", methods=["GET", "POST"])
@pagina
def caja(conexion):
    turno = turno_abierto(conexion)
    if request.method == "POST":
        accion = request.form.get("accion")
        if accion == "abrir":
            abrir_caja(
                conexion,
                float(request.form.get("monto_inicial") or 0),
                request.form.get("notas") or "",
            )
            flash("Caja abierta.", "ok")
        elif accion == "cerrar":
            cerrar_caja(
                conexion,
                float(request.form.get("monto_declarado") or 0),
                request.form.get("notas") or "",
            )
            flash("Turno cerrado.", "ok")
        elif accion == "movimiento":
            registrar_movimiento_caja(
                conexion,
                request.form.get("tipo"),
                float(request.form.get("monto") or 0),
                request.form.get("concepto") or "",
            )
            flash("Movimiento registrado.", "ok")
        return redirect(url_for("caja"))
    resumen = resumen_turno(conexion, turno["id"]) if turno else None
    historial = dicts(
        conexion.execute("SELECT * FROM turnos_caja ORDER BY id DESC LIMIT 10")
    )
    return render_template("caja.html", resumen=resumen, historial=historial)


@app.route("/reportes")
@pagina
def reportes(conexion):
    desde = request.args.get("desde") or date.today().isoformat()
    hasta = request.args.get("hasta") or date.today().isoformat()
    ventas = ventas_rango(conexion, desde, hasta)
    total = sum(float(v["total"]) for v in ventas)
    por_metodo = {}
    for v in ventas:
        por_metodo[v["metodo_pago"]] = por_metodo.get(v["metodo_pago"], 0) + float(v["total"])
    top = dicts(
        conexion.execute(
            "SELECT p.nombre, SUM(vi.piezas) AS piezas, SUM(vi.total) AS total "
            "FROM venta_items vi JOIN productos p ON p.id = vi.producto_id "
            "JOIN ventas v ON v.id = vi.venta_id "
            "WHERE v.fecha >= ? AND v.fecha <= ? "
            "GROUP BY p.id ORDER BY total DESC LIMIT 10",
            (f"{desde} 00:00:00", f"{hasta} 23:59:59"),
        )
    )
    return render_template(
        "reportes.html",
        desde=desde,
        hasta=hasta,
        ventas=ventas,
        total=total,
        por_metodo=por_metodo,
        top=top,
    )


@app.route("/ventas/<int:venta_id>")
@pagina
def ticket(conexion, venta_id):
    return render_template("ticket.html", detalle=venta_completa(conexion, venta_id))


@app.route("/ajustes", methods=["GET", "POST"])
@pagina
def ajustes(conexion):
    if request.method == "POST":
        guardar_config(
            conexion,
            {
                "nombre_farmacia": NOMBRE_MARCA,
                "moneda": request.form.get("moneda") or "$",
                "dias_alerta_caducidad": request.form.get("dias_alerta_caducidad") or "90",
                "ticket_pie": request.form.get("ticket_pie") or "Gracias por su compra · MIFARMA",
                "direccion": request.form.get("direccion") or "",
                "telefono": request.form.get("telefono") or "",
            },
        )
        flash("Ajustes guardados. El nombre comercial sigue siendo MIFARMA.", "ok")
        return redirect(url_for("ajustes"))
    return render_template("ajustes.html", ruta_bd=str(RUTA_BD))


@app.route("/ajustes/respaldo")
@pagina
def ajustes_respaldo(conexion):
    dump = exportar_respaldo(conexion)
    nombre = f"MIFARMA-respaldo-{datetime.now().strftime('%Y%m%d-%H%M%S')}.json"
    destino = CARPETA_RESPALDOS / nombre
    guardar_respaldo_archivo(dump, destino)
    return send_file(destino, as_attachment=True, download_name=nombre)


@app.route("/ajustes/restaurar", methods=["POST"])
@pagina
def ajustes_restaurar(conexion):
    archivo = request.files.get("archivo")
    if not archivo or not archivo.filename:
        raise ErrorNegocio("Selecciona un archivo JSON de respaldo.")
    dump = json.loads(archivo.read().decode("utf-8"))
    restaurar_respaldo(conexion, dump)
    flash("Respaldo restaurado. El nombre comercial permanece MIFARMA.", "ok")
    return redirect(url_for("ajustes"))


@app.route("/api/productos")
@pagina
def api_productos(conexion):
    q = request.args.get("q", "")
    lista = []
    for p in buscar_productos(conexion, q, solo_activos=True):
        lista.append(
            {
                **{k: p[k] for k in p.keys()},
                "precio_caja": float(p["precio_caja"]),
                "precio_blister": float(p["precio_blister"]),
                "precio_unidad": float(p["precio_unidad"]),
                "stock": int(p["stock"]),
            }
        )
    return jsonify({"ok": True, "productos": lista})


@app.route("/api/venta", methods=["POST"])
@pagina
def api_venta(conexion):
    payload = request.get_json(force=True, silent=True) or {}
    detalle = registrar_venta(conexion, payload)
    return jsonify({"ok": True, "detalle": _json_seguro(detalle)})


@app.route("/api/estado")
@pagina
def api_estado(conexion):
    t = turno_abierto(conexion)
    return jsonify({"ok": True, "marca": NOMBRE_MARCA, "caja_abierta": bool(t), "turno": dict(t) if t else None})


def _formulario_producto() -> dict:
    return {
        "codigo_barras": request.form.get("codigo_barras"),
        "nombre": request.form.get("nombre"),
        "principio_activo": request.form.get("principio_activo"),
        "laboratorio": request.form.get("laboratorio"),
        "categoria": request.form.get("categoria"),
        "unidades_por_blister": request.form.get("unidades_por_blister") or 1,
        "blister_por_caja": request.form.get("blister_por_caja") or 1,
        "precio_caja": request.form.get("precio_caja") or 0,
        "precio_blister": request.form.get("precio_blister") or 0,
        "precio_unidad": request.form.get("precio_unidad") or 0,
        "costo": request.form.get("costo") or 0,
        "stock_minimo": request.form.get("stock_minimo") or 0,
        "requiere_receta": bool(request.form.get("requiere_receta")),
        "activo": True if request.form.get("activo_presente") is None else bool(request.form.get("activo")),
    }


def _json_seguro(obj):
    if isinstance(obj, dict):
        return {k: _json_seguro(v) for k, v in obj.items()}
    if isinstance(obj, list):
        return [_json_seguro(v) for v in obj]
    if hasattr(obj, "keys"):
        return {k: _json_seguro(obj[k]) for k in obj.keys()}
    return obj


def crear_app() -> Flask:
    preparar_bd()
    conexion = conectar()
    try:
        sembrar_si_vacia(conexion)
    finally:
        conexion.close()
    return app


if __name__ == "__main__":
    crear_app()
    app.run(host=HOST, port=PUERTO, debug=True)
