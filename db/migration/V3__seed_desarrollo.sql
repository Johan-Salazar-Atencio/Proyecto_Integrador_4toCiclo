-- ============================================================
-- PROYECTO PLANTIA - SEED DE DESARROLLO
-- Version: V3
-- Datos minimos para levantar el movil con informacion real.
-- ============================================================

-- Curva de niveles
INSERT INTO niveles (nivel, xp_requerido, recompensa_monedas) VALUES
    (1, 0,    0),
    (2, 500,  20),
    (3, 1000, 25),
    (4, 1700, 30),
    (5, 2450, 40),
    (6, 3000, 50),
    (7, 4000, 60),
    (8, 5500, 75),
    (9, 7500, 90),
    (10, 10000, 120);

-- Usuario temporal (sin auth en este sprint)
INSERT INTO usuarios (id_usuario, nombre, email, password_hash, rol, nivel, experiencia_xp, monedas, racha_dias, region)
VALUES (1, 'Johan', 'johan@plantia.test', 'TEMP_NO_AUTH', 'USER', 5, 2450, 320, 3, 'LIMA');

SELECT setval(pg_get_serial_sequence('usuarios', 'id_usuario'), (SELECT MAX(id_usuario) FROM usuarios));

-- Catalogo base
INSERT INTO plantas_catalogo
    (nombre_comun, nombre_cientifico, frecuencia_riego_dias, frecuencia_sol_horas,
     temperatura_min_c, temperatura_max_c, es_toxica_mascotas,
     sprite_base_url, etapa_semilla_url, etapa_brote_url, etapa_adulta_url)
VALUES
    ('Monstera', 'Monstera deliciosa', 7, 4, 15, 30, TRUE,
     '/sprites/monstera/base.png', '/sprites/monstera/semilla.png',
     '/sprites/monstera/brote.png', '/sprites/monstera/adulta.png'),
    ('Ficus', 'Ficus elastica', 5, 6, 12, 28, TRUE,
     '/sprites/ficus/base.png', '/sprites/ficus/semilla.png',
     '/sprites/ficus/brote.png', '/sprites/ficus/adulta.png');

-- Cuidados generados por IA (secciones 1:N)
INSERT INTO cuidados_secciones (id_catalogo, tipo, titulo, contenido, orden, generado_por_ia)
SELECT id_catalogo, 'RIEGO', 'Riego moderado',
       'Riega cuando los primeros 3 cm de sustrato esten secos; evita encharcamientos.', 0, TRUE
FROM plantas_catalogo WHERE nombre_cientifico = 'Monstera deliciosa';

INSERT INTO cuidados_secciones (id_catalogo, tipo, titulo, contenido, orden, generado_por_ia)
SELECT id_catalogo, 'LUZ', 'Luz indirecta brillante',
       'Prefiere luz indirecta abundante; el sol directo quema sus hojas.', 1, TRUE
FROM plantas_catalogo WHERE nombre_cientifico = 'Monstera deliciosa';

INSERT INTO cuidados_secciones (id_catalogo, tipo, titulo, contenido, orden, generado_por_ia)
SELECT id_catalogo, 'TOXICIDAD', 'Toxica para mascotas',
       'Contiene oxalato de calcio; puede irritar a perros y gatos si la mastican.', 2, TRUE
FROM plantas_catalogo WHERE nombre_cientifico = 'Monstera deliciosa';

-- Planta en crianza del usuario (tamagotchi)
INSERT INTO plantas_usuario
    (id_usuario, id_catalogo, apodo, salud_porcentaje, hidratacion_porcentaje, sol_porcentaje, etapa_crecimiento)
SELECT 1, id_catalogo, 'Monstera', 80, 60, 90, 'BROTE'
FROM plantas_catalogo WHERE nombre_cientifico = 'Monstera deliciosa';

-- Mi coleccion (plantas vistas)
INSERT INTO coleccion_usuario (id_usuario, id_catalogo, foto_url, veces_vista)
SELECT 1, id_catalogo, NULL, 1
FROM plantas_catalogo WHERE nombre_cientifico = 'Monstera deliciosa';

-- Mision diaria
INSERT INTO misiones (titulo, descripcion, tipo, meta, recompensa_monedas, activa)
VALUES ('Riego Consciente', 'Riega 2 de tus plantas hoy', 'RIEGO', 2, 50, TRUE);

-- Desastre activo (para WeatherAlertCard)
INSERT INTO eventos_desastre
    (id_creador, tipo_evento, region, mensaje_notificacion, fecha_inicio, fecha_fin, estado, severidad)
VALUES (1, 'HUAYCO', 'GLOBAL',
        'Resguarda las macetas de terraza para evitar exceso de lluvia y danos.',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '7 days', 'ACTIVO', 'CRITICO');

-- Tienda
INSERT INTO items_tienda (nombre, tipo, precio_monedas, nivel_requerido, asset_url) VALUES
    ('Maceta de barro', 'MACETA', 80, 1, '/assets/maceta_barro.png'),
    ('Abono organico', 'ABONO', 30, 1, '/assets/abono_organico.png'),
    ('Fondo atardecer', 'FONDO', 150, 3, '/assets/fondo_atardecer.png');

-- Logros
INSERT INTO logros (nombre, descripcion, icono_url, recompensa_monedas) VALUES
    ('Primer Brote', 'Identifica tu primera planta con la IA.', '/assets/logro_brote.png', 50),
    ('Coleccionista', 'Registra 10 plantas en tu coleccion.', '/assets/logro_coleccion.png', 100);
