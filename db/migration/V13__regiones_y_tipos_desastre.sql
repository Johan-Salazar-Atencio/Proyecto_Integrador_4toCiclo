-- V13: regiones y tipos de desastre como tablas.
-- Aditiva: se conservan usuarios.region y eventos_desastre.tipo_evento (texto) para que
-- Spring y Django sigan funcionando. Un trigger rellena las columnas nuevas desde las viejas.
-- Las columnas de texto se eliminan en una migracion posterior, cuando el codigo ya use las llaves.

CREATE TABLE regiones (
    id_region  SERIAL PRIMARY KEY,
    codigo     VARCHAR(100) UNIQUE NOT NULL,
    nombre     VARCHAR(100) NOT NULL,
    activa     BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO regiones (codigo, nombre) VALUES ('GLOBAL', 'Global');

-- Regiones que ya existen como texto en usuarios y eventos
INSERT INTO regiones (codigo, nombre)
SELECT DISTINCT r, INITCAP(LOWER(r))
FROM (SELECT region AS r FROM usuarios UNION SELECT region FROM eventos_desastre) x
WHERE r IS NOT NULL AND r <> ''
ON CONFLICT (codigo) DO NOTHING;

CREATE TABLE tipos_desastre (
    id_tipo_desastre  SERIAL PRIMARY KEY,
    codigo            VARCHAR(50) UNIQUE NOT NULL,
    nombre            VARCHAR(100) NOT NULL,
    descripcion       TEXT,
    icono_url         VARCHAR(255),
    color_hex         VARCHAR(7) NOT NULL DEFAULT '#888888',
    dano_bajo         INT NOT NULL DEFAULT 10,
    dano_moderado     INT NOT NULL DEFAULT 25,
    dano_critico      INT NOT NULL DEFAULT 50,
    activo            BOOLEAN NOT NULL DEFAULT TRUE,
    id_creador        INT REFERENCES usuarios(id_usuario) ON DELETE SET NULL,
    fecha_creacion    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO tipos_desastre (codigo, nombre, descripcion, color_hex) VALUES
    ('HUAYCO',       'Huayco',        'Lluvia fuerte y lodo que arrastra las macetas.', '#8D6E63'),
    ('HELADA',       'Helada',        'Frio intenso que congela hojas y raices.',        '#4FC3F7'),
    ('OLA_DE_CALOR', 'Ola de calor',  'Calor extremo que seca la tierra.',               '#FF7043'),
    ('PLAGA',        'Plaga',         'Insectos que devoran hojas y tallos.',            '#7CB342');

-- Usuarios: region como llave foranea
ALTER TABLE usuarios ADD COLUMN id_region INT REFERENCES regiones(id_region) ON DELETE SET NULL;
UPDATE usuarios u SET id_region = r.id_region
FROM regiones r WHERE r.codigo = COALESCE(NULLIF(u.region, ''), 'GLOBAL');

-- Eventos: tipo y region como llaves foraneas, mas la marca de "ya procesado"
ALTER TABLE eventos_desastre ADD COLUMN id_tipo_desastre INT REFERENCES tipos_desastre(id_tipo_desastre);
ALTER TABLE eventos_desastre ADD COLUMN id_region INT REFERENCES regiones(id_region) ON DELETE SET NULL;
ALTER TABLE eventos_desastre ADD COLUMN procesado BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE eventos_desastre e SET id_tipo_desastre = t.id_tipo_desastre
FROM tipos_desastre t WHERE t.codigo = e.tipo_evento;
UPDATE eventos_desastre e SET id_region = r.id_region
FROM regiones r WHERE r.codigo = COALESCE(NULLIF(e.region, ''), 'GLOBAL');
UPDATE eventos_desastre SET procesado = TRUE WHERE estado = 'FINALIZADO';

-- Los tipos nuevos que cree el admin ya no caben en la lista fija de 4 valores
ALTER TABLE eventos_desastre DROP CONSTRAINT IF EXISTS eventos_desastre_tipo_evento_check;

CREATE INDEX idx_eventos_tipo   ON eventos_desastre(id_tipo_desastre);
CREATE INDEX idx_eventos_region ON eventos_desastre(id_region);
CREATE INDEX idx_usuarios_region ON usuarios(id_region);

-- Transicion: el codigo actual escribe texto; el trigger completa las llaves nuevas.
CREATE OR REPLACE FUNCTION fn_sincronizar_llaves_v13() RETURNS trigger AS $$
BEGIN
    IF TG_TABLE_NAME = 'usuarios' THEN
        IF NEW.id_region IS NULL THEN
            SELECT id_region INTO NEW.id_region FROM regiones
            WHERE codigo = COALESCE(NULLIF(NEW.region, ''), 'GLOBAL');
        END IF;
    ELSE
        IF NEW.id_tipo_desastre IS NULL THEN
            SELECT id_tipo_desastre INTO NEW.id_tipo_desastre FROM tipos_desastre
            WHERE codigo = NEW.tipo_evento;
        END IF;
        IF NEW.id_region IS NULL THEN
            SELECT id_region INTO NEW.id_region FROM regiones
            WHERE codigo = COALESCE(NULLIF(NEW.region, ''), 'GLOBAL');
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_usuarios_llaves_v13 BEFORE INSERT OR UPDATE ON usuarios
    FOR EACH ROW EXECUTE FUNCTION fn_sincronizar_llaves_v13();
CREATE TRIGGER trg_eventos_llaves_v13 BEFORE INSERT OR UPDATE ON eventos_desastre
    FOR EACH ROW EXECUTE FUNCTION fn_sincronizar_llaves_v13();
