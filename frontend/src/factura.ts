// Pantalla 2: alta / edicion / detalle de UNA factura.
// factura.html        -> nueva
// factura.html?id=5   -> editar (o solo ver, si ya esta informada)

import {
  actualizarFactura, buscarArticulos, buscarClientes, Cliente, crearFactura, Factura,
  FacturaRequest, informarFactura, listasDePrecio, obtenerFactura, PrecioArticulo,
} from "./api.js";
import { abrirExplorador } from "./explorador.js";
import { $, condicionIva, dinero, documento, fecha, html, nroComprobante, tipoComprobante } from "./formato.js";

/** Un renglon mientras lo editamos en pantalla. */
interface Renglon {
  articuloId: number;
  codigo: string;
  descripcion: string;
  alicuotaIva: number;
  precio: number;
  cantidad: number;
}

// ---------- Estado de la pantalla ----------
let facturaId: number | null = null;
let cliente: Cliente | null = null;
let renglones: Renglon[] = [];
let bloqueada = false;

// ---------- Elementos del HTML ----------
const titulo = $<HTMLHeadingElement>("titulo");
const form = $<HTMLFormElement>("form-factura");
const selTipo = $<HTMLSelectElement>("tipo");
const inpFecha = $<HTMLInputElement>("fecha");
const txtCliente = $<HTMLInputElement>("cliente-texto");
const detalleCliente = $<HTMLParagraphElement>("cliente-detalle");
const btnBuscarCliente = $<HTMLButtonElement>("btn-buscar-cliente");
const selLista = $<HTMLSelectElement>("lista");
const btnAgregar = $<HTMLButtonElement>("btn-agregar");
const cuerpoItems = $<HTMLTableSectionElement>("items-cuerpo");
const lblTotal = $<HTMLElement>("total");
const btnGuardar = $<HTMLButtonElement>("btn-guardar");
const btnInformar = $<HTMLButtonElement>("btn-informar");
const panelArca = $<HTMLDivElement>("panel-arca");
const mensaje = $<HTMLDivElement>("mensaje");

// ---------- Arranque ----------

async function iniciar() {
  // 1) Llenar el combo de listas de precio
  const listas = await listasDePrecio();
  selLista.innerHTML = listas.map((l) => `<option value="${l.id}">${html(l.nombre)}</option>`).join("");

  // 2) Si viene ?id= en la URL, cargamos esa factura
  const id = new URLSearchParams(location.search).get("id");
  if (id) {
    mostrarFactura(await obtenerFactura(Number(id)));
  } else {
    inpFecha.value = hoy();
    dibujarRenglones();
  }
}

function hoy(): string {
  const d = new Date();
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset()); // fecha local, no UTC
  return d.toISOString().substring(0, 10);
}

/** Pasa una factura del back a la pantalla. */
function mostrarFactura(f: Factura) {
  facturaId = f.id;
  bloqueada = f.informadaArca;
  selTipo.value = String(f.tipoComprobante);
  inpFecha.value = f.fecha;
  selLista.value = String(f.listaPrecioId);
  elegirCliente(f.cliente);
  renglones = f.items.map((i) => ({
    articuloId: i.articuloId, codigo: i.codigo, descripcion: i.descripcion,
    alicuotaIva: i.alicuotaIva, precio: i.precioUnitario, cantidad: i.cantidad,
  }));

  titulo.textContent = `${tipoComprobante(f.tipoComprobante)} ${nroComprobante(f.puntoVenta, f.numero)}`;
  btnInformar.hidden = bloqueada;
  mostrarPanelArca(f);
  aplicarBloqueo();
  dibujarRenglones();
}

/** LA regla del TP en el front: si esta informada, todo queda en solo lectura. */
function aplicarBloqueo() {
  form.querySelectorAll<HTMLInputElement | HTMLSelectElement | HTMLButtonElement>("input, select, button")
    .forEach((el) => (el.disabled = bloqueada));
  btnGuardar.hidden = bloqueada;
}

