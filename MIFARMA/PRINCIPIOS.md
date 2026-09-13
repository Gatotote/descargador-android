# Principios de MIFARMA

Este proyecto es de un **cliente** y vive aparte de cualquier otra app
(descargador, GPS, administrativo, etc.). El código de trabajo en la
máquina de desarrollo debe quedar en `/home/gatotote/Clientes/MIFARMA`.

## Marca

- El nombre comercial es **MIFARMA**, siempre en mayúsculas.
- No se acepta `Mifarma`, `MiFarma` ni `mi farma` en la interfaz ni en
  tickets. La configuración fuerza mayúsculas al guardar.
- No reutilizar paletas, paquetes ni módulos de otros proyectos.

## Datos: solo local

- De inicio no hay nube, API remota ni cuenta de usuario en internet.
- Todo se guarda en `data/mifarma.db` (SQLite) en esta carpeta.
- Respaldo y restauración son archivos JSON que salen y entran a la
  misma máquina.
- Si más adelante el cliente pide sincronizar sucursales, se diseña
  encima de este núcleo. No se anticipa un backend.

## Cómo trabajamos juntos

- Cambios pequeños y revisables. No reescribir pantallas que el otro
  acaba de tocar sin motivo.
- Lógica de negocio (stock, FEFO, caja) en `servicios.py`, no en las
  plantillas.
- Nombres de dominio en español: producto, lote, venta, caducidad, caja.
- Una venta nunca deja stock negativo. Si no hay piezas, se rechaza.
- Al vender se descuenta el lote que caduca primero (FEFO).
- El ticket impreso sigue el formato del negocio (RFC, régimen 601,
  total en letras, folio numérico, Facebook MIFARMA CDMX). No lleva
  lote ni caducidad en el papel.

## Git

- En el día a día del cliente, commits locales. No mezclar este árbol
  con repos de proyectos personales.
