# Informe de auditoría y correcciones — PlantIA

**Fecha:** 11/11/2026 · **Rama:** `fix/auditoria-seguridad` (sale de `feat/flujo-escaneo-guardado-virtualizacion`) · **Commits:** 16

Se revisaron Spring Boot, Django, React, Kotlin y las migraciones. Se encontraron 14 problemas: 5 críticos de seguridad, 5 altos (arquitectura y rendimiento) y 4 medios (calidad). Los 14 se corrigieron, cada uno en su propio commit. Las rutas y líneas son las del código **después** de corregir.

## Resumen

| # | Problema | Gravedad | Commit | Verificado |
|---|---|---|---|---|
| 1 | Tokens de sesión fijos en las migraciones y en el móvil | 🔴 Crítica | `fd24137` | En vivo: el token fijo da 401 |
| 2 | Panel admin de Django sin contraseña ni token | 🔴 Crítica | `ac90baf` | En vivo + 7 tests |
| 3 | Un usuario podía leer y modificar datos de otro (IDOR) | 🔴 Crítica | `82e12f8` | En vivo + 4 tests |
| 4 | Subida de archivos permitía inyectar HTML/JS | 🔴 Crítica | `6a74696` | En vivo (415) + 3 tests |
| 5 | Motor de IA abierto a toda la red y sin límite | 🔴 Crítica | `fac4c0e` | En vivo + 2 tests |
| 6 | Jardín duplicado: la web lo simulaba en el navegador | 🟠 Alta | `30e61ed` | En vivo (API y navegador) + 2 tests |
| 7 | La llamada a la IA retenía una conexión de BD hasta 60 s | 🟠 Alta | `3b4f8b1` | En vivo + 2 tests |
| 8 | Sesiones frágiles: token en claro, sin caducidad | 🟠 Alta | `f94f7e5` | En vivo + 3 tests |
| 9 | Sin manejo global de errores | 🟠 Alta | `4ad4868` | En vivo |
| 10 | Consultas N+1 en listados | 🟠 Alta | `f560073` | Tests de toda la suite |
| 11 | Casi sin tests y sin CI | 🟡 Media | `769a553` + tests en cada commit | Copia limpia del repo |
| 12 | Errores pequeños de lógica | 🟡 Media | `b7c118f` | En vivo + 2 tests |
| 13 | Configuración de desarrollo fija en todos los entornos | 🟡 Media | `0d15378` | Revisión |
| 14 | Mocks del móvil mezclados con código real | 🟡 Media | `b95e242` | Compilación y tests Android |

Además, dos commits (`c9ed558`, `46a36c1`) arreglan tests de contrato que estaban rotos desde el commit `3b41270` (campo `apariencia`).

**Estado de los tests:** Spring 23 tests ✅ (antes 7) · Django 26 tests ✅ (antes 17) · Web compila ✅ · Android compila y pasa sus tests ✅. Spring y la web se comprobaron también en una copia limpia del repositorio (sin los cambios sin commitear de otras personas).

---

## 🔴 Críticos

### 1. Tokens de sesión fijos — `fd24137`

**Problema.** `db/migration/V8__seed_session_tokens.sql` daba a los usuarios 1 y 2 los tokens `dev-default-token-user-1/2`, y el móvil traía el primero escrito en el código. Flyway aplica V8 en todos los entornos, así que cualquiera podía hacerse pasar por el usuario 1.

**Corrección.**
- `db/migration/V10__invalidar_tokens_fijos.sql` (nuevo): borra esos tokens de la BD.
- `mobile-kotlin/app/src/main/java/com/plantia/data/Sesion.kt:41`: se elimina `TOKEN_POR_DEFECTO`; sin token guardado no hay sesión y hay que iniciar sesión.

**Verificado:** `GET /usuarios/me` con el token fijo responde 401.

### 2. Panel admin de Django sin autenticación real — `ac90baf`

**Problema.** Bastaba con enviar `X-Admin-Id: <id>` para editar el catálogo, lanzar eventos y ver estadísticas. Además, el login revelaba si un correo existía ("no es administrador").

**Corrección.**
- `backend-django/catalog/admin_views.py:70` (`AdminLoginView`): el login genera un token aleatorio y lo devuelve. El mismo 401 sirve para "clave incorrecta" y "no es admin", y si el correo no existe se compara contra un hash ficticio para no revelar nada por el tiempo de respuesta.
- `backend-django/catalog/admin_views.py:42` (`_admin_desde_request`): exige `X-Admin-Id` + `X-Admin-Token` y compara en tiempo constante (`hmac.compare_digest`).
- `backend-django/catalog/models.py:21`: campos `session_token` y `session_expira` en el modelo.
- Web: `frontend-react/src/services/admin/auth.js` (`cabecerasAdmin()`), usada por `catalogo.js`, `eventos.js` y `estadisticas.js`.

