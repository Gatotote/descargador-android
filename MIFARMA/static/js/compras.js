(() => {
  const form = document.getElementById("form-compra");
  if (!form) return;
  const lineas = [];
  const lista = document.getElementById("compra-lineas");
  const oculto = document.getElementById("items_json");
  const producto = document.getElementById("compra-producto");

  const pintar = () => {
    lista.innerHTML = "";
    lineas.forEach((item, i) => {
      const nombre = producto.querySelector(`option[value="${item.producto_id}"]`)?.textContent || item.producto_id;
      const li = document.createElement("li");
      li.textContent = `${nombre} · lote ${item.lote} · cad ${item.caducidad} · ${item.cantidad} pzas · ${item.costo_unitario}`;
      const btn = document.createElement("button");
      btn.type = "button";
      btn.textContent = "Quitar";
      btn.addEventListener("click", () => {
        lineas.splice(i, 1);
        pintar();
      });
      li.appendChild(btn);
      lista.appendChild(li);
    });
    oculto.value = JSON.stringify(lineas);
  };

  document.getElementById("compra-agregar").addEventListener("click", () => {
    if (!producto.value) return;
    lineas.push({
      producto_id: Number(producto.value),
      lote: document.getElementById("compra-lote").value || "S/L",
      caducidad: document.getElementById("compra-caducidad").value,
      cantidad: Number(document.getElementById("compra-cantidad").value || 0),
      costo_unitario: Number(document.getElementById("compra-costo").value || 0),
    });
    pintar();
  });

  form.addEventListener("submit", (ev) => {
    if (!lineas.length) {
      ev.preventDefault();
      window.alert("Agrega al menos un renglón.");
    }
  });
})();
