// Todo lo que habla con el backend esta aca. Las pantallas solo llaman a estas funciones.

const API = "http://localhost:8080/api";

// ---------- Tipos (la "forma" de los JSON que manda el back) ----------

export interface Pagina<T> {
  contenido: T[];
  pagina: number;
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
}

export interface Cliente {
  id: number;
  razonSocial: string;
  docTipo: number;
  docNro: number;
  condicionIva: number;
  direccion: string;
}

export interface ListaPrecio {
  id: number;
  nombre: string;
}

export interface PrecioArticulo {
  articuloId: number;
  codigo: string;
  descripcion: string;
  alicuotaIva: number;
  precio: number;
}

export interface FacturaResumen {
  id: number;
  puntoVenta: number;
  tipoComprobante: number;
  numero: number | null;
  fecha: string;
  cliente: string;
  importeTotal: number;
  informadaArca: boolean;
  cae: string | null;
}

export interface FacturaItem {
  articuloId: number;
  codigo: string;
  descripcion: string;
  cantidad: number;
  precioUnitario: number;
  alicuotaIva: number;
  subtotal: number;
}

export interface Factura {
  id: number;
  puntoVenta: number;
  tipoComprobante: number;
  numero: number | null;
  fecha: string;
  cliente: Cliente;
  listaPrecioId: number;
  listaPrecioNombre: string;
  items: FacturaItem[];
  importeNeto: number;
  importeIva: number;
  importeTotal: number;
  informadaArca: boolean;
  cae: string | null;
  caeVencimiento: string | null;
  fechaInforme: string | null;
  observacionesArca: string | null;
}

export interface FacturaRequest {
  clienteId: number;
  listaPrecioId: number;
  tipoComprobante: number;
  fecha: string;
  items: { articuloId: number; cantidad: number }[];
}

export interface FiltroFactura {
  numero?: string;
  fechaDesde?: string;
  fechaHasta?: string;
  cliente?: string;
  informada?: string;
}

export interface EstadoArca {
  simulado: boolean;
  ambiente: string;
  cuit: number;
  puntoVenta: number;
}

// ---------- Funcion base: hace el fetch y convierte los errores en Error con mensaje lindo ----------

async function pedir<T>(ruta: string, opciones: RequestInit = {}): Promise<T> {
  let respuesta: Response;
  try {
    respuesta = await fetch(API + ruta, {
      ...opciones,
      headers: { "Content-Type": "application/json", ...(opciones.headers ?? {}) },
    });
  } catch {
    throw new Error("No se pudo conectar con el servidor. ¿Esta levantado el backend en el puerto 8080?");
  }

  if (!respuesta.ok) {
    // El back siempre manda { mensaje, detalles } cuando hay error (ver ManejadorErrores.java)
    const cuerpo = await respuesta.json().catch(() => null);
    const detalles: string[] = cuerpo?.detalles ?? [];
    throw new Error([cuerpo?.mensaje ?? `Error ${respuesta.status}`, ...detalles].join("\n"));
  }
  if (respuesta.status === 204) {
    return undefined as T;
  }
  return respuesta.json() as Promise<T>;
}

/** Arma "?a=1&b=2" salteando los valores vacios. */
function queryString(params: Record<string, string | number | undefined>): string {
  const qs = new URLSearchParams();
  for (const [clave, valor] of Object.entries(params)) {
    if (valor !== undefined && valor !== "") {
      qs.set(clave, String(valor));
    }
  }
  return "?" + qs.toString();
}

// ---------- Facturas ----------

export function buscarFacturas(filtro: FiltroFactura, pagina: number, tamanio: number) {
  return pedir<Pagina<FacturaResumen>>("/facturas" + queryString({ ...filtro, pagina, tamanio }));
}

export function obtenerFactura(id: number) {
  return pedir<Factura>(`/facturas/${id}`);
}

export function crearFactura(datos: FacturaRequest) {
  return pedir<Factura>("/facturas", { method: "POST", body: JSON.stringify(datos) });
}

export function actualizarFactura(id: number, datos: FacturaRequest) {
  return pedir<Factura>(`/facturas/${id}`, { method: "PUT", body: JSON.stringify(datos) });
}

export function eliminarFactura(id: number) {
  return pedir<void>(`/facturas/${id}`, { method: "DELETE" });
}

export function informarFactura(id: number) {
  return pedir<Factura>(`/facturas/${id}/informar`, { method: "POST" });
}

// ---------- Exploradores ----------

export function buscarClientes(texto: string, pagina: number) {
  return pedir<Pagina<Cliente>>("/clientes" + queryString({ texto, pagina, tamanio: 8 }));
}

export function listasDePrecio() {
  return pedir<ListaPrecio[]>("/listas-precio");
}

export function buscarArticulos(listaId: number, texto: string, pagina: number) {
  return pedir<Pagina<PrecioArticulo>>(`/listas-precio/${listaId}/articulos` + queryString({ texto, pagina, tamanio: 8 }));
}

// ---------- ARCA ----------

export function estadoArca() {
  return pedir<EstadoArca>("/arca/estado");
}