**Verificado:** solo con `X-Admin-Id` → 401; con token → 200; token falso → 401; usuario normal → 401. Tests en `backend-django/catalog/tests.py`.

### 3. Acceso a datos de otros usuarios (IDOR) — `82e12f8`

**Problema.** `GET /coleccion/{id}`, `POST /plantas/{id}/cuidados`, `POST /plantas/{id}/activar`, `POST /plantas` y `POST /coleccion` aceptaban ids de otros usuarios: bastaba con cambiar el número de la URL.

**Corrección.**
- `backend-spring-boot/.../services/ColeccionService.java:45`: `detalle(userId, id)` filtra por dueño.
- `backend-spring-boot/.../controllers/ColeccionController.java:66`: el escaneo debe ser del usuario.
- `backend-spring-boot/.../services/PlantasService.java:264` (`propia`): devuelve 404 si la planta no es del usuario. Lo usan `cuidar`, `activar` y `seleccionar`.
- Todos los controladores: `X-User-Id` pasa a ser obligatorio (antes tenía `defaultValue = "1"`).

**Verificado:** Lucía pide la colección de Johan → 404; Johan la suya → 200; Lucía riega la planta de Johan → 404. Tests: `PlantasServiceTest`.

### 4. Subida de archivos peligrosa — `6a74696`

**Problema.** `StorageService` guardaba el archivo con la extensión que mandaba el cliente y no revisaba el contenido. Un `.html` o `.svg` con JavaScript se servía desde `/files/`, que pasa por el mismo origen que la web, y podía robar la sesión.

**Corrección.**
- `backend-spring-boot/.../services/StorageService.java:89` (`extensionSegunContenido`): detecta JPG, PNG o WEBP por los primeros bytes del archivo. Cualquier otra cosa da 415.
- `StorageService.java:39`: la extensión del archivo guardado sale del contenido, nunca del nombre.
- `StorageService.java:65` (`delete`): borra fotos de escaneos fallidos, sin poder salir de la carpeta de imágenes.

**Verificado:** un HTML con nombre `.jpg` → 415. Tests: `StorageServiceTest`.

### 5. Motor de IA abierto — `fac4c0e`

**Problema.** `/api/ia/identificar` y `/api/ia/cuidados` no pedían autenticación ni tenían límite de uso, y Django arrancaba con `DEBUG=True`, `ALLOWED_HOSTS=*`, CORS abierto y una `SECRET_KEY` por defecto. Cualquiera en la red podía gastar la cuota de Gemini.

**Corrección.**
- `backend-django/ia_engine/permisos.py:19` (`SoloClienteInterno`, nuevo): solo acepta la clave `IA_CLAVE_INTERNA` (header `X-Ia-Clave`) o, si no hay clave configurada, peticiones desde la propia máquina.
- `backend-django/ia_engine/views.py:40` (`_VistaIa`): aplica el permiso y el límite de uso a ambos endpoints.
- `backend-django/config/settings.py:28`: `DEBUG` es `false` por defecto, `ALLOWED_HOSTS=127.0.0.1,localhost`, y sin `DJANGO_SECRET_KEY` (y sin DEBUG) no arranca. Línea 144: límite `IA_LIMITE` (30/min). Línea 148: CORS cerrado por defecto (la web usa el proxy de Vite).
- `backend-spring-boot/.../config/IaConfig.java:18` (nuevo): Spring envía `X-Ia-Clave` si `IA_CLAVE_INTERNA` está definida.
- `backend-django/.env.example`: documenta las variables nuevas.

**Verificado:** desde la máquina responde; el escaneo completo por Spring sigue funcionando. Tests: `PermisoMotorIaTests`.

---

## 🟠 Altos

### 6. Jardín duplicado — `30e61ed`

**Problema.** El backend ya tenía `/api/v1/plantas`, pero la web guardaba "Mi jardín" en `localStorage` (`store/jardinSimulado.js`). Lo que se cuidaba en la web no existía en el móvil ni en otro navegador. Además, recargar la página a menudo evitaba que la planta se secara.

