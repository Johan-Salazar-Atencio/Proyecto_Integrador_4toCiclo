# Bitácora — Referencia técnica (REF)

Tablas de consulta rápida para depurar: endpoints, variables de entorno, constantes con valores, paleta, tablas y columnas tocadas y mapa de archivos. Todo se ha sacado del código de `main` local en `96ff15a` (18-nov-2026).

- [REF-01 Endpoints de Spring (`/api/v1`)](#ref-01--endpoints-de-spring)
- [REF-02 Endpoints de Django](#ref-02--endpoints-de-django)
- [REF-03 Paleta Esmeralda y Lima (web y móvil)](#ref-03--paleta-esmeralda-y-lima-web-y-móvil)
- [REF-04 Variables de entorno](#ref-04--variables-de-entorno)
- [REF-05 Constantes con valor fijo en el código](#ref-05--constantes-con-valor-fijo-en-el-código)
- [REF-06 Tablas y columnas tocadas en el periodo](#ref-06--tablas-y-columnas-tocadas-en-el-periodo)
- [REF-07 Códigos HTTP y su significado en PlantIA](#ref-07--códigos-http-y-su-significado-en-plantia)
- [REF-08 Mapa de archivos clave](#ref-08--mapa-de-archivos-clave)
- [REF-09 Glosario](#ref-09--glosario)

---

## REF-01 — Endpoints de Spring

Todos exigen `X-User-Id` y `X-Session-Token`, salvo `/api/v1/auth/*` y `/api/health` (ver `AuthFilter.java:44`). Los `{id}` se comprueban contra el dueño: un id ajeno responde 404 ([SEG-03](10-fichas-seguridad.md#seg-03--idor-acceso-a-plantas-colección-y-escaneos-de-otros-usuarios)).

| Método | Ruta | Controlador | Notas |
|---|---|---|---|
| GET | `/api/health` | `HealthController` | sin autenticación |
| POST | `/api/v1/auth/registro` | `AuthController` | contraseña ≤ 72 bytes UTF-8 |
| POST | `/api/v1/auth/login` | `AuthController` | devuelve el token en claro una sola vez; invalida la sesión anterior |
| GET | `/api/v1/accesos` | `AccesoController` | |
| GET | `/api/v1/fondos` | `ArteController` | catálogo de fondos (temas × día/tarde/noche) |
| GET | `/api/v1/animaciones` | `ArteController` | tiras de fotogramas de desastres |
| GET | `/api/v1/catalogo` | `CatalogoController` | |
| GET | `/api/v1/catalogo/{id}/cuidados` | `CatalogoController` | `{id}` es una especie (recurso público) |
| GET | `/api/v1/coleccion` | `ColeccionController` | usa `CatalogosPorId` (sin N+1) |
| GET | `/api/v1/coleccion/{id}` | `ColeccionController` | comprueba el dueño |
| POST | `/api/v1/coleccion` | `ColeccionController` | `{ idEscaneo }`; idempotente; guarda la foto |
| GET | `/api/v1/escaneos/recientes` | `EscaneoController` | 20 últimos, con `fotoUrl` |
| POST | `/api/v1/escaneos` | `EscaneoController` | multipart `foto`; JPG/PNG/WEBP ≤ 15 MB |
| GET | `/api/v1/eventos/activo` | `EventoController` | |
| GET | `/api/v1/misiones` | `MisionController` | |
| GET | `/api/v1/misiones/diaria` | `MisionController` | |
| GET | `/api/v1/plantas` | `PlantasController` | |
| GET | `/api/v1/plantas/activa` | `PlantasController` | |
| POST | `/api/v1/plantas` | `PlantasController` | digitalizar: `idEscaneo` o `idCatalogo`, `apariencia` |
| POST | `/api/v1/plantas/{id}/seleccionar` | `PlantasController` | |
| POST | `/api/v1/plantas/{id}/activar` | `PlantasController` | |
| POST | `/api/v1/plantas/{id}/cuidados` | `PlantasController` | REGAR / SOL / ABONAR |
| POST | `/api/v1/plantas/{id}/abono-tienda` | `PlantasController` | `{ idItem }` opcional; bloquea al usuario; 402/404/409 |
| GET | `/api/v1/plantas/{id}/fondos` | `PlantasController` | |
| PUT | `/api/v1/plantas/{id}/fondo` | `PlantasController` | |
| POST | `/api/v1/plantas/{id}/prevenir` | `PlantasController` | |
| POST | `/api/v1/plantas/{id}/refugio` | `PlantasController` | cuenta la misión REFUGIO |
| PUT | `/api/v1/plantas/macetas` | `PlantasController` | |
| POST | `/api/v1/plantas/{id}/baul` | `PlantasController` | |
| POST | `/api/v1/plantas/{id}/sacar-del-baul` | `PlantasController` | |
| PUT | `/api/v1/plantas/{id}/apodo` | `PlantasController` | vacío = nombre de la especie |
| POST | `/api/v1/plantas/{id}/revivir` | `PlantasController` | bloquea al usuario; cobra |
| POST | `/api/v1/plantas/{id}/golpe-visto` | `PlantasController` | |
| GET | `/api/v1/regiones` | `RegionController` | |
| GET | `/api/v1/tienda` | `TiendaController` | `{ monedas, nivel, items[...] }` |
| POST | `/api/v1/tienda/{id}/comprar` | `TiendaController` | bloquea al usuario; 402 sin saldo, 409 ya comprado o falta nivel |
| GET | `/api/v1/usuarios/me` | `UsuarioController` | |
| POST | `/api/v1/usuarios` | `UsuarioController` | |
| PUT | `/api/v1/usuarios/me` | `UsuarioController` | |
| PUT | `/api/v1/usuarios/me/nombre` | `UsuarioController` | |
| PUT | `/api/v1/usuarios/me/invernadero` | `UsuarioController` | |

**Archivos estáticos:** las fotos se sirven en `/files/<nombre>` (no `/files/images/`; está en `CLAUDE.md`). Las sube `StorageService` y viven en el disco, en `STORAGE_IMAGES_DIR` (`storage/images`).

---

## REF-02 — Endpoints de Django

| Método | Ruta | Vista | Protección |
|---|---|---|---|
| GET | `/api/health` | `health` | ninguna |
| POST | `/api/ia/identificar` | `ia_engine.views.IdentificarView` | `SoloClienteInterno` + *throttle* `ia` (`IA_LIMITE`, 30/min) |
| POST | `/api/ia/cuidados` | `ia_engine.views.CuidadosView` | ídem |
| GET | `/api/catalog/` | `CatalogoListView` | lectura pública |
| GET | `/api/catalog/<pk>/` | `CatalogoDetailView` | lectura |
| GET | `/api/catalog/regiones/` | `RegionListView` | lectura |
| GET | `/api/catalog/eventos/` | `EventoListView` | lectura |
| GET | `/api/catalog/eventos/activo/` | `EventoActivoView` | lectura |
| POST | `/api/admin/login/` | `AdminLoginView` | devuelve el token; mismo 401 para cualquier fallo |
| PATCH | `/api/admin/catalogo/<pk>/` | `AdminCatalogoUpdateView` | `X-Admin-Id` + `X-Admin-Token` |
| POST | `/api/admin/eventos/` | `AdminEventoCreateView` | admin |
| PATCH | `/api/admin/eventos/<pk>/` | `AdminEventoFinalizarView` | admin |
| — | `/api/admin/eventos/<pk>/efectos/` | `AdminEventoEfectosView` | admin |
| GET | `/api/admin/stats/` | `AdminStatsView` | admin |
| GET/POST | `/api/admin/misiones/` | `AdminMisionListCreateView` | admin |
| PATCH | `/api/admin/misiones/<pk>/` | `AdminMisionDetailView` | admin |
| — | `/api/admin/parametros/`, `/api/admin/parametros/<clave>/` | `AdminParametros*` | admin |
| — | `/api/admin/tipos-desastre/`, `/<pk>/` | `AdminTipos*Desastre*` | admin |
| — | `/api/admin/reglas/`, `/<pk>/` | `AdminRegla*` | admin (edita `reglas_progreso`, ver ECO-04) |
| — | `/api/admin/tienda/`, `/<pk>/` | `AdminTienda*` | admin |
| — | `/admin/` | Django admin nativo | sesión de Django |

Las rutas `/api/catalog/admin/login/`, `/catalogo/<pk>/`, `/eventos/`, `/eventos/<pk>/` y `/stats/` son **duplicados antiguos** de las de `/api/admin/` (`catalog/urls.py:12-16`). Si se cambia la protección de una, revisa también la otra.

La web llega a Django a través del proxy de Vite (`/django` → 8000). Por eso CORS no hace falta (`settings.py:150`).

---

## REF-03 — Paleta Esmeralda y Lima (web y móvil)

Web: `frontend-react/src/styles/tema.css`. Móvil: `mobile-kotlin/app/src/main/java/com/plantia/ui/theme/Color.kt`.

| Uso | Variable web | Constante móvil (claro / oscuro) | Claro | Oscuro |
|---|---|---|---|---|
| Fondo de página | `--fondo` | `BackgroundLightClaro` / `ColorDarkBackground` | `#f6faf7` | `#0b1512` |
| Tarjeta | `--superficie` | `SurfaceWhiteClaro` / `ColorDarkSurface` | `#ffffff` | `#121f1a` |
| Tarjeta elevada, campos | `--superficie-2` | `SuperficieElevadaClaro` / `ColorDarkSurfaceVariant` | `#eaf3ec` | `#1a2b24` |
| Bordes | `--linea` | `DividerColorClaro` / `ColorDarkOutline` | `#d5e4da` | `#243830` |
| Texto | `--texto` | `TextPrimaryClaro` / `ColorDarkText` | `#10261d` | `#e8f3ec` |
| Texto secundario | `--texto-suave` | `TextSecondaryClaro` / `ColorDarkTextMuted` | `#5a7164` | `#8fa89b` |
| Primario (botones) | `--primario` | `PrimarioClaro` / `PrimarioOscuro` | `#15803d` | `#a3e635` |
| Texto sobre primario | `--sobre-primario` | `SobrePrimarioClaro` / `SobrePrimarioOscuro` | `#ffffff` | `#12200a` |
| Acento lima | `--lima` / `--sobre-lima` | — | `#a3e635` / `#12200a` | — |
| Agua | `--agua` | `AguaClaro` / `AguaOscuro` | `#0284c7` | `#38bdf8` |
| Luz | `--luz` | `LuzClaro` / `LuzOscuro` | `#d97706` | `#fbbf24` |
| Abono | `--abono` | `AbonoClaro` / `AbonoOscuro` | `#9333ea` | `#c084fc` |
| Evento | `--evento` | `EventoClaro` / `EventoOscuro` | `#ea580c` | `#fb923c` |
| Texto sobre evento | `--sobre-evento` | — | — | `#1c1208` |
| Peligro | `--peligro` | `PeligroClaro` / `PeligroOscuro` | `#e11d48` | `#fb7185` |
| Éxito | `--ok` | — | `#15803d` | `#4ade80` |
| Tinte del primario | `--tinte` | `GreenLightClaro` / `GreenLightOscuro` | `rgba(21,128,61,.12)` | `rgba(163,230,53,.14)` |
| Cielo de la escena | `--cielo` | — | `linear-gradient(#a9ddf2, #e3f5e8)` | `linear-gradient(#143846, #2b6a66)` |
| Césped | `--cesped` / `--cesped-2` | — | `#5aa81a` / `#4d9b0f` | `#4d9b0f` / `#3f7d1c` |
| Velo de ventanas | `--velo` | — | `rgba(16,38,29,.45)` | `rgba(4,10,7,.66)` |
| Radio | `--radio` / `--radio-chico` | 24 dp en `Estilo.kt` | `24px` / `14px` | — |

**Alias de compatibilidad en la web:**
- `--verde` y `--verde-oscuro` → `--primario`;
- `--verde-claro` → `--tinte`;
- `--ambar` → `--luz`;
- `--azul` → `--agua`;
- `--rojo` → `--peligro`;
- `--ambar-claro`, `--azul-claro` y `--rojo-claro` con transparencias.

**Trampa de contraste:** en modo oscuro el primario es **lima**, muy claro. El texto encima debe ser oscuro (`--sobre-primario: #12200a`), no blanco. Johan corrigió varios casos en `5280b8b` y `97f494a`. Si un botón se ve "blanco sobre lima" ilegible, falta usar `var(--sobre-primario)`.

---

## REF-04 — Variables de entorno

### Spring (`backend-spring-boot/src/main/resources/application.yml`)

| Variable | Defecto | Uso | Ficha |
|---|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/plantia?stringtype=unspecified` | En esta máquina usar `127.0.0.1` | OPE-02 |
| `DB_USER` / `DB_PASSWORD` | `plantia` / `plantia` | | |
| `FLYWAY_LOCATIONS` | `filesystem:../db/migration` | Las migraciones están **fuera** del módulo de Spring. Si se arranca desde otra carpeta, Flyway no las encuentra | BD-01 |
| `IA_BASE_URL` | `http://localhost:8000` | **Usar `http://127.0.0.1:8000`** (IPv6) | SEG-05 |
| `IA_PROVIDER` | `auto` | Se reenvía a Django en el multipart | |
| `IA_TIMEOUT` | `60` | Segundos de lectura y conexión hacia Django | IA-03 |
| `IA_CLAVE_INTERNA` | (vacío) | Cabecera `X-Ia-Clave`; mismo valor que en Django | SEG-05 |
| `LOG_LEVEL` | `INFO` | `DEBUG` para depurar | CI-05 |
| `SQL_LOG` | `false` | `true` para ver el SQL (útil para N+1) | REN-02 |
| `STORAGE_IMAGES_DIR` | `storage/images` | Carpeta de fotos (relativa a donde arranca Spring) | SEG-04 |
| `STORAGE_PUBLIC_URL` | `http://localhost:8080/files` | Prefijo de las URLs de foto **guardadas en la BD** | FUN-04 |

### Django (`backend-django/config/settings.py` y `.env`)

| Variable | Defecto | Uso |
|---|---|---|
| `DJANGO_DEBUG` | `False` | `true` solo en desarrollo |
| `DJANGO_SECRET_KEY` | (vacío) | Obligatoria sin DEBUG; si falta, no arranca |
| `DJANGO_ALLOWED_HOSTS` | `127.0.0.1,localhost` | |
| `CORS_ALLOW_ALL` | `False` | |
| `DB_ENGINE` | `django.db.backends.postgresql` | `sqlite3` para los tests y el CI |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | `localhost` / `5432` / `plantia` / `plantia` / `plantia` | |
| `IA_PROVIDER` | `auto` | `openai`, `webui`, `gemini`, `local`, `mock` o `auto` |
| `IA_CLAVE_INTERNA` | (vacío) | Sin ella, solo acepta 127.0.0.1 o ::1 |
| `IA_LIMITE` | `30/min` | *Throttle* del motor |
| `GEMINI_API_KEY` | (vacío) | Sin ella, Gemini no entra en la cadena |
| `GEMINI_MODEL` | `gemini-flash-latest` | **En esta máquina: `gemini-flash-lite-latest`** (IA-04) |
| `GEMINI_FALLBACK_MODELS` | (vacío → `_RESPALDOS_DEFECTO`) | Lista separada por comas |
| `WEBUI_BASE_URL` / `WEBUI_API_KEY` / `WEBUI_MODEL` | (vacío) / (vacío) / `Qwen/Qwen3.6-35B-A3B-FP8` | IA local del instituto |
| `OPENAI_API_KEY` / `OPENAI_MODEL` | (vacío) / `gpt-4o` | |
| `LOCAL_IA_URL` / `LOCAL_IA_MODEL` | `http://localhost:11434` / `llava` | Ollama; entra en la cadena solo si `LOCAL_IA_URL` **está definida** |
| `SPRING_PUBLIC_URL` | `http://localhost:8080` | |

Variables de arranque útiles en Windows: `PYTHONIOENCODING=utf-8`, para que los logs con tildes no rompan la consola.

---

## REF-05 — Constantes con valor fijo en el código

| Constante | Valor | Archivo | Para qué |
|---|---|---|---|
| `TokenSesion.DURACION` | `Duration.ofDays(30)` | `util/TokenSesion.java` | caducidad de la sesión |
| bytes del token | 16 (32 hexadecimales) | `TokenSesion.generar` | entropía de 128 bits |
| `EscaneoService.CONFIANZA_MINIMA` | `50.0` | `services/EscaneoService.java:140` | umbral de identificación (IA-05) |
| límite de BCrypt | 72 bytes | `AuthService.java:54` | SEG-07 |
| cabecera leída | 12 bytes | `StorageService.cabecera` | *magic bytes* |
| tamaño máximo de subida | 15 MB | `ManejadorErrores.archivoGrande` y configuración multipart | ERR-01 |
| `app.ia.timeout-seconds` | 60 | `IaClient` | IA-03 |
| `_TIMEOUT_MS` | 20000 | `gemini_provider.py` | IA-01 |
| `_PRESUPUESTO_S` | 45 | `gemini_provider.py` | IA-01 |
| `_SALTAR_MODELO` | `{404, 429, 500, 502, 503, 504}` | `gemini_provider.py` | IA-01 |
| `_RESPALDOS_DEFECTO` | `gemini-flash-lite-latest,gemini-3.5-flash,gemini-3.5-flash-lite` | `gemini_provider.py` | IA-01 |
| `_ORDEN_AUTO` | `("openai", "webui", "gemini", "local")` | `factory.py` | cadena de proveedores |
| `_COOLDOWN_SEGUNDOS` | 60 | `factory.py` | *circuit breaker* |
| `_WEBUI_CONECTAR` | 1.0 s | `webui_provider.py` | IA-03 |
| `_WEBUI_TIMEOUT` | 15.0 s | `webui_provider.py` | IA-03 |
| `_INTENTOS` (WebUI) | 2 | `webui_provider.py` | IA-03 |
| `SALUD_MARCHITA` | 30 | `frontend-react/src/pages/user/jardin/etapas.js` | FUN-03 |
| `tope_diario` de `ESCANEO_NUEVA` | 5 | BD (V28) | IA-05 |
| `medios_dias` de los abonos | 1 / 2 / 6 | BD (V26) | ECO-03 |
| precios de los abonos | 15 / 30 / 80 | BD (`items_tienda`) | ECO-03 |
| monedas iniciales | 100 | `AuthService.java:65` | ECO-04 |
| pool de HikariCP | 10 (por defecto) | Spring Boot | REN-01 |

---

## REF-06 — Tablas y columnas tocadas en el periodo

| Tabla | Columna o restricción | Tipo | Origen | Notas |
|---|---|---|---|---|
| `usuarios` | `session_token` | texto (64 hexadecimales) | V11 cambia su significado | SHA-256 del token; compartido por Spring y Django |
| `usuarios` | `session_expira` | `TIMESTAMP` sin zona | V11 | hora de Lima |
| `usuarios` | `ck_usuarios_monedas_no_negativas` | `CHECK (monedas >= 0)` | V27 | |
| `plantas_usuario` | `crecimiento` | `INT NOT NULL DEFAULT 0` | V12 | |
| `plantas_usuario` | `apariencia` | `TEXT` (JSON) | V12 | forma y colores de la IA |
| `plantas_usuario` | `foto_url` | `VARCHAR(500)` | V12 | |
| `plantas_usuario` | `es_activa` | `BOOLEAN NOT NULL DEFAULT FALSE` | V12 | |
| `plantas_usuario` | `medio_dia_pendiente` | `BOOLEAN NOT NULL DEFAULT FALSE` | V26 | |
| `items_tienda` | `medios_dias` | `INT NOT NULL DEFAULT 2 CHECK (1..24)` | V26 | |
| `items_tienda` | filas `ABONO_MEDIO`, `ABONO_3` | — | V26 | `ABONO_1` renombra el existente |
| `coleccion_usuario` | `foto_url` (relleno) | — | V26 | foto del escaneo más reciente |
| `reglas_progreso` | `tope_diario` de `ESCANEO_NUEVA` | `5` | V28 | |
| `inventario_usuario` | `ux_inventario_usuario_item` | índice único `(id_usuario, id_item)` | V21 | origen de los 500, hoy 409 (ECO-01) |
| `escaneos_historial` | `confianza_ia` | `NUMERIC(5,2)` | — | escala 0-100 |
| `escaneos_historial` | `respuesta_ia_raw` | JSON | — | respuesta cruda de la IA, para auditar |

---

## REF-07 — Códigos HTTP y su significado en PlantIA

| Código | Cuándo | Quién lo pone |
|---|---|---|
| 400 | dato inválido (`IllegalArgumentException`), JSON mal formado, tipo de parámetro equivocado, contraseña > 72 bytes | `ManejadorErrores`, `AuthService` |
| 401 | falta `X-User-Id` o token, token incorrecto o sesión caducada; login fallido | `AuthFilter`, `ManejadorErrores.sinCabecera`, `AuthService`, Django admin |
| 402 | saldo insuficiente | `TiendaService`, `AbonoService`, `PlantasService.revivir` |
| 403 | el motor de IA rechaza la petición (clave o IP) | Django `SoloClienteInterno` |
| 404 | ruta inexistente, recurso inexistente **o de otro usuario** | Spring MVC, servicios |
| 405 | método no permitido | Spring MVC, respetado por `ManejadorErrores` (ERR-02) |
| 409 | ya abonado hoy, artículo ya comprado, falta nivel, choque con una restricción de la BD | servicios, `ManejadorErrores.conflictoDeDatos` |
| 413 | imagen de más de 15 MB | `ManejadorErrores.archivoGrande` |
| 415 | el archivo no es JPG, PNG ni WEBP | `StorageService.save` |
| 429 | *throttle* de la IA superado | Django DRF |
| 500 | error no previsto (traza en `spring.log` tras "Error no controlado") | `ManejadorErrores.inesperado` |
| 502 | ningún motor de IA real respondió | Django `ia_engine` |

---

## REF-08 — Mapa de archivos clave

```
backend-spring-boot/src/main/java/com/plantia/
├── config/
│   ├── AuthFilter.java            autenticación X-User-Id + X-Session-Token        (SEG-06)
│   ├── IaConfig.java              cabecera X-Ia-Clave hacia Django                (SEG-05)
│   ├── ManejadorErrores.java      formato {"error"} y códigos HTTP                (ERR-01/02/03)
│   └── WebConfig.java             CORS (allowedOriginPatterns "*")                (SEG-09c)
├── repositories/
│   ├── UsuarioRepository.java     bloquear(id): SELECT … FOR UPDATE               (ECO-01)
│   └── CatalogosPorId.java        evitar N+1                                       (REN-02)
├── services/
│   ├── EscaneoService.java        flujo de escaneo, esIdentificado, recientes     (IA-05, REN-01, WEB-03)
│   ├── IaClient.java              cliente HTTP a Django, @Autowired, nombreSeguro (IA-02, SEG-07)
│   ├── StorageService.java        fotos: magic bytes, delete seguro                (SEG-04)
│   ├── ProgresoService.java       único sitio que suma XP y monedas               (ECO-01, ECO-04)
│   ├── TiendaService.java         compras                                          (ECO-01)
│   ├── AbonoService.java          abonos de tienda                                 (ECO-01, ECO-03)
│   ├── PlantasService.java        jardín, cuidados, revivir, sumarMediosDias       (WEB-02, ECO-03)
│   ├── ColeccionService.java      colección, fotos                                  (FUN-02, FUN-04)
│   └── AuthService.java           registro y login                                  (SEG-06, SEG-07)
└── util/TokenSesion.java          generar / hash / coincide                         (SEG-06)

backend-django/
├── config/settings.py             DEBUG, SECRET_KEY, hosts, throttle, claves IA    (SEG-05)
├── catalog/admin_views.py         panel admin con token                             (SEG-02)
└── ia_engine/
    ├── permisos.py                SoloClienteInterno                                (SEG-05)
    ├── views.py                   IdentificarView, CuidadosView                     (SEG-05)
    └── providers/
        ├── factory.py             cadena de proveedores y circuit breaker          (IA-01)
        ├── gemini_provider.py     respaldo entre modelos                            (IA-01, IA-04)
        ├── webui_provider.py      IA local del instituto                            (IA-03)
        └── base.py                normalizar_identificacion (apariencia)           (IA-06)

db/migration/V10 … V28                                                               (BD-02)

frontend-react/src/
├── services/api.js                llamar(): cabeceras, errores, 401                (SEG-08, ERR-01)
├── services/tienda.js             comprarAbono(idPlanta, idItem)                    (ECO-02)
├── services/jardin.js             jardín contra el servidor                         (WEB-02)
├── pages/user/jardin/Jardin.jsx   jardín, abonos                                    (ECO-02)
├── pages/user/jardin/etapas.js    etapas del pixel art                              (FUN-03)
└── styles/tema.css                paleta                                            (REF-03)

mobile-kotlin/app/src/main/
├── AndroidManifest.xml            ACCESS_LOCAL_NETWORK                              (MOV-02)
└── java/com/plantia/
    ├── MainActivity.kt            pedirPermisoRedLocal                              (MOV-02)
    ├── data/local/Sesion.kt       sesión guardada                                   (SEG-01, SEG-09g)
    └── ui/garden/                 GardenViewModel, HojaAbonos                       (MOV-03)
```

---

## REF-09 — Glosario

| Término | Significado |
|---|---|
| **Hojas** | La moneda del juego. En la BD es `usuarios.monedas`; en la interfaz, "hojas 🍃" |
| **Digitalizar** | Pasar una planta escaneada o de la colección al jardín virtual (`POST /plantas`) |
| **Colección** | Especies que el usuario ha guardado (`coleccion_usuario`), con su foto y cuántas veces la vio |
| **Baúl** | Plantas guardadas fuera del jardín activo (`en_baul`) |
| **Medio día** | Unidad de crecimiento de los abonos (`medios_dias`); 2 = 1 día |
| **Desastre / evento** | Huayco, helada, ola de calor o plaga que lanza el admin; golpea plantas no refugiadas |
| **Refugio / prevenir** | Proteger una planta antes del desastre |
| **Racha** | Días seguidos con actividad (`racha_dias`, `ultima_actividad`) |
| **IDOR** | Acceso a un recurso de otro cambiando su id (SEG-03) |
| **N+1** | Una consulta por fila de una lista (REN-02) |
| **Magic bytes** | Primeros bytes de un archivo que identifican su formato real (SEG-04) |
| **Circuit breaker** | Saltar un servicio caído durante un tiempo en lugar de esperarlo en cada petición (IA-01) |
| **Bloqueo pesimista** | Bloquear la fila antes de leerla para que otra transacción espere (ECO-01) |
| **Fallback** | Respuesta por defecto cuando la IA no responde (`IaClient.fallback()`) |
