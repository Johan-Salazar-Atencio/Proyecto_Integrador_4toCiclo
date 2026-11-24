# Guía de inicio manual (Plantia)

Pasos para levantar el proyecto completo a mano y probarlo igual que en el
entorno de desarrollo. La app móvil habla con **Spring Boot**, que a su vez
consulta el motor de IA de **Django**, y ambos comparten la misma base
**PostgreSQL**.

## Arquitectura de puertos

| Servicio     | Puerto | Rol                                              |
|--------------|--------|--------------------------------------------------|
| PostgreSQL   | 5432   | Base de datos compartida (Docker)                |
| Django       | 8000   | Motor de IA (Gemini/local/mock) + admin          |
| Spring Boot  | 8080   | API que consume el móvil                         |
| App Android  | -      | Se conecta a Spring vía `adb reverse`            |

## Requisitos

- Docker Desktop (corriendo)
- JDK 17 + Maven (o IntelliJ IDEA)
- Python 3.11 o superior
- Android Studio / SDK + `adb` (con el teléfono por USB)
- Una clave de Gemini (gratuita): https://aistudio.google.com/apikey

## 1) Base de datos (PostgreSQL)

Desde la raíz del repo:

```powershell
docker compose up -d postgres flyway
```

Levanta Postgres en `5432` y aplica el esquema (migraciones Flyway de
`db/migration`).

> **Si el puerto 5432 ya está ocupado** por otro Postgres en la máquina, usa
> este contenedor en `5433`:
>
> ```powershell
> docker run -d --name plantia-postgres -e POSTGRES_DB=plantia -e POSTGRES_USER=plantia -e POSTGRES_PASSWORD=plantia -p 5433:5432 -v plantia_pgdata:/var/lib/postgresql/data postgres:16-alpine
> ```
>
> y ajusta el puerto a **5433** en los pasos 2 y 3.

## 2) Django (IA + admin) — puerto 8000

```powershell
cd backend-django
py -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

Crea `backend-django\.env` (cópialo de `.env.example`) con:

```
DB_NAME=plantia
DB_USER=plantia
DB_PASSWORD=plantia
DB_HOST=localhost
DB_PORT=5432
IA_PROVIDER=auto
GEMINI_API_KEY=<tu clave de Gemini>
GEMINI_MODEL=gemini-flash-latest
CORS_ALLOW_ALL=true
```

Arranca:

```powershell
.\.venv\Scripts\python.exe manage.py migrate --run-syncdb
.\.venv\Scripts\python.exe manage.py runserver 127.0.0.1:8000
```

> Sin ninguna clave (Gemini, WebUI, OpenAI o `LOCAL_IA_URL`) el escaneo responde
> un error claro (502): no se inventa ninguna planta. `IA_PROVIDER=mock` (siempre
> Monstera) existe solo para pruebas y hay que pedirlo a propósito.

## 3) Spring Boot (API del móvil) — puerto 8080

Desde `backend-spring-boot`, define las variables y arranca:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/plantia?stringtype=unspecified"
$env:DB_USER="plantia"
$env:DB_PASSWORD="plantia"
$env:IA_BASE_URL="http://127.0.0.1:8000"
mvn spring-boot:run
```

> Usa **127.0.0.1** (no `localhost`) en `IA_BASE_URL`: en algunas máquinas
> `localhost` resuelve a IPv6 (`::1`) y la conexión falla.
>
> Alternativa con IntelliJ: abre `backend-spring-boot` y ejecuta
> `com.plantia.PlantiaApplication` con esas mismas variables de entorno.

## 4) Probar el backend (sin teléfono)

Con una foto de planta a mano (`foto.jpg`):

```powershell
curl http://127.0.0.1:8000/api/health

curl -X POST http://127.0.0.1:8080/api/v1/escaneos `
  -H "X-User-Id: 1" `
  -F "foto=@foto.jpg"
```

Debe devolver `nombreComun`, `nombreCientifico`, `confianza` y `cuidados`
reales.

### Pruebas de concurrencia de Spring (Postgres real)

`QaPostgresTest` y `LogrosPostgresTest` (carreras de escaneos, misiones,
cuidados, cierre de eventos, logros pagados una sola vez) **solo corren si
existe `PLANTIA_TEST_DB_URL`**; sin ella el resumen de Maven las cuenta como
*Skipped*. La base debe llamarse con "prueba" o "test" (nunca la de desarrollo):

```powershell
createdb -U postgres plantia_prueba
$env:PLANTIA_TEST_DB_URL = "jdbc:postgresql://localhost:5432/plantia_prueba?stringtype=unspecified"
mvn test
```

Usuario y clave por defecto `plantia`/`plantia` (o `PLANTIA_TEST_DB_USER` y
`PLANTIA_TEST_DB_PASSWORD`). Flyway aplica todas las migraciones desde cero.

### Primer arranque: la base empieza sin usuarios

