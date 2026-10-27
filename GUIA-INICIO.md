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

> Si no pones clave de Gemini, deja `IA_PROVIDER=mock`: el flujo funciona pero
> la identificación es simulada (siempre Monstera).

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
| `mock` | Simulado, sin claves (siempre devuelve Monstera) |
| `auto` | Elige el primero disponible: openai → webui → gemini → local → mock |

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

## Problemas comunes

- **Al guardar una foto la identificación es siempre "Monstera"**: estás en
  `mock`. Revisa `IA_PROVIDER` en `.env`.
- **Django responde 503/429 con Gemini**: es saturación temporal del tier
  gratuito; el proveedor reintenta solo. Si persiste, prueba de nuevo.
- **`webui` da 401**: el API key de Open WebUI expiró o no es el correcto.
  Revísalo en Open WebUI → *Settings → API Keys*.
- **El modelo no identifica bien una planta**: debe ser multimodal. Con un
  modelo solo-texto la respuesta sale sin especie.
- **La app no conecta ("failed to connect")**: la URL base se fija al compilar.
  Revisa `plantia.api.baseUrl` en `local.properties` y que la IP sea la actual
  (`ipconfig`). Si usas USB, ejecuta `scripts\conectar-dispositivo.bat`: el
  puente `adb reverse` se pierde en cada reconexión del cable.
- **Spring no alcanza a Django**: confirma `IA_BASE_URL=http://127.0.0.1:8000`
  y que Django escuche en `127.0.0.1:8000`.
- **El puerto 5432 está ocupado**: usa el contenedor en `5433` (paso 1) y ajusta
  los puertos en Django (`.env`) y Spring (`DB_URL`).
- **El puerto 8000 está ocupado** (WSL/Docker lo toma en `::`): levanta Django
  en `8001` y ajusta `IA_BASE_URL=http://127.0.0.1:8001`.
