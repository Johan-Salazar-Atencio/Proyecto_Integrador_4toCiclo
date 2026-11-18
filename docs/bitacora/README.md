# Bitácora técnica de PlantIA — 10 al 18 de noviembre de 2026

Registro detallado de todos los cambios, fallos y correcciones del periodo, pensado como **guía para resolver errores futuros**. Cada problema tiene una ficha con:
- el síntoma;
- la causa raíz;
- el **archivo y la línea** donde está;
- el código antes y después;
- cómo se verificó;
- cómo diagnosticarlo si vuelve.

Además, cada commit tiene un anexo generado desde git con el diff completo y sus ubicaciones exactas.

## Cómo usarla

| Si… | Abre |
|---|---|
| algo falla y no sabes por dónde empezar | [05 — Guía de diagnóstico por síntoma](05-guia-diagnostico.md) |
| buscas un fallo concreto de seguridad | [10 — Fichas de seguridad (SEG)](10-fichas-seguridad.md) |
| el fallo es de IA, monedas, rendimiento o códigos HTTP | [20 — Fichas de IA, economía, rendimiento y errores (IA, ECO, REN, ERR)](20-fichas-economia-ia.md) |
| el fallo es de la web, el móvil, la BD, el CI, o buscas una función nueva | [25 — Fichas de web, móvil, BD, CI y funciones (WEB, MOV, BD, CI, FUN)](25-fichas-web-movil-bd-ci.md) |
| necesitas arrancar, probar, restaurar datos o integrar ramas | [30 — Operación y entorno (OPE)](30-operacion-y-entorno.md) |
| buscas un endpoint, una variable de entorno, una constante, un color o una columna | [40 — Referencia (REF)](40-referencia.md) |
| quieres ver el código exacto que cambió un commit | [anexos/README.md](anexos/README.md) (68 anexos, uno por commit) |
| quieres el resumen ejecutivo | [`INFORME-CAMBIOS.md`](../../INFORME-CAMBIOS.md) en la raíz |

## Alcance

| Bloque | Commits | Autor | Dónde está |
|---|---|---|---|
| Auditoría de seguridad, flujo de escaneo, jardín pixel art, CI, errores 4xx (10-11 nov) | 26 | jose | GitHub (`main`) |
| IA: constructor de `IaClient`, timeouts de WebUI y Spring (13-15 nov) | 2 | jose | GitHub (`main`) |
| Rediseño Esmeralda/Lima, abonos de 3 tipos, fotos, Android 17 (17 nov) | 37 | Johan Salazar Atencio | `origin/rama-de-johan`; integrado en `integracion/rediseno-johan` |
| Abonos en la web, doble gasto, especies falsas (18 nov) | 3 | jose | **solo en `main` local** |

**Total:** 68 commits sin contar las fusiones, más la fusión `0200c35`.

## Índice de fichas