- Desde la migración **V33** la base no trae datos de demostración: Flyway
  borra los usuarios que sembraban V3/V4/V6/V8/V9 (`admin@`, `johan@` y
  `lucia@plantia.test`, con su jardín, colección, escaneos y misiones) y el
  evento HUAYCO de prueba. Una base nueva (V1 a V33) queda con **0 usuarios y
  0 eventos**; sí trae la configuración del juego (catálogo, tienda, misiones,
  niveles, logros, tipos de desastre, regiones, parámetros, temas y fondos).
- Los jugadores se **registran desde la app** (móvil o web).
- El administrador se crea a propósito, **nunca con una clave fija**:

```powershell
cd backend-django
python manage.py crear_admin --email admin@tu-dominio.pe --nombre "Admin Plantia"
```

  La clave se pide dos veces por teclado (o se toma de la variable
  `PLANTIA_ADMIN_PASSWORD` en un despliegue automatizado; mínimo 12
  caracteres). Si el correo ya existe, pasa a ADMIN, cambia su clave y cierra
  sus sesiones. Mientras no haya ningún admin, Spring lo avisa en el log al
  arrancar con el mismo comando.
- Django en producción: `DJANGO_DEBUG=false`, `DJANGO_SECRET_KEY` propia y
  `DJANGO_ALLOWED_HOSTS` reales. Spring: `CORS_ORIGENES` con el origen de la web.

## 5) App Android (móvil)

Crea `mobile-kotlin\local.properties`:

```
sdk.dir=C\:\\Users\\<usuario>\\AppData\\Local\\Android\\Sdk
```

La app necesita saber a qué host conectarse. Se configura **por máquina** en
`local.properties` (está en `.gitignore`, no se sube al repo):

```
plantia.api.baseUrl=http://<IP-WIFI-DE-TU-PC>:8080/
plantia.api.fallbackUrls=http://127.0.0.1:8080/,http://10.0.2.2:8080/
```

| Clave | Significado |
|---|---|
| `plantia.api.baseUrl` | Principal. La **IP de tu PC en la WiFi** → el móvil va directo, sin cable |
| `plantia.api.fallbackUrls` | Respaldos: `127.0.0.1` (USB con `adb reverse`) y `10.0.2.2` (emulador) |

Al arrancar, la app **prueba** la principal y, si no responde, cae al primer
respaldo que conteste. Así funciona por WiFi y por USB sin recompilar.

También puedes fijarlo en la invocación de Gradle (tiene prioridad sobre
`local.properties`):

```powershell
.\gradlew.bat :app:installDebug -PapiBaseUrl=http://<IP-LAN-PC>:8080/
```

### Puente USB automático

`adb reverse` vive en la sesión ADB del cable: **al desconectar y reconectar se
pierde** y el móvil deja de alcanzar `127.0.0.1:8080`. Para que se reponga solo,
deja el script corriendo:

```powershell
scripts\conectar-dispositivo.bat
```

Vigila la conexión ADB y reaplica `adb reverse tcp:8080 tcp:8080` cada 2 s.
Puedes dejarloMinimizado; cierra con `Ctrl+C`.

### Verificar desde el teléfono

```powershell
adb shell curl -s http://127.0.0.1:8080/api/v1/usuarios/me   # por USB
adb shell curl -s http://<IP-LAN-PC>:8080/api/v1/usuarios/me # por WiFi
```

Abre la app → cámara → **Capturar** una planta y verás la especie real.

> Con **emulador** no hace falta `adb reverse`; `10.0.2.2` ya está en los
> respaldos.

## Orden de arranque

1. `docker compose up -d postgres flyway`
2. Django en **8000**
3. Spring Boot en **8080**
4. App móvil (`adb reverse` + install)

## IA institucional (Open WebUI + vLLM)

El motor por defecto del proyecto es el servidor institucional: **Open WebUI**
(proxy de vLLM) con API compatible con OpenAI.

| Dato | Valor |
|---|---|
| Base URL | `http://192.168.17.11:3000/api/v1` |
| Modelo | `Qwen/Qwen3.6-35B-A3B-FP8` (multimodal, con visión) |
| Auth | `Authorization: Bearer <API key de Open WebUI>` |

En `backend-django/.env`:

```
IA_PROVIDER=webui
WEBUI_BASE_URL=http://192.168.17.11:3000/api/v1
WEBUI_API_KEY=<tu API key de Open WebUI>
WEBUI_MODEL=Qwen/Qwen3.6-35B-A3B-FP8
```

Proveedores disponibles en Django (`ia_engine/providers/`):

| `IA_PROVIDER` | Uso |
|---|---|
| `webui` | **Institucional** (Open WebUI / vLLM). Requiere `WEBUI_API_KEY` |
| `gemini` | Nube de Google. Requiere `GEMINI_API_KEY` |
| `openai` | Nube de OpenAI. Requiere `OPENAI_API_KEY` |
| `local` | Ollama directo (`LOCAL_IA_URL` / `LOCAL_IA_MODEL`) |
| `mock` | **Solo pruebas**: inventa siempre Monstera. Solo se usa si se pide a propósito |
| `auto` | Elige el primero disponible: openai → webui → gemini → local (sin ninguno: error 502, nunca `mock`) |

### Respaldo automático entre motores

