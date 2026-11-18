# Bitácora — Operación, entorno y procedimientos (OPE)

Procedimientos que se usaron de verdad durante el periodo (10 al 18 de noviembre de 2026) en la máquina de desarrollo (Windows 11, Git Bash y PowerShell), con los problemas que dio cada uno.

- [OPE-01 Levantar todo (PG, Django, Spring, Vite)](#ope-01--levantar-todo)
- [OPE-02 Problemas típicos al arrancar](#ope-02--problemas-típicos-al-arrancar)
- [OPE-03 Ejecutar los tests](#ope-03--ejecutar-los-tests)
- [OPE-04 Usuarios de prueba y cómo restaurarlos](#ope-04--usuarios-de-prueba-y-cómo-restaurarlos)
- [OPE-05 Consultar la BD desde Python](#ope-05--consultar-la-bd-desde-python)
- [OPE-06 Sesión única: por qué se cierra la sesión del navegador](#ope-06--sesión-única-por-qué-se-cierra-la-sesión-del-navegador)
- [OPE-07 Probar un escaneo con curl en Windows](#ope-07--probar-un-escaneo-con-curl-en-windows)
- [OPE-08 Usar la app desde el móvil por internet](#ope-08--usar-la-app-desde-el-móvil-por-internet)
- [OPE-09 Prueba de concurrencia (carrera)](#ope-09--prueba-de-concurrencia-carrera)
- [OPE-10 Reglas de git del proyecto y lo que pasó al integrar](#ope-10--reglas-de-git-del-proyecto-y-lo-que-pasó-al-integrar)
- [OPE-11 Estado de las ramas al cierre de esta bitácora](#ope-11--estado-de-las-ramas-al-cierre-de-esta-bitácora)

---

## OPE-01 — Levantar todo

Orden recomendado: PostgreSQL → Django → Spring (aplica las migraciones al arrancar) → Vite.

| Servicio | Puerto | Comando (Git Bash, desde la raíz del repo) |
|---|---|---|
| PostgreSQL 18 nativo | 5432 | servicio de Windows (Docker Desktop no arranca bien en esta máquina) |
| Django | 8000 | `cd backend-django && DB_PORT=5432 DB_HOST=127.0.0.1 PYTHONIOENCODING=utf-8 .venv/Scripts/python.exe manage.py runserver 127.0.0.1:8000 --noreload` |
| Spring | 8080 | ver el bloque PowerShell más abajo |
| Vite (web) | 5173 | `cd frontend-react && npx vite --host 127.0.0.1 --port 5173` |

Spring desde PowerShell: `mvn` no está en el PATH, así que se usa el Maven del *wrapper*.

```powershell
$mvn = Get-ChildItem "$env:USERPROFILE\.m2\wrapper\dists\apache-maven-*\*\bin" -Directory | Select-Object -First 1
$env:Path = "$($mvn.FullName);$env:Path"
$env:DB_URL = "jdbc:postgresql://127.0.0.1:5432/plantia?stringtype=unspecified"
$env:IA_BASE_URL = "http://127.0.0.1:8000"
Set-Location "backend-spring-boot"
mvn.cmd -q spring-boot:run "-Dspring-boot.run.jvmArguments=--enable-native-access=ALL-UNNAMED"
```

Spring está listo cuando el log muestra:
- `Successfully applied N migrations ... now at version vNN` (si había migraciones pendientes);
- `Started PlantiaApplication in X seconds`.

**Por qué cada opción:**
- `--noreload` en Django: el *autoreloader* lanza un segundo proceso que, en segundo plano, deja el puerto ocupado al pararlo.
- `127.0.0.1` en lugar de `localhost`: ver OPE-02.
- `stringtype=unspecified`: deja que PostgreSQL convierta los textos de Java a columnas JSON o *enum* sin errores de tipo.
- `--enable-native-access=ALL-UNNAMED`: Java 26 avisa o bloquea el acceso nativo de algunas librerías.

Comprobaciones rápidas:

```bash
curl -s http://127.0.0.1:8000/api/health      # Django
curl -s http://127.0.0.1:8080/api/health      # Spring
curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:5173/   # Vite → 200
```

---

## OPE-02 — Problemas típicos al arrancar

| Síntoma | Causa | Solución |
|---|---|---|
| Spring: `Connection refused` a la BD | `localhost` → `::1` (IPv6) y PG escucha en IPv4 | `DB_URL` con `127.0.0.1` |
| Todos los escaneos dan "Planta desconocida" | `IA_BASE_URL` con `localhost`, Django apagado, o el fallo de IA-02 | `127.0.0.1`; revisar `spring.log` |
| `Port 8080 was already in use` | quedó un Spring anterior | buscar el PID con `ps -W` en Git Bash (`Get-NetTCPConnection` no devuelve nada aquí) o `netstat -ano \| grep ":8080 "`, y matarlo |
| Django: `ImproperlyConfigured: Falta DJANGO_SECRET_KEY` | `.env` sin clave y sin DEBUG | `DJANGO_DEBUG=true` en desarrollo |
| Flyway: `Found more than one migration with version N` | dos ramas crearon la misma versión | BD-01 |
| Flyway: `Validate failed: checksum mismatch` | alguien editó una migración ya aplicada | revertir la edición; nunca editar una aplicada |
| Spring: `UnicodeEncodeError` o caracteres raros en Django | consola de Windows en cp1252 | `PYTHONIOENCODING=utf-8` |
| Tests de Spring: 92 errores de Mockito | Java 26 | OPE-03 |
| La web muestra la pantalla de acceso nada más entrar | sesión cerrada por un login en otro sitio | OPE-06 |

---

## OPE-03 — Ejecutar los tests

| Proyecto | Comando | Resultado al 18-nov |
|---|---|---|
| Spring | `JAVA_HOME="/c/Program Files/Java/jdk-21.0.13.0.3" mvn test` (Git Bash) | **186 tests, 0 fallos y 0 errores** |
| Django | `cd backend-django && DB_ENGINE=sqlite3 .venv/Scripts/python.exe manage.py test ia_engine catalog` | pasa en el CI |
| Web | `cd frontend-react && npm run build` | compila (no hay tests ni lint) |
| Móvil | `cd mobile-kotlin && ./gradlew :app:testDebugUnitTest` | **no ejecutado en este periodo** |

Con `-q`, Maven no muestra el resumen. Para contar los resultados, lee los informes de `target/surefire-reports/*.txt`:

```bash
cat target/surefire-reports/*.txt | grep "Tests run" \
  | awk -F'[ ,]+' '{t+=$3; f+=$5; e+=$7} END {print "total",t,"fallos",f,"errores",e}'
```

**Clases de test tocadas en el periodo:**
- `ManejadorErroresTest` (4 tests)
- `EscaneoServiceTest` (4)
- `AbonoServiceTest` (9)
- `PlantasServiceTest`
- `StorageServiceTest`
- `TokenSesionTest`
- `IaClientTest`
- `ContratoMovilTest`
- Django: `PermisoMotorIaTests` y `catalog/tests.py`

---

## OPE-04 — Usuarios de prueba y cómo restaurarlos

| Correo | `id_usuario` | Contraseña | Rol | Estado de referencia |
|---|---|---|---|---|
| `johan@plantia.test` | 1 | `plantia123` | USER | — |
| `lucia@plantia.test` | 2 | `plantia123` | USER | 150 monedas, 1000 XP, nivel 3, racha 1, `ultima_actividad = NULL` |
| `admin@plantia.test` | — | `plantia123` | ADMIN | panel admin |

Las contraseñas vienen de V6 y V9 (deuda de seguridad, SEG-09a).

**Estado de lucia tras las pruebas del 18-nov.** Se usó para las pruebas de carrera y escaneo, y se restauró dos veces:
- sin las macetas 10 y 11 en `inventario_usuario`;
- sin los escaneos 41–44 ni los movimientos 36–38 de `historial_progreso`;
- `misiones_usuario` 20 (misión de escaneo) con progreso 0 y sin completar.

Plantilla de restauración (adaptar los ids):

```python
import psycopg
with psycopg.connect('host=127.0.0.1 dbname=plantia user=plantia password=plantia') as c:
    c.execute('delete from historial_progreso where id_usuario=2 and id_movimiento in (...)')
    c.execute('delete from escaneos_historial where id_usuario=2 and id_escaneo between ... and ...')
    c.execute('delete from inventario_usuario where id_usuario=2 and id_item in (...)')
    c.execute('update misiones_usuario set progreso=0, completada=false where id_mision_usuario=... and id_usuario=2')
    c.execute('update usuarios set monedas=150, experiencia_xp=1000, racha_dias=1, ultima_actividad=null where id_usuario=2')
# al salir del with sin errores se hace commit
```

No olvides borrar las fotos de prueba en `backend-spring-boot/storage/images/scan_AAAAMMDD_HHMMSS_xxxxxxxx.*`.

> **Error que ya pasó:** un `rollback()` en la misma conexión deshizo la restauración. Usa el `with` o un `commit()` explícito (ECO-01).

---

## OPE-05 — Consultar la BD desde Python

No hay `psql` en el PATH. Se usa el `psycopg` del *venv* de Django:

```bash
backend-django/.venv/Scripts/python.exe -c "
import psycopg
c = psycopg.connect('host=127.0.0.1 dbname=plantia user=plantia password=plantia')
for r in c.execute('select id_usuario, monedas, experiencia_xp, nivel from usuarios order by 1'): print(r)
"
```

Consultas útiles:

```sql
-- versión de esquema aplicada
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
-- últimos escaneos con la respuesta de la IA
SELECT id_escaneo, id_catalogo, confianza_ia, respuesta_ia_raw->>'nombre_cientifico'
FROM escaneos_historial ORDER BY id_escaneo DESC LIMIT 10;
-- premios de hoy de un usuario
SELECT h.*, r.motivo FROM historial_progreso h LEFT JOIN reglas_progreso r USING (id_regla)
WHERE id_usuario = 2 AND fecha >= current_date ORDER BY fecha;
-- especies sospechosas (IA-05)
SELECT id_catalogo, nombre_cientifico FROM plantas_catalogo WHERE nombre_cientifico ILIKE 'plantae%';
```

Nombres de columnas que confunden:
- la clave de `historial_progreso` es `id_movimiento`, no `id_historial`;
- la de `misiones_usuario` es `id_mision_usuario`.

---

## OPE-06 — Sesión única: por qué se cierra la sesión del navegador

`AuthService.login` sobrescribe `usuarios.session_token`. Consecuencias que se vieron en las pruebas:
- Iniciar sesión como lucia con curl para una prueba **cerró la sesión de lucia en el navegador**: la siguiente acción en la web dio 401 y volvió a la pantalla de acceso.
- El panel admin (Django) y la app (Spring) usan la **misma** columna: iniciar sesión como admin en uno cierra la sesión del admin en el otro.

**Procedimiento:**
1. Guarda la sesión de la prueba en un archivo (`lucia.txt` con `id token`).
2. Reutilízala en todos los scripts en lugar de hacer login cada vez.
3. Si además quieres usar el navegador con el mismo usuario, vuelve a iniciar sesión en él al terminar.

---

## OPE-07 — Probar un escaneo con curl en Windows

**Problema:** el `curl.exe` nativo de Windows no entiende rutas `/c/...` de Git Bash en `-F "foto=@/c/..."`, y falla con `Failed to open/read local data`.

**Solución:** ejecutar desde la carpeta donde está la foto, con una ruta relativa.

```bash
cd <carpeta con la foto>
read U T < lucia.txt
curl -s -m 90 -H "X-User-Id: $U" -H "X-Session-Token: $T" \
     -F "foto=@tomate.jpg;type=image/jpeg" http://127.0.0.1:8080/api/v1/escaneos
```

- `;type=image/jpeg` hace falta si la extensión no es estándar (por ejemplo `.img`). Spring valida por *magic bytes*, pero el `contentType` se reenvía a la IA.
- **Cada escaneo identificado da premio y cuenta misiones.** Restaura al usuario después (OPE-04).

Fotos que sirven para probar: las ya subidas en `backend-spring-boot/storage/images/`, que se descargan en `http://127.0.0.1:8080/files/<nombre>`. Por ejemplo, la del tomate (`id_catalogo` 11) se identificó con 99 %. El dibujo sintético de monstera (`plant-monstera.png`, generado por `create_monstera.py`) **no** es una buena prueba positiva: la IA responde "Plantae gen. sp." y desde IA-05 se rechaza, como debe ser.

---

## OPE-08 — Usar la app desde el móvil por internet

Se planteó el 18-nov. Hay dos opciones:

| Opción | Cómo | Ventajas | Inconvenientes |
|---|---|---|---|
| 1. Desplegar en la nube | PG gestionado (Neon, Supabase, RDS), Spring y Django en un servicio de contenedores, web estática | Funciona con el PC apagado | Coste, configurar variables (REF-04), fotos en almacenamiento externo (FUN-04), `DJANGO_SECRET_KEY`, `IA_CLAVE_INTERNA` |
| 2. Túnel desde el PC | `cloudflared tunnel` o `ngrok http 8080`; **el portátil hace de servidor** | Rápido y gratis | El PC debe estar encendido con todo levantado; URL pública temporal |

Para la opción 2:
- el móvil debe apuntar a la URL del túnel (configuración de conexión del móvil, `ConexionConfig.kt`);
- `STORAGE_PUBLIC_URL` debe ser `https://<túnel>/files`, o las fotos nuevas tendrán `localhost` en la URL;
- con una URL pública, las deudas de seguridad SEG-09 (contraseñas demo, sin límite de login, CORS `*`) pasan a ser **urgentes**.

---

## OPE-09 — Prueba de concurrencia (carrera)

Script guardado en [`docs/bitacora/herramientas/carrera.py`](herramientas/carrera.py). Se ejecuta desde una carpeta que tenga `lucia.txt` (`<id> <token>`) y con Spring levantado:

```bash
cd <carpeta con lucia.txt>
python "<repo>/docs/bitacora/herramientas/carrera.py"
```

Qué hace:
- antes de cada intento, pone a lucia en 150 monedas y le quita las macetas 10 (80 hojas) y 11 (100 hojas);
- lanza 16 hilos con `threading.Barrier(16)` para que salgan a la vez, alternando `POST /api/v1/tienda/10/comprar` y `/tienda/11/comprar`;
- cuenta los códigos de respuesta y, al final, consulta en la BD los artículos y el saldo;
- calcula `pagado = 150 - monedas` y `valor = 80·(tiene 10) + 100·(tiene 11)`; si `valor > pagado`, imprime `<-- DOBLE GASTO`;
- 5 intentos.

**Ojo:** el script deja a lucia sin las macetas 10 y 11 y con el saldo que quede. Restáurala después (OPE-04).

**Criterio de éxito:** en cada repetición, como mucho 1 compra con 200, saldo ≥ 0 y ningún 500 en `spring.log`.

Usa el mismo patrón para cualquier operación que cambie saldo (abonar, revivir, premios).

---

## OPE-10 — Reglas de git del proyecto y lo que pasó al integrar

### Reglas
1. **Nunca `git add -A`.** Suele haber cambios sin commitear de compañeros:
   - scripts `run_*.ps1`, `start_server.py`;
   - logs (`django_*.log`, `spring_boot.log`);
   - `.idea`, `CLAUDE.md`, `.claude/`;
   - `test_gemini.py`.

   Añade solo tus archivos, por ruta.
2. **Archivo con cambios tuyos y ajenos mezclados:** prepara solo tu parte sin tocar el archivo de trabajo:
   ```bash
   blob=$(git show :ruta | <aplicar tu cambio> | git hash-object -w --stdin)   # o hash-object -w de una copia editada
   git update-index --cacheinfo 100644,$blob,ruta
   ```
3. **Integrar ramas solo con `merge`** (no `rebase` ni `push --force`) y no borrar ramas: es lo que se pidió ("no uses nada que pueda eliminar otras cosas").
4. **Antes de una integración grande, crea una rama de respaldo y súbela:** `respaldo/jose-2026-11-18` (`e1f499b`).
5. **Copia de lo no commiteado:** `respaldos/respaldo-local-sin-commitear.tgz`.
6. **Los mensajes de commit no llevan coautoría de herramientas** (preferencia del autor). Algunos commits del 10-nov la llevan; se dejaron como estaban, porque reescribir el historial publicado está prohibido por la regla 3.

### Integraciones del periodo

| Fecha | Qué | Cómo | Resultado |
|---|---|---|---|
| 15-nov | PR #2 `fix/auditoria-seguridad` → `feat/flujo-escaneo-guardado-virtualizacion` | `gh pr merge --merge` | sin conflictos; CI en verde tras CI-02 y CI-03 |
| 15-nov | PR #1 `feat/flujo-escaneo-guardado-virtualizacion` → `main` | `gh pr merge --merge` | sin conflictos |
| 16-nov | PR #3 de Johan (127 commits, V13–V25) en `main` | (hecho por Johan) | — |
| 18-nov | `rama-de-johan` (37 commits) → `integracion/rediseno-johan` | `git merge` local, subida la rama | sin conflictos (`0200c35`); se comprobó que seguían las correcciones de la auditoría |
| 18-nov | `main` local hasta `0200c35` | *fast-forward*, **sin push** | — |

---

## OPE-11 — Estado de las ramas al cierre de esta bitácora

| Rama | Commit | Dónde |
|---|---|---|
| `main` (local) | `96ff15a` | **41 commits por delante de `origin/main`** (37 de Johan, la fusión `0200c35`, y `76da9a7`, `3033a0d` y `96ff15a`) |
| `origin/main` | `e1f499b` | GitHub |
| `origin/respaldo/jose-2026-11-18` | `e1f499b` | respaldo previo a la integración |
| `origin/integracion/rediseno-johan` | `0200c35` | fusión del rediseño, sin las 3 correcciones locales |
| `origin/rama-de-johan` | `d2034ba` | rama de trabajo de Johan |

**Pendiente de decidir:** subir `main` (o abrir un PR desde una rama con estos commits). Antes, avisar al equipo de V27 y V28 (BD-01).