**Corrección.**
- `db/migration/V12__jardin_digital.sql` (nuevo): `plantas_usuario` gana `crecimiento`, `apariencia` (JSON de la IA), `foto_url` y `es_activa`.
- `backend-spring-boot/.../services/PlantasService.java`:
  - Línea 73 (`digitalizar`): crea la planta desde un escaneo o desde la colección; si ya existía la especie, la reinicia.
  - Línea 154 (`cuidar`): crecimiento y marchitez con las mismas reglas que tenía la web.
  - Línea 140 (`seleccionar`): elige la planta que se muestra.
  - Línea 206 (`envejecer`): desgaste por horas **completas**, así las consultas frecuentes ya no evitan que la planta se seque.
- `backend-spring-boot/.../controllers/PlantasController.java:33`: `POST /plantas` acepta `idEscaneo` o `idCatalogo` y `apariencia`; nuevo `POST /plantas/{id}/seleccionar`.
- `PlantSummaryDto` agrega `id`, `crecimiento`, `apariencia`, `fotoUrl` y `activa`. El móvil ignora estos campos y se mantiene un constructor compatible.
- Web: `frontend-react/src/services/jardin.js` (nuevo) reemplaza a `jardinSimulado.js` (borrado) en `Jardin.jsx:45`, `FichaPlanta.jsx` y `Coleccion.jsx`. Los textos de los cuidados pasan a `pages/user/jardin/acciones.js`.

**Verificado:** digitalizar, regar, dar sol y abonar por la API (el crecimiento sube 1→2→3); en el navegador la planta sale en "Brote" desde el servidor y el riego se guarda; otro usuario no puede regarla (404). Tests de desgaste y crecimiento en `PlantasServiceTest`.

### 7. La IA dentro de la transacción — `3b4f8b1`

**Problema.** `EscaneoService.escanear()` era `@Transactional` y dentro esperaba hasta 60 s a la IA, reteniendo una conexión del pool (10 por defecto). Con unos 10 escaneos a la vez se colgaba toda la API.

**Corrección.** `backend-spring-boot/.../services/EscaneoService.java:64`: la IA se llama antes, y solo `registrar` (línea 86) va en una transacción corta (`TransactionTemplate`). Si el registro falla, se borra la foto ya guardada.

**Verificado:** escaneo real por la API (unos 25 s, todos fuera de la transacción). Tests: `EscaneoServiceTest` comprueba el orden "IA → transacción" y el borrado de la foto.

### 8. Sesiones frágiles — `f94f7e5`

**Problema.** El token se guardaba en claro, nunca caducaba, se generaba con `UUID` y se comparaba con `String.equals`. La web, sin sesión, actuaba como el usuario 1.

**Corrección.**
- `backend-spring-boot/.../util/TokenSesion.java:33` (nuevo): en la BD solo va el SHA-256 del token. Los tokens son de 128 bits (`SecureRandom`) y se comparan con `MessageDigest.isEqual`.
- `db/migration/V11__session_hash_y_caducidad.sql` (nuevo): columna `session_expira` (30 días); invalida los tokens antiguos.
- `backend-spring-boot/.../config/AuthFilter.java:66`: comprueba la caducidad y el hash.
- `backend-spring-boot/.../services/AuthService.java:106` (`abrirSesion`).
- Django admin: mismo esquema en `admin_views.py`.
- `frontend-react/src/store/sesionGuardada.js:29`: sin sesión devuelve `null`, no el usuario 1.

**Verificado:** token válido → 200; falso → 401; el admin de Django funciona con el hash. Tests: `TokenSesionTest` (incluye que el hash robado de la BD no sirve como token).

### 9. Sin manejo global de errores — `4ad4868`

**Problema.** Una `IllegalArgumentException` terminaba en 500 y el mensaje de error no llegaba al cliente. La web solo leía `mensaje`/`message`, así que mostraba "Error 401"; además se rompía si la respuesta no era JSON y no volvía al login cuando la sesión caducaba.

**Corrección.**
- `backend-spring-boot/.../config/ManejadorErrores.java:21` (nuevo, `@RestControllerAdvice`): todas las respuestas de error son `{"error": "..."}`. Los datos inválidos dan 400, las imágenes de más de 15 MB dan 413 y los errores inesperados no filtran detalles internos.
- `frontend-react/src/services/api.js`: línea 36 (`leerJson`) no se rompe con HTML; línea 47 (`sesionCaducada`) borra la sesión ante un 401 y vuelve a `/acceso`.

**Verificado:** 415 con mensaje legible, 404 "Planta no encontrada" y 401 sin sesión.

### 10. Consultas N+1 — `f560073`

**Problema.** Los listados hacían un `findById` del catálogo por cada fila: 20 escaneos = 21 consultas.

