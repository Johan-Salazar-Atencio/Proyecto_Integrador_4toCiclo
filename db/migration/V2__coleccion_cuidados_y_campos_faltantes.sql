-- ============================================================
-- PROYECTO PLANTIA - DELTAS SPRINT COLECCION / CUIDADOS / MISIONES
-- Version: V2
-- Requerido por: modulo de escaneo, mi coleccion, cuidados (IA),
--                misiones diarias y cache de lectura del movil.
-- ============================================================

-- ------------------------------------------------------------
-- A. CAMPOS FALTANTES EN USUARIOS
--    region: los desastres (eventos_desastre.region) impactan por zona.
--    updated_at: soporte de cache de lectura (ETag / last-modified).
-- ------------------------------------------------------------
ALTER TABLE usuarios ADD COLUMN region VARCHAR(100) DEFAULT 'GLOBAL';
ALTER TABLE usuarios ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- ------------------------------------------------------------
-- B. MI COLECCION (plantas vistas / escaneadas del coleccionista)
--    Separada de plantas_usuario (crianza tamagotchi).
--    Una fila por especie vista; veces_vista se incrementa al re-escanear.
-- ------------------------------------------------------------
CREATE TABLE coleccion_usuario (
    id_coleccion    SERIAL PRIMARY KEY,
    id_usuario      INT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    id_catalogo     INT REFERENCES plantas_catalogo(id_catalogo) ON DELETE SET NULL,
    id_escaneo      INT REFERENCES escaneos_historial(id_escaneo) ON DELETE SET NULL,
    foto_url        VARCHAR(255),
    veces_vista     INT DEFAULT 1,
    primera_vez     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ultima_vez      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id_usuario, id_catalogo)
);

-- ------------------------------------------------------------
-- C. BASE DE CONOCIMIENTO DE CUIDADOS (secciones 1:N al catalogo)
--    Generado por IA; cada planta puede tener N secciones.
-- ------------------------------------------------------------
CREATE TABLE cuidados_secciones (
    id_seccion      SERIAL PRIMARY KEY,
    id_catalogo     INT NOT NULL REFERENCES plantas_catalogo(id_catalogo) ON DELETE CASCADE,
    tipo            VARCHAR(30) NOT NULL
                    CHECK (tipo IN ('RIEGO','LUZ','SUSTRATO','TEMPERATURA','PODA','PLAGAS','TOXICIDAD','ABONO','OTRO')),
    titulo          VARCHAR(100) NOT NULL,
    contenido       TEXT NOT NULL,
    orden           INT DEFAULT 0,
    generado_por_ia BOOLEAN DEFAULT TRUE,
    fecha_creacion  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- D. CURVA DE NIVELES (experiencia_xp -> nivel)
-- ------------------------------------------------------------
CREATE TABLE niveles (
    nivel         INT PRIMARY KEY,
    xp_requerido  INT NOT NULL,
    recompensa_monedas INT DEFAULT 0
);

-- ------------------------------------------------------------
-- E. MISIONES DIARIAS
-- ------------------------------------------------------------
CREATE TABLE misiones (
    id_mision       SERIAL PRIMARY KEY,
    titulo          VARCHAR(100) NOT NULL,
    descripcion     TEXT,
    tipo            VARCHAR(30) NOT NULL
                    CHECK (tipo IN ('RIEGO','ESCANEO','SOL','ABONO','COLECCION')),
    meta            INT NOT NULL,
    recompensa_monedas INT DEFAULT 0,
    activa          BOOLEAN DEFAULT TRUE
);

CREATE TABLE misiones_usuario (
    id_mision_usuario  SERIAL PRIMARY KEY,
    id_usuario         INT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    id_mision          INT NOT NULL REFERENCES misiones(id_mision) ON DELETE CASCADE,
    progreso           INT DEFAULT 0,
    completada         BOOLEAN DEFAULT FALSE,
    fecha              DATE DEFAULT CURRENT_DATE,
    UNIQUE (id_usuario, id_mision, fecha)
);

-- ------------------------------------------------------------
-- F. updated_at en tablas mutables (cache de lectura)
-- ------------------------------------------------------------
ALTER TABLE plantas_catalogo ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE plantas_usuario  ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE eventos_desastre ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE items_tienda     ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- ------------------------------------------------------------
-- G. FIX: un admin no debe bloquear el borrado por eventos creados
-- ------------------------------------------------------------
ALTER TABLE eventos_desastre DROP CONSTRAINT eventos_desastre_id_creador_fkey;
ALTER TABLE eventos_desastre
    ADD CONSTRAINT eventos_desastre_id_creador_fkey
    FOREIGN KEY (id_creador) REFERENCES usuarios(id_usuario) ON DELETE SET NULL;

-- ------------------------------------------------------------
-- H. INDICES NUEVOS
-- ------------------------------------------------------------
CREATE INDEX idx_coleccion_usuario ON coleccion_usuario(id_usuario);
CREATE INDEX idx_cuidados_catalogo ON cuidados_secciones(id_catalogo);
CREATE INDEX idx_cuidados_tipo ON cuidados_secciones(tipo);
CREATE INDEX idx_misiones_usuario ON misiones_usuario(id_usuario);