Los motores se prueban **en cadena**. Si el configurado no está disponible
(servidor caído, sin red, saturado), el siguiente responde:

```
webui (institucional)  ->  cae  ->  gemini  ->  cae  ->  openai  ->  ...  ->  502
```

- El motor caído se marca durante **60 s** (circuit breaker) para no pagar su
  timeout en cada escaneo.
- La respuesta trae `provider` con el motor que realmente respondió.
- Solo se devuelve error (502) si **todos** los motores reales fallan; nunca se
  inventan datos con `mock`.

| Evento | Resultado |
|---|---|
| IA local disponible | Responde `webui` |
| IA local caída | Responde `gemini`, sin intervention |
| Todo caído | HTTP 502 con el detalle de cada motor |

### Usarlo como modelo en opencode

El repo incluye `opencode.json` con el proveedor `plantia-institucional`.
Define la clave por variable de entorno para no subirla al repositorio:

```powershell
setx PLANTIA_WEBUI_API_KEY "sk-..."   # una vez, luego reinicia opencode
```

Después aparece en `/models` como
`plantia-institucional/Qwen/Qwen3.6-35B-A3B-FP8`.

## Pantallas de la app

| Pantalla | Endpoint que consume |
|---|---|
| **Inicio** | `/usuarios/me`, `/eventos/activo`, `/plantas/activa`, `/misiones/diaria`, `/accesos` |
| **Escanear** | `POST /escaneos` (cámara o galería) + `/escaneos/recientes` |
| **Historial** | `/escaneos/recientes` |
| **Mi colección** | `/coleccion` y `/coleccion/{id}` (ficha con cuidados) |
| **Enciclopedia** | `/catalogo?q=&toxica=` y `/catalogo/{id}/cuidados` |
| **Mi jardín** | `/plantas/activa` |
| **Perfil** | `/usuarios/me` |

### Sesión de usuario

Cada jugador crea su cuenta desde **Registro** e inicia sesión con su correo
y clave (`POST /api/v1/auth/login`). La app guarda su id y su token de sesión
y los añade a **todas** las peticiones (`X-User-Id` y `X-Session-Token`). La
base no trae usuarios de prueba (V33).

## Conectar el móvil sin depender de la red

La IP del PC cambia al cambiar de WiFi, así que la app prueba varias rutas y
se queda con la primera que responde:

| Prioridad | Ruta | Cuándo funciona |
|---|---|---|
| 1 | **Host escrito a mano** (Perfil → Conexión) | Siempre, si escribes la IP actual |
| 2 | **mDNS** (`plantia.local`) | Móvil y PC en la misma red, sin conocer la IP |
| 3 | `plantia.api.baseUrl` de `local.properties` | La IP con la que se compiló |
| 4 | `127.0.0.1` (USB con `adb reverse`) | Con el cable conectado |
| 5 | `10.0.2.2` | Emulador de Android |

### Publicar el PC en la red (mDNS)

En el PC, con el backend arrancado:

```powershell
scripts\anunciar-plantia.bat
```

Publica el servicio `_plantia._tcp.local.` con la IP **actual**. La app lo
encuentra sola al arrancar, así que da igual a qué WiFi te conectes.

> Requiere `python -m pip install -r scripts\requirements-mdns.txt` (el script
> lo instala solo si falta).

### Si el mDNS no funciona

 Algunos routers filtran el tráfico mDNS. En ese caso, **Perfil → Conexión →
 Dirección del servidor** y escribe `http://<IP-DEL-PC>:8080/`. La app lo
 guarda y lo usa siempre.

## Problemas comunes

- **Al guardar una foto la identificación es siempre "Monstera"**: estás en
  `mock`. Revisa `IA_PROVIDER` en `.env`.
- **El escaneo dice que no hay ningún motor de IA configurado**: falta la clave
  de Gemini (u otro motor) en `backend-django\.env`.
- **No puedo entrar al panel admin**: la base no trae admin. Créalo con
  `python manage.py crear_admin --email <tu-correo>` (sección 4).
- **Django responde 503/429 con Gemini**: es saturación temporal del tier
  gratuito; el proveedor reintenta solo. Si persiste, prueba de nuevo.
- **`webui` da 401**: el API key de Open WebUI expiró o no es el correcto.
  Revísalo en Open WebUI → *Settings → API Keys*.
- **El modelo no identifica bien una planta**: debe ser multimodal. Con un
  modelo solo-texto la respuesta sale sin especie.
- **La app no conecta ("failed to connect")**: revisa la sección *Conectar el
  móvil sin depender de la red*; normalmente es la IP del PC que cambió.
- **Spring no alcanza a Django**: confirma `IA_BASE_URL=http://127.0.0.1:8000`
  y que Django escuche en `127.0.0.1:8000`.
- **El puerto 5432 está ocupado**: usa el contenedor en `5433` (paso 1) y ajusta
  los puertos en Django (`.env`) y Spring (`DB_URL`).
- **El puerto 8000 está ocupado** (WSL/Docker lo toma en `::`): levanta Django
  en `8001` y ajusta `IA_BASE_URL=http://127.0.0.1:8001`.
