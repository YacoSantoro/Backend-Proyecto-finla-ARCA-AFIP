// Pantalla 1: grilla de facturas con filtros y paginacion.

import { buscarFacturas, eliminarFactura, estadoArca, FacturaResumen, FiltroFactura, informarFactura } from "./api.js";
import { $, dinero, fecha, html, nroComprobante, tipoComprobante } from "./formato.js";

const TAMANIO_PAGINA = 10;
let paginaActual = 0;
let filtroActual: FiltroFactura = {};

const formFiltros = $<HTMLFormElement>("filtros");
const cuerpoGrilla = $<HTMLTableSectionElement>("grilla-cuerpo");
const infoPagina = $<HTMLSpanElement>("info-pagina");
const btnAnterior = $<HTMLButtonElement>("btn-anterior");
const btnSiguiente = $<HTMLButtonElement>("btn-siguiente");
const mensaje = $<HTMLDivElement>("mensaje");

// ---------- 1. Cargar la grilla ----------

async function cargarGrilla() {
  cuerpoGrilla.innerHTML = `<tr><td colspan="8" class="vacio">Cargando...</td></tr>`;
  try {
    const pagina = await buscarFacturas(filtroActual, paginaActual, TAMANIO_PAGINA);
    cuerpoGrilla.innerHTML = pagina.contenido.length
      ? pagina.contenido.map(filaHtml).join("")
      : `<tr><td colspan="8" class="vacio">No hay facturas con esos filtros</td></tr>`;
    infoPagina.textContent = `Página ${pagina.pagina + 1} de ${Math.max(pagina.totalPaginas, 1)} · ${pagina.totalElementos} facturas`;
    btnAnterior.disabled = pagina.pagina === 0;
    btnSiguiente.disabled = pagina.pagina + 1 >= pagina.totalPaginas;
  } catch (e) {
    cuerpoGrilla.innerHTML = `<tr><td colspan="8" class="vacio error">${html((e as Error).message)}</td></tr>`;
  }
}

/** Una fila de la tabla. Si esta informada, no mostramos Editar / Eliminar / Informar. */
function filaHtml(f: FacturaResumen): string {
  const estado = f.informadaArca
    ? `<span class="chip ok">Informada</span>`
    : `<span class="chip pendiente">Pendiente</span>`;
  const acciones = f.informadaArca
    ? `<a class="btn chico" href="factura.html?id=${f.id}">Ver</a>`
    : `<a class="btn chico" href="factura.html?id=${f.id}">Editar</a>
       <button class="btn chico primario" data-informar="${f.id}">Informar a ARCA</button>
       <button class="btn chico peligro" data-eliminar="${f.id}">Eliminar</button>`;
  return `<tr>
    <td>${html(tipoComprobante(f.tipoComprobante))}</td>
    <td class="mono">${html(nroComprobante(f.puntoVenta, f.numero))}</td>
    <td>${html(fecha(f.fecha))}</td>
    <td>${html(f.cliente)}</td>
    <td class="der">${html(dinero(f.importeTotal))}</td>
    <td>${estado}</td>
    <td class="mono">${html(f.cae ?? "-")}</td>
    <td class="acciones">${acciones}</td>
  </tr>`;
}

// ---------- 2. Filtros ----------

formFiltros.addEventListener("submit", (e) => {
  e.preventDefault(); // que no recargue la pagina
  const datos = new FormData(formFiltros);
  filtroActual = {
    numero: String(datos.get("numero") ?? ""),
    fechaDesde: String(datos.get("fechaDesde") ?? ""),
    fechaHasta: String(datos.get("fechaHasta") ?? ""),
    cliente: String(datos.get("cliente") ?? ""),
    informada: String(datos.get("informada") ?? ""),
  };
  paginaActual = 0;
  cargarGrilla();
});

formFiltros.addEventListener("reset", () => {
  filtroActual = {};
  paginaActual = 0;
  setTimeout(cargarGrilla); // esperamos a que el form se limpie
});

// ---------- 3. Paginacion ----------

btnAnterior.addEventListener("click", () => { paginaActual--; cargarGrilla(); });
btnSiguiente.addEventListener("click", () => { paginaActual++; cargarGrilla(); });

// ---------- 4. Botones de cada fila (un solo listener para toda la tabla) ----------

cuerpoGrilla.addEventListener("click", async (e) => {
  const boton = (e.target as HTMLElement).closest<HTMLButtonElement>("button");
  if (!boton) return;

  if (boton.dataset.eliminar) {
    if (!confirm("¿Eliminar la factura? No se puede deshacer.")) return;
    await ejecutar(boton, () => eliminarFactura(Number(boton.dataset.eliminar)), "Factura eliminada.");
  }

  if (boton.dataset.informar) {
    if (!confirm("¿Informar la factura a ARCA? Después ya no se va a poder modificar.")) return;
    await ejecutar(boton, async () => {
      const f = await informarFactura(Number(boton.dataset.informar));
      if (!f.informadaArca) {
        throw new Error("ARCA rechazó la factura:\n" + (f.observacionesArca ?? "sin detalle"));
      }
      return f;
    }, "Factura informada. CAE asignado.");
  }
});

async function ejecutar(boton: HTMLButtonElement, accion: () => Promise<unknown>, textoOk: string) {
  boton.disabled = true;
  try {
    await accion();
    mostrarMensaje(textoOk, "ok");
  } catch (e) {
    mostrarMensaje((e as Error).message, "error");
  }
  await cargarGrilla();
}

function mostrarMensaje(texto: string, tipo: "ok" | "error") {
  mensaje.className = "mensaje " + tipo;
  mensaje.textContent = texto;
  mensaje.hidden = false;
}

// ---------- 5. Cartel del modo ARCA ----------

async function mostrarModoArca() {
  try {
    const estado = await estadoArca();
    const cartel = $<HTMLSpanElement>("modo-arca");
    cartel.textContent = estado.simulado
      ? "ARCA: SIMULADO (no se envía nada)"
      : `ARCA: ${estado.ambiente} · CUIT ${estado.cuit} · PV ${estado.puntoVenta}`;
    cartel.className = "chip " + (estado.simulado ? "pendiente" : "ok");
  } catch {
    // si el back no responde, la grilla ya muestra el error
  }
}

mostrarModoArca();
cargarGrilla();
