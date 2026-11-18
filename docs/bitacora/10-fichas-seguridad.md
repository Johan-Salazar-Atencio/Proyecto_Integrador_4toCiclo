# Bitácora — Fichas de seguridad (SEG)

Cada ficha documenta un fallo real que tuvo PlantIA: cómo se vio, por qué pasaba, en qué archivo y línea vivía, cómo se corrigió, cómo se comprobó y qué mirar si algo parecido vuelve a aparecer.

Los números de línea corresponden al estado de `main` local en el commit `96ff15a` (18-nov-2026). El código completo de cada cambio está en el anexo del commit, enlazado en la cabecera de cada ficha (`anexos/AAAA-MM-DD-hash.md`).

**Índice de fichas:**
- [SEG-01 Tokens de sesión fijos sembrados por V8](#seg-01--tokens-de-sesión-fijos-sembrados-por-la-migración-v8)
- [SEG-02 Panel admin de Django sin autenticación real](#seg-02--el-panel-admin-de-django-aceptaba-cualquier-x-admin-id)
- [SEG-03 IDOR: acceso a recursos de otros usuarios](#seg-03--idor-acceso-a-plantas-colección-y-escaneos-de-otros-usuarios)
- [SEG-04 Subida de archivos que no eran imágenes](#seg-04--subida-de-archivos-que-no-eran-imágenes-xss-almacenado)
- [SEG-05 Motor de IA de Django abierto a la red](#seg-05--el-motor-de-ia-de-django-estaba-abierto-a-cualquiera)
- [SEG-06 Sesiones guardadas en texto plano y sin caducidad](#seg-06--tokens-de-sesión-en-texto-plano-sin-caducidad-y-comparados-con-equals)
- [SEG-07 Inyección en la cabecera multipart y límite de BCrypt](#seg-07--inyección-en-la-cabecera-multipart-y-límite-de-72-bytes-de-bcrypt)
- [SEG-08 La web actuaba como el usuario 1 sin sesión](#seg-08--la-web-actuaba-como-el-usuario-1-cuando-no-había-sesión)
- [SEG-09 Pendientes de seguridad conocidos (no corregidos)](#seg-09--pendientes-de-seguridad-conocidos-y-todavía-abiertos)

---

## Modelo de autenticación vigente (contexto para todas las fichas)

Antes de leer las fichas conviene tener claro cómo funciona hoy la autenticación, porque casi todos los fallos de esta sección eran huecos en ella.

| Pieza | Cliente | Cabeceras | Dónde se valida |
|---|---|---|---|
| API de la app (`/api/v1/*`) | web React y app Android | `X-User-Id` + `X-Session-Token` | `AuthFilter` (Spring): `backend-spring-boot/src/main/java/com/plantia/config/AuthFilter.java:41-72` |
| Login y registro (`/api/v1/auth/*`) | web y móvil | ninguna | excluidos en `AuthFilter.java:44` |
| Panel admin (`/api/admin/*` en Django; las rutas antiguas `/api/catalog/admin/*` siguen activas) | web admin | `X-Admin-Id` + `X-Admin-Token` | `_admin_desde_request` en `backend-django/catalog/admin_views.py:49-67` |
| Motor IA (`/api/ia/*` en Django) | solo Spring | `X-Ia-Clave` (o petición desde 127.0.0.1) | `SoloClienteInterno` en `backend-django/ia_engine/permisos.py` |

Flujo de una sesión:

1. `POST /api/v1/auth/login` → `AuthService.login` (`services/AuthService.java:77`).
2. Se genera el token con `TokenSesion.generar()` (`util/TokenSesion.java`): 16 bytes de `SecureRandom` en hexadecimal, 32 caracteres.
3. En la BD se guarda **solo** `TokenSesion.hash(token)` (SHA-256 en hexadecimal, 64 caracteres) en `usuarios.session_token` (`AuthService.java:107-108`), y la caducidad en `usuarios.session_expira` = ahora + `TokenSesion.DURACION` (30 días).
4. El cliente recibe el token en claro una sola vez y lo envía en cada petición.
5. `AuthFilter` comprueba en este orden:
   - que `X-User-Id` sea un entero (si no, 401 "Falta o es inválido X-User-Id");
   - que el usuario exista (si no, 401 "Usuario no encontrado");
   - que `session_expira` no sea nula ni pasada;
   - que `TokenSesion.coincide(hashGuardado, tokenRecibido)` dé verdadero; usa `MessageDigest.isEqual`, que compara en tiempo constante.
6. Solo hay **una sesión por usuario**: un login nuevo sobrescribe `session_token`. Si haces login desde la API (curl, scripts de prueba), el navegador queda deslogueado. Esto confundió varias veces durante las pruebas; ver [OPE-06](30-operacion-y-entorno.md).

---

## SEG-01 — Tokens de sesión fijos sembrados por la migración V8

| Campo | Valor |
|---|---|
| Fecha de corrección | 11-nov-2026 |
| Commit | `fd24137` — [anexo](anexos/2026-11-11-fd24137.md) |
| Severidad | **Crítica** (suplantación total de usuarios) |
| Áreas | Base de datos, móvil |
| Estado | Corregido y en GitHub |

### Síntoma
No había ningún síntoma visible: la app funcionaba perfectamente. El problema era justo ese, que cualquiera podía funcionar como si fuera otro usuario.

### Cómo se detectó
Durante la auditoría de seguridad, al revisar las migraciones Flyway una por una. `V8__seed_session_tokens.sql` asignaba tokens con valores fijos y legibles (`dev-default-token-user-1`, `dev-default-token-user-2`) a los usuarios 1 y 2. Además, el móvil tenía uno de esos tokens escrito en `mobile-kotlin/app/src/main/java/com/plantia/data/Sesion.kt` como valor por defecto.

### Causa raíz
1. **Flyway aplica todas las migraciones en todos los entornos.** Una semilla pensada "solo para desarrollo" dentro de `db/migration/` acaba también en cualquier servidor real que use esa carpeta.
2. El token estaba en el código fuente del móvil, así que cualquiera que viera el repositorio o descompilara el APK lo tenía.
3. Con `X-User-Id: 1` y `X-Session-Token: dev-default-token-user-1` se pasaba `AuthFilter` sin contraseña.

### Ubicación
- Origen: `db/migration/V8__seed_session_tokens.sql`. **No se edita**: Flyway guarda su checksum y modificarla rompería el arranque en las BD donde ya se aplicó.
- Corrección: `db/migration/V10__invalidar_tokens_fijos.sql` (nueva).
- Móvil: `mobile-kotlin/app/src/main/java/com/plantia/data/Sesion.kt` (−6 / +2 líneas). Hoy el archivo vive en `mobile-kotlin/app/src/main/java/com/plantia/data/local/Sesion.kt`.

### Código de la corrección

```sql
-- db/migration/V10__invalidar_tokens_fijos.sql
UPDATE usuarios SET session_token = NULL
WHERE session_token LIKE 'dev-default-token-%';
```

En `Sesion.kt` desapareció el token por defecto: sin sesión guardada, el móvil manda al usuario a la pantalla de acceso.

### Por qué funciona
- Una migración nueva es la única forma segura de deshacer lo que hizo una antigua sin tocar su checksum.
- Con `session_token = NULL`, `TokenSesion.coincide` devuelve `false` siempre, porque `hashGuardado == null` → `false` en `TokenSesion.java:44-46`, y el filtro responde 401.
- Más tarde, V11 ([SEG-06](#seg-06--tokens-de-sesión-en-texto-plano-sin-caducidad-y-comparados-con-equals)) vació **todos** los tokens al pasar a hash, lo que refuerza esta corrección.

### Verificación
- `SELECT id_usuario, session_token FROM usuarios WHERE session_token LIKE 'dev-%';` no devuelve filas.
- `curl -H "X-User-Id: 1" -H "X-Session-Token: dev-default-token-user-1" http://127.0.0.1:8080/api/v1/usuarios/me` responde **401**.

### Si vuelve a pasar
- Busca en todo el repositorio valores de token, contraseña o clave escritos a mano: `git grep -nE "token|password|clave" -- db/migration mobile-kotlin frontend-react/src`.
- Revisa si alguna migración nueva hace `INSERT`/`UPDATE` sobre `session_token`, `password_hash` o columnas parecidas.

### Prevención
- **Regla:** ninguna migración en `db/migration/` debe sembrar credenciales. Los datos de prueba con credenciales van en un script aparte que no ejecute Flyway, o se crean con el endpoint de registro.
- **Regla:** ningún cliente (web o móvil) puede tener un token o id de usuario "por defecto".
- **Pendiente relacionado:** V6 y V9 siguen sembrando contraseñas demo conocidas (`plantia123`) para johan@, lucia@ y admin@. Ver [SEG-09](#seg-09--pendientes-de-seguridad-conocidos-y-todavía-abiertos).

---

## SEG-02 — El panel admin de Django aceptaba cualquier `X-Admin-Id`

| Campo | Valor |
|---|---|
| Fecha de corrección | 11-nov-2026 |
| Commit | `ac90baf` — [anexo](anexos/2026-11-11-ac90baf.md) |
| Severidad | **Crítica** (cualquiera podía editar el catálogo y lanzar desastres) |
| Áreas | Django (`catalog/`), web admin |
| Estado | Corregido y en GitHub |

### Síntoma
Ninguno visible. El panel admin funcionaba normalmente, pero cualquier persona en la red podía usarlo sin contraseña.

### Cómo se detectó
Al leer `backend-django/catalog/admin_views.py`: cada vista de escritura (catálogo, eventos, tienda, misiones, economía) solo comprobaba que `request.headers["X-Admin-Id"]` fuese el id de un usuario con rol `ADMIN`. Los ids son enteros pequeños y consecutivos, así que bastaba con probar `X-Admin-Id: 1, 2, 3…`.

Además, el login del admin respondía con mensajes distintos para "el correo no existe", "no es admin" y "contraseña incorrecta". Eso permitía averiguar qué correos estaban registrados (**enumeración de usuarios**).

### Causa raíz
Se confundía **identificación** (quién dices ser) con **autenticación** (demostrar que lo eres). El id no es secreto.

### Ubicación (estado actual)
- `backend-django/catalog/admin_views.py:45` → `_hash_token(token)`.
- `backend-django/catalog/admin_views.py:49-67` → `_admin_desde_request(request)`: lee `X-Admin-Id` y `X-Admin-Token`, busca el usuario, comprueba rol, caducidad y hash.
- `backend-django/catalog/admin_views.py:80-95` → vista de login: genera el token, guarda `session_token` (hash) y `session_expira`, y devuelve `_CREDENCIALES_INVALIDAS` (el mismo 401) para cualquier fallo.
- Cada vista protegida empieza con el patrón `if not admin: return Response({"error": "Se requiere un administrador"}, status=401)` (por ejemplo, líneas 114, 133, 186, 203, 309, 314 y 328).
- Web: `frontend-react/src/services/admin/auth.js` guarda el token; `services/admin/catalogo.js`, `estadisticas.js` y `eventos.js` envían las dos cabeceras.
- `backend-django/catalog/models.py`: el modelo `Usuario` (no gestionado, apunta a la tabla de Spring) expone `session_token` y `session_expira`.

### Código clave

```python
# backend-django/catalog/admin_views.py (resumen de _admin_desde_request)
caducada = u.session_expira is None or _como_aware(u.session_expira) < timezone.now()
...
if not hmac.compare_digest(guardado, _hash_token(token)):
    return None
```

Puntos a tener en cuenta:
- **`hmac.compare_digest`** compara en tiempo constante. Con `==`, la comparación se corta en el primer carácter distinto y, midiendo tiempos, se puede adivinar el hash poco a poco.
- **`_como_aware`** (línea 37): `session_expira` es `TIMESTAMP` **sin zona horaria**. Spring y Django la guardan en hora de Lima (`DATABASES['default']['TIME_ZONE']`). Django trabaja con fechas *aware* (con zona), así que hay que convertirla antes de compararla con `timezone.now()`. Si no, salta `TypeError: can't compare offset-naive and offset-aware datetimes` y el panel da 500.

### Por qué funciona
El token es aleatorio (128 bits), solo lo conoce quien hizo login con contraseña, caduca y se compara en tiempo constante. El mismo esquema que Spring ([SEG-06](#seg-06--tokens-de-sesión-en-texto-plano-sin-caducidad-y-comparados-con-equals)) permite que ambos backends compartan la columna `session_token`.

> **Consecuencia importante:** como Spring y Django escriben la **misma** columna, iniciar sesión en el panel admin con la cuenta admin invalida la sesión de esa misma cuenta en la app, y al revés. Es el comportamiento esperado con una única sesión por usuario.

### Verificación
- Tests en `backend-django/catalog/tests.py` (añadidos en `769a553`): login correcto, token incorrecto, token caducado y rol no admin. Se ejecutan con `DB_ENGINE=sqlite3 python manage.py test catalog`.
- Manual: `curl -X PATCH -H "X-Admin-Id: 3" http://127.0.0.1:8000/api/catalog/...` sin token responde **401**.

### Si vuelve a pasar
- Si aparece una vista nueva en `admin_views.py`, comprueba que su primera instrucción sea `_admin_desde_request(request)` y que devuelva 401 si es `None`. Una vista sin esa línea queda abierta.
- Para revisarlo rápido: `git grep -n "def \(get\|post\|patch\|delete\)" backend-django/catalog/admin_views.py` y comprobar que cada método tiene el chequeo justo debajo.

### Prevención
- Mejor que repetir el chequeo en cada vista: crear una clase de permiso DRF (`EsAdministrador(BasePermission)`) y ponerla en `permission_classes`, igual que `SoloClienteInterno` en el motor de IA. Así una vista nueva no puede olvidarlo. Queda propuesto, no hecho.

---

## SEG-03 — IDOR: acceso a plantas, colección y escaneos de otros usuarios

| Campo | Valor |
|---|---|
| Fecha de corrección | 11-nov-2026 |
| Commit | `82e12f8` — [anexo](anexos/2026-11-11-82e12f8.md) |
| Severidad | **Alta** (leer y modificar datos ajenos) |
| Áreas | Spring (controladores y servicios) |
| Estado | Corregido y en GitHub; regla vigente en `CLAUDE.md` |

### Síntoma
Ninguno para el usuario normal. Un usuario malicioso podía:
- leer la colección de otro: `GET /api/v1/coleccion/{id}`;
- regar, abonar o activar sus plantas: `POST /api/v1/plantas/{id}/cuidados`, `/activar`;
- digitalizar a partir del escaneo de otro: `POST /api/v1/plantas` con un `idEscaneo` ajeno;
- guardar en su colección el escaneo de otro: `POST /api/v1/coleccion`.

### Causa raíz
**IDOR** (*Insecure Direct Object Reference*): el servidor recibía un id en la URL o el cuerpo, lo buscaba con `findById(id)` y operaba sobre él sin comprobar que perteneciera al usuario de la sesión. `AuthFilter` garantiza **quién** eres, pero no que el objeto sea tuyo.

Había además un agravante: varios controladores declaraban `@RequestHeader(value = "X-User-Id", defaultValue = "1")`. Si una ruta quedaba fuera del filtro, la petición se ejecutaba como el usuario 1.

### Ubicación (estado actual del patrón corregido)
- `services/PlantasService.java:558-559`: buscar planta del usuario o 404.
  ```java
  .filter(p -> userId.equals(p.getIdUsuario()))
  .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Planta no encontrada"));
  ```
- `services/PlantasService.java:101-102`: el escaneo de origen debe ser del usuario ("Escaneo no encontrado").
- `services/ColeccionService.java:52`: `.filter(c -> userId.equals(c.getIdUsuario()))` en el detalle.
- `services/AbonoService.java` (método `abonar`, línea 42 en adelante): `plantaRepo.findById(idPlantaUsuario).filter(x -> userId.equals(x.getIdUsuario()))`.
- Controladores (`ColeccionController`, `EscaneoController`, `EventoController`, `MisionController`, `PlantasController`, `UsuarioController`): `@RequestHeader("X-User-Id") Integer userId` **sin** `defaultValue`.

### Por qué 404 y no 403
Con 403 ("prohibido") el atacante sabría que el recurso existe pero es de otro, y podría enumerar ids. Con 404 una planta ajena y una inexistente son indistinguibles. Esta convención quedó escrita en `CLAUDE.md`: *"Todo recurso con `{id}` debe comprobar que es del usuario (404 si no)"*.

### Verificación
`PlantasServiceTest` (+82 líneas en este commit) prueba que cuidar, activar y digitalizar con ids ajenos lanza `ResponseStatusException` 404.

### Si vuelve a pasar: cómo auditar un endpoint nuevo
1. Lista los endpoints con `{id}`: `git grep -n "@PathVariable" backend-spring-boot/src/main/java`.
2. Por cada uno, sigue la llamada hasta el servicio y busca `findById(`. Justo después debe ir `.filter(x -> userId.equals(x.getIdUsuario()))`, o la consulta debe ser `findByIdUsuarioAnd...`.
3. Busca también los ids que llegan **en el cuerpo** (`idEscaneo`, `idCatalogo`, `idItem`, `idPlanta`): son igual de peligrosos.
4. Comprueba que ningún `@RequestHeader("X-User-Id")` tenga `defaultValue`: `git grep -n 'defaultValue' -- '*Controller.java'`.

### Excepciones legítimas
`idItem` (tienda) e `idCatalogo` (especie del catálogo) son **recursos públicos compartidos**: no pertenecen a un usuario y no se filtran por dueño. Lo que sí se valida es su estado (activo, tipo, nivel requerido); ver `AbonoService` en [ECO-02](20-fichas-economia-ia.md#eco-02--el-jardín-web-solo-ofrecía-un-abono-y-anunciaba-1-día-siempre).

---

## SEG-04 — Subida de archivos que no eran imágenes (XSS almacenado)

| Campo | Valor |
|---|---|
| Fecha de corrección | 11-nov-2026 |
| Commit | `6a74696` — [anexo](anexos/2026-11-11-6a74696.md) |
| Severidad | **Alta** (robo de sesión a cualquier usuario que abriera un enlace) |
| Áreas | Spring (`StorageService`) |
| Estado | Corregido y en GitHub |

### Síntoma
Ninguno en el uso normal. El ataque consistía en:
1. Subir como "foto de escaneo" o "avatar" un archivo `ataque.html` (o `.svg`) con JavaScript dentro.
2. Spring lo guardaba con la extensión que mandaba el cliente y lo servía en `/files/scan_..._x.html`.
3. Como `/files/` se sirve desde **el mismo origen** que la web (el proxy de Vite y el despliegue lo exponen junto a `/api`), ese JavaScript podía leer `localStorage`, donde están `X-User-Id` y `X-Session-Token`, y enviarlos al atacante.

### Causa raíz
`StorageService.save` confiaba en `file.getOriginalFilename()` (el nombre que elige el cliente) para decidir la extensión, y no miraba el contenido.

### Ubicación
`backend-spring-boot/src/main/java/com/plantia/services/StorageService.java`:
- `save(MultipartFile file)`, línea 32: ahora obtiene la extensión con `extensionSegunContenido(cabecera(file))` y responde **415** si es `null`.
- `cabecera(file)`, línea 82: lee los primeros 12 bytes con `in.readNBytes(12)`.
- `extensionSegunContenido(byte[] b)`, línea 89: comprueba los *magic bytes*.
- `delete(String url)`, línea 65: método nuevo para borrar fotos huérfanas, protegido contra *path traversal*.

### Código clave

```java
static String extensionSegunContenido(byte[] b) {
    if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
        return ".jpg";                                  // JPEG: FF D8 FF
    }
    if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
            && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
        return ".png";                                  // PNG: 89 50 4E 47 0D 0A 1A 0A
    }
    if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
            && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
        return ".webp";                                 // WEBP: RIFF....WEBP
    }
    return null;
}
```

```java
public void delete(String url) {
    if (url == null || !url.startsWith(publicBaseUrl + "/")) return;      // solo URLs nuestras
    String name = url.substring(publicBaseUrl.length() + 1);
    Path dir = Paths.get(imagesDir).toAbsolutePath().normalize();
    Path target = dir.resolve(name).normalize();
    if (!target.startsWith(dir)) return;                                  // bloquea "../../"
    Files.deleteIfExists(target);
}
```

Más detalles:
- **`(b[0] & 0xFF)`:** en Java `byte` tiene signo (de −128 a 127). `0xFF` como byte vale −1, así que comparar `b[0] == 0xFF` (el entero 255) da siempre `false`. La máscara `& 0xFF` lo convierte a 0–255.
- **El nombre final lo genera el servidor:** `"scan_" + yyyyMMdd_HHmmss + "_" + 8 caracteres de UUID + ext`. El nombre del cliente nunca llega al disco.
- **Se copia con `Files.copy`, no con `transferTo`:** `transferTo` mueve el temporal y el `MultipartFile` queda ilegible para la llamada posterior a la IA. Está anotado en el comentario de la línea 51.

### Verificación
`StorageServiceTest` (+62 líneas):
- JPG, PNG y WEBP reales se aceptan con su extensión;
- HTML, SVG y texto dan 415;
- `delete` ignora URLs ajenas y rutas con `../`.

### Si vuelve a pasar
- Si se añade un tipo nuevo (por ejemplo HEIC, que usan los iPhone), hay que añadir su firma a `extensionSegunContenido`. **Nunca** basta con añadir la extensión a una lista.
- Si se sube un archivo que no es imagen y se ve servido en `/files/`, mira si alguien guarda archivos por otro camino que no pase por `StorageService.save` (`git grep -n "Files.write\|transferTo" backend-spring-boot`).

### Prevención (defensa en profundidad, pendiente)
- Servir `/files/` con la cabecera `Content-Security-Policy: default-src 'none'` y `X-Content-Type-Options: nosniff`.
- O mejor, servir las fotos desde otro dominio (S3 o CDN), que no comparta origen con la web. Ver `STORAGE_PUBLIC_URL` en [OPE](30-operacion-y-entorno.md).

---

## SEG-05 — El motor de IA de Django estaba abierto a cualquiera

| Campo | Valor |
|---|---|
| Fecha de corrección | 11-nov-2026 |
| Commit | `fac4c0e` — [anexo](anexos/2026-11-11-fac4c0e.md) |
| Severidad | **Alta** (gasto de cuota de pago, DEBUG con trazas en producción) |
| Áreas | Django (`ia_engine/`, `config/settings.py`), Spring (`IaConfig`) |
| Estado | Corregido y en GitHub |

### Síntoma
Ninguno visible. Riesgos:
- `POST /api/ia/identificar` y `/api/ia/cuidados` aceptaban peticiones de cualquier IP, sin límite. Cada llamada gasta cuota de Gemini u OpenAI, así que con un bucle se agotaba el saldo o la cuota del proyecto.
- `settings.py` tenía `DEBUG=True` por defecto, `ALLOWED_HOSTS=['*']`, CORS abierto y una `SECRET_KEY` de ejemplo. Con DEBUG, cualquier error muestra la traza completa, las rutas y la configuración.

### Ubicación (estado actual)
- `backend-django/ia_engine/permisos.py`: clase `SoloClienteInterno(BasePermission)` (archivo nuevo).
- `backend-django/ia_engine/views.py:43-44`: `permission_classes = [SoloClienteInterno]` y `throttle_scope = "ia"`.
- `backend-django/config/settings.py`:
  - línea 28: `DEBUG = env_bool("DJANGO_DEBUG", False)`;
  - líneas 29-35: sin `DJANGO_SECRET_KEY` y sin DEBUG, lanza `ImproperlyConfigured("Falta DJANGO_SECRET_KEY (o usa DJANGO_DEBUG=true en desarrollo)")`;
  - línea 36: `ALLOWED_HOSTS = env_list("DJANGO_ALLOWED_HOSTS", "127.0.0.1,localhost")`;
  - línea 147: `"DEFAULT_THROTTLE_RATES": {"ia": os.getenv("IA_LIMITE", "30/min")}`;
  - línea 151: `CORS_ALLOW_ALL_ORIGINS = env_bool("CORS_ALLOW_ALL", False)`;
  - línea 154: `IA_CLAVE_INTERNA = os.getenv("IA_CLAVE_INTERNA", "")`.
- `backend-spring-boot/src/main/java/com/plantia/config/IaConfig.java`: un `RestClientCustomizer` añade `X-Ia-Clave` a todas las peticiones de `IaClient` cuando `IA_CLAVE_INTERNA` está definida.
- `backend-django/.env.example`: documenta las variables.

### Código clave

```python
# backend-django/ia_engine/permisos.py
class SoloClienteInterno(BasePermission):
    message = "Acceso al motor de IA no autorizado"

    def has_permission(self, request, view):
        clave = settings.IA_CLAVE_INTERNA
        if clave:
            return hmac.compare_digest(clave, request.headers.get("X-Ia-Clave", ""))
        return request.META.get("REMOTE_ADDR") in _LOCALES     # {"127.0.0.1", "::1"}
```

```java
// IaConfig.java
@Bean
RestClientCustomizer claveInternaIa(@Value("${IA_CLAVE_INTERNA:}") String clave) {
    return builder -> { if (!clave.isBlank()) builder.defaultHeader("X-Ia-Clave", clave); };
}
```

### Por qué funciona
- **Con clave:** solo Spring la conoce, porque la lee de la misma variable de entorno.
- **Sin clave** (desarrollo): se aceptan solo peticiones de la propia máquina, y Spring y Django corren en la misma.
- **El *throttle*** limita a 30 por minuto aunque alguien consiga la clave.

### Trampa conocida: `localhost` frente a `127.0.0.1`
En esta máquina Windows, `localhost` puede resolverse a `::1` (IPv6) y Django escucha en `127.0.0.1` (IPv4). Por eso `CLAUDE.md` exige `IA_BASE_URL=http://127.0.0.1:8000`. Si Spring usa `localhost` y la conexión va por IPv6, el resultado es **connection refused**: todos los escaneos caen al *fallback* "Planta desconocida".

### Verificación
- `PermisoMotorIaTests` en `backend-django/ia_engine/tests.py`: con clave correcta pasa, con clave incorrecta da 403, y sin clave solo acepta 127.0.0.1.
- Manual: desde otra máquina de la red, `curl -F imagen=@x.jpg http://IP:8000/api/ia/identificar` responde **403**.

### Si vuelve a pasar
- Escaneos que de repente dan siempre "Planta desconocida": en el log de Django aparecen 403 en `/api/ia/identificar`. Causa típica: `IA_CLAVE_INTERNA` definida en Django y no en Spring, o al revés. Las dos deben tener el mismo valor, o ninguna.
- Django no arranca y muestra `ImproperlyConfigured: Falta DJANGO_SECRET_KEY`: en desarrollo, exportar `DJANGO_DEBUG=true` o definir una clave en `.env`.

---

## SEG-06 — Tokens de sesión en texto plano, sin caducidad y comparados con `equals`

| Campo | Valor |
|---|---|
| Fecha de corrección | 11-nov-2026 |
| Commit | `f94f7e5` — [anexo](anexos/2026-11-11-f94f7e5.md) |
| Severidad | **Alta** |
| Áreas | Spring (`AuthFilter`, `AuthService`, `Usuario`, `TokenSesion`), Django (admin), web (`api.js`, `sesionGuardada.js`), BD (V11) |
| Estado | Corregido y en GitHub |

### Problemas que tenía el esquema anterior
1. **`usuarios.session_token` guardaba el token tal cual.** Cualquiera que leyera la BD (una copia de seguridad, un volcado, un compañero con acceso) podía usar todas las sesiones.
2. **Las sesiones no caducaban nunca.** Un token robado servía para siempre.
3. **`AuthFilter` comparaba con `String.equals`.** `equals` termina en el primer carácter distinto: midiendo el tiempo de respuesta se puede deducir el token carácter a carácter (ataque de tiempo).
4. **Los tokens eran `UUID.randomUUID()`.** Son 122 bits aleatorios, aceptables, pero con un formato reconocible y sin garantía explícita de usar un generador criptográfico. Se cambió a `SecureRandom` con 128 bits.

### Ubicación (estado actual)
- `backend-spring-boot/src/main/java/com/plantia/util/TokenSesion.java` (archivo nuevo, 51 líneas):
  - `DURACION = Duration.ofDays(30)`;
  - `ALEATORIO = new SecureRandom()`;
  - `generar()`: 16 bytes en hexadecimal;
  - `hash(token)`: SHA-256 en hexadecimal;
  - `coincide(hashGuardado, tokenRecibido)`: `MessageDigest.isEqual`.
- `config/AuthFilter.java:65-66`: caducidad y comparación.
- `models/Usuario.java`: campo `sessionExpira` (`session_expira`).
- `services/AuthService.java:107-108`: guarda el hash y la caducidad al hacer login.
- `db/migration/V11__session_hash_y_caducidad.sql`.
- Django `catalog/admin_views.py`: `_hash_token` usa el mismo SHA-256 en hexadecimal.
- Web:
  - `src/store/sesionGuardada.js`: `obtenerUsuarioId`, `obtenerToken`, `borrarSesion`;
  - `src/services/api.js`: si no hay sesión no envía `X-User-Id` (antes enviaba 1; ver SEG-08).

### Código clave

```java
// TokenSesion.coincide
if (hashGuardado == null || hashGuardado.isBlank() || tokenRecibido == null || tokenRecibido.isBlank()) {
    return false;
}
return MessageDigest.isEqual(
        hashGuardado.getBytes(StandardCharsets.US_ASCII),
        hash(tokenRecibido).getBytes(StandardCharsets.US_ASCII));
```

```sql
-- V11__session_hash_y_caducidad.sql
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS session_expira TIMESTAMP;
UPDATE usuarios SET session_token = NULL, session_expira = NULL;   -- todos deben volver a iniciar sesión
```

### Por qué SHA-256 y no BCrypt para el token
BCrypt es lento a propósito, para que adivinar **contraseñas** (de baja entropía) sea caro. Un token de 128 bits aleatorios no se puede adivinar, así que basta con un hash rápido. Con BCrypt, cada petición a la API tardaría unos 100 ms más solo en autenticarse.

### Efectos secundarios que hay que conocer
- **Tras aplicar V11, todos los usuarios quedan deslogueados.** Es esperado.
- **`session_expira` no tiene zona horaria.** Spring escribe `LocalDateTime.now().plus(DURACION)` en hora del servidor (Lima, UTC−5). Django la interpreta con `TIME_ZONE` de `DATABASES`. Si un servidor se despliega en UTC, las sesiones caducarían 5 horas antes o después de lo previsto. Conviene migrar a `TIMESTAMPTZ` algún día.
- **Una sesión por usuario.** Ver el modelo al inicio de este documento.

### Verificación
`TokenSesionTest` (+32 líneas) comprueba que:
- el token tiene 32 caracteres hexadecimales;
- dos tokens seguidos son distintos;
- `hash` es determinista y tiene 64 caracteres;
- `coincide` es verdadero con el token correcto y falso con otro, con `null` o con texto en blanco.

### Si vuelve a pasar: "me sale 401 aunque acabo de iniciar sesión"
1. ¿Hiciste login en otro sitio (otra pestaña con otra cuenta, curl, el móvil, el panel admin con la misma cuenta)? El último login gana.
2. Comprueba en la BD: `SELECT session_token IS NOT NULL, session_expira FROM usuarios WHERE id_usuario = N;`.
3. Comprueba que el cliente envía las dos cabeceras. En la web, desde la consola del navegador: `localStorage`. Las claves están en `sesionGuardada.js`.
4. Si `session_expira` está en el pasado nada más hacer login, hay un desfase de zona horaria entre la JVM y PostgreSQL.

---

## SEG-07 — Inyección en la cabecera multipart y límite de 72 bytes de BCrypt

| Campo | Valor |
|---|---|
| Fecha de corrección | 11-nov-2026 |
| Commit | `b7c118f` — [anexo](anexos/2026-11-11-b7c118f.md) |
| Severidad | Media |
| Áreas | Spring (`IaClient`, `AuthService`) |
| Estado | Corregido y en GitHub |

### Parte A — Nombre de archivo dentro de la cabecera multipart
**Contexto:** `IaClient.identificar` (`services/IaClient.java:58` en adelante) construye **a mano** el cuerpo multipart hacia Django. El comentario del código explica por qué: el *writer* de Spring usa `Transfer-Encoding: chunked`, y el servidor de desarrollo de Django no lo procesa. Por eso el cuerpo se envía como `byte[]` con `Content-Length`.

**Problema:** el nombre de archivo que manda el cliente se concatenaba dentro de `Content-Disposition: form-data; name="imagen"; filename="<aquí>"`. Un nombre como `a.jpg"\r\nContent-Type: text/html\r\n\r\n...` cierra las comillas e inyecta cabeceras o partes nuevas en la petición hacia Django.

**Corrección:** método `nombreSeguro(String original)`, cerca de la línea 87:

```java
static String nombreSeguro(String original) {
    if (original == null || original.isBlank()) return "scan.jpg";
    StringBuilder limpio = new StringBuilder(original.length());
    for (char c : original.toCharArray()) {
        limpio.append(c == '"' || c == '\\' || Character.isISOControl(c) ? '_' : c);
    }
    return limpio.toString();
}
```

`Character.isISOControl` cubre `\r`, `\n`, `\t` y el resto de caracteres de control. Lo prueba `IaClientTest`.

### Parte B — El límite de 72 de BCrypt se mide en bytes
**Problema:** el registro rechazaba contraseñas de más de 72 **caracteres**. BCrypt solo usa los primeros 72 **bytes**. Con tildes, eñes o emojis (2 a 4 bytes cada uno en UTF-8), una contraseña de 40 caracteres puede superar los 72 bytes. Lo que pasa del byte 72 se ignora en silencio, y dos contraseñas distintas que coinciden en esos 72 bytes darían el mismo hash.

**Corrección:** `AuthService.java:53-55`:

```java
// BCrypt solo usa los primeros 72 BYTES: con tildes o emojis 72 caracteres pasan de 72 bytes
if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña no puede tener más de 72 caracteres");
}
```

> Nota: el mensaje dice "caracteres" para no confundir al usuario, pero la medida real son bytes.

### Parte C — Constructor protegido de `IaClient` (origen de un fallo posterior)
Este mismo commit añadió `protected IaClient()` (`IaClient.java:53`) para que los tests pudieran crear dobles de `IaClient` sin red: Mockito no puede simular clases concretas en Java 26. **Ese constructor provocó dos días después el fallo [IA-02](20-fichas-economia-ia.md#ia-02--spring-usaba-el-constructor-vacío-de-iaclient-y-ningún-escaneo-llegaba-a-la-ia)**: Spring empezó a elegirlo en lugar del real. Es un buen ejemplo de cómo una mejora para los tests puede romper la aplicación en marcha.

---

## SEG-08 — La web actuaba como el usuario 1 cuando no había sesión

| Campo | Valor |
|---|---|
| Fecha de corrección | 11-nov-2026 |
| Commit | `f94f7e5` (misma corrección que SEG-06) — [anexo](anexos/2026-11-11-f94f7e5.md) |
| Severidad | Media (amplificaba SEG-01) |
| Áreas | Web (`src/store/sesionGuardada.js`, `src/services/api.js`) |

### Problema
`obtenerUsuarioId()` devolvía `1` si `localStorage` estaba vacío. Toda la web, incluidas las pantallas antes del login, mandaba `X-User-Id: 1`. Junto con el token fijo de SEG-01, la web entera funcionaba como johan sin iniciar sesión.

### Corrección
- `sesionGuardada.js`: sin sesión devuelve `null`.
- `api.js` (`llamar`, línea 7): solo añade la cabecera si `usuarioId != null`.
- Si llega un 401, `sesionCaducada()` borra la sesión y lleva a `RUTAS.ACCESO`, salvo en las rutas públicas (`RUTAS.WELCOME`, `RUTAS.ACCESO`, `RUTAS.TERMINOS`).

```js
// src/services/api.js
const usuarioId = obtenerUsuarioId()
if (usuarioId != null) cabeceras['X-User-Id'] = String(usuarioId)
const token = obtenerToken()
if (token) cabeceras['X-Session-Token'] = token
...
if (!respuesta.ok) {
  if (respuesta.status === 401) sesionCaducada()
  ...
}
```

### Si vuelve a pasar
Si la web vuelve a la pantalla de acceso "sola" con frecuencia, revisa en la pestaña Red del navegador qué petición devuelve 401. Lo más común es que se haya iniciado sesión en otro sitio con la misma cuenta (sesión única).

---

## SEG-09 — Pendientes de seguridad conocidos y todavía abiertos

Hallazgos de `INFORME-CRITICO.md` (18-nov) que **no** se han corregido. Se dejan aquí para que nadie los dé por resueltos.

| ID | Riesgo | Dónde | Propuesta |
|---|---|---|---|
| SEG-09a | Contraseñas demo conocidas (`plantia123`) sembradas por migraciones | `db/migration/V6__password_demo.sql`, `V9__admin_user.sql` | Migración nueva que invalide esos hashes en entornos que no sean de desarrollo, o que obligue a cambiarlos en el primer login |
| SEG-09b | Sin límite de intentos de login (fuerza bruta) | `AuthService.login` (`services/AuthService.java:77`) y login admin de Django | Contador por correo e IP con bloqueo temporal; en Django, `throttle` como en la IA |
| SEG-09c | CORS de Spring permisivo: `allowedOriginPatterns("*")` | `backend-spring-boot/src/main/java/com/plantia/config/WebConfig.java:25` y `:29` | Lista de orígenes por variable de entorno |
| SEG-09d | `AuthService` sin tests | `backend-spring-boot/src/test/...` | Tests de registro (72 bytes, correo duplicado), login (contraseña incorrecta o correo inexistente con el mismo 401) y caducidad |
| SEG-09e | Fotos en disco local servidas desde el mismo origen | `StorageService`, `app.storage.public-base-url` | Servirlas desde otro dominio o bucket, y `nosniff` con CSP |
| SEG-09f | `session_expira` sin zona horaria | `usuarios.session_expira` | Migrar a `TIMESTAMPTZ` |
| SEG-09g | El móvil guarda la sesión sin cifrar (`SharedPreferences` normales) | `mobile-kotlin/app/src/main/java/com/plantia/data/local/Sesion.kt` | `EncryptedSharedPreferences` de `androidx.security` |
