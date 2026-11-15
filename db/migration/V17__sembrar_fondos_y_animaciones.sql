-- V17: fondos de escena y animaciones de desastre con su arte en pixel art.
-- Los archivos viven en backend-spring-boot/src/main/resources/arte y Spring los sirve en /files/arte/**
-- (se generan con scripts/arte/generar_arte.py). Aqui solo se guardan sus rutas.
-- Aditiva: solo inserta. El admin podra agregar mas fondos y animaciones despues.

-- Un fondo predeterminado por momento del dia: tira horizontal de 8 frames de 96x96
INSERT INTO fondos_escena (momento, nombre, imagen_url, total_frames, ancho_px, alto_px, ms_por_frame, es_predeterminado)
SELECT v.momento, v.nombre, v.imagen_url, 8, 96, 96, 250, TRUE
FROM (VALUES
    ('DIA',   'Campo de dia',    '/files/arte/fondos/dia.png'),
    ('TARDE', 'Campo al atardecer', '/files/arte/fondos/tarde.png'),
    ('NOCHE', 'Campo de noche',  '/files/arte/fondos/noche.png')
) AS v(momento, nombre, imagen_url)
WHERE NOT EXISTS (SELECT 1 FROM fondos_escena f WHERE f.momento = v.momento AND f.es_predeterminado);

-- Dos fases por tipo de desastre: AVISO (4 frames, tenue) y PICO (8 frames).
-- El aviso va al doble de ms por frame porque tiene la mitad de frames.
INSERT INTO animaciones_desastre (id_tipo_desastre, fase, ms_por_frame, repetir)
SELECT t.id_tipo_desastre, f.fase,
       CASE t.codigo WHEN 'HUAYCO' THEN 150 WHEN 'HELADA' THEN 300 WHEN 'OLA_DE_CALOR' THEN 250 ELSE 200 END
         * CASE f.fase WHEN 'AVISO' THEN 2 ELSE 1 END,
       TRUE
FROM tipos_desastre t
CROSS JOIN (VALUES ('AVISO'), ('PICO')) AS f(fase)
WHERE t.codigo IN ('HUAYCO', 'HELADA', 'OLA_DE_CALOR', 'PLAGA')
ON CONFLICT (id_tipo_desastre, fase) DO NOTHING;

-- Un renglon por frame: /files/arte/desastres/<tipo>_<fase>_<n>.png
INSERT INTO frames_animacion (id_animacion, orden, imagen_url)
SELECT a.id_animacion, n,
       '/files/arte/desastres/'
         || CASE t.codigo WHEN 'OLA_DE_CALOR' THEN 'calor' ELSE lower(t.codigo) END
         || '_' || lower(a.fase) || '_' || n || '.png'
FROM animaciones_desastre a
JOIN tipos_desastre t ON t.id_tipo_desastre = a.id_tipo_desastre
CROSS JOIN LATERAL generate_series(1, CASE a.fase WHEN 'AVISO' THEN 4 ELSE 8 END) AS n
WHERE t.codigo IN ('HUAYCO', 'HELADA', 'OLA_DE_CALOR', 'PLAGA')
ON CONFLICT (id_animacion, orden) DO NOTHING;
