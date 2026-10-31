// Funciones chiquitas para mostrar datos lindos en pantalla.

const pesos = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" });

export function dinero(valor: number): string {
  return pesos.format(valor);
}

/** "2026-10-31" -> "31/10/2026" */
export function fecha(iso: string | null): string {
  if (!iso) return "-";
  const [a, m, d] = iso.substring(0, 10).split("-");
  return `${d}/${m}/${a}`;
}

const LETRA: Record<number, string> = { 1: "A", 6: "B", 11: "C" };

export function tipoComprobante(tipo: number): string {
  return "Factura " + (LETRA[tipo] ?? tipo);
}

/** Punto de venta 1, numero 8 -> "0001-00000008". Sin numero -> "(sin numero)". */
export function nroComprobante(puntoVenta: number, numero: number | null): string {
  if (numero === null) return "(sin número)";
  return String(puntoVenta).padStart(4, "0") + "-" + String(numero).padStart(8, "0");
}

const DOC: Record<number, string> = { 80: "CUIT", 96: "DNI", 99: "Sin identificar" };
const COND_IVA: Record<number, string> = { 1: "Resp. Inscripto", 4: "Exento", 5: "Consumidor Final", 6: "Monotributo" };

export function documento(docTipo: number, docNro: number): string {
  return docTipo === 99 ? "Sin identificar" : `${DOC[docTipo] ?? docTipo} ${docNro}`;
}

export function condicionIva(codigo: number): string {
  return COND_IVA[codigo] ?? String(codigo);
}

/** Escapa texto para meterlo en innerHTML sin riesgo. */
export function html(texto: string | number | null | undefined): string {
  return String(texto ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

/** Atajo para document.getElementById con tipo. */
export function $<T extends HTMLElement>(id: string): T {
  const el = document.getElementById(id);
  if (!el) throw new Error("No existe el elemento #" + id);
  return el as T;
}
