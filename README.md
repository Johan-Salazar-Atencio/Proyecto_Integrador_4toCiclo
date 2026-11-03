# Plantia 🌱

App integral de cuidado de plantas: app móvil nativa (Kotlin), web de usuario + panel de administración (React) y dos backends conectados a una base PostgreSQL común.

## Arquitectura general

```
                       ┌────────────────────┐
   App móvil  ─────────┤   SPRING BOOT 8080 │────► PostgreSQL "plantia" (5432)
   (Kotlin, /api/v1)   │   API de la app    │      (Flyway: esquema V1–V5)
                       └─────────┬──────────┘
                                 │ llama a
                       ┌─────────▼──────────┐
   Web React ──────────┤   DJANGO 8000      │────► misma BD (modo lectura)
   (5173) /api → 8080  │   Motor IA + admin │      (webui → gemini → mock)
           /django → 8000                        WebUI institucional: 192.168.17.11:3000
```

**Reparto de responsabilidades**

| Componente | Backend | Uso |
|---|---|---|
| App móvil (auth, colección, escaneos, plantas, eventos) | Spring Boot `/api/v1` | Real |
| Web de usuario (Home, colección, escaneo, catálogo…) | Spring Boot `/api/v1` vía proxy Vite | Real |
| Panel admin — lectura (catálogo, eventos, dashboard) | Django `/api/catalog/…` vía proxy Vite | Real |
| Panel admin — escritura (login admin, guardar/publicar especie, lanzar evento) | *(pendiente — `TODO(django)`)* | Simulado en navegador |

La autenticación móvil usa el header `X-User-Id` (igual en web y móvil). El motor IA vive en Django (`POST /api/ia/identificar`), Spring lo consume de forma transparente.

## Cómo correr cada pieza

### 1. Base de datos (PostgreSQL 5432)
- BD: `plantia`, usuario/contraseña: `plantia/plantia`
- Flyway aplica migraciones de `db/migration/` (V1–V5) automáticamente al arrancar Spring Boot
> V5 agregó `publicada BOOLEAN DEFAULT TRUE` a `plantas_catalogo`

### 2. Spring Boot — API de la app
```bash
cd backend-spring-boot
mvn spring-boot:run        # puerto 8080
```
- Saludo: `GET /api/health` → `{"status":"ok"}`
- Contrato de la app verificado por `ContratoMovilTest` (DTOs = modelos del móvil)
- Debe escuchar en `0.0.0.0` para el túnel adb del móvil

### 3. Django — motor IA + servicios admin
```bash
cd backend-django
pip install -r requirements.txt   # (primera vez)
python manage.py runserver 0.0.0.0:8000 --noreload
```
- Saludo: `GET /api/health`
- Endpoints IA: `POST /api/ia/identificar` (multipart, campo `imagen`), `POST /api/ia/cuidados`
- Config en `.env`: `WEBUI_*` (motor institucional), `GEMINI_API_KEY`, `IA_PROVIDER`

**Cadena de motores IA** (`ia_engine/providers/factory.py`): OpenAI → WebUI → Gemini → Local → Mock.
El WebUI institucional (`192.168.17.11:3000`), su timeout es corto (3 s en
`webui_provider.py`) para pasar rápido a Gemini cuando no es alcanzable desde la red local.

### 4. Web React
```bash
cd frontend-react
npm install        # primera vez
npm run dev        # http://localhost:5173
```
El proxy de `vite.config.js` reenvía `/api` y `/files` a Spring (8080) y `/django` a Django (8000).

### 5. App móvil
```bash
cd mobile-kotlin
# Android Studio o: ./gradlew installDebug
```
- URL del API configurada en `local.properties`:
  - `plantia.api.baseUrl=http://192.168.1.70:8080/` (WiFi direto)
  - fallback: `127.0.0.1:8080` (vía `adb reverse`)
- **Túnel USB**: `adb reverse tcp:8080 tcp:8080`. El script `tunel-plantia.ps1`
  re-establece el reverse automáticamente si se desconecta el cable:
  ```powershell
  powershell -ExecutionPolicy Bypass -File tunel-plantia.ps1
  ```
- Firewall: regla TCP 8080 ya creada (`Plantia API 8080`) para la vía WiFi

## Usuarios de prueba (seed, Flyway V3/V4)

| ID | Nombre | Email |
|---|---|---|
| 1 | Johan | johan@plantia.test |
| 2 | *(2° usuario)* | — |

## Estado actual

| Pieza | Estado |
|---|---|
| Spring Boot 8080 | ✅ Funcionando (fixes de compilación en `feat/backend-integracion`) |
| Django 8000 | ✅ Funcionando |
| IA de identificación | ✅ Funcional (Gemini; WebUI solo si se está en la red institucional) |
| Esquema BD | ✅ Flyway V1–V5 |
| Móvil Android | ✅ Compila, instala y conecta |
| Web usuario (React) | ✅ Todo contra Spring (endpoint reales) |
| Panel admin (React) | 🟡 Lectura real (Django); escritura simulada |
| Autenticación real (JWT / sesiones) | ⬜ Pendiente (equipo) |
| Endpoints WRITER de Django (login admin, catálogo, eventos) | ⬜ Pendiente — ver `frontend-react/src/services/admin/*.js` con los `TODO(django)` |

## Convenciones de trabajo

- Los DTOs del API móvil viven únicamente en `backend-spring-boot/src/main/java/com/plantia/dto/` (contract test: `ContratoMovilTest`)
- Ramas de trabajo del equipo: `feat/...`, `chore/...`; `main` se integra por PR
- Migraciones de BD: siempre SQL nuevo en `db/migration/V{n}__descripcion.sql`, nunca editar scripts aplicados