function mostrarPanelArca(f: Factura) {
  if (f.informadaArca) {
    panelArca.className = "panel-arca ok";
    panelArca.innerHTML = `
      <strong>Informada a ARCA — no se puede modificar</strong>
      <dl>
        <dt>CAE</dt><dd class="mono">${html(f.cae)}</dd>
        <dt>Vence</dt><dd>${html(fecha(f.caeVencimiento))}</dd>
        <dt>Neto</dt><dd>${html(dinero(f.importeNeto))}</dd>
        <dt>IVA</dt><dd>${html(dinero(f.importeIva))}</dd>
      </dl>
      ${f.observacionesArca ? `<p class="obs">${html(f.observacionesArca)}</p>` : ""}`;
  } else if (f.observacionesArca) {
    panelArca.className = "panel-arca error";
    panelArca.innerHTML = `<strong>ARCA rechazó el último intento:</strong><p class="obs">${html(f.observacionesArca)}</p>`;
  } else {
    panelArca.className = "panel-arca";
    panelArca.innerHTML = "";
  }
  panelArca.hidden = panelArca.innerHTML === "";
}

// ---------- Cliente (explorer) ----------

function elegirCliente(c: Cliente) {
  cliente = c;
  txtCliente.value = c.razonSocial;
  detalleCliente.textContent = `${documento(c.docTipo, c.docNro)} · ${condicionIva(c.condicionIva)}`;
}

btnBuscarCliente.addEventListener("click", async () => {
  const elegido = await abrirExplorador<Cliente>({
    titulo: "Buscar cliente",
    buscar: buscarClientes,
    columnas: [
      { titulo: "Razón social", valor: (c) => c.razonSocial },
      { titulo: "Documento", valor: (c) => documento(c.docTipo, c.docNro) },
      { titulo: "Condición IVA", valor: (c) => condicionIva(c.condicionIva) },
    ],
  });
  if (elegido) elegirCliente(elegido);
});

// ---------- Articulos de la lista (explorer) ----------

btnAgregar.addEventListener("click", async () => {
  const listaId = Number(selLista.value);
  const elegido = await abrirExplorador<PrecioArticulo>({
    titulo: `Buscar artículo — lista ${selLista.selectedOptions[0]?.text ?? ""}`,
    buscar: (texto, pagina) => buscarArticulos(listaId, texto, pagina),
    columnas: [
      { titulo: "Código", valor: (a) => a.codigo },
      { titulo: "Descripción", valor: (a) => a.descripcion },
      { titulo: "IVA", valor: (a) => a.alicuotaIva + "%", derecha: true },
      { titulo: "Precio", valor: (a) => dinero(a.precio), derecha: true },
    ],
  });
  if (!elegido) return;

  // Si ya estaba, le sumamos 1 en vez de duplicar el renglon
  const existente = renglones.find((r) => r.articuloId === elegido.articuloId);
  if (existente) {
    existente.cantidad += 1;
  } else {
    renglones.push({
      articuloId: elegido.articuloId, codigo: elegido.codigo, descripcion: elegido.descripcion,
      alicuotaIva: elegido.alicuotaIva, precio: elegido.precio, cantidad: 1,
    });
  }
  dibujarRenglones();
});

// Cambiar de lista cambia los precios: vaciamos los renglones
let listaAnterior = "";
selLista.addEventListener("focus", () => (listaAnterior = selLista.value));
selLista.addEventListener("change", () => {
  if (renglones.length && !confirm("Al cambiar la lista se quitan los artículos cargados. ¿Seguir?")) {
    selLista.value = listaAnterior;
    return;
  }
  renglones = [];
  dibujarRenglones();
});

// ---------- Renglones ----------

