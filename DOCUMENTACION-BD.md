# Documentación de la base de datos — Plantia

Guía para entender cómo funciona la capa de datos del proyecto: dónde vive el
DDL, quién lo aplica, cómo lo leen/escriben los backends y cómo inspeccionarla.

## 1. La idea central

Hay **una sola base PostgreSQL** compartida por **dos backends**, y una regla de
oro que explica todo el diseño:

| Backend | Rol | ¿Toca el DDL? |
|---|---|---|
| **Spring Boot** (`backend-spring-boot`) | Dueño del esquema. Aplica las migraciones **Flyway** y sirve la API del móvil | **Sí** (crea tablas) |
| **Django** (`backend-django`) | Admin + motor de IA. Solo **lee/escribe datos** | **No** (modelos `managed = False`) |

El DDL **nunca vive en Java ni en Python**: vive en `db/migration/*.sql`. Si
Django generara sus migraciones, chocaría con Spring.

```
db/migration/
├── V1__schema_inicial.sql                        # 11 tablas base
├── V2__coleccion_cuidados_y_campos_faltantes.sql # +6 tablas y columnas nuevas
└── V3__seed_desarrollo.sql                       # datos de prueba
```

## 2. ¿Quién ejecuta esas migraciones?

Dos vías, ambas apuntan a la **misma tabla `flyway_schema_history`** (no se pisan):

- El servicio `flyway` de `docker-compose.yml` (contenedor).
- Spring al arrancar (`application.yml`), buscando en `filesystem:../db/migration`.

## 3. El esquema (17 tablas)

### Núcleo
- **`usuarios`** — `id_usuario` (PK, SERIAL), email único, `password_hash`,
  `rol` (USER/ADMIN), `nivel`, `experiencia_xp`, `monedas`, `racha_dias`,
  `region`, `updated_at` (añadido en V2).
- **`plantas_catalogo`** — el corazón: `id_catalogo` (PK), `nombre_comun`,
  `nombre_cientifico` (**UNIQUE**), frecuencias de riego/sol, rangos de
  temperatura, `es_toxica_mascotas`, sprites por etapa, `datos_ia_raw` (**JSONB**).

### IA / colección
- **`escaneos_historial`** — una fila por cada foto escaneada: `id_usuario`,
  `id_catalogo`, `imagen_foto_url`, `confianza_ia NUMERIC(5,2)`,
  `respuesta_ia_raw` (**JSONB**, la respuesta cruda del modelo).
- **`cuidados_secciones`** — **1:N con `plantas_catalogo`**. Cada sección:
  `tipo` (RIEGO/LUZ/SUSTRATO/TEMPERATURA/PODA/PLAGAS/TOXICIDAD/ABONO/OTRO),
  `titulo`, `contenido`, `orden`, `generado_por_ia`.
- **`coleccion_usuario`** — "mi colección": `UNIQUE(id_usuario, id_catalogo)`,
  `veces_vista`, `primera_vez`, `ultima_vez`.

### Juego / resto
- **`plantas_usuario`** — la planta "tamagotchi": `salud_porcentaje`,
  `hidratacion_porcentaje`, `sol_porcentaje`, `etapa_crecimiento`
  (SEMILLA/BROTE/ADULTA/MUERTA), `modo_tiempo`, etc.
- **`eventos_desastre`** + **`registros_desastre_usuario`** — clima y su impacto.
- **`items_tienda`** + **`inventario_usuario`** — tienda.
- **`logros`** + **`logros_usuario`** (N:M).
- **`niveles`**, **`misiones`**, **`misiones_usuario`** — progresión.

### Relaciones

```
usuarios 1─N escaneos_historial         (CASCADE)
usuarios 1─N plantas_usuario            (CASCADE)
usuarios 1─N coleccion_usuario          (CASCADE)
usuarios 1─N eventos_desastre           (SET NULL, id_creador)
plantas_catalogo 1─N cuidados_secciones (CASCADE)
plantas_catalogo 1─N escaneos_historial (SET NULL)
plantas_catalogo 1─N plantas_usuario    (RESTRICT)
plantas_catalogo 1─N coleccion_usuario  (SET NULL)
escaneos_historial 1─N coleccion_usuario (SET NULL)
eventos_desastre 1─N registros_desastre_usuario (CASCADE)
plantas_usuario  1─N registros_desastre_usuario (CASCADE)
items_tienda     1─N inventario_usuario (CASCADE)
misiones         1─N misiones_usuario   (CASCADE)
usuarios         N─M logros (via logros_usuario)
```

### Convenciones del esquema
- Nombres **snake_case** y PK siempre `id_<tabla>`.
- `SERIAL` + `@GeneratedValue(IDENTITY)` en JPA.
- `CHECK` para enums (rol, etapa, estado…).
- `JSONB` para respuestas de IA (`datos_ia_raw`, `respuesta_ia_raw`).
- `updated_at` para cache/ETag del móvil.
- Índices en FKs y columnas de filtro (`db/migration/V1__schema_inicial.sql`).