| ID | Título | Severidad | Commit |
|---|---|---|---|
| [SEG-01](10-fichas-seguridad.md#seg-01--tokens-de-sesión-fijos-sembrados-por-la-migración-v8) | Tokens de sesión fijos sembrados por V8 | Crítica | `fd24137` |
| [SEG-02](10-fichas-seguridad.md#seg-02--el-panel-admin-de-django-aceptaba-cualquier-x-admin-id) | Panel admin aceptaba cualquier `X-Admin-Id` | Crítica | `ac90baf` |
| [SEG-03](10-fichas-seguridad.md#seg-03--idor-acceso-a-plantas-colección-y-escaneos-de-otros-usuarios) | IDOR en plantas, colección y escaneos | Alta | `82e12f8` |
| [SEG-04](10-fichas-seguridad.md#seg-04--subida-de-archivos-que-no-eran-imágenes-xss-almacenado) | Subida de archivos que no eran imágenes | Alta | `6a74696` |
| [SEG-05](10-fichas-seguridad.md#seg-05--el-motor-de-ia-de-django-estaba-abierto-a-cualquiera) | Motor de IA abierto a la red | Alta | `fac4c0e` |
| [SEG-06](10-fichas-seguridad.md#seg-06--tokens-de-sesión-en-texto-plano-sin-caducidad-y-comparados-con-equals) | Tokens en texto plano y sin caducidad | Alta | `f94f7e5` |
| [SEG-07](10-fichas-seguridad.md#seg-07--inyección-en-la-cabecera-multipart-y-límite-de-72-bytes-de-bcrypt) | Inyección multipart y límite de BCrypt | Media | `b7c118f` |
| [SEG-08](10-fichas-seguridad.md#seg-08--la-web-actuaba-como-el-usuario-1-cuando-no-había-sesión) | La web actuaba como el usuario 1 | Media | `f94f7e5` |
| [SEG-09](10-fichas-seguridad.md#seg-09--pendientes-de-seguridad-conocidos-y-todavía-abiertos) | Pendientes de seguridad abiertos | — | — |
| [IA-01](20-fichas-economia-ia.md#ia-01--gemini-saturado-dejaba-la-app-sin-ia-y-el-mock-inventaba-monstera) | Gemini saturado y *mock* que inventaba | Alta | `7022e9e` |
| [IA-02](20-fichas-economia-ia.md#ia-02--spring-usaba-el-constructor-vacío-de-iaclient-y-ningún-escaneo-llegaba-a-la-ia) | Constructor vacío de `IaClient` | Crítica | `b23f9fb` |
| [IA-03](20-fichas-economia-ia.md#ia-03--la-ia-local-del-instituto-webui-se-cortaba-siempre-al-segundo) | WebUI cortado al segundo | Media | `7f34c61` |
| [IA-04](20-fichas-economia-ia.md#ia-04--el-modelo-principal-de-gemini-respondía-503504) | Modelo de Gemini saturado | Media | `.env` |
| [IA-05](20-fichas-economia-ia.md#ia-05--especies-falsas-plantae-sp-que-pagaban-premio-de-especie-nueva) | Especies falsas que pagaban premio | Alta | `96ff15a` |
| [IA-06](20-fichas-economia-ia.md#ia-06--apariencia-de-la-planta-forma-y-colores-que-devuelve-la-ia) | Apariencia de la planta y tests de contrato | — | `3b41270` |
| [ECO-01](20-fichas-economia-ia.md#eco-01--doble-gasto-de-monedas-por-condición-de-carrera) | Doble gasto por condición de carrera | Alta | `3033a0d` |
| [ECO-02](20-fichas-economia-ia.md#eco-02--el-jardín-web-solo-ofrecía-un-abono-y-anunciaba-1-día-siempre) | La web solo ofrecía un abono | Media | `76da9a7` |
| [ECO-03](20-fichas-economia-ia.md#eco-03--abonos-de-medio-día-1-día-y-3-días-medios_dias-y-medio_dia_pendiente) | Abonos por medios días | — | `781648b` |
| [ECO-04](20-fichas-economia-ia.md#eco-04--mapa-de-dónde-se-ganan-y-se-gastan-hojas-y-xp) | Mapa de ganancias y gastos | — | — |
| [REN-01](20-fichas-economia-ia.md#ren-01--la-llamada-a-la-ia-dentro-de-una-transacción-agotaba-el-pool-de-conexiones) | IA dentro de la transacción | Alta | `3b4f8b1` |
| [REN-02](20-fichas-economia-ia.md#ren-02--consultas-n1-al-cargar-especies-de-una-lista) | Consultas N+1 | Media | `f560073` |
| [ERR-01](20-fichas-economia-ia.md#err-01--sin-manejador-global-errores-con-formatos-distintos-y-500-por-datos-inválidos) | Sin manejador global de errores | Media | `4ad4868` |
| [ERR-02](20-fichas-economia-ia.md#err-02--los-4xx-propios-de-spring-mvc-salían-como-500) | 4xx de Spring como 500 | Media | `a519198` |
| [ERR-03](20-fichas-economia-ia.md#err-03--violaciones-de-restricciones-de-la-bd-salían-como-500) | Restricciones de la BD como 500 | Media | `3033a0d` |
| [WEB-01](25-fichas-web-movil-bd-ci.md#web-01--la-pantalla-ficha-salía-en-blanco) | `/ficha` en blanco | Alta | `ec753b0` |
| [WEB-02](25-fichas-web-movil-bd-ci.md#web-02--mi-jardín-de-la-web-vivía-en-localstorage-y-no-en-el-servidor) | Jardín en `localStorage` | Alta | `30e61ed` |
| [WEB-03](25-fichas-web-movil-bd-ci.md#web-03--escaneos-sin-identificar-mostraban-confianza-0) | "Confianza 0%" | Baja | `b7c118f` |
| [WEB-04](25-fichas-web-movil-bd-ci.md#web-04--las-animaciones-no-se-ven-en-un-navegador-con-movimiento-reducido) | Animaciones y movimiento reducido | — | — |
| [WEB-05](25-fichas-web-movil-bd-ci.md#web-05--rediseño-esmeralda-y-lima-de-la-web) | Rediseño web | — | Johan |
| [MOV-01](25-fichas-web-movil-bd-ci.md#mov-01--login-simulado-sin-uso-y-monstera-inventada-en-el-detalle) | Login simulado y Monstera inventada | Media | `b95e242` |
| [MOV-02](25-fichas-web-movil-bd-ci.md#mov-02--android-17-bloqueaba-la-conexión-con-el-pc) | Android 17 y red local | Alta | `d2034ba` |
| [MOV-03](25-fichas-web-movil-bd-ci.md#mov-03--rediseño-del-móvil-y-hoja-de-abonos) | Rediseño móvil y hoja de abonos | — | Johan |
| [BD-01](25-fichas-web-movil-bd-ci.md#bd-01--choque-de-versiones-de-migración-v25-renombrada-a-v26) | Choque de versión de migración | Alta | `293278d` |
| [BD-02](25-fichas-web-movil-bd-ci.md#bd-02--catálogo-de-migraciones-v10v28-qué-hace-cada-una) | Catálogo V10–V28 | — | — |
| [BD-03](25-fichas-web-movil-bd-ci.md#bd-03--el-panel-admin-ya-no-borra-solo-activa-o-desactiva) | El admin ya no borra | — | `e0c6f18` |
| [CI-01](25-fichas-web-movil-bd-ci.md#ci-01--no-había-integración-continua) | No había CI | Media | `769a553` |
| [CI-02](25-fichas-web-movil-bd-ci.md#ci-02--bcrypt-faltaba-en-requirementstxt) | Falta bcrypt | Baja | `b1f279f` |
| [CI-03](25-fichas-web-movil-bd-ci.md#ci-03--los-tests-de-webui-dependían-del-env-local) | Tests dependientes del `.env` | Baja | `f32c297` |
| [CI-04](25-fichas-web-movil-bd-ci.md#ci-04--mockito-falla-con-java-26-92-errores) | Mockito y Java 26 | entorno | — |
| [CI-05](25-fichas-web-movil-bd-ci.md#ci-05--configuración-ruidosa-sql-y-debug-siempre-activos) | SQL y DEBUG siempre activos | Baja | `0d15378` |
| [FUN-01…05](25-fichas-web-movil-bd-ci.md#fun-01--prototipo-de-animación-vectorial-monstera) | Funciones nuevas | — | varios |

## Regenerar los anexos

```bash
python docs/bitacora/generar_anexos.py
```

Para incluir commits nuevos, añade su rango en la lista `RANGOS` del script. Los anexos se generan siempre igual a partir de git; no hay que editarlos a mano.

## Cómo añadir una ficha nueva

1. Elige el archivo según el área y el siguiente número libre (por ejemplo, `SEG-10`).
2. Copia la estructura de una ficha existente: cabecera (fecha, commit, severidad, archivos), **síntoma**, cómo se detectó, **causa raíz**, **ubicación con `archivo:línea`**, código, por qué funciona, **verificación**, **si vuelve a pasar** y prevención.
3. Añade el síntoma a la [guía de diagnóstico](05-guia-diagnostico.md) y una fila a la tabla de arriba.
4. Regenera los anexos.
