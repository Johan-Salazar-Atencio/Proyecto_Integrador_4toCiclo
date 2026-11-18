# Bitácora — Fichas de web, móvil, base de datos, CI y funciones nuevas

Formato igual que las fichas anteriores. Las líneas son las de `main` local en `96ff15a`.

**Índice:**
- **Web:** [WEB-01](#web-01--la-pantalla-ficha-salía-en-blanco) · [WEB-02](#web-02--mi-jardín-de-la-web-vivía-en-localstorage-y-no-en-el-servidor) · [WEB-03](#web-03--escaneos-sin-identificar-mostraban-confianza-0) · [WEB-04](#web-04--las-animaciones-no-se-ven-en-un-navegador-con-movimiento-reducido) · [WEB-05](#web-05--rediseño-esmeralda-y-lima-de-la-web)
- **Móvil:** [MOV-01](#mov-01--login-simulado-sin-uso-y-monstera-inventada-en-el-detalle) · [MOV-02](#mov-02--android-17-bloqueaba-la-conexión-con-el-pc) · [MOV-03](#mov-03--rediseño-del-móvil-y-hoja-de-abonos)
- **Base de datos:** [BD-01](#bd-01--choque-de-versiones-de-migración-v25-renombrada-a-v26) · [BD-02](#bd-02--catálogo-de-migraciones-v10v28-qué-hace-cada-una) · [BD-03](#bd-03--el-panel-admin-ya-no-borra-solo-activa-o-desactiva)
- **CI y tests:** [CI-01](#ci-01--no-había-integración-continua) · [CI-02](#ci-02--bcrypt-faltaba-en-requirementstxt) · [CI-03](#ci-03--los-tests-de-webui-dependían-del-env-local) · [CI-04](#ci-04--mockito-falla-con-java-26-92-errores) · [CI-05](#ci-05--configuración-ruidosa-sql-y-debug-siempre-activos)
- **Funciones nuevas:** [FUN-01](#fun-01--prototipo-de-animación-vectorial-monstera) · [FUN-02](#fun-02--flujo-escaneo--colección--virtualización) · [FUN-03](#fun-03--planta-pixel-art-con-7-etapas) · [FUN-04](#fun-04--fotos-reales-en-la-colección-y-en-escaneos-recientes) · [FUN-05](#fun-05--página-demo-planta)

---

## WEB-01 — La pantalla `/ficha` salía en blanco

| Campo | Valor |
|---|---|
| Fecha | 10-nov-2026 |
| Commit | `ec753b0` — [anexo](anexos/2026-11-10-ec753b0.md) |
| Severidad | Alta (pantalla inutilizable), corrección de 1 línea |
| Archivo | `frontend-react/src/pages/user/planta/FichaPlanta.jsx` |

### Síntoma
Al abrir la ficha de una planta tras escanear, la página quedaba completamente en blanco.

### Causa
`FichaPlanta.jsx` usaba `urlImagen(...)` sin importarla. En JavaScript (sin TypeScript), eso no da error al compilar: falla al **ejecutar** con `ReferenceError: urlImagen is not defined`. Como React no tenía un *error boundary*, se desmontaba todo el árbol y quedaba la pantalla en blanco.

### Corrección

```js
import { urlImagen } from '../../../services/home'
```

### Si vuelve a pasar: "pantalla en blanco"
1. Abre la consola del navegador (F12): el error dice exactamente qué variable falta y en qué archivo.
2. `npm run build` **no** detecta variables no importadas: Vite solo transpila. Un linter (`eslint` con `no-undef`) sí lo haría, pero el proyecto no tiene ninguno (`CLAUDE.md`: "no hay lint ni tests").
3. **Propuesta:** añadir un *error boundary* en `App.jsx` que muestre "Algo salió mal" con un botón para volver, en lugar del blanco total.

---

## WEB-02 — "Mi jardín" de la web vivía en `localStorage` y no en el servidor

| Campo | Valor |
|---|---|
| Fecha | 11-nov-2026 |
| Commit | `30e61ed` — [anexo](anexos/2026-11-11-30e61ed.md) |
| Severidad | Alta (datos inconsistentes entre dispositivos) |
| Archivos | 13. Destacan `PlantasService.java` (+225), `V12__jardin_digital.sql`, `services/jardin.js` (nuevo) y `store/jardinSimulado.js` (borrado, −145) |

### Síntoma
Lo que se cuidaba en la web no existía en el móvil, ni en otro navegador, ni tras borrar los datos del navegador. Al recargar la página, la planta perdía el desgaste acumulado.

### Causa
La web simulaba el jardín en `src/store/jardinSimulado.js` con `localStorage`, aunque Spring ya tenía `/api/v1/plantas`. La simulación calculaba el desgaste solo mientras la página estaba abierta.

### Corrección
- **BD (V12):** `plantas_usuario` gana cuatro columnas:
  - `crecimiento INT NOT NULL DEFAULT 0`: cuidados útiles acumulados;
  - `apariencia TEXT`: JSON de la IA;
  - `foto_url VARCHAR(500)`;
  - `es_activa BOOLEAN NOT NULL DEFAULT FALSE`: la planta que se muestra.
- **`PlantasService`:**
  - digitalizar, desde un escaneo o desde la colección, reiniciando si la especie ya existía;
  - seleccionar la planta activa;
  - cuidar, sumando crecimiento y gestionando la marchitez;
  - desgaste por **horas completas**, calculado en el servidor al leer, así que no depende de que la página esté abierta.
- **API:**
  - `POST /api/v1/plantas` acepta `idEscaneo` o `idCatalogo`, más `apariencia`;
  - nuevo `POST /api/v1/plantas/{id}/seleccionar`.
- **`PlantSummaryDto`:** añade `id`, `crecimiento`, `apariencia`, `fotoUrl` y `activa`. El móvil los ignora; el contrato se actualizó en `ContratoMovilTest`.
- **Web:** `src/services/jardin.js` sustituye a `jardinSimulado.js` en `Jardin.jsx`, `FichaPlanta.jsx` y `Coleccion.jsx`. Las acciones del jardín están en `src/pages/user/jardin/acciones.js`.

### Verificación
`PlantasServiceTest` cubre el desgaste por horas completas y el crecimiento por cuidados.

### Regla derivada
Escrita en `CLAUDE.md`: *"'Mi jardín' vive en Spring (`PlantasService`, `/api/v1/plantas`); la web lo usa por `src/services/jardin.js`."* El jardín del móvil todavía usaba `MockGardenData` en ese momento. Johan lo conectó después: `GardenViewModel` llama a `garden.renombrar`, `repo.usarAbono`, etc.

---

## WEB-03 — Escaneos sin identificar mostraban "Confianza 0%"

| Campo | Valor |
|---|---|
| Fecha | 11-nov-2026 |
| Commit | `b7c118f` — [anexo](anexos/2026-11-11-b7c118f.md) |
| Archivo | `services/EscaneoService.java`, método `recientes` (líneas 160-184) |

### Causa
`Integer confNum = 0;` nunca es `null`, así que la rama `confNum == null ? "Sin identificar" : ...` no se ejecutaba nunca. Era código muerto.

### Corrección
El texto depende de si hay especie, no de la confianza:

```java
String confianza = e.getIdCatalogo() != null
        ? "Confianza " + confNum + "%"
        : "Sin identificar";
```

Desde [IA-05](20-fichas-economia-ia.md#ia-05--especies-falsas-plantae-sp-que-pagaban-premio-de-especie-nueva), un escaneo con confianza alta pero nombre genérico también queda con `id_catalogo = null` y se muestra "Sin identificar". Es lo esperado.

---

## WEB-04 — Las animaciones "no se ven" en un navegador con movimiento reducido

| Campo | Valor |
|---|---|
| Fecha | 18-nov-2026 (observación, no es un fallo) |
| Archivos | `components/escena.css:13`, `components/planta-pixel.css:16`, `components/brote.css:11`, `components/planta-vector.css:44`, `components/admin/layout-admin.css:46` |

### Qué pasó
Al revisar el arte nuevo en el navegador integrado, los fondos (tiras de 96 fotogramas) y los sprites aparecían **quietos**. Parecía que faltaban las animaciones.

### Causa
El navegador tenía activada la preferencia del sistema `prefers-reduced-motion: reduce`. El CSS la respeta a propósito, por accesibilidad: hay personas a las que el movimiento les marea.

```css
/* components/escena.css:13 */
@media (prefers-reduced-motion: reduce) { .escena-fondo { animation: none; } }
```

### Cómo comprobarlo
- En la consola del navegador: `matchMedia('(prefers-reduced-motion: reduce)').matches`. Si da `true`, las animaciones están desactivadas a propósito.
- Al forzar la animación desde las herramientas de desarrollo, se movían correctamente.
- En Windows, la preferencia se cambia en *Configuración → Accesibilidad → Efectos visuales → Efectos de animación*.

**No tocar este CSS:** es el comportamiento correcto.

---

## WEB-05 — Rediseño "Esmeralda y Lima" de la web

| Campo | Valor |
|---|---|
| Fecha | 17-nov-2026 |
| Commits | Johan: `c885085` (paleta), `442f520`, `cce8453`, `e806822`, `7125d05`, `445c3e1`, `f22c39d`, `7ac9cf5`, `817b3da`, `1cce933`, `6d1e824`, `a85c148`, `7897a8d`, `1f2d673`, `6578382`, `782567f`, `c9d9ff2`, `5280b8b`, `f45ced3`, `736884f`, `7494000`, `009fd9a` — ver el [índice de anexos](anexos/README.md) |
| Tipo | Rediseño visual, sin cambios de API salvo `services/admin/catalogo.js` (`imagen` ↔ `sprite_base_url`) y `services/admin/eventos.js` (`AVISO_EVENTOS`) |

### Sistema de diseño
Todo se apoya en variables CSS de `src/styles/tema.css`. La tabla completa de colores claro y oscuro está en [REF-03](40-referencia.md#ref-03--paleta-esmeralda-y-lima-web-y-móvil).

**Regla:** los componentes usan `var(--primario)`, `var(--superficie)`, etc., **nunca** colores fijos. Los nombres antiguos (`--verde`, `--ambar`, `--azul`, `--rojo` y sus variantes `-claro`) se mantienen como alias para el CSS que aún no se ha migrado.

### Piezas nuevas destacadas

| Archivo | Qué es |
|---|---|
| `src/pages/user/home/xp.js` | `porcentajeXp(usuario)` e `iniciales(nombre)` |
| `src/pages/user/home/components/ModalMisiones.jsx` | misiones del día en una ventana |
| `src/pages/user/home/components/SinEvento.jsx` | tarjeta de "no hay evento activo", con consejos |
| `src/pages/user/misiones/MisionFila.jsx` | fila de misión reutilizable |
| `src/pages/user/coleccion/Iconos.jsx`, `src/pages/user/tienda/Icono.jsx` | iconos SVG en línea en lugar de emojis |
| `src/pages/admin/eventos/ModalLanzar.jsx`, `TarjetaTipo.jsx`, `util.js` | eventos y tipos de evento unidos en una sola sección |
| `src/pages/admin/misiones/kit.jsx` y `kit.css` | piezas compartidas del admin (panel lateral, interruptor) |

### Borrados
- `src/pages/admin/tipos-evento/TiposEventoAdmin.jsx` y su CSS: fusionados en Eventos.
- `src/pages/admin/eventos/VistaNotificacion.jsx`.

Si un enlace antiguo apunta a `/admin/tipos-evento`, ya no existe. Revisa `src/routes/rutas.js`.

### Cómo se verificó tras la fusión
Se recorrieron en el navegador inicio, jardín, tienda y colección con lucia. Así apareció [ECO-02](20-fichas-economia-ia.md#eco-02--el-jardín-web-solo-ofrecía-un-abono-y-anunciaba-1-día-siempre). Las 63 URLs de arte respondían 200.

---

## MOV-01 — Login simulado sin uso y "Monstera" inventada en el detalle

| Campo | Valor |
|---|---|
| Fecha | 11-nov-2026 |
| Commit | `b95e242` — [anexo](anexos/2026-11-11-b95e242.md) |
| Archivos | `data/MockAuthRepository.kt` (borrado), `data/AuthRepository.kt`, `screens/detail/PlantDetailScreen.kt` |

### Problemas
1. `MockAuthRepository` (57 líneas) no lo usaba nadie: `AuthViewModel` usa `ApiAuthRepository`. Era código muerto que confundía al leer el proyecto.
2. `PlantDetailScreen`, si se abría sin un escaneo real, mostraba una **Monstera inventada como si fuera el resultado**. El usuario podía pensar que la IA la había identificado.

### Corrección
Se borró el *mock*. Sin escaneo, el detalle vuelve atrás en lugar de inventar datos:

```kotlin
// Ultimo escaneo real. Si no hay (p. ej. Android cerro la app y la reabrio en esta
// pantalla) se vuelve atras: antes se mostraba una Monstera inventada como si fuera real.
val resultado = remember(plantId) { ScanResultStore.ultimo }
if (resultado == null) {
    LaunchedEffect(Unit) { onBack() }
    return
}
```

El caso real en que ocurría: Android mata la app en segundo plano y, al reabrirla, restaura la pantalla de detalle. `ScanResultStore.ultimo` vive en memoria y ya no existe.

**Regla:** nunca mostrar datos simulados como si fueran reales. Si faltan datos, mostrar un estado vacío o volver.

---

## MOV-02 — Android 17 bloqueaba la conexión con el PC

| Campo | Valor |
|---|---|
| Fecha | 17-nov-2026 |
| Commit | `d2034ba` (Johan) — [anexo](anexos/2026-11-17-d2034ba.md) |
| Archivos | `mobile-kotlin/app/src/main/AndroidManifest.xml`, `MainActivity.kt` |

### Síntoma
En un móvil o emulador con Android 17, la app no conectaba con el backend del PC: ni `10.0.2.2` (emulador), ni `192.168.x.x`, ni `10.x`.

### Causa
Android 17 exige un permiso nuevo en tiempo de ejecución, `ACCESS_LOCAL_NETWORK`, para hablar con equipos de la red local.

### Corrección

```xml
<!-- Android 17 bloquea las conexiones a redes privadas (10.0.2.2, 192.168.x.x, 10.x) sin este permiso -->
<uses-permission android:name="android.permission.ACCESS_LOCAL_NETWORK" />
```

```kotlin
private val pedirRedLocal = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
    if (concedido) RetrofitClient.reconectar()
}

private fun pedirPermisoRedLocal() {
    if (Build.VERSION.SDK_INT < 37) return
    val permiso = "android.permission.ACCESS_LOCAL_NETWORK"
    if (ContextCompat.checkSelfPermission(this, permiso) != PackageManager.PERMISSION_GRANTED) {
        pedirRedLocal.launch(permiso)
    }
}
```

`RetrofitClient.reconectar()` vuelve a buscar el mejor host, porque el primer intento falló antes de tener el permiso.

### Si vuelve a pasar
- "No conecta con el servidor" en Android 17 o superior: comprueba en *Ajustes → Apps → PlantIA → Permisos* que el acceso a la red local está concedido.
- En el emulador se puede usar `adb reverse tcp:8080 tcp:8080`. Así el móvil habla con `127.0.0.1` y no necesita el permiso.
- Uso desde fuera de casa: ver [OPE-08](30-operacion-y-entorno.md#ope-08--usar-la-app-desde-el-móvil-por-internet).

---

## MOV-03 — Rediseño del móvil y hoja de abonos

| Campo | Valor |
|---|---|
| Fecha | 17-nov-2026 |
| Commits | Johan: `4cae3a8`, `293278d`, `1467e16`, `26ffa68`, `94df31d`, `ed27b76`, `a7862c1`, `0a8b6ba`, `4b39c60`, `d6b9e35`, `97f494a`, `3a4c756` |

### Cambios de datos (los que pueden romper cosas)

| Archivo | Cambio | Riesgo |
|---|---|---|
| `data/remote/PlantiaApi.kt` | `abonoTienda(@Path("id") idPlanta: Int, @Body body: Map<String, Int>)` | Envía siempre un cuerpo, `{}` si no se elige abono. El servidor lo acepta |
| `data/repository/TiendaRepository.kt` | `usarAbono(idPlanta: Int, idItem: Int? = null)` | — |
| `data/model/Tienda.kt` | `abonos` (lista ordenada por precio); `abono = abonos.firstOrNull()` | — |
| `data/remote/dto/ScanDtos.kt`, `data/model/RecentScan.kt` | `fotoUrl: String? = null` | Opcional: un servidor antiguo sin el campo no rompe |
| `data/mapper/Mappers.kt` | `fotoUrl = normalizarUrlLocal(fotoUrl)` | Convierte `http://localhost:8080/files/...` en el host real del PC |

### `GardenViewModel` (`ui/garden/viewmodel/GardenViewModel.kt`)
- **Estado:**
  - `abonos: List<TiendaItem>`;
  - `hojas: Int?` (saldo según la última consulta de la tienda);
  - `eligiendoAbono: Boolean`.
- **Funciones:**
  - `abrirAbonos()`: abre la hoja y llama a `cargarAbono()`;
  - `cerrarAbonos()`;
  - `usarAbono(abono, alTerminar)`: tras aplicar, cierra la hoja, avisa con `crecio(actual, r.planta)` y recarga;
  - `crecio(antes, despues)`: según la diferencia de `diasCuidados`, devuelve "ganó medio día" (0), "ganó un día" (1) o "ganó N días";
  - `renombrar(apodo, alTerminar)`: con el apodo vacío vuelve al nombre de la especie.

### `HojaAbonos.kt` (nuevo, 115 líneas)
Hoja inferior con los abonos.
- La variable `alcanza = hojas == null || hojas >= abono.precio` decide si el botón está activo. Si todavía no se conoce el saldo, deja intentar y el servidor responde 402 si no alcanza.
- Si el abono está bloqueado, muestra "🔒" en lugar del precio.

### Visual
- **Paleta:** `ui/theme/Color.kt` y `Theme.kt`, con los mismos valores que la web ([REF-03](40-referencia.md#ref-03--paleta-esmeralda-y-lima-web-y-móvil)).
- **Componentes comunes:** `ui/components/Estilo.kt` y `MiniaturaEscaneo.kt`.
- **Barra inferior:** `ui/navigation/BarraInferior.kt`, flotante y con un botón central lima.
- **Inicio:** `UserHeaderCard`, `PlantSummaryCard`, `EventoCard`, `MisionesResumenCard` y el nuevo `SinEventoCard`, que es un carrusel con `res/drawable-nodpi/sin_evento_1..4.webp`.
- **Escáner** (`ui/scan/ScanScreen.kt`, +106 / −205): cámara a pantalla completa, consejos, marco lima y obturador.
- **Perfil** (`ui/profile/PerfilScreen.kt`, +207 / −168): anillo de XP. `EditarDatosScreen` deja el correo **en solo lectura**; `EditarDatosViewModel` pierde la lógica de cambiar el correo y su test se actualizó.

### Tests del móvil tocados
- `ui/profile/EditarDatosViewModelTest.kt`: se quitan los casos de cambio de correo.
- `ui/tienda/TiendaViewModelTest.kt`: se ajusta a `usarAbono(idPlanta, idItem)`.

> **No verificado en esta sesión:** compilar el móvil (`gradlew :app:compileDebugSources`) ni ejecutar sus tests. Solo se comprobaron las pantallas web y el backend.

---

## BD-01 — Choque de versiones de migración (V25 renombrada a V26)

| Campo | Valor |
|---|---|
| Fecha | 17-nov-2026 |
| Commit | `293278d` (Johan) — [anexo](anexos/2026-11-17-293278d.md) |
| Severidad | Alta (Spring no arranca si dos migraciones tienen la misma versión) |

### Qué pasó
Johan creó `V25__abonos_y_fotos.sql` en su rama mientras en `main` ya existía `V25__quitar_region_y_tipo_evento_en_texto.sql` (`20eb756`, 16-nov). Dos archivos con la misma versión hacen que Flyway falle al arrancar con `Found more than one migration with version 25`. Lo resolvió renombrando la suya a **V26**: el commit lo registra como *rename* con un 96 % de similitud.

### Por qué es peligroso
- Si una BD ya había aplicado la V25 "de abonos" antes del cambio de nombre, Flyway guarda en `flyway_schema_history` versión 25 con **otra** descripción y otro checksum. Al arrancar con el código nuevo dará `Validate failed: Migration description mismatch` o `checksum mismatch`.
- Solo se puede corregir a mano en esa BD: borrando la fila o con `flyway repair`. Eso **sí** puede dejar el esquema incoherente.

### Reglas para evitarlo
1. Antes de crear `V{n}`, haz `git fetch` y mira la versión más alta en **todas** las ramas activas: `git ls-tree -r --name-only origin/main origin/rama-de-johan -- db/migration | sort -V | tail`.
2. Si dos ramas compiten, la que se fusiona **después** renumera la suya **antes** de que nadie la aplique.
3. Nunca edites ni renombres una migración ya aplicada en una BD compartida (está en `CLAUDE.md`).
4. Al integrar ramas, comprueba que la secuencia no tenga huecos ni repetidos: `ls db/migration | sed -E 's/^V([0-9]+)__.*/\1/' | sort -n | uniq -d` no debe imprimir nada.

### Estado actual
V25 (`quitar_region_y_tipo_evento_en_texto`), V26 (`abonos_y_fotos`), V27 (`monedas_no_negativas`) y V28 (`tope_escaneo_nueva`): secuencia limpia. La BD local está en v28.

> **Atención al subir:** V27 y V28 solo existen en `main` local. Si alguien crea otra V27 en su rama antes de que se suban, se repetirá este choque. Hay que avisar al equipo.

---

## BD-02 — Catálogo de migraciones V10–V28: qué hace cada una

Solo las tocadas en el periodo de esta bitácora. Para V1–V9 ver `DOCUMENTACION-BD.md`.

| Versión | Archivo | Qué hace | Destructiva | Ficha |
|---|---|---|---|---|
| V10 | `V10__invalidar_tokens_fijos.sql` | `session_token = NULL` donde `LIKE 'dev-default-token-%'` | Sí, cierra sesiones | SEG-01 |
| V11 | `V11__session_hash_y_caducidad.sql` | añade `session_expira` y vacía **todos** los tokens | Sí, todos deben volver a iniciar sesión | SEG-06 |
| V12 | `V12__jardin_digital.sql` | `plantas_usuario` + `crecimiento`, `apariencia`, `foto_url`, `es_activa` | No | WEB-02 |
| V13–V25 | (127 commits de Johan, PR #3) | eventos, desastres, tienda, temas, economía, baúl, macetas… | ver `DOCUMENTACION-BD.md` | — |
| V26 | `V26__abonos_y_fotos.sql` | `items_tienda.medios_dias`, `plantas_usuario.medio_dia_pendiente`, 2 abonos nuevos, relleno de `coleccion_usuario.foto_url` | No | ECO-03, FUN-04 |
| V27 | `V27__monedas_no_negativas.sql` | saldos negativos a 0 y `CHECK (monedas >= 0)` | Corrige datos inválidos | ECO-01 |
| V28 | `V28__tope_escaneo_nueva.sql` | `tope_diario = 5` en `ESCANEO_NUEVA` | No | IA-05 |

---

## BD-03 — El panel admin ya no borra: solo activa o desactiva

| Campo | Valor |
|---|---|
| Fecha | 17-nov-2026 |
| Commit | `e0c6f18` (Johan) — [anexo](anexos/2026-11-17-e0c6f18.md) |
| Archivos | 9 en `frontend-react/src/pages/admin/` (+4 / −46) |

Se quitaron los botones de eliminar de catálogo, tipos de evento, juego (economía), misiones y tienda. Cada elemento se activa o desactiva con su interruptor.

### Por qué es buena idea
Muchas tablas tienen claves foráneas hacia esas filas: `escaneos_historial.id_catalogo`, `inventario_usuario.id_item`, `misiones_usuario.id_mision`. Borrar una especie o un artículo con datos de usuarios daría un error de clave foránea o, con `CASCADE`, borraría el historial de los usuarios. Desactivar las oculta sin perder nada.

### Para el futuro
Las consultas de la app deben filtrar por `activo = true`. Ya lo hacen, por ejemplo, `findFirstByTipoAndActivoTrueOrderByPrecioMonedasAsc` y el filtro de `AbonoService`. Es la forma correcta de ocultar las especies falsas 8, 9 y 10 de [IA-05](20-fichas-economia-ia.md#ia-05--especies-falsas-plantae-sp-que-pagaban-premio-de-especie-nueva).

---

## CI-01 — No había integración continua

| Campo | Valor |
|---|---|
| Fecha | 11-nov-2026 |
| Commit | `769a553` — [anexo](anexos/2026-11-11-769a553.md) |
| Archivos | `.github/workflows/ci.yml` (nuevo), `backend-django/catalog/tests.py`, `frontend-react/package-lock.json` |

### Qué comprueba
`.github/workflows/ci.yml` se ejecuta en cada `push` a `main` y en cada PR, con tres trabajos en paralelo:

| Trabajo | Entorno | Comandos |
|---|---|---|
| `spring` | Temurin **Java 17**, caché de Maven | `mvn -B test` |
| `django` | Python 3.12, `DB_ENGINE=sqlite3`, `DJANGO_SECRET_KEY=ci-no-es-secreta`, `IA_PROVIDER=mock` | `pip install -r requirements.txt`, `manage.py check`, `manage.py test ia_engine catalog` |
| `web` | Node 20 | `npm ci`, `npm run build` |

Ninguno necesita PostgreSQL ni claves de IA reales.

### Detalle encontrado al crearlo
`package-lock.json` no tenía `@tanstack/react-virtual`, aunque `package.json` sí lo declaraba (añadido en `4b54d1e`). `npm install` lo resolvía, pero `npm ci` (que exige que el lock cuadre) fallaba.

**Regla:** al añadir una dependencia, commitea `package.json` **y** `package-lock.json`.

---

## CI-02 — `bcrypt` faltaba en `requirements.txt`

| Campo | Valor |
|---|---|
| Commit | `b1f279f` — [anexo](anexos/2026-11-11-b1f279f.md) |

**Síntoma:** el trabajo `django` del CI fallaba con `ModuleNotFoundError: No module named 'bcrypt'`. En local funcionaba porque el `.venv` ya lo tenía instalado a mano.

**Corrección:** `bcrypt==5.0.0` en `backend-django/requirements.txt`.

**Regla:** si un `import` nuevo funciona en local pero no está en `requirements.txt`, el CI lo detecta. Antes de commitear, comprueba con `pip freeze | grep -i <paquete>` qué versión tienes.

---

## CI-03 — Los tests de WebUI dependían del `.env` local

| Campo | Valor |
|---|---|
| Commit | `f32c297` — [anexo](anexos/2026-11-11-f32c297.md) |

**Síntoma:** los tests de la cadena de respaldo pasaban en local y fallaban en CI.

**Causa:** el test fijaba `WEBUI_API_KEY` con `override_settings`, pero `WebuiProvider.__init__` también exige `WEBUI_BASE_URL`. En local llegaba del `.env`; en CI no existía, y el proveedor lanzaba la excepción de "no configurado" antes de lo que el test esperaba.

**Corrección:** los `override_settings` incluyen `WEBUI_BASE_URL="http://webui.test"`.

**Regla:** un test no debe depender del `.env` del desarrollador. Fija en el test **todas** las variables que lee el código probado, o bórralas explícitamente.

---

## CI-04 — Mockito falla con Java 26 (92 errores)

| Campo | Valor |
|---|---|
| Tipo | Problema del entorno local, no del código |

### Síntoma
`mvn test` en esta máquina (Java 26 por defecto) da 92 errores del tipo `Mockito cannot mock this class` o `Could not modify all classes`.

### Causa
La versión de Mockito o Byte Buddy que trae Spring Boot 3.3 no soporta el *bytecode* de Java 26. El `pom.xml` apunta a Java 17, y el CI usa 17.

### Cómo ejecutar los tests en local

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.13.0.3"; mvn test
```

Con Java 21: **186 tests, 0 fallos y 0 errores** (18-nov).

### Relación con el código
Por eso `CLAUDE.md` dice: *"Spring no puede simular clases concretas con Mockito (Java 26): usar dobles escritos a mano"*. Así nacieron el constructor protegido de `IaClient` ([SEG-07](10-fichas-seguridad.md#seg-07--inyección-en-la-cabecera-multipart-y-límite-de-72-bytes-de-bcrypt) parte C) y, de rebote, [IA-02](20-fichas-economia-ia.md#ia-02--spring-usaba-el-constructor-vacío-de-iaclient-y-ningún-escaneo-llegaba-a-la-ia).

### Arrancar Spring con Java 26
Funciona, pero hay que añadir `-Dspring-boot.run.jvmArguments=--enable-native-access=ALL-UNNAMED` para evitar los avisos o fallos de acceso nativo.

---

## CI-05 — Configuración ruidosa: SQL y DEBUG siempre activos

| Campo | Valor |
|---|---|
| Commit | `0d15378` — [anexo](anexos/2026-11-11-0d15378.md) |
| Archivos | `backend-spring-boot/src/main/resources/application.yml`, `README.md` |

`application.yml` tenía `show-sql: true` y el nivel de log de `com.plantia` en `DEBUG` para todos los entornos: logs enormes y SQL con datos de usuarios en ellos. Ahora:
- `SQL_LOG=true` activa el SQL;
- `LOG_LEVEL=DEBUG` sube el nivel de log.

Por defecto, ninguno de los dos.

El README describía la autenticación como "solo `X-User-Id`". Se actualizó con `X-Session-Token`, el token del panel admin e `IA_CLAVE_INTERNA`.

---

## FUN-01 — Prototipo de animación vectorial "Monstera"

| Campo | Valor |
|---|---|
| Fecha | 10-nov-2026 |
| Commit | `35c863c` — [anexo](anexos/2026-11-10-35c863c.md) |
| Archivos | `PROTOTIPO-ANIMACION.md`, `frontend-react/src/components/PlantaVectorAnimada.jsx` (117), `planta-vector.css` (49), `mobile-kotlin/.../ui/components/PlantaVectorAnimada.kt` (162) |

Es un prototipo de planta animada con vectores: SVG con animación CSS en la web y `Canvas` de Compose en el móvil. Respeta `prefers-reduced-motion` (`planta-vector.css:44`). Más tarde se usa en la página de demostración (FUN-05). El jardín principal usa pixel art (FUN-03).

---

## FUN-02 — Flujo escaneo → colección → virtualización

| Campo | Valor |
|---|---|
| Fecha | 10-nov-2026 |
| Commit | `4b54d1e` — [anexo](anexos/2026-11-10-4b54d1e.md) |
| Guía de prueba | `PRUEBA-FLUJO.md` (raíz del repo) |

### Qué cambió
- **Antes** el escaneo guardaba automáticamente en la colección. **Ahora** el usuario elige en la ficha entre "Digitalizar", "Guardar en colección" o "Cancelar".
- **Web:** `src/services/coleccion.js` → `agregarColeccion(idEscaneo)` = `POST /api/v1/coleccion` con `{ idEscaneo }`.
- **Móvil:** `PlantiaApi.agregarColeccion(@Body body: Map<String, Int>)` y `CollectionRepository`.
- **Virtualización:** la colección web usa `@tanstack/react-virtual`, que solo dibuja los elementos visibles. El móvil usa `LazyVerticalGrid`, que ya virtualiza por sí mismo.
- **Idempotencia:** guardar dos veces la misma especie no duplica. `ColeccionService.agregar` busca `findByIdUsuarioAndIdCatalogo` y, si existe, suma `veces_vista`.

### Detalle del servidor
`EscaneoService.registrar` lo documenta con un comentario: *"Ya no insertamos automáticamente en colección - eso lo hace el endpoint dedicado"*. `ScanResultDto.enColeccion` siempre es `false`.

---

## FUN-03 — Planta pixel art con 7 etapas

| Campo | Valor |
|---|---|
| Fecha | 10-nov-2026 (después lo amplió Johan) |
| Commits | `daac8ed`, `3b41270` — anexos [daac8ed](anexos/2026-11-10-daac8ed.md), [3b41270](anexos/2026-11-11-3b41270.md) |

### Primera versión (`daac8ed`)
`src/pages/user/jardin/etapas.js` definía:

```js
export const ETAPAS = [
  { nombre: 'Semilla', desde: 0 },
  { nombre: 'Brote', desde: 2 },
  { nombre: 'Planta joven', desde: 5 },
  { nombre: 'Planta adulta', desde: 9 },
  { nombre: 'Con frutos', desde: 14 },
  { nombre: 'Cosecha', desde: 20 },
]
export const SALUD_MARCHITA = 30          // salud < 30 % → marchita (séptima etapa)
export const estaMarchita = (planta) => planta.healthPercent < SALUD_MARCHITA
```

`etapaDe(planta)` devuelve `{ indice, nombre, siguiente, faltan }`. El jardín lo usa para el aviso "¡creció! Ahora es: …" (también en el código de abonos de ECO-02).

### Evolución
- `3b41270`: sprites de 32×32 según la `apariencia` de la IA (ver IA-06).
- Johan: fases en el servidor (`CrecimientoPlanta.faseSegun(dias, diasTotales)`, que usa `PlantasService.subirFase`), 9 formas y arte en tiras de fotogramas.

> **Ojo al depurar:** la etapa que se dibuja la calcula la web con `etapas.js` a partir de `crecimiento` y `healthPercent`, mientras que `etapa_crecimiento` (la fase) la calcula el servidor con `diasCuidados`. Si no coinciden, revisa ambos lados.

---

## FUN-04 — Fotos reales en la colección y en escaneos recientes

| Campo | Valor |
|---|---|
| Fecha | 17-nov-2026 |
| Commits | `781648b` (backend), `4cae3a8` (móvil) — Johan |

- **Contrato:** `RecentScanDto` tiene un componente nuevo, `fotoUrl`. `ContratoMovilTest` lo fija.
- **`ColeccionService.agregar(userId, idCatalogo, escaneo)`:** guarda `idEscaneo` y `fotoUrl`. Si la planta ya estaba, la foto se **reemplaza** por la más nueva.
- **`ColeccionService.fotoDe(c)`:** si la fila no tiene foto, usa la del escaneo más reciente de esa especie (`findFirstByIdUsuarioAndIdCatalogoOrderByFechaEscaneoDesc`).
- **V26:** rellena las fotos que faltaban con `SELECT DISTINCT ON (id_usuario, id_catalogo) ... ORDER BY ... fecha_escaneo DESC`.
- **Móvil:**
  - `MiniaturaEscaneo.kt` dibuja la foto;
  - `normalizarUrlLocal` cambia `localhost` por el host real, porque el servidor guarda URLs con `http://localhost:8080/files/...` (valor por defecto de `app.storage.public-base-url`).

> **Problema de despliegue:** las URLs de foto guardadas en la BD llevan el host (`http://localhost:8080/files/...`). Si cambia el dominio, todas las fotos antiguas apuntan al host viejo. Lo correcto sería guardar solo el nombre (`scan_....jpg`) y montar la URL al responder. Es deuda técnica (ver SEG-09e).

---

## FUN-05 — Página `/demo-planta`

| Campo | Valor |
|---|---|
| Commit | `7f34c61` — [anexo](anexos/2026-11-15-7f34c61.md) |
| Archivos | `frontend-react/src/pages/user/DemoPlanta.jsx` (18 líneas), `src/routes/rutas.js`, `src/App.jsx` |

Página de demostración de la planta animada (FUN-01), aportada por compañeros y commiteada con sus cambios. No necesita datos del servidor.