function dibujarRenglones() {
  cuerpoItems.innerHTML = renglones.length
    ? renglones.map((r, i) => `
      <tr>
        <td class="mono">${html(r.codigo)}</td>
        <td>${html(r.descripcion)}</td>
        <td><input type="number" min="0.001" step="any" value="${r.cantidad}" data-cantidad="${i}" ${bloqueada ? "disabled" : ""}></td>
        <td class="der">${html(dinero(r.precio))}</td>
        <td class="der">${r.alicuotaIva}%</td>
        <td class="der">${html(dinero(r.precio * r.cantidad))}</td>
        <td>${bloqueada ? "" : `<button type="button" class="btn-icono" data-quitar="${i}" aria-label="Quitar">✕</button>`}</td>
      </tr>`).join("")
    : `<tr><td colspan="7" class="vacio">Todavía no agregaste artículos</td></tr>`;
  const total = renglones.reduce((suma, r) => suma + r.precio * r.cantidad, 0);
  lblTotal.textContent = dinero(total);
}

cuerpoItems.addEventListener("change", (e) => {
  const input = e.target as HTMLInputElement;
  if (input.dataset.cantidad === undefined) return;
  renglones[Number(input.dataset.cantidad)].cantidad = Number(input.value);
  dibujarRenglones();
});

cuerpoItems.addEventListener("click", (e) => {
  const boton = (e.target as HTMLElement).closest<HTMLButtonElement>("[data-quitar]");
  if (!boton) return;
  renglones.splice(Number(boton.dataset.quitar), 1);
  dibujarRenglones();
});

// ---------- Guardar ----------

form.addEventListener("submit", async (e) => {
  e.preventDefault();
  if (!cliente) return mostrarMensaje("Elegí un cliente con el botón Buscar.", "error");
  if (!renglones.length) return mostrarMensaje("Agregá al menos un artículo.", "error");
  if (renglones.some((r) => !(r.cantidad > 0))) return mostrarMensaje("Todas las cantidades tienen que ser mayores a 0.", "error");

  const datos: FacturaRequest = {
    clienteId: cliente.id,
    listaPrecioId: Number(selLista.value),
    tipoComprobante: Number(selTipo.value),
    fecha: inpFecha.value,
    items: renglones.map((r) => ({ articuloId: r.articuloId, cantidad: r.cantidad })),
  };

  btnGuardar.disabled = true;
  try {
    const guardada = facturaId === null ? await crearFactura(datos) : await actualizarFactura(facturaId, datos);
    history.replaceState(null, "", `factura.html?id=${guardada.id}`); // la URL ahora apunta a la factura guardada
    mostrarFactura(guardada);
    mostrarMensaje(`Factura guardada. Total ${dinero(guardada.importeTotal)}.`, "ok");
  } catch (err) {
    mostrarMensaje((err as Error).message, "error");
  } finally {
    btnGuardar.disabled = bloqueada;
  }
});

// ---------- Informar a ARCA ----------

btnInformar.addEventListener("click", async () => {
  if (facturaId === null) return mostrarMensaje("Primero guardá la factura.", "error");
  if (!confirm("¿Informar a ARCA? Se usan los datos GUARDADOS y después no se puede modificar.")) return;

  btnInformar.disabled = true;
  try {
    const f = await informarFactura(facturaId);
    mostrarFactura(f);
    if (f.informadaArca) {
      mostrarMensaje(`¡Aprobada! CAE ${f.cae}`, "ok");
    } else {
      mostrarMensaje("ARCA rechazó la factura. Mirá las observaciones.", "error");
    }
  } catch (err) {
    mostrarMensaje((err as Error).message, "error");
  } finally {
    btnInformar.disabled = bloqueada;
  }
});

function mostrarMensaje(texto: string, tipo: "ok" | "error") {
  mensaje.className = "mensaje " + tipo;
  mensaje.textContent = texto;
  mensaje.hidden = false;
  mensaje.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

iniciar().catch((e) => mostrarMensaje((e as Error).message, "error"));
