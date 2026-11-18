# Guía de diagnóstico por síntoma

Empieza por aquí cuando algo falle. Busca el síntoma que ves, sigue los pasos de comprobación en orden y abre la ficha que corresponda.

Los logs están donde se redirijan al arrancar (OPE-01):
- `spring.log`: errores de Spring;
- `django.log`: llamadas a la IA;
- consola del navegador (F12): errores de la web.

---

## A. Escaneo e IA

### A1. "Planta desconocida" al instante (menos de 1 s), con cualquier foto
1. ¿Django está levantado? `curl http://127.0.0.1:8000/api/health`.
   - **No responde:** arrancarlo (OPE-01).
2. ¿Aparece `POST /api/ia/identificar` en `django.log` al escanear?
   - **No:** la petición no sale de Spring. Busca `Motor IA no disponible` en `spring.log`:
     - `NullPointerException` → [IA-02](20-fichas-economia-ia.md#ia-02--spring-usaba-el-constructor-vacío-de-iaclient-y-ningún-escaneo-llegaba-a-la-ia): `@Autowired` en el constructor;
     - `Connection refused` → `IA_BASE_URL` con `localhost` ([SEG-05](10-fichas-seguridad.md#seg-05--el-motor-de-ia-de-django-estaba-abierto-a-cualquiera), OPE-02).
   - **Sí, con 403:** `IA_CLAVE_INTERNA` distinta entre Spring y Django ([SEG-05](10-fichas-seguridad.md#seg-05--el-motor-de-ia-de-django-estaba-abierto-a-cualquiera)).
   - **Sí, con 429:** se superó `IA_LIMITE` (30/min).

### A2. "Planta desconocida" tras 20–60 s
1. Mira `django.log`:
   - `Gemini '<modelo>' no disponible (503)` en todos los modelos → saturación o cuota ([IA-01](20-fichas-economia-ia.md#ia-01--gemini-saturado-dejaba-la-app-sin-ia-y-el-mock-inventaba-monstera), [IA-04](20-fichas-economia-ia.md#ia-04--el-modelo-principal-de-gemini-respondía-503504));
   - código 404 → modelo retirado: cambia `GEMINI_MODEL` en `.env`;
   - `Proveedor de IA 'webui' fallo: Request timed out` → normal fuera del instituto; dentro, ver [IA-03](20-fichas-economia-ia.md#ia-03--la-ia-local-del-instituto-webui-se-cortaba-siempre-al-segundo).
2. Si Django respondió 200 pero Spring guardó "Planta desconocida", Spring cortó antes: el presupuesto de Django superó `IA_TIMEOUT`.

### A3. La IA responde, pero el escaneo sale "Sin identificar"
1. Mira lo que respondió: `SELECT confianza_ia, respuesta_ia_raw->>'nombre_cientifico' FROM escaneos_historial ORDER BY id_escaneo DESC LIMIT 5;`.
   - confianza < 50 → es el umbral `CONFIANZA_MINIMA` ([IA-05](20-fichas-economia-ia.md#ia-05--especies-falsas-plantae-sp-que-pagaban-premio-de-especie-nueva));
   - nombre `Plantae…`, `…indet.` o `…genérica` → la IA no supo; correcto.
2. Si plantas reales caen de forma habitual en 40–49, valora bajar el umbral (y actualizar el test).

### A4. Aparecen "especies" raras en el catálogo
[IA-05](20-fichas-economia-ia.md#ia-05--especies-falsas-plantae-sp-que-pagaban-premio-de-especie-nueva): añade el patrón nuevo a `esIdentificado` y a su test, y desactiva la especie desde el panel admin ([BD-03](25-fichas-web-movil-bd-ci.md#bd-03--el-panel-admin-ya-no-borra-solo-activa-o-desactiva)).

### A5. El escaneo devuelve 415
El archivo no es JPG, PNG ni WEBP por contenido ([SEG-04](10-fichas-seguridad.md#seg-04--subida-de-archivos-que-no-eran-imágenes-xss-almacenado)). Las fotos HEIC de iPhone caen aquí.

### A6. El escaneo devuelve 413
La foto supera los 15 MB (`spring.servlet.multipart.max-file-size`).

---

## B. Sesión y acceso

### B1. Me echa a la pantalla de acceso al poco de entrar
[OPE-06](30-operacion-y-entorno.md#ope-06--sesión-única-por-qué-se-cierra-la-sesión-del-navegador): ¿hiciste login con esa cuenta en otro sitio (curl, móvil, otra pestaña, el panel admin)? Solo vale la última sesión.

### B2. 401 en todas las peticiones justo después de iniciar sesión
- Comprueba en la pestaña Red del navegador que salen `X-User-Id` **y** `X-Session-Token`.
- Comprueba `session_expira` en la BD: si ya está en el pasado, hay desfase de zona horaria ([SEG-06](10-fichas-seguridad.md#seg-06--tokens-de-sesión-en-texto-plano-sin-caducidad-y-comparados-con-equals)).

### B3. El panel admin da 401 o "Se requiere un administrador"
- Cuenta sin rol ADMIN, token caducado, o sesión cerrada por un login de la misma cuenta en la app ([SEG-02](10-fichas-seguridad.md#seg-02--el-panel-admin-de-django-aceptaba-cualquier-x-admin-id)).
- Un 500 con `can't compare offset-naive and offset-aware datetimes` → falta `_como_aware`.

### B4. Puedo ver o modificar algo de otro usuario
**Grave.** [SEG-03](10-fichas-seguridad.md#seg-03--idor-acceso-a-plantas-colección-y-escaneos-de-otros-usuarios): sigue el procedimiento de auditoría de endpoints.

---

## C. Economía (hojas, XP, tienda, abonos)

### C1. Saldo que no cuadra, artículos "gratis"
1. ¿El método que cambia el saldo llama a `usuarioRepo.bloquear(userId)` al principio? Revisa el inventario de escrituras en [ECO-01](20-fichas-economia-ia.md#eco-01--doble-gasto-de-monedas-por-condición-de-carrera).
2. Reprodúcelo con [`herramientas/carrera.py`](herramientas/carrera.py) (OPE-09).

### C2. 409 "La operación choca con datos existentes"
Mira `Restriccion de la BD:` en `spring.log`:
- `ux_inventario_usuario_item` → compra repetida (concurrencia normal);
- `ck_usuarios_monedas_no_negativas` → un saldo iba a quedar negativo; revisa la lógica de precios;
- si se repite siempre en peticiones normales, es un bug de lógica ([ERR-03](20-fichas-economia-ia.md#err-03--violaciones-de-restricciones-de-la-bd-salían-como-500)).

### C3. 402 al comprar o abonar
Saldo insuficiente. Es lo esperado. En el móvil, `HojaAbonos` deja intentarlo si aún no conoce el saldo (`hojas == null`).

### C4. 409 al abonar
Ya se abonó esa planta hoy (`ultimo_dia_abono_tienda == hoy`). Un abono al día por planta.

### C5. El abono dice "ganó medio día" con un abono de 1 o 3 días
La planta ya estaba en el máximo de días ([ECO-02](20-fichas-economia-ia.md#eco-02--el-jardín-web-solo-ofrecía-un-abono-y-anunciaba-1-día-siempre), [ECO-03](20-fichas-economia-ia.md#eco-03--abonos-de-medio-día-1-día-y-3-días-medios_dias-y-medio_dia_pendiente)). Se cobra igual (deuda conocida).

### C6. Un premio no llega
- Mira el tope diario de la regla (ECO-04): `ESCANEO_NUEVA` 5, `ESCANEO_REPETIDO` 3, `REGAR` 5…
- Comprueba que la regla tiene `activa = true`.
- Las misiones pagan una sola vez al día.

---

## D. Web

### D1. Pantalla en blanco
Abre la consola (F12): casi siempre es `ReferenceError: X is not defined` por un `import` olvidado ([WEB-01](25-fichas-web-movil-bd-ci.md#web-01--la-pantalla-ficha-salía-en-blanco)).

### D2. Error con mensaje "Error 500"
El mensaje real está en `spring.log`, después de `Error no controlado` ([ERR-02](20-fichas-economia-ia.md#err-02--los-4xx-propios-de-spring-mvc-salían-como-500)).

### D3. Las animaciones no se mueven
`prefers-reduced-motion` está activo en el sistema ([WEB-04](25-fichas-web-movil-bd-ci.md#web-04--las-animaciones-no-se-ven-en-un-navegador-con-movimiento-reducido)). No es un fallo.

### D4. Texto ilegible en modo oscuro (blanco sobre lima)
Usa `var(--sobre-primario)` ([REF-03](40-referencia.md#ref-03--paleta-esmeralda-y-lima-web-y-móvil)).

### D5. Fotos rotas
- La URL guardada tiene otro host (`localhost` frente al real): [FUN-04](25-fichas-web-movil-bd-ci.md#fun-04--fotos-reales-en-la-colección-y-en-escaneos-recientes).
- Las fotos se sirven en `/files/<nombre>`, **no** en `/files/images/`.

---

## E. Móvil

### E1. No conecta con el servidor
- En Android 17 o superior, comprueba el permiso de red local ([MOV-02](25-fichas-web-movil-bd-ci.md#mov-02--android-17-bloqueaba-la-conexión-con-el-pc)).
- En el emulador: `adb reverse tcp:8080 tcp:8080`.
- Desde fuera de casa: [OPE-08](30-operacion-y-entorno.md#ope-08--usar-la-app-desde-el-móvil-por-internet).

### E2. El detalle de la planta se cierra solo
Comportamiento esperado si Android cerró la app y no queda el último escaneo en memoria ([MOV-01](25-fichas-web-movil-bd-ci.md#mov-01--login-simulado-sin-uso-y-monstera-inventada-en-el-detalle)).

---

## F. Arranque, base de datos y tests

| Síntoma | Ficha |
|---|---|
| `Found more than one migration with version N` | [BD-01](25-fichas-web-movil-bd-ci.md#bd-01--choque-de-versiones-de-migración-v25-renombrada-a-v26) |
| `checksum mismatch` o `description mismatch` de Flyway | [BD-01](25-fichas-web-movil-bd-ci.md#bd-01--choque-de-versiones-de-migración-v25-renombrada-a-v26) |
| 92 errores de Mockito en `mvn test` | [CI-04](25-fichas-web-movil-bd-ci.md#ci-04--mockito-falla-con-java-26-92-errores) |
| CI: `ModuleNotFoundError` | [CI-02](25-fichas-web-movil-bd-ci.md#ci-02--bcrypt-faltaba-en-requirementstxt) |
| CI: un test pasa en local y falla en CI | [CI-03](25-fichas-web-movil-bd-ci.md#ci-03--los-tests-de-webui-dependían-del-env-local) |
| CI: `npm ci` falla | [CI-01](25-fichas-web-movil-bd-ci.md#ci-01--no-había-integración-continua) (lock desactualizado) |
| `ContratoMovilTest` falla | [IA-06](20-fichas-economia-ia.md#ia-06--apariencia-de-la-planta-forma-y-colores-que-devuelve-la-ia) (DTO del móvil cambiado) |
| API colgada con varios escaneos | [REN-01](20-fichas-economia-ia.md#ren-01--la-llamada-a-la-ia-dentro-de-una-transacción-agotaba-el-pool-de-conexiones) |
| Listas lentas | [REN-02](20-fichas-economia-ia.md#ren-02--consultas-n1-al-cargar-especies-de-una-lista) (activar `SQL_LOG=true`) |
| `Port already in use` | [OPE-02](30-operacion-y-entorno.md#ope-02--problemas-típicos-al-arrancar) |
