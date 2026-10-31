# Base de Datos - Plantia

PostgreSQL compartida por los dos backends:
- **Spring Boot** (Usuario / Juego / API del movil) - dueno del DDL via Flyway.
- **Django** (Admin / Motor IA) - modelos `managed = False`, solo lee/escribe datos.

## Estructura

```
db/
└── migration/
    ├── V1__schema_inicial.sql                    # Esquema original
    └── V2__coleccion_cuidados_y_campos_faltantes.sql
```

## Levantar la base de datos

```bash
docker compose up -d postgres flyway
```

- Postgres: `localhost:5432` / DB `plantia` / user `plantia` / pass `plantia`
- Flyway aplica las migraciones de `db/migration`.

Solo Postgres (sin Flyway, lo aplica Spring al arrancar):

```bash
docker compose up -d postgres
```

## Reglas de migracion

1. **Nadie edita migraciones ya aplicadas.** Cambios nuevos = nuevo archivo `V{n}__descripcion.sql`.
2. Flyway lo ejecuta **Spring Boot** (`spring.flyway.locations=filesystem:../db/migration`).
3. Django **no** corre migraciones sobre estas tablas (`managed = False`).

## Cambios de V2 (sprint coleccion / cuidados)

| Cambio | Motivo |
|---|---|
| `usuarios.region`, `usuarios.updated_at` | Impacto de desastres por zona + cache de lectura |
| `coleccion_usuario` | "Mi coleccion": plantas vistas, separada de `plantas_usuario` |
| `cuidados_secciones` | Conocimiento de cuidados 1:N generado por IA |
| `niveles` | Curva de XP -> nivel |
| `misiones`, `misiones_usuario` | Mision del dia que muestra el movil |
| `updated_at` en varias tablas | ETag / last-modified para cache offline de lectura |
| Fix FK `eventos_desastre.id_creador` | Evitar bloquear borrado de admins |