**Corrección.** `backend-spring-boot/.../repositories/CatalogosPorId.java:17` (nuevo) carga todas las especies con un solo `findAllById`. Lo usan `EscaneoService.java:140` (`recientes`), `ColeccionService.java:37` (`listar`) y `PlantasService.java:129` (`listar`).

---

## 🟡 Medios

### 11. Tests y CI — `769a553` (y tests en cada commit)

**Problema.** Spring tenía un solo test, Django uno, y ni la web ni la app tenían tests ni lint. No había CI, así que nada se comprobaba antes de fusionar. Además, `package-lock.json` no incluía `@tanstack/react-virtual`, que `package.json` ya declaraba.

**Corrección.**
- `.github/workflows/ci.yml` (nuevo): ejecuta `mvn test`, `manage.py test` (con SQLite y sin claves de IA) y `npm ci && npm run build` en cada push y PR.
- Tests nuevos: `PlantasServiceTest`, `StorageServiceTest`, `EscaneoServiceTest`, `TokenSesionTest`, `IaClientTest`, `PermisoMotorIaTests` y `catalog/tests.py`.
- `frontend-react/package-lock.json`: se agrega la dependencia que faltaba; sin ella `npm ci` falla.

**Verificado:** en una copia limpia del repositorio pasan `npm ci` y el build, y todos los tests de Spring.

### 12. Errores pequeños de lógica — `b7c118f`

- `EscaneoService.java:157`: un escaneo sin identificar mostraba "Confianza 0%"; ahora muestra "Sin identificar".
- `AuthService.java:54`: el límite de 72 de BCrypt se mide en bytes UTF-8, no en caracteres.
- `IaClient.java:84` (`nombreSeguro`): el nombre de archivo del cliente se limpia antes de meterlo en la cabecera multipart hacia Django.
- `IaClient`: constructor protegido para pruebas, para que los tests no dependan de cambios sin commitear.

### 13. Configuración fija de desarrollo — `0d15378`

- `backend-spring-boot/src/main/resources/application.yml:19`: el SQL en consola solo se muestra con `SQL_LOG=true`. Línea 44: el nivel de log viene de `LOG_LEVEL` (por defecto `INFO`, antes `DEBUG`).
- `README.md`: describe `X-Session-Token`, el token del panel admin y las variables nuevas.

### 14. Mocks del móvil — `b95e242`

- `mobile-kotlin/.../data/MockAuthRepository.kt`: se borró porque no se usaba (el login usa `ApiAuthRepository`).
- `mobile-kotlin/.../screens/detail/PlantDetailScreen.kt:179`: sin un escaneo real mostraba una Monstera inventada como si fuera el resultado; ahora vuelve atrás.

---

## Qué cambia para el equipo

- **Hay que volver a iniciar sesión** en la web y en el móvil: V10 y V11 invalidan los tokens anteriores.
- **Django sin `.env`** ya no arranca con DEBUG. Copiar `backend-django/.env.example`, que trae `DJANGO_DEBUG=true` para desarrollo.
- **Si Spring y Django corren en máquinas distintas**, definir la misma `IA_CLAVE_INTERNA` en las dos.
- **Las migraciones V10, V11 y V12** se aplican solas al arrancar Spring.

## Lo que queda pendiente

1. **Contraseñas demo conocidas.** V6 y V9 dejan `plantia123` para los usuarios demo y para `admin@plantia.test`, también en producción. No se cambió para no romper el entorno de desarrollo. Antes de desplegar hay que cambiar esas contraseñas o mover las semillas a un perfil solo de desarrollo.
2. **Jardín del móvil.** Sigue usando `MockGardenData` en memoria. Los endpoints que necesita ya existen (`POST /plantas`, `/cuidados`, `/seleccionar`).
3. **Una sola sesión por usuario.** Iniciar sesión en el móvil cierra la de la web (una columna por usuario). Para varias sesiones haría falta una tabla de sesiones.
4. **Zona horaria.** Spring guarda la caducidad en hora local y Django en UTC: unas 5 horas de diferencia sobre 30 días. No afecta, pero conviene usar UTC en ambos.
5. **Commit `3b4f8b1`.** Por sí solo no compila sus tests en una copia limpia; el commit siguiente (`b7c118f`) lo corrige. La rama completa compila y pasa todo.
6. **Cambios sin commitear de otras personas.** No se tocaron: `IaClient.java` (timeout), `application.yml` (timeout), `App.jsx`, `rutas.js`, `DemoPlanta.jsx`, `local_provider.py`, `webui_provider.py` y archivos de Kotlin.
