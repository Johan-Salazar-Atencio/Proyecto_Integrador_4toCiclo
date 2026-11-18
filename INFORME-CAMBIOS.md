# Informe técnico de los últimos cambios — PlantIA

**Fecha:** 18 de noviembre de 2026
**Rama local:** `main` en `96ff15a`
**Rama remota de referencia:** `origin/main` en `e1f499b` (respaldo: `respaldo/jose-2026-11-18`)
**Alcance:** todo lo que cambió desde la auditoría de seguridad (11 nov) hasta hoy, con el detalle de código, variables, migraciones y pruebas.

---

## 0. Resumen ejecutivo

| # | Bloque | Commits | Autor | Estado en GitHub |
|---|---|---|---|---|
| A | Correcciones de CI, errores HTTP e IA (11–15 nov) | `b1f279f`, `f32c297`, `a519198`, `b23f9fb`, `7f34c61` | jose | **Subido** (PR #2 y PR #1 fusionados en `main`) |
| B | Rediseño “Esmeralda y Lima”, abonos de 3 tipos y fotos en colección | 37 commits de `rama-de-johan` (`781648b` … `d2034ba`) | Johan Salazar Atencio | En `origin/rama-de-johan`; integrado en `integracion/rediseno-johan` (`0200c35`) |
| C | Fusión del rediseño sobre `main` | `0200c35` | jose | Subido como rama `integracion/rediseno-johan`; **no** en `origin/main` |
| D | Web: elegir entre los tres abonos | `76da9a7` | jose | **Solo local** |
| E | Economía: evitar el doble gasto | `3033a0d` | jose | **Solo local** |
| F | Escaneo: no crear especies falsas ni pagar escaneos dudosos | `96ff15a` | jose | **Solo local** |

Estado de `main` local frente a `origin/main`: **41 commits por delante** (37 de Johan + la fusión + 3 correcciones). Nada se ha empujado a `origin/main`.

Migraciones nuevas: **V26** (Johan), **V27** y **V28** (jose). La BD local está en la versión **v28**.

Pruebas: **186 tests de Spring** pasan con Java 21 (`jdk-21.0.13.0.3`), 0 fallos y 0 errores.

---

## 1. Bloque A — Correcciones ya subidas (11–15 nov)

### A.1 `b1f279f` — bcrypt en requirements de Django

- **Archivo:** `backend-django/requirements.txt` (+1 línea: `bcrypt==5.0.0`).
- **Causa:** `admin_views` importa `bcrypt` para validar la contraseña del admin, pero el paquete no estaba declarado. En CI fallaba con `ModuleNotFoundError: No module named 'bcrypt'`.

### A.2 `f32c297` — Los tests de la IA no dependen del `.env` local

- **Archivo:** `backend-django/ia_engine/tests.py`.
- **Causa:** los tests de la cadena de respaldo solo fijaban `WEBUI_API_KEY`. `WebuiProvider` también exige `WEBUI_BASE_URL`, que en local venía del `.env` y en CI no existía.
- **Cambio:** los `override_settings` de esos tests incluyen ahora `WEBUI_BASE_URL="http://webui.test"`.

### A.3 `a519198` — Los errores 4xx de Spring MVC ya no salen como 500

- **Archivos:** `config/ManejadorErrores.java`, `config/ManejadorErroresTest.java` (+52 / −1).
- **Causa:** el `@ExceptionHandler(Exception.class)` genérico atrapaba también las excepciones propias de Spring (método no permitido, ruta inexistente, `Content-Type` no soportado) y respondía `500 "Error interno del servidor"`.
- **Cambio en `inesperado(Exception e)`:**

```java
if (e instanceof ErrorResponse er && er.getStatusCode().is4xxClientError()) {
    HttpStatusCode codigo = er.getStatusCode();
    String mensaje = er.getBody().getDetail() != null ? er.getBody().getDetail() : "Error " + codigo.value();
    return ResponseEntity.status(codigo).body(Map.of("error", mensaje));
}
```

- **Nuevo manejador** `peticionMalFormada` para `HttpMessageNotReadableException` (JSON mal formado) y `TypeMismatchException` (por ejemplo `"abc"` donde va un id). Ambos responden **400** con `{"error": "Peticion mal formada"}`.
- **Tests:**
  - `metodoNoPermitidoDa405yNo500`
  - `rutaInexistenteDa404`
  - `errorPropioSigueSiendo500`

### A.4 `b23f9fb` — Spring usaba el constructor vacío de `IaClient`

- **Archivo:** `services/IaClient.java` (+2).
- **Síntoma:** ningún escaneo identificaba la planta. Siempre devolvía “Planta desconocida” y confianza 0.
- **Causa:** `IaClient` tiene dos constructores: el real y uno `protected` sin argumentos, pensado para los dobles de los tests. Como ninguno estaba anotado, Spring elegía el vacío. El campo `restClient` quedaba en `null`, así que cada escaneo lanzaba `NullPointerException` y caía en `IaClient.fallback()` sin llegar nunca a Django.
- **Cambio:**

```java
@Autowired // hay un segundo constructor (para tests): sin esto Spring usaria el vacio
public IaClient(RestClient.Builder builder, ...)
```

### A.5 `7f34c61` — Timeouts de la IA, IA local institucional y demo

Commit que reúne cambios de compañeros que estaban sin commitear. Solo se incluyó código: se dejaron fuera logs, scripts `run_*`, `test_gemini.py`, `.idea` y `CLAUDE.md`.

**`backend-django/ia_engine/providers/webui_provider.py`** (IA local del instituto, OpenWebUI):

| Variable | Antes | Ahora | Motivo |
|---|---|---|---|
| `_INTENTOS` | `3` | `2` | Menos espera acumulada si el servidor falla |
| `_WEBUI_CONECTAR` | — | `1.0` s | Fuera de la red del instituto el host no existe; se detecta al conectar y se pasa enseguida a Gemini |
| `_WEBUI_TIMEOUT` | `timeout=3.0` fijo | `15.0` s | La identificación real (especie, cuidados y apariencia) tarda unos 5 s; antes se cortaba siempre |
| Cliente OpenAI | `timeout=3.0` | `timeout=httpx.Timeout(_WEBUI_TIMEOUT, connect=_WEBUI_CONECTAR)` | Separa el tiempo para conectar del tiempo para responder |
| Espera entre reintentos | `2 * (intento + 1)` s | `0.5 * (intento + 1)` s | — |

Además, la llamada se ejecuta en un hilo (`threading.Thread`, `daemon=True`) con `thread.join(timeout=_WEBUI_TIMEOUT)`. Los métodos auxiliares son `_thread_target(fn, args, result_holder)` y `_do_chat_wrapper(self, mensajes)`. El resultado vuelve en `result_holder["value"]` o `result_holder["error"]`.

> **Observaciones del revisor:**
> 1. Si la petición se cuelga, el hilo `daemon` sigue vivo en segundo plano hasta que `httpx` corta por su lado. No bloquea al usuario, pero deja hilos huérfanos mientras dura la petición.
> 2. El mensaje de timeout usa `f"WebUI no respondi{chr(111) if intento % 2 else 'ó'}"`, que escribe “respondio” o “respondió” según el intento. Conviene simplificarlo.

**`backend-spring-boot/.../IaClient.java`:** nuevo parámetro `@Value("${app.ia.timeout-seconds:60}") int timeoutSeconds`. El `RestClient` usa un `SimpleClientHttpRequestFactory` con ese timeout. En `application.yml` se añadió la propiedad, configurable con la variable de entorno `IA_TIMEOUT`.

**Otros cambios del commit:**
- `local_provider.py`: un ajuste menor.
- `frontend-react/src/pages/user/DemoPlanta.jsx`: página nueva, con su ruta en `rutas.js` y `App.jsx` (`/demo-planta`).
- `mobile-kotlin/.../PlantaVectorAnimada.kt`: ajustes de la animación.

**Configuración local, fuera de git:** en `.env` se cambió `GEMINI_MODEL=gemini-flash-lite-latest`, porque `gemini-flash-latest` respondía 503/504 por saturación.

### A.6 Integración en GitHub

- PR #2 (`fix/auditoria-seguridad` → `feat/flujo-escaneo-guardado-virtualizacion`) fusionado con `--merge`.
- PR #1 (`feat/flujo-escaneo-guardado-virtualizacion` → `main`) fusionado con `--merge`.
- No se borró ninguna rama.

---

## 2. Bloque B — Rediseño de Johan (`rama-de-johan`, 37 commits, 17 nov)

**Volumen:** 164 archivos, +5 793 / −3 751 líneas.
- Backend y BD: 13 archivos (+162 / −28).
- Web y móvil: 150 archivos (+5 629 / −3 721).

### B.1 Backend Spring — `781648b` “abonos de medio día, 1 y 3 días y foto del escaneo”

#### Migración `db/migration/V26__abonos_y_fotos.sql`

```sql
ALTER TABLE items_tienda ADD COLUMN medios_dias INT NOT NULL DEFAULT 2 CHECK (medios_dias BETWEEN 1 AND 24);
ALTER TABLE plantas_usuario ADD COLUMN medio_dia_pendiente BOOLEAN NOT NULL DEFAULT FALSE;
```

- El crecimiento se mide en **medios días**: `1` es medio día, `2` un día y `6` tres días.
- El abono que ya existía pasa a `codigo = 'ABONO_1'` y `medios_dias = 2`.
- Se insertan dos abonos nuevos que reutilizan el `asset_url` del existente:

| `codigo` | `nombre` | `precio_monedas` | `medios_dias` | Efecto |
|---|---|---|---|---|
| `ABONO_MEDIO` | Abono suave | 15 | 1 | +½ día |
| `ABONO_1` | (el que ya existía) | sin cambio | 2 | +1 día |
| `ABONO_3` | Abono reforzado | 80 | 6 | +3 días |

- **Relleno de fotos:** las filas de `coleccion_usuario` con `foto_url IS NULL` toman la foto del escaneo más reciente de esa especie. Usa `SELECT DISTINCT ON (id_usuario, id_catalogo) … ORDER BY … fecha_escaneo DESC`.

#### Modelos

- `ItemTienda.mediosDias`: `@Column(name = "medios_dias") Integer`, valor por defecto `2`.
- `PlantaUsuario.medioDiaPendiente`: `@Column(name = "medio_dia_pendiente", nullable = false) Boolean`, valor por defecto `false`. Su getter es `isMedioDiaPendiente()` y devuelve `Boolean.TRUE.equals(...)`.

#### `PlantasService.sumarMediosDias` (método estático nuevo)

```java
static void sumarMediosDias(PlantaUsuario p, int medios, int diasTotales) {
    int total = medios + (p.isMedioDiaPendiente() ? 1 : 0);
    int nuevos = Math.min(diasTotales, valor(p.getDiasCuidados()) + total / 2);
    p.setDiasCuidados(nuevos);
    p.setMedioDiaPendiente(total % 2 == 1 && nuevos < diasTotales);
    subirFase(p, diasTotales);
}
```

Ejemplo: dos abonos suaves seguidos (1 + 1) suman un día, y el medio día suelto se guarda en `medio_dia_pendiente`. Al llegar a `diasTotales` no sobra nada.

#### `AbonoService.abonar`

- Nueva firma: `abonar(Integer userId, Integer idPlantaUsuario, Integer idItem)`, más la sobrecarga con `LocalDate hoy` para los tests.
- Si `idItem == null`, usa el más barato: `itemRepo.findFirstByTipoAndActivoTrueOrderByPrecioMonedasAsc("ABONO")`.
- Si no, usa `itemRepo.findById(idItem)` filtrado a `tipo == "ABONO"` y `activo == true`.
- Si no encuentra el abono: `404 "Ese abono no esta en la tienda"`.
- Para crecer llama a `PlantasService.sumarMediosDias(p, valor(abono.getMediosDias()), total)`. Antes usaba `sumarDia`.

#### `PlantasController` — `POST /api/v1/plantas/{id}/abono-tienda`

- Nuevo cuerpo opcional: `@RequestBody(required = false) Map<String, Integer> body` con la forma `{ "idItem": n }`.
- Sin cuerpo se usa el abono más barato, lo que mantiene la compatibilidad con clientes antiguos.
- Códigos de respuesta: 409 si no corresponde abonar y 402 si faltan monedas.

#### Fotos en la colección y el historial

- `RecentScanDto` tiene un campo nuevo, `String fotoUrl`. **Cambia el contrato con el móvil**; `ContratoMovilTest` se actualizó para reflejarlo.
- `EscaneoService.recientes` llena ese campo con `e.getImagenFotoUrl()`.
- `ColeccionService.agregar(userId, idCatalogo, EscaneoHistorial escaneo)` guarda `idEscaneo` y `fotoUrl`. Si la planta ya estaba, suma una vista y la reemplaza por la foto más nueva. Antes guardaba `null`.
- Nuevo método privado `fotoDe(ColeccionUsuario c)`: si falta la foto, busca `escaneoRepo.findFirstByIdUsuarioAndIdCatalogoOrderByFechaEscaneoDesc(...)`, un método nuevo de `EscaneoHistorialRepository`.
- `ColeccionController` pasa el `escaneo` completo a `agregar`.
- **Tests:** `AbonoServiceTest` (+55 / −8 líneas) cubre los tres tipos de abono y el medio día pendiente.

### B.2 Web (React) — rediseño “Esmeralda y Lima”

#### Paleta nueva en `src/styles/tema.css`

Variables CSS en `:root` para el modo claro y en `prefers-color-scheme: dark` para el oscuro:

| Token | Claro | Oscuro | Uso |
|---|---|---|---|
| `--fondo` | `#f6faf7` | `#0b1512` | Fondo de página |
| `--superficie` | `#ffffff` | `#121f1a` | Tarjeta |
| `--superficie-2` | `#eaf3ec` | `#1a2b24` | Tarjeta elevada y campos |
| `--linea` | `#d5e4da` | `#243830` | Bordes |
| `--texto` / `--texto-suave` | `#10261d` / `#5a7164` | `#e8f3ec` / `#8fa89b` | Texto |
| `--primario` | `#15803d` (esmeralda) | `#a3e635` (lima) | Botones y acentos |
| `--sobre-primario` | `#ffffff` | `#12200a` | Texto sobre el primario |
| `--lima` / `--sobre-lima` | `#a3e635` / `#12200a` | — | Botón central y chips |
| `--agua` | `#0284c7` | `#38bdf8` | Riego |
| `--luz` | `#d97706` | `#fbbf24` | Sol |
| `--abono` | `#9333ea` | `#c084fc` | Abono |
| `--evento` | `#ea580c` | `#fb923c` | Desastres |
| `--peligro` | `#e11d48` | `#fb7185` | Acciones destructivas |
| `--radio` / `--radio-chico` | `24px` / `14px` | — | Bordes redondeados |

Los nombres antiguos (`--verde`, `--ambar`, `--azul`, `--rojo` y sus variantes `-claro`) se mantienen como alias de los nuevos para no romper el CSS existente.

#### Pantallas rediseñadas

- **Inicio:**
  - Archivos: `Home.jsx`, `TarjetaUsuario.jsx`, `TarjetaPlanta.jsx`, `MisionDiaria.jsx`, `AlertaClima.jsx`.
  - Nuevos componentes: `ModalMisiones.jsx` (misiones en una ventana) y `SinEvento.jsx` (aviso cuando no hay evento).
  - Nuevo `xp.js` con `porcentajeXp(usuario)`, que calcula el avance a partir de `experienceFromLevel` y `experienceToNextLevel`, e `iniciales(nombre)` (por ejemplo, “Johan Demo” da “JD”).
- **Colección:** `Coleccion.jsx` y `ColeccionVirtual.jsx`, con pestañas, buscador, invernadero con columna lateral y baúl. Nuevo `Iconos.jsx`.
- **Escanear:** `Escaneo.jsx` y `EsperaEscaneo.jsx`, con zona de subida grande, consejos y escaneos recientes con foto.
- **Tienda:** `Tienda.jsx` con categorías en chips y cuadrícula de 4 columnas. Nuevo `Icono.jsx`.
- **Perfil:** `Perfil.jsx` en dos columnas, con anillo de XP, ajustes agrupados y cierre de sesión con estilo de peligro.
- **Misiones:** `MisionesUsuario.jsx`, con un componente nuevo `MisionFila.jsx`.
- **Historial y catálogo:** cuadrícula.
- **Acceso, bienvenida, términos, jardín y ficha:** solo cambian colores y tarjetas.

#### Panel de administración

- Menú lateral flotante y nuevo acceso al panel (`LoginAdmin.jsx`). El ojo de la contraseña pasa de emoji a icono.
- **Dashboard:** indicadores, gráfico y escaneos en filas.
- **Eventos:** se unen eventos y tipos de evento en una sola sección.
  - Nuevos componentes: `ModalLanzar.jsx`, `TarjetaTipo.jsx` y `util.js`.
  - Se eliminan `TiposEventoAdmin.jsx` y `VistaNotificacion.jsx`.
  - Nueva constante `AVISO_EVENTOS = 'plantia:eventos'` en `services/admin/eventos.js`. Las páginas la emiten en `window` al lanzar o cancelar un evento, y así se refresca la insignia del menú.
- **Misiones, tienda, catálogo y economía:** formularios en panel lateral e interruptor activo/inactivo.
  - Las piezas compartidas viven en `misiones/kit.jsx` y `kit.css`.
  - `services/admin/catalogo.js` mapea el campo nuevo `imagen` desde y hacia `sprite_base_url`.
- **`e0c6f18`:** se quitan los botones de eliminar del panel. Ahora todo se activa o desactiva con su interruptor, un borrado lógico que evita perder datos.

### B.3 Móvil (Kotlin, Compose)

- **Paleta:** `ui/theme/Color.kt` y `Theme.kt` usan los mismos valores que la web:
  - Claro: `PrimarioClaro = 0xFF15803D`, `BackgroundLightClaro = 0xFFF6FAF7`, …
  - Oscuro: `PrimarioOscuro = 0xFFA3E635`, `ColorDarkBackground = 0xFF0B1512`, …
  - Se mantienen alias como `GreenLightClaro` para el código antiguo, y hay un nuevo `LocalTemaOscuro = staticCompositionLocalOf { false }`.
- **Estilo común:** `ui/components/Estilo.kt`, con tarjetas de 24 dp, borde de 1 dp y brillo según su significado. También hay un nuevo `MiniaturaEscaneo.kt`.
- **Barra inferior:** `BarraInferior.kt`, flotante y con un botón central lima.
- **Inicio:** `UserHeaderCard`, `PlantSummaryCard`, `EventoCard`, `MisionesResumenCard` y el nuevo `SinEventoCard`.
  - `SinEventoCard` muestra un carrusel con las imágenes `res/drawable-nodpi/sin_evento_1..4.webp`.
  - Se recorta `DailyMissionCard` (−115 líneas).
- **Jardín y abonos:**
  - Nueva hoja inferior `HojaAbonos.kt`. Cada abono muestra su precio, o “🔒” si está bloqueado. La variable `alcanza = hojas == null || hojas >= abono.precio` marca si el usuario tiene saldo suficiente.
  - `GardenViewModel`:
    - `abono: TiendaItem?` pasa a `abonos: List<TiendaItem>`.
    - Estados nuevos: `hojas: Int?` y `eligiendoAbono: Boolean`.
    - Funciones nuevas: `abrirAbonos()`, `cerrarAbonos()`, `usarAbono(abono, alTerminar)`, `crecio(antes, despues)` (devuelve “ganó medio día”, “un día” o “N días”) y `renombrar(apodo, alTerminar)`, que permite editar el apodo desde el jardín.
- **Datos:**
  - `Tienda.abonos` devuelve la lista de abonos ordenada por precio; `abono` pasa a ser `abonos.firstOrNull()`.
  - `PlantiaApi.abonoTienda(@Path("id") idPlanta: Int, @Body body: Map<String, Int>)` envía el cuerpo siempre, `{}` si no se elige abono.
  - `TiendaRepository.usarAbono(idPlanta, idItem: Int? = null)`.
  - `RecentScanDto` y `RecentScan` tienen un campo nuevo, `fotoUrl: String? = null`. `Mappers.kt` lo normaliza con `normalizarUrlLocal`.
- **Escáner:**
  - `ScanScreen.kt` (+106 / −205 líneas) pasa a cámara completa con consejos, marco lima y obturador.
  - Se simplifican `ScanControlsBar`, `ScanOverlay` y `FlashToggleButton`.
- **Perfil:** `PerfilScreen.kt` (+207 / −168 líneas) con anillo de XP.
  - `EditarDatosScreen` deja el correo en solo lectura.
  - `EditarDatosViewModel` pierde la lógica de cambiar el correo, y su test se ajusta.
- **Colección:** se abre en la pestaña de escaneos y se quita la “enciclopedia”.
- **`d2034ba` (Android 17):**
  - `AndroidManifest.xml` declara `android.permission.ACCESS_LOCAL_NETWORK`.
  - `MainActivity.pedirPermisoRedLocal()` lo pide solo si `Build.VERSION.SDK_INT >= 37`.
  - Al concederlo, el callback `pedirRedLocal` llama a `RetrofitClient.reconectar()`.
  - Sin este permiso, Android 17 bloquea `10.0.2.2`, `192.168.x.x` y `10.x`, así que la app no llegaba al backend del PC.

---

## 3. Bloque C — `0200c35` Fusión del rediseño sobre `main`

- **Antes de fusionar:**
  - Se empujó el estado previo a `origin/respaldo/jose-2026-11-18` (`e1f499b`).
  - Se guardaron los archivos locales sin commitear en `respaldos/respaldo-local-sin-commitear.tgz`.
- **Fusión:** `git merge` sin conflictos. Se creó y empujó la rama `integracion/rediseno-johan`. Después, `main` local avanzó en *fast-forward* hasta ella, sin push a `origin/main`.
- **Comprobaciones tras la fusión:**
  - Las correcciones de la auditoría siguen presentes: `@Autowired` en `IaClient`, los 4xx de `ManejadorErrores`, los timeouts de WebUI, el hash de los tokens de sesión y la comprobación de dueño en `/plantas`, `/coleccion` y `/escaneos`.
  - Arte verificado:
    - 5 temas de fondo × día/tarde/noche, en tiras de 96 fotogramas.
    - 4 desastres, cada uno con aviso de 4 fotogramas y pico de 8.
    - Sprites de 32×32 en 9 formas y 7 etapas.
    - Las **63 URLs** de imágenes responden 200.
    - Hoja de contacto: `respaldos/muestra-arte.png`.

---

## 4. Bloque D — `76da9a7` Web: elegir entre los tres abonos (local)

**Problema:** tras V26 la tienda tiene tres abonos, pero el jardín web hacía `t.items.find(i => i.tipo === 'ABONO')`. Así tomaba solo el primero, no enviaba `idItem` y el aviso decía siempre “+1 día de crecimiento”, aunque fuera medio día o tres.

**Archivos:** `Jardin.jsx`, `jardin.css` y `services/tienda.js` (+43 / −17).

### `src/services/tienda.js`

```js
// antes
export const comprarAbono = (idPlanta) => llamar(`/plantas/${idPlanta}/abono-tienda`, { metodo: 'POST' })
// ahora
export const comprarAbono = (idPlanta, idItem) =>
  llamar(`/plantas/${idPlanta}/abono-tienda`, { metodo: 'POST', cuerpo: idItem ? { idItem } : undefined })
```

### `src/pages/user/jardin/Jardin.jsx`

- **Estado:** `const [abono, setAbono] = useState(null)` pasa a `const [abonos, setAbonos] = useState([])`.
- **Carga:** `t.items.filter((i) => i.tipo === 'ABONO').sort((a, b) => a.precio - b.precio)`. Si falla, el estado queda en `[]`.
- **Función nueva:**

```js
function diasGanados(antes, despues) {
  const dias = (despues.diasCuidados || 0) - (antes.diasCuidados || 0)
  if (dias <= 0) return 'ganó medio día'
  return dias === 1 ? 'ganó un día' : `ganó ${dias} días`
}
```

- **`abonarConTienda(abono)`:** recibe el abono elegido, llama a `comprarAbono(estado.planta.id, abono.id)` y avisa con `` `${abono.nombre} aplicado: ${planta.name} ${diasGanados(estado.planta, planta)} 🌿` ``. Si la planta sube de etapa, el aviso sigue siendo “¡creció! Ahora es: …”.
- **Interfaz:** un contenedor `.jardin-abonos` con título “🌿 Abonos · máximo 1 al día” y un botón `.jardin-abono` por abono, con:
  - `nombre`;
  - `descripcion` (`.jardin-abono-desc`);
  - el precio, o “🔒 Nivel N” si está bloqueado.

  El botón queda deshabilitado si `cuidando || abono.bloqueado`, y su `title` explica en qué nivel se desbloquea.

### `jardin.css`

```css
.jardin-abonos { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; }
.jardin-abonos-titulo { grid-column: 1 / -1; ... }
.jardin-abono-desc { font-size: 0.75rem; line-height: 1.3; opacity: 0.8; }
@media (max-width: 480px) { .jardin-abonos { grid-template-columns: 1fr; } }
```

**Verificación:** en el navegador se ven los tres abonos con su precio, y el aviso muestra el crecimiento correcto.

---

## 5. Bloque E — `3033a0d` Economía: evitar el doble gasto (local)

### Problema (condición de carrera)

`TiendaService.comprar`, `AbonoService.abonar`, `PlantasService.revivir` y `ProgresoService` seguían esta secuencia:

1. Leer el usuario (`findById`).
2. Comprobar `monedas >= precio`.
3. Restar.
4. Guardar (`save`).

No había bloqueo. Dos peticiones simultáneas leían el mismo saldo y las dos pasaban la comprobación.

**Prueba de carrera** (`scratchpad/carrera.py`): 16 hilos compran a la vez los artículos 10 y 11 para lucia, que tiene 150 monedas. Antes de la corrección:

| Métrica | Antes | Después |
|---|---|---|
| Intentos en los que se llevó **ambos** artículos pagando uno | **5 de 5** | **0 de 5** |
| Compras aceptadas por intento | 2 | 1, cobrada correctamente |
| Respuestas por intento | 2×200 + errores | 1×200, 8×402, 7×409 |
| Errores 500 en el log | 32 | 0 |

### Solución 1: bloqueo pesimista del usuario

`repositories/UsuarioRepository.java`:

```java
@Query(value = "SELECT id_usuario FROM usuarios WHERE id_usuario = :id FOR UPDATE", nativeQuery = true)
Integer bloquear(@Param("id") Integer id);
```

Se llama como **primera instrucción** de cada transacción que mueve monedas o XP, antes de cargar el usuario. Así la segunda transacción espera a que termine la primera y lee el saldo ya actualizado.

| Servicio | Método | Línea añadida |
|---|---|---|
| `TiendaService` | `comprar` | `usuarioRepo.bloquear(userId); // antes de leer saldo e inventario` |
| `AbonoService` | `abonar(userId, idPlanta, idItem, hoy)` | `usuarioRepo.bloquear(userId); // evita pagar o abonar dos veces` |
| `PlantasService` | `revivir` | `usuarioRepo.bloquear(userId); // evita revivir dos veces pagando una` |
| `ProgresoService` | `aplicar(...)`, que cubre `otorgar`, `registrarAccion` y `otorgarSinRacha` | `usuarioRepo.bloquear(userId); // premios simultaneos no se pisan` |

Proteger `ProgresoService.aplicar` también evita que dos premios simultáneos se salten un tope diario, porque `topeAlcanzado` cuenta el historial dentro de la misma transacción bloqueada.

**Compatibilidad con los tests:** con Mockito, `bloquear` devuelve `null` y no tiene efecto, así que los tests existentes no cambian.

### Solución 2: migración `V27__monedas_no_negativas.sql`

```sql
UPDATE usuarios SET monedas = 0 WHERE monedas IS NULL OR monedas < 0;
ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_monedas_no_negativas CHECK (monedas >= 0);
```

Es la última defensa: aunque una ruta futura olvidara el bloqueo, la BD rechazaría un saldo negativo. **Verificado:** `UPDATE usuarios SET monedas = -5` falla con `ck_usuarios_monedas_no_negativas`.

### Solución 3: 409 en lugar de 500

`ManejadorErrores.conflictoDeDatos(DataIntegrityViolationException e)`:
- registra `log.warn("Restriccion de la BD: {}", e.getMostSpecificCause().getMessage())`;
- responde **409** con `{"error": "La operacion choca con datos existentes; vuelve a intentarlo"}`.

Cubre el índice único `ux_inventario_usuario_item` (comprar dos veces el mismo artículo) y el `CHECK` de V27.

**Test nuevo:** `ManejadorErroresTest.choqueConRestriccionDeLaBdDa409`.

### Verificación posterior

- Spring aplicó V27 al arrancar.
- `GET` de `/plantas`, `/coleccion`, `/tienda`, `/misiones` y `/usuarios/me` responden 200. El riego, que da premio bajo el bloqueo, también.
- lucia quedó restaurada: 150 monedas y sin los artículos de la prueba.

---

## 6. Bloque F — `96ff15a` Escaneo: no crear especies falsas (local)

### Problema

`EscaneoService.registrar` consideraba identificado cualquier resultado con `confianza > 0` y un nombre distinto de “desconocido”, “unknown”, “no identificable” o “no identificado”. Las respuestas genéricas de la IA pasaban el filtro:

| `id_catalogo` | `nombre_comun` | `nombre_cientifico` | Confianza |
|---|---|---|---|
| 8 | Planta verde desconocida | `Plantae indet.` | 10 % |
| 9 | Planta verde desconocida | `Plantae sp.` | 10 % y 50 % |
| 10 | Planta verde genérica | `Plantae generica` | 50 % |

Cada una creaba una especie en `plantas_catalogo` y pagaba `ESCANEO_NUEVA` (**70 XP y 20 hojas**), que además **no tenía tope diario**. Con fotos vagas se podía cobrar el premio de especie nueva una y otra vez.

### Solución en `services/EscaneoService.java`

El bloque en línea se sustituye por `boolean identificado = esIdentificado(ia);` y se añade:

```java
static final double CONFIANZA_MINIMA = 50.0;   // escala 0-100, igual que confianza_ia

static boolean esIdentificado(IaResultDto ia) {
    if (ia.confianza() == null || ia.confianza() < CONFIANZA_MINIMA) return false;
    String nombre = ia.nombreCientifico() == null ? "" : ia.nombreCientifico().trim().toLowerCase();
    if (nombre.isBlank() || !nombre.matches(".*[a-z].*")) return false;
    return !nombre.startsWith("plantae")
        && !nombre.startsWith("planta ")
        && !nombre.matches(".*\\b(desconocid[ao]|unknown|indet\\.?|indeterminad[ao]|gen[eé]ric[ao]|no identificad[ao]|no identificable)\\b.*");
}
```

- **Umbral de 50:** en los datos reales, las especies verdaderas llegan con 70–99 % y las genéricas con 10–50 %. El nombre filtra los casos de 50 %, como `Plantae sp.`.
- **Géneros sin especie:** “Ficus sp.” **sí** pasa, porque un género real es información útil. Solo se rechaza el nivel de reino (`Plantae…`) y los marcadores de “no sé”.
- **Si no está identificado:** el escaneo se guarda igualmente, con `id_catalogo = null` y la confianza real. No se crea especie, no se paga y la web y el móvil lo muestran como “Sin identificar”. El resto del flujo (`especieNueva`, `progresoService.otorgar`, `ScanResultDto`) no cambia.

### Migración `V28__tope_escaneo_nueva.sql`

```sql
UPDATE reglas_progreso SET tope_diario = 5 WHERE motivo = 'ESCANEO_NUEVA' AND tope_diario IS NULL;
```

Como máximo se pagan 5 especies nuevas al día. `ProgresoService.topeAlcanzado` ya aplica `tope_diario` contando `historial_progreso` del día, ahora bajo el bloqueo del bloque E. Reglas actuales de escaneo:

| `motivo` | XP | Hojas | `tope_diario` |
|---|---|---|---|
| `ESCANEO` | 20 | 0 | — (no se usa en el flujo actual) |
| `ESCANEO_NUEVA` | 70 | 20 | **5** (antes sin tope) |
| `ESCANEO_REPETIDO` | 10 | 3 | 3 |

### Test nuevo

`EscaneoServiceTest.soloUnaEspecieConcretaConConfianzaSuficienteCuentaComoIdentificada`, con 11 casos:

| Entrada | Esperado |
|---|---|
| `Monstera deliciosa`, 92.5 | identificado |
| `Ficus sp.`, 70 | identificado |
| `Echinocactus grusonii`, 50 (límite) | identificado |
| `Monstera deliciosa`, 10 | rechazado (confianza baja) |
| `Monstera deliciosa`, `null` | rechazado |
| `Plantae sp.`, 50 | rechazado |
| `Plantae indet.`, 90 | rechazado |
| `Plantae generica`, 90 | rechazado |
| `Desconocido`, 90 | rechazado |
| `Especie indeterminada`, 90 | rechazado |
| `"  "`, 90 | rechazado |

### Prueba en vivo (Spring en v28, Gemini `gemini-flash-lite-latest`)

| Foto | Respuesta de la IA | `identificado` | Premio |
|---|---|---|---|
| Dibujo sintético de monstera | `Plantae gen. sp.`, 50 % | `false` | ninguno |
| Foto oscura | `Desconocido`, 0 % | `false` | ninguno |
| Cactus (ya escaneado antes) | 10 % esta vez | `false` | ninguno |
| Tomate real | `Solanum lycopersicum`, 99 % | `true` | 110 XP / 38 hojas (racha + especie nueva + misión) |

**Limpieza tras la prueba:**
- Se borraron los escaneos 41–44, sus 4 fotos en `storage/images/` y los movimientos 36–38 de `historial_progreso`.
- La misión de escaneo de hoy volvió a progreso 0.
- lucia quedó en 150 monedas, 1000 XP, racha 1 y `ultima_actividad = NULL`.

**No se tocó:** las entradas genéricas 8, 9 y 10 siguen en el catálogo, porque se pidió no borrar datos. Ya no se pueden volver a crear ni a cobrar.

---

## 7. Pruebas y cómo reproducirlas

```powershell
# Spring (Java 21; con Java 26 Mockito falla por el entorno, no por el código)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.13.0.3"; mvn test
```

- **Resultado:** 186 tests, 0 fallos y 0 errores. Las clases tocadas son `EscaneoServiceTest` (4 tests), `ManejadorErroresTest` (4) y `AbonoServiceTest` (9).
- **CI (GitHub Actions):** usa Java 17 y pasa en los PR #1 y #2.
- **Prueba de carrera:** `python carrera.py`, en la carpeta temporal de la sesión.

---

## 8. Riesgos y pendientes

| Prioridad | Tema | Detalle |
|---|---|---|
| Alta | Subir a GitHub | `main` local va 41 commits por delante de `origin/main`. V27 y V28 solo existen en local, así que hay que avisar al equipo antes de fusionar, porque cambian el esquema. |
| Media | Contrato móvil | `RecentScanDto.fotoUrl` (V26) es un campo nuevo. Los móviles antiguos lo ignoran, pero conviene publicar la versión nueva de la app junto con el backend. |
| Media | Catálogo sucio | Las especies 8, 9 y 10 (`Plantae …`) siguen visibles. Se pueden desactivar desde el panel admin (interruptor) sin borrarlas. |
| Media | Seguridad (informe crítico) | Contraseñas demo en V6/V9, sin límite de intentos de login, CORS `*`, `AuthService` sin tests, `STORAGE_PUBLIC_URL` y almacenamiento de fotos local. |
| Baja | WebUI | Hilos `daemon` huérfanos si la petición se cuelga y el mensaje con `chr(111)` (ver A.5). |
| Baja | Umbral IA | `CONFIANZA_MINIMA = 50` es una constante en código. Si el equipo quiere ajustarla sin recompilar, se puede pasar a `application.yml`. |
| Baja | README | No describe aún los abonos de 3 tipos, V26–V28 ni el rediseño. |

---

## 9. Lista de archivos tocados por jose en los commits locales (D, E, F)

```
frontend-react/src/pages/user/jardin/Jardin.jsx                            (D)
frontend-react/src/pages/user/jardin/jardin.css                            (D)
frontend-react/src/services/tienda.js                                      (D)
backend-spring-boot/src/main/java/com/plantia/repositories/UsuarioRepository.java   (E)
backend-spring-boot/src/main/java/com/plantia/services/TiendaService.java           (E)
backend-spring-boot/src/main/java/com/plantia/services/AbonoService.java            (E)
backend-spring-boot/src/main/java/com/plantia/services/PlantasService.java          (E)
backend-spring-boot/src/main/java/com/plantia/services/ProgresoService.java         (E)
backend-spring-boot/src/main/java/com/plantia/config/ManejadorErrores.java          (E)
backend-spring-boot/src/test/java/com/plantia/config/ManejadorErroresTest.java      (E)
db/migration/V27__monedas_no_negativas.sql                                  (E)
backend-spring-boot/src/main/java/com/plantia/services/EscaneoService.java          (F)
backend-spring-boot/src/test/java/com/plantia/services/EscaneoServiceTest.java      (F)
db/migration/V28__tope_escaneo_nueva.sql                                    (F)
```

Los archivos sin commitear de otras personas (scripts `run_*`, logs, `.idea`, `CLAUDE.md`) no se incluyeron en ningún commit.
