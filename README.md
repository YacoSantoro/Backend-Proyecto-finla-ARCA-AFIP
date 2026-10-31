# CRUD Factura de Venta + ARCA/AFIP (Backend Spring Boot)

TP final: CRUD de **Factura de Venta** con front HTML + TypeScript, back Spring y conexión a ARCA (ex AFIP).

| Requisito de la consigna | Dónde está |
|---|---|
| Filtros: Nro factura, Fecha desde/hasta, Cliente, informada o no | `FacturaFiltros.java` + `index.html` |
| Paginación de la grilla | `PaginaDto.java` + `listado.ts` |
| Buscar Cliente y Buscar ListaPrecioArticulo como "explorer" | `ExploradorController.java` + `explorador.ts` |
| Una vez informada a ARCA se bloquea la modificación | `FacturaService.verificarQueNoEsteInformada()` + `factura.ts aplicarBloqueo()` |
| Conexión ARCA/AFIP | paquete `arca/` (WSAA + WSFEv1) |

El front está en la carpeta hermana `../Frontend-Proyecto-final-ARCA-AFIP`.

---

## PARTE 1 — Lo que tenés que armar VOS (a mano)

### Paso 0. Programas
- Java 17, Maven, Node.js (ya los tenés).
- OpenSSL: viene con **Git Bash** (`openssl version` para verificar).

### Paso 1. Generar tu clave privada y el pedido de certificado (CSR)
Abrí **Git Bash** dentro de la carpeta `certificados/` de este repo:

```bash
openssl genrsa -out privada_test.key 2048
```

```bash
openssl req -new -key privada_test.key -subj "/C=AR/O=UTN TP/CN=facturacion-tp/serialNumber=CUIT 20XXXXXXXXX" -out solicitud_test.csr
```

> Cambiá `20XXXXXXXXX` por **tu CUIT** (el mismo con el que entrás a ARCA). Tiene que decir `CUIT`, un espacio y el número.
> `privada_test.key` es tu secreto: **no se comparte ni se sube a GitHub** (ya está en `.gitignore`).

### Paso 2. Sacar el certificado de HOMOLOGACIÓN en ARCA
1. Entrá a https://www.afip.gob.ar con tu **clave fiscal**.
2. Si no lo ves, agregá el servicio **"WSASS - Autogestión Certificados Homologación"** desde el *Administrador de Relaciones de Clave Fiscal*.
3. En WSASS → **Nuevo Certificado**: poné un nombre (ej. `facturacion-tp`) y pegá TODO el contenido de `solicitud_test.csr` (incluyendo `-----BEGIN...` y `-----END...`).
4. Te muestra el certificado: copialo y guardalo como `certificados/certificado_test.crt`.
5. En WSASS → **Crear autorización a servicio**: elegí ese certificado y el servicio **wsfe** (Facturación Electrónica). Confirmá.

### Paso 3. Configurar el backend
Editá `src/main/resources/application.properties`:

```properties
arca.simulado=false          # true = inventa el CAE (para probar sin certificado)
arca.produccion=false        # false = homologación (testing)
arca.cuit=20XXXXXXXXX        # tu CUIT, sin guiones
arca.punto-venta=1           # en homologación sirve el 1
arca.clave-privada=certificados/privada_test.key
arca.certificado=certificados/certificado_test.crt
```

> **Qué factura podés hacer:** si sos monotributista → **Factura C (11)**. Si sos Responsable Inscripto → **A (1)** o **B (6)**. Si ARCA rechaza por el tipo, probá con C.

### Paso 4. Levantar todo
Backend (en esta carpeta):

```bash
mvn spring-boot:run
```

Frontend (en `../Frontend-Proyecto-final-ARCA-AFIP`):

```bash
npm install
```

```bash
npm run build
```

```bash
npm start
```

Se abre http://localhost:5500. Arriba a la derecha dice si estás en **SIMULADO** u **HOMOLOGACION**.

### Paso 5. Probar la conexión con ARCA (antes de facturar)
Con el back levantado, abrí http://localhost:8080/api/arca/ultimo/11 — si devuelve `{"ultimoNumero": N}` el certificado y la autorización están OK.

