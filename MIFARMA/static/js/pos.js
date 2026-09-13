(() => {
  const raiz = document.querySelector(".pos");
  if (!raiz) return;

  const moneda = raiz.dataset.moneda || "$";
  const cajaAbierta = raiz.dataset.caja === "1";
  const busqueda = document.getElementById("busqueda");
  const resultados = document.getElementById("resultados");
  const lineasEl = document.getElementById("lineas");
  const subtotalEl = document.getElementById("subtotal");
  const totalEl = document.getElementById("total");
  const descuentoEl = document.getElementById("descuento");
  const recibidoEl = document.getElementById("recibido");
  const cambioEl = document.getElementById("cambio");
  const clienteEl = document.getElementById("cliente");

  const carrito = [];
  let temporizador = null;

  const dinero = (n) => `${moneda}${Number(n).toFixed(2)}`;

  const metodo = () => {
    const elegido = document.querySelector("input[name=metodo]:checked");
    return elegido ? elegido.value : "efectivo";
  };

  const subtotal = () => carrito.reduce((acc, item) => acc + item.precio * item.cantidad, 0);

  const total = () => Math.max(0, subtotal() - Number(descuentoEl.value || 0));

  const pintarTotales = () => {
    subtotalEl.textContent = dinero(subtotal());
    totalEl.textContent = dinero(total());
    const rec = Number(recibidoEl.value || 0);
    cambioEl.textContent = dinero(metodo() === "efectivo" && rec ? Math.max(0, rec - total()) : 0);
  };

  const pintarCarrito = () => {
    lineasEl.innerHTML = "";
    carrito.forEach((item, indice) => {
      const li = document.createElement("li");
      li.innerHTML = `
        <strong>${item.nombre}</strong>
        <div class="meta">
          <span>${item.cantidad} ${item.unidad} · ${dinero(item.precio)}</span>
          <span>${dinero(item.precio * item.cantidad)}</span>
        </div>
        <div class="meta">
          <span>stock ${item.stock} pzas${item.requiere_receta ? " · receta" : ""}</span>
          <button type="button" data-quitar="${indice}">Quitar</button>
        </div>`;
      lineasEl.appendChild(li);
    });
    pintarTotales();
  };

  const agregar = (producto, unidad) => {
    const precio = Number(producto[`precio_${unidad}`] || 0);
    if (!precio) {
      window.alert(`${producto.nombre} no tiene precio para ${unidad}.`);
      return;
    }
    const existente = carrito.find((item) => item.producto_id === producto.id && item.unidad === unidad);
    if (existente) {
      existente.cantidad += 1;
    } else {
      carrito.push({
        producto_id: producto.id,
        nombre: producto.nombre,
        unidad,
        cantidad: 1,
        precio,
        stock: producto.stock,
        requiere_receta: producto.requiere_receta,
      });
    }
    pintarCarrito();
    busqueda.value = "";
    busqueda.focus();
    resultados.innerHTML = "";
  };

  const pintarResultados = (productos) => {
    resultados.innerHTML = "";
    productos.forEach((p) => {
      const articulo = document.createElement("article");
      articulo.className = "resultado";
      articulo.tabIndex = 0;
      articulo.innerHTML = `
        <div>
          <strong>${p.nombre}</strong>
          <small>${p.principio_activo || ""} ${p.laboratorio ? "· " + p.laboratorio : ""} · stock ${p.stock}</small>
        </div>
        <div class="unidades">
          <button type="button" data-unidad="unidad">unidad ${dinero(p.precio_unidad)}</button>
          <button type="button" data-unidad="blister">blíster ${dinero(p.precio_blister)}</button>
          <button type="button" data-unidad="caja">caja ${dinero(p.precio_caja)}</button>
        </div>`;
      articulo.addEventListener("click", (ev) => {
        const boton = ev.target.closest("button[data-unidad]");
        agregar(p, boton ? boton.dataset.unidad : "unidad");
      });
      resultados.appendChild(articulo);
    });
  };

  const buscar = async () => {
    const q = busqueda.value.trim();
    if (!q) {
      resultados.innerHTML = "";
      return;
    }
    const resp = await fetch(`/api/productos?q=${encodeURIComponent(q)}`);
    const data = await resp.json();
    pintarResultados(data.productos || []);
  };

  busqueda.addEventListener("input", () => {
    clearTimeout(temporizador);
    temporizador = setTimeout(buscar, 180);
  });

  busqueda.addEventListener("keydown", (ev) => {
    if (ev.key === "Enter") {
      ev.preventDefault();
      const primero = resultados.querySelector(".resultado");
      if (primero) primero.click();
    }
  });

  lineasEl.addEventListener("click", (ev) => {
    const boton = ev.target.closest("[data-quitar]");
    if (!boton) return;
    carrito.splice(Number(boton.dataset.quitar), 1);
    pintarCarrito();
  });

  descuentoEl.addEventListener("input", pintarTotales);
  recibidoEl.addEventListener("input", pintarTotales);
  document.querySelectorAll("input[name=metodo]").forEach((el) => el.addEventListener("change", pintarTotales));

  document.getElementById("btn-vaciar").addEventListener("click", () => {
    carrito.splice(0, carrito.length);
    pintarCarrito();
  });

  const cobrar = async () => {
    if (!cajaAbierta) {
      window.alert("Abre caja antes de vender.");
      return;
    }
    if (!carrito.length) {
      window.alert("El ticket está vacío.");
      return;
    }
    const receta = carrito.find((item) => item.requiere_receta);
    if (receta && !window.confirm(`${receta.nombre} requiere receta. ¿Continuar?`)) return;

    const cuerpo = {
      items: carrito.map((item) => ({
        producto_id: item.producto_id,
        unidad: item.unidad,
        cantidad: item.cantidad,
      })),
      descuento: Number(descuentoEl.value || 0),
      metodo_pago: metodo(),
      recibido: recibidoEl.value === "" ? null : Number(recibidoEl.value),
      cliente_id: clienteEl.value ? Number(clienteEl.value) : null,
    };
    const resp = await fetch("/api/venta", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(cuerpo),
    });
    const data = await resp.json();
    if (!data.ok) {
      window.alert(data.error || "No se pudo cobrar.");
      return;
    }
    window.location = `/ventas/${data.detalle.venta.id}`;
  };

  document.getElementById("btn-cobrar").addEventListener("click", cobrar);
  document.addEventListener("keydown", (ev) => {
    if (ev.key === "F2") {
      ev.preventDefault();
      cobrar();
    }
  });
})();