## 4. Cómo lo lee/escribe cada backend

### Lado Spring (JPA) — `backend-spring-boot/src/main/java/com/plantia/models/`

Cada tabla tiene su `@Entity` espejo. Ejemplo `PlantaCatalogo.java`: mapeo 1:1 con
filas y columnas (`@Column(name = "nombre_comun")`). El JSONB se mapea como
`String` con `columnDefinition = "jsonb"`.

El acceso va por **repositorios** `JpaRepository`:

| Repositorio | Consultas clave |
|---|---|
| `PlantaCatalogoRepository` | `findByNombreCientificoIgnoreCase` + `JpaSpecificationExecutor` |
| `EscaneoHistorialRepository` | `findTop20ByIdUsuarioOrderByFechaEscaneoDesc` |
| `CuidadoSeccionRepository` | `findByIdCatalogoOrderByOrdenAsc`, `deleteByIdCatalogo` |
| `ColeccionUsuarioRepository` | `findByIdUsuarioAndIdCatalogo`, `countByIdUsuario` |

La lógica de negocio está en `services/` y se envuelve con `@Transactional`.

### Lado Django (ORM) — `backend-django/catalog/models.py`

Mismos nombres de tabla/columna, pero **`managed = False`**: Django puede
consultar y escribir, pero **nunca crea ni altera** tablas. Se refuerza en
`config/settings.py` con `MIGRATION_MODULES = {...: None}`.

## 5. El flujo real de un escaneo

`EscaneoService.escanear` (`services/EscaneoService.java`) es un solo
`@Transactional`:

1. **Guarda la foto** en disco → `StorageService.save` y devuelve URL
   `.../files/scan_*.jpg`.
2. **Llama a la IA** (Django) → `IaClient.identificar` hace
   `POST /api/ia/identificar`. Si falla, usa `IaClient.fallback()`.
3. **Upsert catálogo** → `CatalogoService.upsertDesdeIa`: busca por
   `nombre_cientifico`; si no existe lo crea con defaults; guarda `datos_ia_raw`;
   **borra y recrea** `cuidados_secciones`.
4. **Registra el escaneo** en `escaneos_historial`.
5. **Upsert colección** → `upsertColeccion`: si ya existe esa especie,
   incrementa `veces_vista` y `ultima_vez`; si no, inserta y devuelve
   `nuevoEnColeccion=true`.

El lado Django del mismo paso está en `ia_engine/services.py`
(`persistir_catalogo`), que hace lo mismo (get_or_create por científico +
reemplazar secciones con `bulk_create`).

Cadena completa:
**App móvil → Spring (:8080) → Django (:8000) → IA (Gemini) → Postgres**.

## 6. Detalles que costaron (y por qué)

- **`stringtype=unspecified`** en `DB_URL`: sin esto, Postgres no sabe castear un
  `String` de Java a `jsonb` al insertar.
- **Django `respuesta_ia_raw NOT NULL`**: por eso el fallback manda `{}` en vez
  de `null`.
- **Los datos curados se preservan**: `persistir_catalogo` solo sobrescribe lo
  que la IA aporta de verdad.
- **Puerto 5432 ocupado**: el contenedor corre en **5433** (ajustado en `.env` y
  `DB_URL`).

## 7. Cómo explorarla tú mismo

Abre `psql` dentro del contenedor:

```powershell
docker exec -it plantia-postgres psql -U plantia -d plantia
```

Consultas útiles:

```sql
\dt                                              -- lista tablas
\d plantas_catalogo                              -- columnas/constraints
SELECT id_catalogo, nombre_comun, nombre_cientifico FROM plantas_catalogo;
SELECT * FROM cuidados_secciones WHERE id_catalogo = 1 ORDER BY orden;
SELECT e.id_escaneo, e.confianza_ia, p.nombre_comun
FROM escaneos_historial e
LEFT JOIN plantas_catalogo p ON p.id_catalogo = e.id_catalogo
ORDER BY e.fecha_escaneo DESC LIMIT 10;
SELECT * FROM flyway_schema_history ORDER BY installed_rank;  -- migraciones aplicadas
```

Ver el SQL que genera Spring: `logging.level.com.plantia: DEBUG` + `show-sql: true`
en `application.yml`.

Probar el ORM de Django sin tocar la web:

```powershell
cd backend-django
.\.venv\Scripts\python.exe manage.py shell
```

```python
from catalog.models import PlantaCatalogo, CuidadoSeccion
PlantaCatalogo.objects.count()
CuidadoSeccion.objects.filter(id_catalogo=1).values_list("tipo", "orden")
```

Tests que documentan el contrato: `backend-django/ia_engine/tests.py` y las
pruebas de Spring en `src/test`.

## 8. Reglas de oro para no romper nada

1. **Nunca** edites una migración ya aplicada; crea `V4__…sql`.
2. Cambios de esquema → **solo** por migración Flyway.
3. Django sigue con `managed = False`.
4. Toda FK nueva: define `ON DELETE` a conciencia (CASCADE / SET NULL / RESTRICT).
