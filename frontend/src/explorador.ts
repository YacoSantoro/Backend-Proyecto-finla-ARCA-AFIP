// "Interfaz explorer" generica: una ventanita con buscador + grilla paginada + seleccionar.
// La usamos para "Buscar Cliente" y "Buscar ListaPrecioArticulo".

import { Pagina } from "./api.js";
import { html } from "./formato.js";

export interface Columna<T> {
  titulo: string;
  valor: (fila: T) => string;
  derecha?: boolean;
}

export interface OpcionesExplorador<T> {
  titulo: string;
  columnas: Columna<T>[];
  buscar: (texto: string, pagina: number) => Promise<Pagina<T>>;
}

/**
 * Abre el explorador y devuelve una Promesa:
 *  - se resuelve con la fila elegida, o
 *  - con null si el usuario cierra sin elegir.
 */
export function abrirExplorador<T>(opciones: OpcionesExplorador<T>): Promise<T | null> {
  const dialogo = document.createElement("dialog");
  dialogo.className = "explorador";
  dialogo.innerHTML = `
    <header>
      <h2>${html(opciones.titulo)}</h2>
      <button type="button" class="btn-icono" data-cerrar aria-label="Cerrar">✕</button>
    </header>
    <input type="search" placeholder="Escribí para buscar..." autocomplete="off">
    <div class="tabla-scroll">
      <table>
        <thead><tr>${opciones.columnas.map((c) => `<th${c.derecha ? ' class="der"' : ""}>${html(c.titulo)}</th>`).join("")}</tr></thead>
        <tbody></tbody>
      </table>
    </div>
    <footer>
      <button type="button" class="btn" data-anterior>‹ Anterior</button>
      <span data-info></span>
      <button type="button" class="btn" data-siguiente>Siguiente ›</button>
    </footer>
    <p class="ayuda">Doble clic (o Enter) sobre una fila para elegirla.</p>`;
  document.body.appendChild(dialogo);

  const input = dialogo.querySelector("input")!;
  const cuerpo = dialogo.querySelector("tbody")!;
  const info = dialogo.querySelector<HTMLSpanElement>("[data-info]")!;
  const btnAnterior = dialogo.querySelector<HTMLButtonElement>("[data-anterior]")!;
  const btnSiguiente = dialogo.querySelector<HTMLButtonElement>("[data-siguiente]")!;

  let pagina = 0;
  let filas: T[] = [];
  let espera: number | undefined;

  return new Promise<T | null>((resolver) => {
    const terminar = (elegido: T | null) => {
      dialogo.close();
      dialogo.remove();
      resolver(elegido);
    };

    async function cargar() {
      cuerpo.innerHTML = `<tr><td colspan="${opciones.columnas.length}" class="vacio">Buscando...</td></tr>`;
      try {
        const resultado = await opciones.buscar(input.value.trim(), pagina);
        filas = resultado.contenido;
        cuerpo.innerHTML = filas.length
          ? filas
              .map((fila, i) =>
                `<tr tabindex="0" data-i="${i}">${opciones.columnas
                  .map((c) => `<td${c.derecha ? ' class="der"' : ""}>${html(c.valor(fila))}</td>`)
                  .join("")}</tr>`)
              .join("")
          : `<tr><td colspan="${opciones.columnas.length}" class="vacio">No hay resultados</td></tr>`;
        info.textContent = `Página ${resultado.pagina + 1} de ${Math.max(resultado.totalPaginas, 1)} · ${resultado.totalElementos} resultados`;
        btnAnterior.disabled = resultado.pagina === 0;
        btnSiguiente.disabled = resultado.pagina + 1 >= resultado.totalPaginas;
      } catch (e) {
        cuerpo.innerHTML = `<tr><td colspan="${opciones.columnas.length}" class="vacio error">${html((e as Error).message)}</td></tr>`;
      }
    }

    function filaElegida(evento: Event): T | null {
      const tr = (evento.target as HTMLElement).closest<HTMLTableRowElement>("tr[data-i]");
      return tr ? filas[Number(tr.dataset.i)] : null;
    }

    // "debounce": esperamos 300ms despues de la ultima tecla para no llamar al back por cada letra
    input.addEventListener("input", () => {
      clearTimeout(espera);
      espera = window.setTimeout(() => {
        pagina = 0;
        cargar();
      }, 300);
    });
    input.addEventListener("keydown", (e) => {
      if (e.key === "ArrowDown") cuerpo.querySelector<HTMLElement>("tr[data-i]")?.focus();
    });
    cuerpo.addEventListener("dblclick", (e) => {
      const fila = filaElegida(e);
      if (fila) terminar(fila);
    });
    cuerpo.addEventListener("keydown", (e) => {
      const fila = filaElegida(e);
      if (!fila) return;
      const tr = e.target as HTMLElement;
      if (e.key === "Enter") {
        e.preventDefault(); // sin esto el Enter "rebota" en el boton que abrio el explorador y lo reabre
        terminar(fila);
      }
      if (e.key === "ArrowDown") (tr.nextElementSibling as HTMLElement | null)?.focus();
      if (e.key === "ArrowUp") {
        const anterior = tr.previousElementSibling as HTMLElement | null;
        if (anterior) anterior.focus(); else input.focus();
      }
    });
    btnAnterior.addEventListener("click", () => { pagina--; cargar(); });
    btnSiguiente.addEventListener("click", () => { pagina++; cargar(); });
    dialogo.querySelector("[data-cerrar]")!.addEventListener("click", () => terminar(null));
    dialogo.addEventListener("cancel", (e) => { e.preventDefault(); terminar(null); }); // tecla Escape

    dialogo.showModal();
    input.focus();
    cargar();
  });
}
