-- V14: fondos de la escena (dia, tarde, noche) y animaciones de desastre.
-- Las imagenes no se guardan en la base: solo su ruta (imagen_url, ej. /files/fondos/dia.png).
-- El archivo vive en la carpeta que Spring ya sirve en /files/**.
-- Sin datos iniciales: se cargan cuando existan las imagenes.

CREATE TABLE fondos_escena (
    id_fondo           SERIAL PRIMARY KEY,
    momento            VARCHAR(10) NOT NULL CHECK (momento IN ('DIA', 'TARDE', 'NOCHE')),
    nombre             VARCHAR(100) NOT NULL,
    imagen_url         VARCHAR(255) NOT NULL,
    total_frames       INT NOT NULL DEFAULT 1 CHECK (total_frames >= 1),
    ancho_px           INT,
    alto_px            INT,
    ms_por_frame       INT NOT NULL DEFAULT 200 CHECK (ms_por_frame > 0),
    es_predeterminado  BOOLEAN NOT NULL DEFAULT FALSE,
    activo             BOOLEAN NOT NULL DEFAULT TRUE,
    id_creador         INT REFERENCES usuarios(id_usuario) ON DELETE SET NULL,
    fecha_creacion     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Un solo fondo predeterminado por momento del dia
CREATE UNIQUE INDEX ux_fondo_predeterminado ON fondos_escena(momento) WHERE es_predeterminado;

-- Los fondos tambien se pueden vender en la tienda
ALTER TABLE items_tienda ADD COLUMN id_fondo INT REFERENCES fondos_escena(id_fondo) ON DELETE SET NULL;

CREATE TABLE animaciones_desastre (
    id_animacion      SERIAL PRIMARY KEY,
    id_tipo_desastre  INT NOT NULL REFERENCES tipos_desastre(id_tipo_desastre) ON DELETE CASCADE,
    fase              VARCHAR(10) NOT NULL CHECK (fase IN ('AVISO', 'PICO', 'FINAL', 'REFUGIO', 'DANO')),
    ms_por_frame      INT NOT NULL DEFAULT 150 CHECK (ms_por_frame > 0),
    repetir           BOOLEAN NOT NULL DEFAULT TRUE,
    activa            BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (id_tipo_desastre, fase)
);

CREATE TABLE frames_animacion (
    id_frame      SERIAL PRIMARY KEY,
    id_animacion  INT NOT NULL REFERENCES animaciones_desastre(id_animacion) ON DELETE CASCADE,
    orden         INT NOT NULL CHECK (orden >= 1),
    imagen_url    VARCHAR(255) NOT NULL,
    UNIQUE (id_animacion, orden)
);

CREATE INDEX idx_animaciones_tipo ON animaciones_desastre(id_tipo_desastre);
CREATE INDEX idx_frames_animacion ON frames_animacion(id_animacion);
CREATE INDEX idx_items_fondo ON items_tienda(id_fondo);
