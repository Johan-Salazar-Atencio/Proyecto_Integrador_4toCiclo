# Bitácora — Fichas de IA, economía, rendimiento y errores HTTP

Mismo formato que las fichas de seguridad. Las líneas citadas son del estado de `main` local en `96ff15a`.

**Índice:**
- **IA:** [IA-01](#ia-01--gemini-saturado-dejaba-la-app-sin-ia-y-el-mock-inventaba-monstera) · [IA-02](#ia-02--spring-usaba-el-constructor-vacío-de-iaclient-y-ningún-escaneo-llegaba-a-la-ia) · [IA-03](#ia-03--la-ia-local-del-instituto-webui-se-cortaba-siempre-al-segundo) · [IA-04](#ia-04--el-modelo-principal-de-gemini-respondía-503504) · [IA-05](#ia-05--especies-falsas-plantae-sp-que-pagaban-premio-de-especie-nueva) · [IA-06](#ia-06--apariencia-de-la-planta-forma-y-colores-que-devuelve-la-ia)
- **Economía:** [ECO-01](#eco-01--doble-gasto-de-monedas-por-condición-de-carrera) · [ECO-02](#eco-02--el-jardín-web-solo-ofrecía-un-abono-y-anunciaba-1-día-siempre) · [ECO-03](#eco-03--abonos-de-medio-día-1-día-y-3-días-medios_dias-y-medio_dia_pendiente) · [ECO-04](#eco-04--mapa-de-dónde-se-ganan-y-se-gastan-hojas-y-xp)
- **Rendimiento:** [REN-01](#ren-01--la-llamada-a-la-ia-dentro-de-una-transacción-agotaba-el-pool-de-conexiones) · [REN-02](#ren-02--consultas-n1-al-cargar-especies-de-una-lista)
- **Errores HTTP:** [ERR-01](#err-01--sin-manejador-global-errores-con-formatos-distintos-y-500-por-datos-inválidos) · [ERR-02](#err-02--los-4xx-propios-de-spring-mvc-salían-como-500) · [ERR-03](#err-03--violaciones-de-restricciones-de-la-bd-salían-como-500)

---

## Cómo viaja un escaneo (contexto para las fichas de IA)

```
Web / Móvil
  │ POST /api/v1/escaneos  (multipart: foto)            X-User-Id + X-Session-Token
  ▼
Spring  EscaneoController → EscaneoService.escanear(userId, foto)        services/EscaneoService.java:68
  1. storage.save(foto)          → URL /files/scan_....jpg       (fuera de transacción)
  2. identificar(foto)           → IaClient.identificar(foto)     (fuera de transacción, hasta 60 s)
        │ POST http://127.0.0.1:8000/api/ia/identificar   multipart a mano + X-Ia-Clave
        ▼
     Django  ia_engine/views.py  (SoloClienteInterno + throttle "ia")
        get_provider_chain(): openai → webui → gemini → local        providers/factory.py
        cada motor devuelve vía normalizar_identificacion(...)        providers/base.py
        ◄── JSON {nombre_comun, nombre_cientifico, confianza, cuidados[], apariencia{...}}
     si falla cualquier cosa → IaClient.fallback() = "Planta desconocida", confianza 0
  3. transaccion.execute(registrar(...))   (transacción corta)       EscaneoService.java:72
        esIdentificado(ia)? → catalogoService.upsertDesdeIa(ia)       (crea o actualiza la especie)
        especieNueva = !escaneoRepo.existsByIdUsuarioAndIdCatalogo(...)
        escaneoRepo.save(escaneo)
        progresoService.otorgar(userId, [ESCANEO_NUEVA|ESCANEO_REPETIDO], ["ESCANEO"], idEscaneo)
  4. si 3 falla → storage.delete(imagenUrl) y se relanza el error   EscaneoService.java:74
  ◄── ScanResultDto {idEscaneo, idCatalogo, nombreComun, nombreCientifico, confianza, imagenUrl,
                     enColeccion=false, cuidados[], identificado, apariencia, progreso}
```

La escala de `confianza` es **de 0 a 100** en todo el sistema: en la IA, en `IaResultDto.confianza` (`Double`) y en la columna `escaneos_historial.confianza_ia` (`NUMERIC(5,2)`).

---

## IA-01 — Gemini saturado dejaba la app sin IA y el *mock* inventaba "Monstera"

| Campo | Valor |
|---|---|
| Fecha | 10-nov-2026 |
| Commit | `7022e9e` — [anexo](anexos/2026-11-10-7022e9e.md) |
| Severidad | Alta (funcional y de confianza del usuario) |
| Archivos | `backend-django/ia_engine/providers/gemini_provider.py`, `providers/factory.py`, `config/settings.py` |

### Síntoma
- A ciertas horas todos los escaneos fallaban o tardaban muchísimo.
- Peor aún: cuando la IA real fallaba, la app respondía **"Monstera deliciosa"** fuera cual fuera la foto. El usuario creía que la IA se equivocaba, cuando en realidad no había respondido.

### Causa raíz
1. El nivel gratuito de Gemini devuelve **503** (modelo saturado) o **429** (cuota). Además, Google **retira** modelos y entonces responde **404**. El proveedor solo probaba un modelo.
2. `factory.py` añadía `MockProvider` al final de la cadena **siempre**, y el *mock* devuelve una Monstera fija. Así, un fallo real quedaba disfrazado de identificación.

### Corrección (estado actual)
`gemini_provider.py`:

```python
_SALTAR_MODELO = {404, 429, 500, 502, 503, 504}
_TIMEOUT_MS = 20000  # por llamada; Spring espera hasta 60 s en total
_PRESUPUESTO_S = 45  # tiempo máximo probando modelos, por debajo del timeout de Spring
_RESPALDOS_DEFECTO = "gemini-flash-lite-latest,gemini-3.5-flash,gemini-3.5-flash-lite"

def _modelos() -> list[str]:
    respaldos = getattr(settings, "GEMINI_FALLBACK_MODELS", None) or _RESPALDOS_DEFECTO
    lista = [settings.GEMINI_MODEL] + [m.strip() for m in respaldos.split(",")]
    return list(dict.fromkeys(m for m in lista if m))       # quita duplicados conservando el orden
```

`GeminiProvider._generar(partes)` recorre `self.modelos`:
- si el error es un `errors.APIError` cuyo `exc.code` está en `_SALTAR_MODELO`, prueba el siguiente modelo;
- si es otro código, lo relanza;
- si es un error de red (timeout), también prueba el siguiente;
- se detiene al superar `_PRESUPUESTO_S`;
- guarda el modelo que respondió en `self.modelo_usado`.

`factory.py` (`get_provider_chain`, al final):

```python
# mock solo cuando no hay ningún motor real. Si los reales fallan se devuelve
# 502: inventar siempre "Monstera" esconde el fallo y engaña al usuario.
if not cadena:
    cadena.append(MockProvider())
```

### Presupuesto de tiempos (importante para no romperlo)

| Capa | Límite | Dónde |
|---|---|---|
| Una llamada a un modelo de Gemini | 20 s | `_TIMEOUT_MS` |
| Todos los modelos de Gemini juntos | 45 s | `_PRESUPUESTO_S` |
| Spring esperando a Django | 60 s | `app.ia.timeout-seconds` / `IA_TIMEOUT` ([IA-03](#ia-03--la-ia-local-del-instituto-webui-se-cortaba-siempre-al-segundo)) |

**Regla:** el presupuesto de Django debe ser **menor** que el timeout de Spring. Si no, Spring corta antes, guarda "Planta desconocida" y Django sigue gastando cuota para nada.

### Circuit breaker
`factory.py` tiene `_COOLDOWN_SEGUNDOS = 60` y el diccionario `_caidos`, protegido por `threading.Lock()`.
- Cuando un motor falla por indisponibilidad (`es_indisponibilidad(exc)` reconoce errores de conexión, timeouts y códigos 429/5xx), `marcar_caido(nombre)` lo salta durante 60 s.
- Un error de "la IA respondió mal" (JSON inválido, por ejemplo) **no** lo marca como caído.

### Si vuelve a pasar
1. Mira `django.log`. Las líneas `Gemini '<modelo>' no disponible (503); probando el siguiente` indican saturación. Si se repiten en todos los modelos, la cuota está agotada.
2. Si los nombres de modelo dan 404, Google los retiró: actualiza `GEMINI_MODEL` o `GEMINI_FALLBACK_MODELS` en `.env` (no hace falta tocar código).
3. Si el escaneo devuelve "Planta desconocida" muy rápido (menos de 1 s), el problema no es Gemini sino que Spring no llega a Django. Ver [IA-02](#ia-02--spring-usaba-el-constructor-vacío-de-iaclient-y-ningún-escaneo-llegaba-a-la-ia) y [SEG-05](10-fichas-seguridad.md#seg-05--el-motor-de-ia-de-django-estaba-abierto-a-cualquiera).

---

## IA-02 — Spring usaba el constructor vacío de `IaClient` y ningún escaneo llegaba a la IA

| Campo | Valor |
|---|---|
| Fecha | 13-nov-2026 |
| Commit | `b23f9fb` — [anexo](anexos/2026-11-13-b23f9fb.md) |
| Severidad | **Crítica** (la función principal de la app no funcionaba) |
| Archivo | `backend-spring-boot/src/main/java/com/plantia/services/IaClient.java:32-33` |

### Síntoma (reportado por el usuario)
> "no identifica la planta, al parecer cae antes del análisis"

Cualquier foto devolvía al instante "Planta desconocida" con confianza 0. Django no registraba ninguna petición.

### Cómo se diagnosticó
1. El log de Django no mostraba `POST /api/ia/identificar`, así que la petición no salía de Spring.
2. El log de Spring tenía `Motor IA no disponible: ...` (escrito por `EscaneoService.identificar`, línea 84), con un `NullPointerException` sobre `restClient`.
3. `IaClient` tenía **dos constructores**:
   - el real, `public IaClient(RestClient.Builder, String baseUrl, String provider, int timeoutSeconds)`;
   - `protected IaClient()`, añadido en `b7c118f` para los dobles de los tests ([SEG-07](10-fichas-seguridad.md#seg-07--inyección-en-la-cabecera-multipart-y-límite-de-72-bytes-de-bcrypt) parte C), que deja `restClient = null`.
4. **Regla de Spring:** si una clase tiene **un solo** constructor, lo usa. Si tiene **varios** y ninguno lleva `@Autowired`, usa el constructor **sin argumentos** si existe. Por eso Spring elegía el vacío.

### Corrección

```java
@Autowired // hay un segundo constructor (para tests): sin esto Spring usaria el vacio
public IaClient(RestClient.Builder builder,
                @Value("${app.ia.base-url:http://localhost:8000}") String baseUrl,
                @Value("${app.ia.provider:auto}") String provider,
                @Value("${app.ia.timeout-seconds:60}") int timeoutSeconds) {
```

### Por qué no lo detectaron los tests
Los tests unitarios crean `new IaClient() { @Override identificar(...) }` a mano y no arrancan el contexto de Spring. Ningún test comprobaba **qué constructor elige Spring**.

### Prevención
- **Regla:** toda clase `@Service` o `@Component` con más de un constructor debe marcar el de producción con `@Autowired`.
- Para encontrarlas: `git grep -n "protected .*() {" backend-spring-boot/src/main/java`.
- Propuesta: un test de humo con `@SpringBootTest` que compruebe `iaClient.restClient != null`. Necesitaría PostgreSQL o Testcontainers, por eso no se hizo.

### Si vuelve a pasar
"Planta desconocida" instantánea y sin rastro en Django:
- revisa `spring.log` buscando `Motor IA no disponible`, y lee el mensaje de la excepción;
- un `NullPointerException` apunta a este fallo;
- `Connection refused` apunta a `localhost` en lugar de `127.0.0.1` o a Django apagado;
- un 403 apunta a `IA_CLAVE_INTERNA` distinta entre Spring y Django.

---

## IA-03 — La IA local del instituto (WebUI) se cortaba siempre al segundo

| Campo | Valor |
|---|---|
| Fecha | 15-nov-2026 |
| Commit | `7f34c61` — [anexo](anexos/2026-11-15-7f34c61.md) |
| Severidad | Media |
| Archivos | `backend-django/ia_engine/providers/webui_provider.py`, `services/IaClient.java`, `application.yml` |

### Síntoma
La IA local institucional (OpenWebUI, modelo `WEBUI_MODEL = "Qwen/Qwen3.6-35B-A3B-FP8"`) nunca respondía, ni siquiera dentro de la red del instituto. Todo pasaba a Gemini y en el log aparecía `Proveedor de IA 'webui' fallo: Request timed out.`

### Causa raíz
El cliente OpenAI se creaba con `timeout=3.0` (más tarde 1 s) para **todo**: conectar y recibir la respuesta. Una identificación real (especie, cuidados y apariencia) tarda unos **5 s**, así que se cortaba siempre. El tiempo corto era intencionado, para que fuera del instituto (donde el servidor no existe) se pasara rápido a Gemini, pero mezclaba dos cosas distintas.

### Corrección
Separar el tiempo para **conectar** del tiempo para **responder**:

| Variable | Valor | Significado |
|---|---|---|
| `_WEBUI_CONECTAR` | `1.0` s | Si en 1 s no hay conexión TCP, el servidor no está en la red y se pasa a Gemini |
| `_WEBUI_TIMEOUT` | `15.0` s | Con conexión, hay 15 s para responder |
| `_INTENTOS` | `2` | Reintentos solo con códigos reintentables (`_REINTENTABLES`) |

```python
timeout=httpx.Timeout(_WEBUI_TIMEOUT, connect=_WEBUI_CONECTAR)
```

Además, la llamada se ejecuta en un hilo (`threading.Thread(daemon=True)`) con `thread.join(timeout=_WEBUI_TIMEOUT)`, mediante `_thread_target` y `_do_chat_wrapper`. Este código lo aportaron compañeros y se commiteó tal cual.

En Spring, `IaClient` recibe `@Value("${app.ia.timeout-seconds:60}") int timeoutSeconds`. Con él configura `conn.setReadTimeout(timeoutSeconds * 1000)` y `conn.setConnectTimeout(timeoutSeconds * 1000)` (`IaClient.java:41-42`). En `application.yml:40`: `timeout-seconds: ${IA_TIMEOUT:60}`.

### Observaciones del revisor (deuda técnica)
1. **Hilos huérfanos:** si la llamada se cuelga, `join` vuelve a los 15 s, pero el hilo `daemon` sigue vivo hasta que `httpx` lo corta. No bloquea al usuario, pero con muchos escaneos colgados se acumulan hilos. El `httpx.Timeout` ya cubre el caso, así que el hilo sobra y se podría quitar.
2. **Mensaje raro:** `f"WebUI no respondi{chr(111) if intento % 2 else 'ó'} en ..."` escribe "respondio" o "respondió" según el intento. Es un resto de depuración.
3. **`connectTimeout` de Spring = 60 s:** Django está en la misma máquina; si no responde en 2 o 3 s, no va a responder. Convendría separar los dos valores, igual que en WebUI.

### Si vuelve a pasar
- `Request timed out` en el log, estando en la red del instituto: comprueba la URL con `curl -m 20 $WEBUI_BASE_URL/api/models -H "Authorization: Bearer $WEBUI_API_KEY"`.
- Si responde pero tarda más de 15 s, el modelo está cargado o lento: sube `_WEBUI_TIMEOUT`, **siempre por debajo** del presupuesto total (Spring 60 s).
- Fuera del instituto es **normal** ver un fallo de conexión de WebUI en cada escaneo, que luego pasa a Gemini. Tras el primer fallo, el *circuit breaker* lo salta durante 60 s.

---

## IA-04 — El modelo principal de Gemini respondía 503/504

| Campo | Valor |
|---|---|
| Fecha | 13-nov-2026 |
| Cambio | Solo de configuración: `.env` de Django (no está en git) |
| Severidad | Media |

### Síntoma
Escaneos lentos (20–45 s) y, a veces, "Planta desconocida". El log mostraba `Gemini 'gemini-flash-latest' no disponible (503)` y luego el respaldo.

### Causa
`gemini-flash-latest` (el valor por defecto en `settings.py:172`) estaba saturado en el nivel gratuito. Cada escaneo perdía hasta 20 s esperando al modelo principal antes de pasar al siguiente.

### Solución aplicada
En `backend-django/.env` (ignorado por git; cada uno tiene el suyo):

```
GEMINI_MODEL=gemini-flash-lite-latest
```

### Cómo comprobarlo
`django.log` debe mostrar `POST https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent "HTTP/1.1 200 OK"`.

### A tener en cuenta
- Al ser configuración local, **otra máquina no lo tiene**. Si un compañero dice que la IA va lenta, que revise su `.env`.
- Si el modelo *lite* empieza a fallar, se puede volver al anterior o reordenar `GEMINI_FALLBACK_MODELS`.

---

## IA-05 — Especies falsas ("Plantae sp.") que pagaban premio de especie nueva

| Campo | Valor |
|---|---|
| Fecha | 18-nov-2026 |
| Commit | `96ff15a` — [anexo](anexos/2026-11-18-96ff15a.md) (solo local) |
| Severidad | **Alta** (economía explotable y catálogo contaminado) |
| Archivos | `services/EscaneoService.java:91` y `:140-161`, `services/EscaneoServiceTest.java`, `db/migration/V28__tope_escaneo_nueva.sql` |

### Síntoma
El catálogo tenía entradas que no son especies:

| `id_catalogo` | `nombre_comun` | `nombre_cientifico` | confianza de los escaneos |
|---|---|---|---|
| 8 | Planta verde desconocida | `Plantae indet.` | 10 % |
| 9 | Planta verde desconocida | `Plantae sp.` | 10 % y 50 % |
| 10 | Planta verde genérica | `Plantae generica` | 50 % |

Y cada escaneo así pagaba la regla `ESCANEO_NUEVA` (70 XP y 20 hojas).

### Causa raíz
1. El criterio antiguo era `confianza > 0` y un nombre distinto de cuatro textos exactos ("desconocido", "unknown", "no identificable", "no identificado"). Cuando Gemini no reconoce la planta pero ve que es "una planta verde", responde con variantes como `Plantae sp.`, `Plantae indet.` o `Plantae gen. sp.`, que no estaban en la lista.
2. `CatalogoService.upsertDesdeIa(ia)` crea una especie nueva por cada `nombre_cientifico` distinto. Cada variante era una "especie nueva".
3. La regla `ESCANEO_NUEVA` tenía `tope_diario = NULL`, es decir, sin límite. Con fotos borrosas o de cualquier cosa verde se ganaban monedas sin parar.

### Corrección

```java
/** Confianza minima (0-100) para dar por buena una especie: por debajo la IA esta adivinando. */
static final double CONFIANZA_MINIMA = 50.0;

static boolean esIdentificado(IaResultDto ia) {
    if (ia.confianza() == null || ia.confianza() < CONFIANZA_MINIMA) {
        return false;
    }
    String nombre = ia.nombreCientifico() == null ? "" : ia.nombreCientifico().trim().toLowerCase();
    if (nombre.isBlank() || !nombre.matches(".*[a-z].*")) {
        return false;
    }
    // Reino o marcadores de "no se": no es una especie
    return !nombre.startsWith("plantae")
            && !nombre.startsWith("planta ")
            && !nombre.matches(".*\\b(desconocid[ao]|unknown|indet\\.?|indeterminad[ao]|gen[eé]ric[ao]|no identificad[ao]|no identificable)\\b.*");
}
```

`registrar(...)` usa `boolean identificado = esIdentificado(ia);` en la línea 91. El resto del flujo no cambia.

```sql
-- V28__tope_escaneo_nueva.sql
UPDATE reglas_progreso SET tope_diario = 5 WHERE motivo = 'ESCANEO_NUEVA' AND tope_diario IS NULL;
```

### Decisiones de diseño (y por qué)
| Decisión | Motivo |
|---|---|
| Umbral 50 inclusive | Las especies reales llegaban con 70–99 %; las genéricas, con 10–50 %. Las de 50 % genéricas se filtran por el nombre |
| `Ficus sp.` **sí** vale | Un género real es útil (cuidados similares). Solo se descarta el nivel de reino |
| `!nombre.matches(".*[a-z].*")` | Descarta respuestas sin letras (`"-"`, `"?"`, `"123"`) |
| `\b...\b` en la regex | Detecta "indet." o "genérica" como palabras sueltas sin rechazar especies que las contengan dentro de otra palabra |
| El escaneo no identificado **sí** se guarda | Se conserva el historial y la foto; solo queda `id_catalogo = null`, no paga y se muestra "Sin identificar" |
| `CONFIANZA_MINIMA` constante en código | Más simple; si el equipo quiere ajustarlo sin recompilar, se puede pasar a `application.yml` |

### Verificación
- **Test:** `EscaneoServiceTest.soloUnaEspecieConcretaConConfianzaSuficienteCuentaComoIdentificada`, con 11 casos (3 válidos y 8 rechazados).
- **Prueba en vivo con lucia** (Spring ya en v28):

| Foto | Respuesta de la IA | `identificado` | Premio |
|---|---|---|---|
| `monstera.png` (dibujo sintético) | `Plantae gen. sp.` / "Planta verde genérica", 50 | `false` | ninguno |
| `real.jpg` (foto oscura) | `Desconocido`, 0 | `false` | ninguno |
| Cactus ya escaneado antes | 10 en esta ocasión | `false` | ninguno |
| Tomate real | `Solanum lycopersicum`, 99 | `true` | 110 XP / 38 hojas |

El premio del tomate se desglosa en `RACHA` (10 XP, 3 hojas), `ESCANEO_NUEVA` (70 XP, 20 hojas) y la misión de escaneo (30 XP, 15 hojas).

- **Limpieza tras la prueba:**
  - se borraron los escaneos 41–44 y los movimientos 36–38 de `historial_progreso`;
  - `misiones_usuario` 20 volvió a progreso 0;
  - lucia quedó en 150 monedas, 1000 XP, racha 1 y `ultima_actividad = NULL`;
  - se borraron las 4 fotos de `backend-spring-boot/storage/images/`.

### Datos que quedaron
Las especies 8, 9 y 10 **siguen** en `plantas_catalogo`, porque se pidió no borrar datos. Ya no se pueden recrear ni cobrar. Se pueden ocultar con el interruptor de activo/inactivo del panel admin.

### Si vuelve a pasar
- Si aparecen especies raras en el catálogo, busca los nombres con: `SELECT id_catalogo, nombre_cientifico FROM plantas_catalogo WHERE nombre_cientifico ILIKE ANY (ARRAY['plantae%','%indet%','%sp.']);`.
- Si la IA empieza a usar otra fórmula para decir "no sé" (por ejemplo `Planta no reconocida`), añádela a la expresión regular y al test.
- Si plantas reales salen "Sin identificar", mira la confianza guardada: `SELECT confianza_ia, respuesta_ia_raw->>'nombre_cientifico' FROM escaneos_historial ORDER BY id_escaneo DESC LIMIT 20;`. Si llegan con 40–49 de forma habitual, baja `CONFIANZA_MINIMA`.

---

## IA-06 — Apariencia de la planta (forma y colores que devuelve la IA)

| Campo | Valor |
|---|---|
| Fecha | 11-nov-2026 |
| Commits | `3b41270` (función), `c9ed558` y `46a36c1` (tests de contrato que se rompieron) — [anexo](anexos/2026-11-11-3b41270.md) |
| Tipo | Función nueva, más 2 roturas de tests corregidas |

### Qué hace
En la **misma llamada** de identificación, la IA devuelve un objeto `apariencia`:
- `forma`: arbusto, suculenta, monstera…;
- colores de hoja, flor y fruto.

El jardín pixel art lo usa para dibujar la planta en 32×32 con la paleta de la especie. Así no hace falta una segunda llamada a la IA.

### Dónde vive
- **Django:** `providers/base.py`, `normalizar_identificacion(...)`. Valida la `forma` contra las formas conocidas, los colores contra un formato hexadecimal, y rellena valores por defecto.
- **Spring:**
  - `dto/IaResultDto.java`: componente `apariencia`;
  - `dto/ScanResultDto.java`: componente `apariencia`, más un constructor compatible para no romper a quien lo construye;
  - `EscaneoService.registrar`: devuelve `identificado ? ia.apariencia() : null`.
- **Web:**
  - `src/components/plantaPixelSprites.js` (430 líneas): sprites por forma;
  - `src/components/PlantaPixel.jsx`;
  - `src/pages/user/jardin/etapas.js`: las dos últimas etapas cambian de nombre según el ciclo (frutos, flores o follaje).
- **Después**, Johan amplió las formas a 9 y las etapas a 7 (verificado el 18-nov: 63 URLs de arte responden 200).

### Las dos roturas que provocó (lección)
- `c9ed558`: `ContratoMovilTest` (Spring) fija los componentes de los DTO que consume el móvil. Al añadir `apariencia` a `ScanResultDto` sin actualizar el test, el test falló.
- `46a36c1`: el test de contrato de Django (`ia_engine/tests.py`) esperaba un diccionario sin `apariencia` y también falló.

**Regla:** al añadir un campo a un DTO del móvil:
1. actualiza `ContratoMovilTest`;
2. deja un constructor compatible (está en `CLAUDE.md`);
3. el móvil ignora campos desconocidos, así que añadir es compatible, pero **quitar o renombrar** no lo es.

---

## ECO-01 — Doble gasto de monedas por condición de carrera

| Campo | Valor |
|---|---|
| Fecha | 18-nov-2026 |
| Commit | `3033a0d` — [anexo](anexos/2026-11-18-3033a0d.md) (solo local) |
| Severidad | **Alta** (artículos gratis y saldo incoherente) |
| Archivos | `UsuarioRepository.java:23`, `TiendaService.java:80`, `AbonoService.java:43`, `PlantasService.java:446`, `ProgresoService.java:107`, `ManejadorErrores.java:55-59`, `V27__monedas_no_negativas.sql` |

### Síntoma
Pulsando "Comprar" varias veces muy rápido, o con dos pestañas, se podían conseguir dos artículos pagando uno, y el saldo final no cuadraba.

### Cómo se reprodujo
Script `carrera.py` (en la carpeta temporal de la sesión, fuera del repo):
- 16 hilos lanzan a la vez `POST /api/v1/tienda/10/comprar` y `POST /api/v1/tienda/11/comprar` con la sesión de lucia (150 monedas);
- los dos artículos suman más de 150, así que solo debería poder comprarse uno;
- se repite 5 veces, restaurando a lucia entre intentos.

| Resultado | Antes | Después |
|---|---|---|
| Intentos con **los dos** artículos | 5 de 5 | 0 de 5 |
| Respuestas por intento | varias 200 + errores | 1×200, 8×402, 7×409 |
| Errores 500 en `spring.log` | 32 | 0 |

### Causa raíz: el patrón "leer, comprobar, escribir" sin bloqueo

```
Hilo A: SELECT monedas → 150      Hilo B: SELECT monedas → 150
Hilo A: 150 >= 100 ✔               Hilo B: 150 >= 120 ✔
Hilo A: UPDATE monedas = 50        Hilo B: UPDATE monedas = 30    ← sobrescribe a A
Resultado: 2 artículos, saldo 30 (debería haber sido 1 artículo y 50 o 30)
```

Con el aislamiento por defecto de PostgreSQL (`READ COMMITTED`), cada transacción ve el saldo confirmado al leerlo y no se entera de la otra. Hibernate hace `UPDATE usuarios SET monedas = ? ...` con el valor calculado en memoria, así que gana la última escritura. Los 500 venían del índice único `ux_inventario_usuario_item` cuando dos hilos insertaban el mismo artículo en `inventario_usuario`.

### Corrección 1 — Bloqueo pesimista de la fila del usuario

```java
// repositories/UsuarioRepository.java:16-23
/**
 * Bloquea la fila del usuario hasta que termine la transaccion (SELECT ... FOR UPDATE).
 * Se llama al EMPEZAR toda operacion que cambia sus monedas o su XP, antes de leerlo: asi dos
 * compras o premios simultaneos van uno detras del otro y ninguno trabaja con un saldo viejo.
 */
@Query(value = "SELECT id_usuario FROM usuarios WHERE id_usuario = :id FOR UPDATE", nativeQuery = true)
Integer bloquear(@Param("id") Integer id);
```

| Método (`@Transactional`) | Línea | Qué protege |
|---|---|---|
| `TiendaService.comprar(userId, idItem)` | `TiendaService.java:80` | saldo e inventario (temas, macetas) |
| `AbonoService.abonar(userId, idPlanta, idItem, hoy)` | `AbonoService.java:43` | saldo y "un abono al día por planta" |
| `PlantasService.revivir(userId, idPlanta)` | `PlantasService.java:446` | saldo al revivir |
| `ProgresoService.aplicar(...)` | `ProgresoService.java:107` | XP, monedas, nivel, racha, topes diarios y misiones |

`ProgresoService.aplicar` es privado, pero es el **único camino** de `otorgar(...)` (todas sus sobrecargas), `registrarAccion` y `otorgarSinRacha`. Al proteger ese método quedan cubiertos todos los premios: riego, escaneo, digitalizar, misiones, refugio y racha.

**¿Por qué `SELECT ... FOR UPDATE` nativo y no `@Lock(PESSIMISTIC_WRITE)` en `findById`?**
- `findById` se usa en muchos sitios de solo lectura y no conviene bloquear en todos.
- Un método aparte con nombre explícito (`bloquear`) deja claro en el código **dónde** empieza la sección crítica.
- Debe llamarse **antes** del primer `findById` del usuario. Si no, Hibernate ya tiene la entidad en caché, de la primera lectura, con un saldo viejo.

**¿Afecta a los tests?** No. En los tests con Mockito, `usuarioRepo` es un *mock*: `bloquear` devuelve `null` y no hace nada. Ningún test hubo que cambiarlo.

**¿Puede provocar *deadlocks*?** Cada transacción bloquea **una sola** fila de `usuarios`, la de su propio usuario, y siempre al principio. No hay dos transacciones que bloqueen dos usuarios en distinto orden, así que no hay ciclo posible. **Si en el futuro hay transferencias entre usuarios** (por ejemplo, regalos), habrá que bloquear los dos usuarios **siempre en orden de id**.

### Corrección 2 — Restricción en la BD (V27)

```sql
UPDATE usuarios SET monedas = 0 WHERE monedas IS NULL OR monedas < 0;
ALTER TABLE usuarios
    ADD CONSTRAINT ck_usuarios_monedas_no_negativas CHECK (monedas >= 0);
```

Es la última defensa: aunque un método futuro olvide `bloquear`, la BD nunca acepta un saldo negativo. Se comprobó que `UPDATE usuarios SET monedas = -5 WHERE id_usuario = 2` falla con `ck_usuarios_monedas_no_negativas`.

### Corrección 3 — 409 en lugar de 500
Ver [ERR-03](#err-03--violaciones-de-restricciones-de-la-bd-salían-como-500).

### Verificación completa
- 185 tests en verde (Java 21), más el test nuevo `choqueConRestriccionDeLaBdDa409`.
- La carrera, repetida 5 veces: 0 dobles compras y ningún 500.
- Pruebas de humo:
  - `GET` `/plantas`, `/coleccion`, `/tienda`, `/misiones` y `/usuarios/me` → 200;
  - regar (premio bajo bloqueo) → 200.
- Restauración de lucia: 150 monedas y sin los artículos 10 y 11.

> **Error durante la restauración (lección):** el primer intento de restaurar con Python hizo `rollback()` en la **misma conexión** donde se había hecho el `UPDATE`, así que la restauración se perdió. Hubo que repetirla con conexiones separadas y `commit` explícito. Con psycopg, usa `with psycopg.connect(...) as c:` (hace *commit* al salir sin error) o llama a `c.commit()`.

### Si vuelve a pasar
- **Saldos raros:** compara el saldo con el historial: `SELECT SUM(monedas_ganadas) FROM historial_progreso WHERE id_usuario = N;` frente a `usuarios.monedas` (las compras no quedan en el historial; suma los precios de `inventario_usuario`).
- **Inventario verificado el 18-nov** de todas las escrituras de saldo (`git grep -n "setMonedas\|setExperienciaXp" backend-spring-boot/src/main/java`, sin contar `models/`):

  | Escritura | Método | ¿Bajo bloqueo? |
  |---|---|---|
  | `AbonoService.java:69` | `abonar` | sí (línea 43) |
  | `PlantasService.java:458` | `revivir` | sí (línea 446) |
  | `TiendaService.java:101` | `comprar` | sí (línea 80) |
  | `ProgresoService.java:215` y `:222` | `sumar`, llamado solo desde `aplicar` | sí (línea 107) |
  | `AuthService.java:64-65` | `registrar` (valores iniciales 0 XP y 100 monedas) | no hace falta: es un usuario recién creado que nadie más conoce |

- **Un método nuevo que cambia monedas o XP:** **debe** llamar a `usuarioRepo.bloquear(userId)` en su primera línea. Para encontrar candidatos: `git grep -n "setMonedas\|setExperienciaXp" backend-spring-boot/src/main`. Cada método que aparezca debe estar en un camino que bloquee antes.
- **Peticiones que tardan mucho:** si alguien mete una llamada lenta (red o IA) **dentro** de una transacción que ya bloqueó al usuario, todas las acciones de ese usuario esperan. Ver [REN-01](#ren-01--la-llamada-a-la-ia-dentro-de-una-transacción-agotaba-el-pool-de-conexiones).

---

## ECO-02 — El jardín web solo ofrecía un abono y anunciaba "+1 día" siempre

| Campo | Valor |
|---|---|
| Fecha | 18-nov-2026 |
| Commit | `76da9a7` — [anexo](anexos/2026-11-18-76da9a7.md) (solo local) |
| Severidad | Media (funcional) |
| Archivos | `frontend-react/src/pages/user/jardin/Jardin.jsx`, `jardin.css`, `src/services/tienda.js` |

### Síntoma
Desde V26 hay tres abonos: suave (medio día), orgánico (1 día) y reforzado (3 días). El móvil ya los ofrecía, pero el jardín web:
- mostraba solo uno, el primero que devolvía la tienda;
- siempre decía "+1 día de crecimiento";
- no enviaba `idItem`, así que el servidor aplicaba siempre el más barato.

### Cómo se detectó
Revisando en el navegador las pantallas tras la fusión del rediseño, y comparándolas con el móvil (`HojaAbonos.kt`).

### Causa
El código web era anterior a V26:
- `useState(null)` y `t.items.find((i) => i.tipo === 'ABONO')`, que se queda con el primero;
- `comprarAbono(idPlanta)` sin cuerpo;
- texto fijo `"+1 día"`.

### Corrección (resumen; el código completo está en el anexo)
- **Estado:** `const [abonos, setAbonos] = useState([])`, cargado con `.filter(tipo === 'ABONO').sort((a, b) => a.precio - b.precio)`.
- **`comprarAbono(idPlanta, idItem)`:** envía `cuerpo: idItem ? { idItem } : undefined`.
- **`diasGanados(antes, despues)`:** calcula "ganó medio día", "un día" o "N días" a partir de `diasCuidados`, igual que `GardenViewModel.crecio` en el móvil.
- **Interfaz:** cuadrícula de 3 columnas (1 por debajo de 480 px). Cada botón muestra nombre, descripción y precio; si está bloqueado, "🔒 Nivel N".

### Detalle a tener en cuenta
`diasGanados` devuelve "medio día" cuando la diferencia de `diasCuidados` es ≤ 0. Es correcto para el abono suave, que deja `medio_dia_pendiente = true` sin sumar un día entero. Pero si la planta **ya estaba en el máximo** (`diasTotales`), cualquier abono da diferencia 0 y el aviso también diría "medio día". Hoy el servidor no impide abonar una planta completa, así que es un caso posible (y cobra). Ver [ECO-03](#eco-03--abonos-de-medio-día-1-día-y-3-días-medios_dias-y-medio_dia_pendiente).

---

## ECO-03 — Abonos de medio día, 1 día y 3 días (`medios_dias` y `medio_dia_pendiente`)

| Campo | Valor |
|---|---|
| Fecha | 17-nov-2026 |
| Commit | `781648b` (Johan) — [anexo](anexos/2026-11-17-781648b.md) |
| Tipo | Función nueva |
| Archivos | `V26__abonos_y_fotos.sql`, `ItemTienda.java`, `PlantaUsuario.java`, `AbonoService.java`, `PlantasService.java:500`, `PlantasController.java`, `AbonoServiceTest.java` |

### Modelo de datos
- `items_tienda.medios_dias INT NOT NULL DEFAULT 2 CHECK (medios_dias BETWEEN 1 AND 24)`.
- `plantas_usuario.medio_dia_pendiente BOOLEAN NOT NULL DEFAULT FALSE`.

| `codigo` | Nombre | Precio | `medios_dias` |
|---|---|---|---|
| `ABONO_MEDIO` (`id_item` 16) | Abono suave | 15 | 1 |
| `ABONO_1` (`id_item` 2) | Abono orgánico (ya existía) | 30 | 2 |
| `ABONO_3` (`id_item` 17) | Abono reforzado | 80 | 6 |

Los tres tienen `nivel_requerido = 1`. Los ids son los de la BD local; en otra BD pueden cambiar, porque V26 inserta con `SELECT` sin fijar el id.

### Algoritmo (`PlantasService.sumarMediosDias`, línea 500)

```java
static void sumarMediosDias(PlantaUsuario p, int medios, int diasTotales) {
    int total = medios + (p.isMedioDiaPendiente() ? 1 : 0);
    int nuevos = Math.min(diasTotales, valor(p.getDiasCuidados()) + total / 2);
    p.setDiasCuidados(nuevos);
    p.setMedioDiaPendiente(total % 2 == 1 && nuevos < diasTotales);
    subirFase(p, diasTotales);
}
```

Recorrido con un ejemplo (`diasTotales` = 10, `diasCuidados` = 3, sin pendiente):

| Abono | `total` | `nuevos` | pendiente después |
|---|---|---|---|
| suave (1) | 1 | 3 + 0 = 3 | `true` |
| suave (1) | 1 + 1 = 2 | 3 + 1 = 4 | `false` |
| reforzado (6) | 6 | 4 + 3 = 7 | `false` |
| suave (1) con `diasCuidados` = 10 | 1 | `min(10, 10)` = 10 | `false` (porque `nuevos == diasTotales`) |

### Contrato del endpoint
`POST /api/v1/plantas/{id}/abono-tienda`, cuerpo opcional `{ "idItem": n }`.
- Sin cuerpo, aplica el abono más barato (compatible con clientes antiguos).
- 404 "Ese abono no está en la tienda" si el `idItem` no es un ABONO activo.
- 409 si ya se abonó esa planta hoy.
- 402 si faltan monedas.

### Deuda técnica observada
- No hay comprobación de "planta ya completa": se cobra aunque no crezca.
- El **móvil** envía siempre un cuerpo (`emptyMap()` → `{}` si no elige). Spring lo trata igual que sin cuerpo, porque `body.get("idItem")` da `null`.

---

## ECO-04 — Mapa de dónde se ganan y se gastan hojas y XP

Referencia rápida para depurar saldos. Todo cambio de monedas o XP pasa por estos métodos y, desde ECO-01, todos bloquean al usuario.

| Acción | Método | Regla o precio | Tope |
|---|---|---|---|
| Escanear especie nueva | `ProgresoService.otorgar` desde `EscaneoService.registrar` | `ESCANEO_NUEVA` 70 XP / 20 hojas | 5 al día (V28) |
| Escanear especie repetida | ídem | `ESCANEO_REPETIDO` 10 XP / 3 hojas | 3 al día |
| Primer acto del día | `ProgresoService.aplicar` → `actualizarRacha` | `RACHA` (10 XP / 3 hojas según la tabla) | 1 al día |
| Completar misión | `ProgresoService.avanzarMisiones` | `recompensa_xp` / `recompensa_monedas` de la misión | 1 por misión y día |
| Subir de nivel | `ProgresoService.sumar` | `niveles.recompensa_monedas` | — |
| Comprar tema o maceta | `TiendaService.comprar` | `items_tienda.precio_monedas` | 1 por artículo (índice único) |
| Abonar | `AbonoService.abonar` | precio del abono | 1 al día por planta |
| Revivir planta | `PlantasService.revivir` | precio de revivir | — |

Reglas vigentes en `reglas_progreso` (BD local tras V28):

| `motivo` | XP | Hojas | `tope_diario` | Uso |
|---|---|---|---|---|
| `ABONAR` | 3 | 1 | 3 | cuidado "ABONAR" en `PlantasService` (`switch` en las líneas 208-245) |
| `CUIDAR` | 5 | 0 | 10 | sin uso en el código Java actual (`git grep '"CUIDAR"'` no encuentra nada) |
| `DIGITALIZAR` | 30 | 10 | — | `PlantasService.java:165`, al pasar una planta al jardín (cuenta además las misiones `DIGITALIZAR` y `COLECCION`) |
| `ESCANEO` | 20 | 0 | — | regla antigua; el flujo actual usa las dos siguientes |
| `ESCANEO_NUEVA` | 70 | 20 | **5** | `EscaneoService.motivosDeEscaneo(true)` |
| `ESCANEO_REPETIDO` | 10 | 3 | 3 | `EscaneoService.motivosDeEscaneo(false)` |
| `ESPECIE_NUEVA` | 50 | 0 | — | sin uso en el código Java actual |
| `EVENTO_COMPLETO` | 80 | 40 | — | `ResultadoDesastreService.java:133`, `otorgarSinRacha` al terminar un desastre |
| `MISION` | 0 | 0 | — | solo etiqueta el movimiento en el historial; el premio sale de la misión |
| `RACHA` | 10 | 3 | 1 | `ProgresoService.actualizarRacha`, primera actividad del día |
| `REFUGIO` | 10 | 3 | — | `ResultadoDesastreService.java:113` paga al terminar el desastre; `PlantasService.java:276` solo cuenta la misión (`registrarAccion`) |
| `REGAR` | 2 | 1 | 5 | cuidado "REGAR" (cuenta la misión `RIEGO`) |
| `SOL` | 2 | 1 | 5 | cuidado "SOL" |

`MisionEventoService.java:70` también lee `EVENTO_COMPLETO` para las misiones de evento.

Tablas implicadas:
- `reglas_progreso` (`motivo`, `xp`, `monedas`, `tope_diario`, `activa`);
- `historial_progreso` (`id_movimiento`, `id_usuario`, `id_regla`, `xp_ganado`, `monedas_ganadas`, `nivel_resultante`, `id_referencia`, `fecha`);
- `misiones_usuario` (`id_mision_usuario`, `id_usuario`, `id_mision`, `progreso`, `completada`, `fecha`);
- `usuarios` (`monedas`, `experiencia_xp`, `nivel`, `racha_dias`, `ultima_actividad`).

---

## REN-01 — La llamada a la IA dentro de una transacción agotaba el pool de conexiones

| Campo | Valor |
|---|---|
| Fecha | 11-nov-2026 |
| Commit | `3b4f8b1` — [anexo](anexos/2026-11-11-3b4f8b1.md) |
| Severidad | **Alta** (toda la API se colgaba con unos 10 escaneos simultáneos) |
| Archivo | `services/EscaneoService.java:58-77` |

### Síntoma
Según el análisis del commit, con unos 10 escaneos simultáneos **toda** la API (también el login y la tienda) dejaba de responder hasta que terminaban. Se detectó leyendo el código durante la auditoría, no en producción. El mensaje que deja HikariCP en esta situación es `HikariPool-1 - Connection is not available, request timed out after 30000ms`: si aparece en el log, revisa esta ficha.

### Causa raíz
`escanear()` llevaba `@Transactional`. Una transacción toma una conexión de HikariCP (10 por defecto) **al empezar** y no la suelta hasta terminar. Dentro se esperaba a la IA hasta 60 s, así que con 10 escaneos en curso no quedaban conexiones para nadie más.

### Corrección

```java
public ScanResultDto escanear(Integer userId, MultipartFile foto) {
    String imagenUrl = storage.save(foto);
    try {
        IaResultDto ia = identificar(foto); // fuera de la transaccion
        return transaccion.execute(estado -> registrar(userId, imagenUrl, ia));
    } catch (RuntimeException e) {
        storage.delete(imagenUrl);
        throw e;
    }
}
```

- `transaccion` es un `TransactionTemplate` construido en el constructor con `new TransactionTemplate(transactionManager)`.
- Se usa en lugar de `@Transactional` en `registrar` porque las llamadas a métodos de la **misma clase** no pasan por el proxy de Spring, y `@Transactional` se ignoraría.
- El `try/catch` borra la foto si el registro falla, para que no queden archivos huérfanos.

### Verificación
`EscaneoServiceTest`:
- `laIaSeLlamaAntesDeAbrirLaTransaccion`: el orden de eventos es `["ia", "transaccion"]`;
- `siFallaElGuardadoSeBorraLaFoto`.

### Regla para el futuro
**Nunca** pongas una llamada de red (IA, correo, almacenamiento externo) dentro de `@Transactional`. Con el bloqueo de ECO-01 esto es aún más importante: una transacción lenta que ya bloqueó al usuario congela todas sus acciones.

---

## REN-02 — Consultas N+1 al cargar especies de una lista

| Campo | Valor |
|---|---|
| Fecha | 11-nov-2026 |
| Commit | `f560073` — [anexo](anexos/2026-11-11-f560073.md) |
| Severidad | Media |
| Archivos | `repositories/CatalogosPorId.java` (nuevo), `EscaneoService.recientes`, `ColeccionService.listar`, `PlantasService.listar` |

### Síntoma
Listas lentas: 20 escaneos recientes provocaban **21 consultas** (1 para la lista y 1 `findById` del catálogo por fila).

### Corrección

```java
public static <T> Map<Integer, PlantaCatalogo> de(PlantaCatalogoRepository repo, Collection<T> filas,
                                                  Function<T, Integer> idCatalogo) {
    var ids = filas.stream().map(idCatalogo).filter(Objects::nonNull).collect(Collectors.toSet());
    if (ids.isEmpty()) return Map.of();
    return repo.findAllById(ids).stream()
            .collect(Collectors.toMap(PlantaCatalogo::getIdCatalogo, Function.identity()));
}
```

Uso: `Map<Integer, PlantaCatalogo> catalogos = CatalogosPorId.de(catalogoRepo, escaneos, EscaneoHistorial::getIdCatalogo);` y después `catalogos.get(e.getIdCatalogo())`.

### Cómo detectar un N+1 nuevo
Arranca Spring con `SQL_LOG=true` (ver `0d15378` en [OPE](30-operacion-y-entorno.md)). Si una sola petición imprime decenas de `select ... from plantas_catalogo where id_catalogo=?`, es un N+1.

### Ojo: N+1 introducido después
`ColeccionService.fotoDe(c)` (Johan, `781648b`) hace una consulta por cada fila **sin foto**. Tras V26 casi todas tienen foto, así que no es grave, pero conviene tenerlo presente.

---

## ERR-01 — Sin manejador global: errores con formatos distintos y 500 por datos inválidos

| Campo | Valor |
|---|---|
| Fecha | 11-nov-2026 |
| Commit | `4ad4868` — [anexo](anexos/2026-11-11-4ad4868.md) |
| Archivos | `config/ManejadorErrores.java` (nuevo), `frontend-react/src/services/api.js` |

### Síntoma
- La web mostraba "Error 401" o "Error 500" en lugar de un mensaje legible.
- Un `IllegalArgumentException` (dato inválido) daba 500.
- Una imagen de más de 15 MB no tenía respuesta propia; ahora da 413 con un mensaje claro.

### Causa
- No había `@RestControllerAdvice`. Spring respondía con su formato por defecto (`timestamp`, `status`, `error`, `path`), mientras `AuthFilter` respondía `{"error": "..."}`.
- La web leía `datos.mensaje` o `datos.message`, nunca `datos.error`.

### Corrección
`ManejadorErrores` unifica todas las respuestas en `{"error": "mensaje"}`:

| Excepción | Código | Mensaje |
|---|---|---|
| `ResponseStatusException` | el suyo | su `reason` |
| `IllegalArgumentException` | 400 | su mensaje |
| `MissingRequestHeaderException` | 401 | "Falta la cabecera X" |
| `MaxUploadSizeExceededException` | 413 | "La imagen supera los 15 MB" |
| `Exception` (resto) | 500 | "Error interno del servidor" (la traza solo va al log) |

En la web, `api.js`:
- lee `datos?.error || datos?.mensaje || datos?.message`;
- `leerJson` no rompe si la respuesta es HTML (un proxy caído, por ejemplo);
- un 401 llama a `sesionCaducada()`.

---

## ERR-02 — Los 4xx propios de Spring MVC salían como 500

| Campo | Valor |
|---|---|
| Fecha | 11-nov-2026 |
| Commit | `a519198` — [anexo](anexos/2026-11-11-a519198.md) |
| Archivo | `config/ManejadorErrores.java:62-81` |

### Síntoma (reportado por el usuario)
> "Inténtalo detengo error 500 nuevo"

En las pruebas, un `GET` a una ruta que solo admite `POST`, una URL inexistente o un `Content-Type` incorrecto devolvían 500, y en el log aparecía `Error no controlado`.

### Causa
El `@ExceptionHandler(Exception.class)` de ERR-01 atrapaba también `HttpRequestMethodNotSupportedException` (405), `NoResourceFoundException` (404), `HttpMediaTypeNotSupportedException` (415) y otras. Spring ya sabía su código, pero el manejador lo sobrescribía con 500.

### Corrección

```java
@ExceptionHandler(Exception.class)
ResponseEntity<Map<String, String>> inesperado(Exception e) {
    if (e instanceof ErrorResponse er && er.getStatusCode().is4xxClientError()) {
        HttpStatusCode codigo = er.getStatusCode();
        String mensaje = er.getBody().getDetail() != null ? er.getBody().getDetail() : "Error " + codigo.value();
        return ResponseEntity.status(codigo).body(Map.of("error", mensaje));
    }
    log.error("Error no controlado", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error interno del servidor"));
}

@ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class})
ResponseEntity<Map<String, String>> peticionMalFormada(Exception e) {
    return ResponseEntity.badRequest().body(Map.of("error", "Peticion mal formada"));
}
```

`ErrorResponse` es la interfaz que implementan desde Spring 6 todas las excepciones web con código propio. Comprobarla cubre las presentes y las futuras sin tener que listarlas.

### Tests
`ManejadorErroresTest`: `metodoNoPermitidoDa405yNo500`, `rutaInexistenteDa404` y `errorPropioSigueSiendo500`. El cuarto, `choqueConRestriccionDeLaBdDa409`, es de ERR-03.

### Si vuelve a pasar
Si ves un 500 en la web, busca `Error no controlado` en `spring.log`; justo debajo está la traza completa. Si la excepción tiene un código "natural" (409, 404…), añádele un manejador específico o lánzala como `ResponseStatusException`.

---

## ERR-03 — Violaciones de restricciones de la BD salían como 500

| Campo | Valor |
|---|---|
| Fecha | 18-nov-2026 |
| Commit | `3033a0d` (junto con ECO-01) |
| Archivo | `config/ManejadorErrores.java:51-59` |

```java
@ExceptionHandler(DataIntegrityViolationException.class)
ResponseEntity<Map<String, String>> conflictoDeDatos(DataIntegrityViolationException e) {
    log.warn("Restriccion de la BD: {}", e.getMostSpecificCause().getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("error", "La operacion choca con datos existentes; vuelve a intentarlo"));
}
```

| Restricción | Cuándo salta |
|---|---|
| `ux_inventario_usuario_item` (índice único) | comprar el mismo artículo dos veces a la vez |
| `ck_usuarios_monedas_no_negativas` (V27) | un saldo que quedaría negativo |
| cualquier `NOT NULL`, `FOREIGN KEY` o `CHECK` | datos que no cumplen el esquema |

El log usa **`warn`** y no `error`: no es un fallo del servidor, sino una situación esperada que el cliente puede reintentar. El nombre de la restricción aparece en el log (`getMostSpecificCause()`), así que basta un `grep "Restriccion de la BD" spring.log` para ver cuál fue.

**Riesgo:** un 409 también puede **ocultar un bug** de código que viola una restricción en cada intento (por ejemplo, un `NOT NULL` que falta). Si el mismo 409 se repite con la misma restricción en peticiones normales, no es concurrencia: es un error de lógica.
