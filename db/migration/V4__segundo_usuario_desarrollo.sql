-- ============================================================
-- PROYECTO PLANTIA - SEGUNDO USUARIO DE DESARROLLO
-- Version: V4
-- Permite probar el cambio de identidad desde la app (el
-- backend aun no tiene login: se usa el header X-User-Id).
-- ============================================================

INSERT INTO usuarios (id_usuario, nombre, email, password_hash, rol, nivel, experiencia_xp, monedas, racha_dias, region)
VALUES (2, 'Lucia', 'lucia@plantia.test', 'TEMP_NO_AUTH', 'USER', 3, 1000, 150, 1, 'LIMA');

SELECT setval(pg_get_serial_sequence('usuarios', 'id_usuario'), (SELECT MAX(id_usuario) FROM usuarios));

-- Jardin de Lucia: una planta en crianza y su coleccion.
INSERT INTO plantas_usuario
    (id_usuario, id_catalogo, apodo, salud_porcentaje, hidratacion_porcentaje, sol_porcentaje, etapa_crecimiento)
SELECT 2, id_catalogo, 'Albahaca', 92, 70, 80, 'ADULTA'
FROM plantas_catalogo WHERE nombre_cientifico = 'Ocimum basilicum';

INSERT INTO coleccion_usuario (id_usuario, id_catalogo, foto_url, veces_vista)
SELECT 2, id_catalogo, NULL, 2
FROM plantas_catalogo WHERE nombre_cientifico = 'Ocimum basilicum';

-- Mision diaria de Lucia.
INSERT INTO misiones_usuario (id_usuario, id_mision, progreso, completada)
SELECT 2, id_mision, 1, TRUE
FROM misiones WHERE activa = TRUE LIMIT 1;