| Error | Qué significa |
|---|---|
| `No encuentro la clave privada / certificado` | La ruta en `application.properties` está mal |
| `ns1:cms.cert.untrusted` / `cms.sign.invalid` | El `.crt` no corresponde a esa `.key`, o es de producción |
| `El CEE ya posee un TA valido` | Pediste ticket hace poco y se perdió el archivo `data/ticket-*.properties`. Esperá ~12 hs o usá otro certificado |
| `[600] ValidacionDeToken: No aparecio CUIT en lista de relaciones` | Te faltó el paso 2.5 (autorizar wsfe) o el CUIT del properties no es el tuyo |
| `[10016] ...` / `[10015] ...` | Datos de la factura que ARCA no acepta (tipo, fecha, documento). Se ven en la pantalla |

### Base de datos
H2 en archivo `./data/facturacion` (no hay que instalar nada). Consola: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/facturacion`, usuario `sa`, sin clave). Arranca con 8 clientes, 8 artículos y 2 listas de precio (`data.sql`).

---

## PARTE 2 — El código explicado como para un mono 🐒

### La idea en una imagen

```
 [ Navegador ]  --fetch JSON-->  [ Spring Boot ]  --SQL-->  [ Base H2 ]
  HTML + TS                         |
                                    +--SOAP/XML-->  [ ARCA ]
