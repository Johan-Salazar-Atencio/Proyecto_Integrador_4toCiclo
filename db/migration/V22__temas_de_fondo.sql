-- Temas de fondo: lo que se compra y se elige. Cada tema trae sus escenas de DIA, TARDE y NOCHE
-- (filas de fondos_escena). El fondo se aplica a una planta (plantas_usuario.id_tema_fondo; NULL = el
-- tema predeterminado, "Campo").
CREATE TABLE temas_fondo (
    id_tema            SERIAL PRIMARY KEY,
    nombre             VARCHAR(100) NOT NULL,
    descripcion        VARCHAR(200),
    es_predeterminado  BOOLEAN NOT NULL DEFAULT FALSE,
    activo             BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX ux_tema_predeterminado ON temas_fondo(es_predeterminado) WHERE es_predeterminado;

ALTER TABLE fondos_escena ADD COLUMN id_tema INT REFERENCES temas_fondo(id_tema) ON DELETE CASCADE;
ALTER TABLE fondos_escena ADD COLUMN vista_url VARCHAR(255);
-- Una escena por momento en cada tema
CREATE UNIQUE INDEX ux_fondo_tema_momento ON fondos_escena(id_tema, momento) WHERE id_tema IS NOT NULL;

ALTER TABLE items_tienda ADD COLUMN id_tema INT REFERENCES temas_fondo(id_tema) ON DELETE SET NULL;
CREATE INDEX idx_items_tema ON items_tienda(id_tema);

ALTER TABLE plantas_usuario ADD COLUMN id_tema_fondo INT REFERENCES temas_fondo(id_tema) ON DELETE SET NULL;

-- Tema predeterminado: los tres fondos de siempre
INSERT INTO temas_fondo (nombre, descripcion, es_predeterminado)
VALUES ('Campo', 'El fondo de siempre: un campo verde que cambia con el dia.', TRUE);

UPDATE fondos_escena
SET id_tema = (SELECT id_tema FROM temas_fondo WHERE es_predeterminado),
    vista_url = replace(imagen_url, '.png', '_vista.png')
WHERE es_predeterminado;

-- Los fondos sueltos de V21 pasan a ser temas con sus tres escenas (se vuelven a crear completos;
-- items_tienda.id_fondo queda en NULL al borrar las filas viejas)
DELETE FROM fondos_escena WHERE NOT es_predeterminado AND id_tema IS NULL;

INSERT INTO temas_fondo (nombre, descripcion)
SELECT v.nombre, v.descripcion
FROM (VALUES
    ('Campo nevado',     'Nieve cayendo sobre pinos, de dia, de tarde y de noche.'),
    ('Desierto dorado',  'Dunas, cactus y una planta rodadora a todas horas.'),
    ('Cerezos en flor',  'Petalos de cerezo al viento, del dia a la noche.'),
    ('Ciudad cyberpunk', 'Neones, lluvia y autos voladores. Cambia con el dia, la tarde y la noche.')
) AS v(nombre, descripcion)
WHERE NOT EXISTS (SELECT 1 FROM temas_fondo t WHERE t.nombre = v.nombre);

INSERT INTO fondos_escena (momento, nombre, imagen_url, vista_url, total_frames, ancho_px, alto_px, ms_por_frame,
                           es_predeterminado, id_tema)
SELECT m.momento, t.nombre || ' (' || m.etiqueta || ')',
       '/files/arte/fondos/' || v.codigo || '_' || lower(m.momento) || '.png',
       '/files/arte/fondos/' || v.codigo || '_' || lower(m.momento) || '_vista.png',
       96, 96, 96, 120, FALSE, t.id_tema
FROM (VALUES
    ('nevado',    'Campo nevado'),
    ('desierto',  'Desierto dorado'),
    ('sakura',    'Cerezos en flor'),
    ('cyberpunk', 'Ciudad cyberpunk')
) AS v(codigo, nombre)
JOIN temas_fondo t ON t.nombre = v.nombre
CROSS JOIN (VALUES ('DIA', 'dia'), ('TARDE', 'tarde'), ('NOCHE', 'noche')) AS m(momento, etiqueta)
WHERE NOT EXISTS (SELECT 1 FROM fondos_escena f WHERE f.id_tema = t.id_tema AND f.momento = m.momento);

-- Los articulos de la tienda apuntan al tema; asset_url es la vista previa principal
UPDATE items_tienda i
SET id_tema = t.id_tema,
    asset_url = '/files/arte/fondos/' || v.codigo || '_' || v.preview || '_vista.png',
    descripcion = t.descripcion
FROM temas_fondo t
JOIN (VALUES
    ('Campo nevado',     'nevado',    'dia'),
    ('Desierto dorado',  'desierto',  'tarde'),
    ('Cerezos en flor',  'sakura',    'dia'),
    ('Ciudad cyberpunk', 'cyberpunk', 'noche')
) AS v(nombre, codigo, preview) ON v.nombre = t.nombre
WHERE i.tipo = 'FONDO' AND i.nombre = t.nombre;
