# Frontend — CRUD Factura de Venta (HTML + TypeScript)

Necesita el backend corriendo en `http://localhost:8080` (la carpeta de arriba de este mismo repo).

```bash
npm install
```

```bash
npm run build
```

```bash
npm start
```

Abre http://localhost:5500. Mientras programás, `npm run watch` recompila solo cada vez que guardás un `.ts`.

| Archivo | Qué hace |
|---|---|
| `index.html` + `src/listado.ts` | Grilla de facturas: filtros, paginación, informar, eliminar |
| `factura.html` + `src/factura.ts` | Alta / edición / detalle; se bloquea si está informada a ARCA |
| `src/explorador.ts` | Ventanita "explorer" reutilizable (buscar cliente / artículo de lista) |
| `src/api.ts` | Todas las llamadas al backend |
| `src/formato.ts` | Formato de pesos, fechas y números de comprobante |

La explicación completa está en el [README principal](../README.md).
