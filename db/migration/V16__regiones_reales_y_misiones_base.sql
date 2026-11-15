-- V16: regiones reales y misiones predeterminadas.
-- Aditiva: no borra datos. Las regiones usan codigos estables (los que guardan usuarios y eventos)
-- y las misiones predeterminadas las puede editar, desactivar o ampliar el admin.

-- Regiones del pais. Se conservan GLOBAL y LIMA, que ya existen.
INSERT INTO regiones (codigo, nombre) VALUES
    ('LIMA',          'Lima Metropolitana'),
    ('AREQUIPA',      'Arequipa'),
    ('CUSCO',         'Cusco y Altiplano'),
    ('COSTA_NORTE',   'Costa norte'),
    ('SELVA_CENTRAL', 'Selva central')
ON CONFLICT (codigo) DO UPDATE SET nombre = EXCLUDED.nombre;

-- Misiones: marca de predeterminada y orden de aparicion
ALTER TABLE misiones ADD COLUMN predeterminada BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE misiones ADD COLUMN orden INT NOT NULL DEFAULT 0;

-- La mision sembrada desde V3 pasa a ser la primera predeterminada
UPDATE misiones
SET predeterminada = TRUE, orden = 1, recompensa_xp = 20
WHERE titulo = 'Riego Consciente';

INSERT INTO misiones (titulo, descripcion, tipo, meta, recompensa_monedas, recompensa_xp, predeterminada, orden)
SELECT 'Explorador Botanico', 'Escanea 1 planta hoy', 'ESCANEO', 1, 30, 20, TRUE, 2
WHERE NOT EXISTS (SELECT 1 FROM misiones WHERE titulo = 'Explorador Botanico');

INSERT INTO misiones (titulo, descripcion, tipo, meta, recompensa_monedas, recompensa_xp, predeterminada, orden)
SELECT 'Jardinero Digital', 'Digitaliza 1 planta nueva a tu coleccion', 'DIGITALIZAR', 1, 40, 30, TRUE, 3
WHERE NOT EXISTS (SELECT 1 FROM misiones WHERE titulo = 'Jardinero Digital');

INSERT INTO misiones (titulo, descripcion, tipo, meta, recompensa_monedas, recompensa_xp, predeterminada, orden)
SELECT 'A Resguardo', 'Refugia tu planta durante un desastre', 'REFUGIO', 1, 50, 40, TRUE, 4
WHERE NOT EXISTS (SELECT 1 FROM misiones WHERE titulo = 'A Resguardo');