```

Hay **tres capas** en el back, como una cocina:
1. **Controller** (`web/`) = el **mozo**. Recibe el pedido (HTTP), se lo pasa a la cocina y devuelve el plato (JSON). No cocina.
2. **Service** (`service/`) = el **cocinero**. Acá están las reglas ("si está informada, no se toca").
3. **Repository** (`repository/`) = la **heladera**. Guarda y saca cosas de la base. Spring lo escribe solo a partir de la interfaz.

### `entity/` — las tablas
Cada clase con `@Entity` es una tabla. Cada atributo, una columna.
- `Cliente`, `Articulo`, `ListaPrecio`: datos maestros.
- `ListaPrecioArticulo`: "en la lista *Minorista*, el *Martillo* sale *$12.000*".
- `Factura` + `FacturaDetalle`: la cabecera y sus renglones. `@OneToMany(cascade = ALL, orphanRemoval = true)` significa: "si guardo la factura, guardá los renglones; si saco un renglón de la lista, borralo de la base".
- `Factura.informadaArca`: el **candado**. `numero` y `cae` quedan en `null` hasta que ARCA aprueba.
- El renglón guarda una **copia del precio**: si mañana cambia la lista, la factura vieja no cambia.

### `repository/` — buscar en la base
- `extends JpaRepository<Factura, Long>` → te regala `findById`, `save`, `delete`, `findAll`… sin escribir SQL.
- `@Query("select ...")` → cuando querés una búsqueda propia (los exploradores buscan por texto).
- `FacturaFiltros` → arma el `WHERE` **a medida**: cada filtro vacío no se agrega. Es lo que hace andar "Nro / Fecha desde / hasta / Cliente / Informada" combinados.
- `Pageable` → Spring agrega solo el `LIMIT/OFFSET` de la paginación.

### `dto/` — lo que viaja por la red
Las entidades son "de la cocina"; los DTO son "el plato servido". `FacturaRequest` es lo que manda el front (cliente, lista, artículos y cantidades — **los precios no**, para que nadie haga trampa desde el navegador). `PaginaDto` es lo que necesita la grilla: contenido + página + total.

### `service/CalculadoraImportes` — las cuentas
Los precios de la lista son **finales** (IVA incluido).
- Factura C: neto = total, IVA = 0.
- Factura A/B: agrupa por alícuota y hace `neto = total / 1,21` (o 1,105) e `IVA = total − neto`. Así neto + IVA siempre da exactamente el total.
Usa `BigDecimal` y no `double` porque `0.1 + 0.2` en `double` da `0.30000000000000004` — con plata no se juega.

### `service/FacturaService` — el cocinero
- `crear` / `actualizar`: busca cliente y lista, **busca cada precio en la lista**, arma renglones, calcula totales.
- `actualizar`, `eliminar` e `informar` empiezan llamando a `verificarQueNoEsteInformada()`: si el candado está puesto tira `FacturaBloqueadaException` → el front recibe **409**. Es la regla del TP, y está en el back (el front también deshabilita los botones, pero el que manda es el back).
- `informar`: le pide el CAE a `ArcaService`. Si aprueba → guarda número + CAE + pone el candado. Si rechaza → guarda las observaciones para mostrarlas.
- `@Transactional`: "todo o nada". Si algo falla a mitad de camino, la base vuelve a como estaba.

### `arca/` — hablar con ARCA (la parte difícil, en 3 pasos)

**Paso 1 — WSAA, el portero (`WsaaClient`)**
1. Escribimos un papelito XML (el **TRA**): "soy yo, quiero usar `wsfe`, válido de tal hora a tal hora".
2. Lo **firmamos** con la clave privada + certificado usando BouncyCastle (formato CMS/PKCS#7). Es como firmar un cheque: ARCA verifica con tu certificado que lo firmaste vos.
3. Lo mandamos a `loginCms`. ARCA contesta un **ticket** con `token` + `sign` que dura ~12 horas.
4. Lo guardamos en `data/ticket-...properties` y lo reusamos, porque **ARCA no te da otro mientras el anterior siga vivo**.

**Paso 2 — ¿qué número sigue? (`FECompUltimoAutorizado`)**
ARCA lleva la numeración. Le preguntamos el último número autorizado para (punto de venta, tipo) y le sumamos 1.

**Paso 3 — pedir el CAE (`FECAESolicitar`)**
`WsfeXml.solicitarCae` arma el XML con importes, documento del cliente, condición IVA (obligatoria desde la RG 5616) y, para A/B, el detalle de IVA. **El orden de los tags importa** (sale del WSDL). ARCA responde `A` (aprobado, con CAE y vencimiento) o `R` (rechazado, con observaciones).

Piezas:
- `SoapHttp`: el cartero. SOAP no es magia: es un **POST con un XML adentro**.
- `WsfeXml`: arma y lee los XML. No se conecta a nada, por eso se testea sin internet.
- `ArcaService`: hace los 3 pasos en orden. Si `arca.simulado=true`, inventa un CAE (para trabajar mientras sacás el certificado).

> Diferencia con el ejemplo del profe (`ARCAGestion`): ahí `AfipFacturaService` fuerza la URL de **producción** aunque el ticket sea de homologación. Acá la URL sale de `arca.produccion`, así que siempre coinciden.

### `web/` — los mozos
- `FacturaController`: `GET/POST/PUT/DELETE /api/facturas` y `POST /api/facturas/{id}/informar`.
- `ExploradorController`: `/api/clientes` y `/api/listas-precio/{id}/articulos` para las ventanitas explorer.
- `ArcaController`: `/api/arca/estado` y `/api/arca/ultimo/{tipo}` para probar la conexión.
- `ManejadorErrores`: convierte cada excepción en `{ "mensaje": "...", "detalles": [...] }` con su código (404, 409, 422, 502). El front siempre sabe dónde leer el error.
- `CorsConfig`: permite que la página en `localhost:5500` llame al back en `localhost:8080`.

### El front (`../Frontend-Proyecto-final-ARCA-AFIP`)
- `src/*.ts` se compila a `js/*.js` con `npm run build` (TypeScript es JavaScript con tipos; el navegador solo entiende JS).
- `api.ts`: **el único que hace `fetch`**. Las pantallas llaman a `buscarFacturas()`, `crearFactura()`, etc.
- `listado.ts` (`index.html`): filtros → `buscarFacturas(filtro, pagina)` → dibuja filas. Si la factura está informada, solo muestra "Ver".
- `factura.ts` (`factura.html?id=N`): alta/edición. `aplicarBloqueo()` deshabilita todo si está informada.
- `explorador.ts`: **una sola** ventanita reutilizable (`<dialog>`) con buscador + grilla paginada. Se le pasa *qué columnas mostrar* y *cómo buscar*; devuelve la fila elegida. Se usa para clientes y para artículos de la lista.

### Tests
```bash
mvn test
```
- `CalculadoraImportesTest`: las cuentas de IVA.
- `WsfeXmlTest`: el XML que mandamos y cómo leemos respuestas aprobadas, rechazadas y con error (sin internet).
- `FacturaServiceTest`: crear, modificar, borrar, **bloqueo al informar** y filtros (base en memoria + ARCA simulado).
