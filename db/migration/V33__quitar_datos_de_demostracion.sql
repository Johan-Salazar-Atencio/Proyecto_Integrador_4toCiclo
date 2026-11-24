-- ============================================================
-- PROYECTO PLANTIA - Version V33: quitar los datos de demostracion (QA-09)
-- V3, V4, V6, V8 y V9 (ya aplicadas, no se editan) sembraban:
--   - usuarios demo con clave conocida: johan@plantia.test (V3), lucia@plantia.test (V4),
--     admin@plantia.test (V9); sus claves (V6) y tokens (V8, ya anulados en V10);
--   - su planta, su coleccion y su mision (V3, V4);
--   - un evento HUAYCO critico de 7 dias creado por el usuario 1 (V3, texto con tilde desde V32);
--   - tres articulos de tienda de relleno (V3): dos quedaron inactivos y sin arte real.
-- Desde aqui una base nueva arranca SIN usuarios: el admin se crea con
-- `python manage.py crear_admin --email ...` y los jugadores se registran desde la app.
-- Se conserva todo lo que es configuracion del juego (catalogo, niveles, misiones, tienda activa,
-- logros, tipos de desastre, regiones, parametros, temas y fondos).
-- Solo borra filas reconocibles: los tres correos demo, el evento por su texto exacto y los
-- articulos de relleno si nadie los tiene. No toca a ningun otro usuario.
-- ============================================================

-- 1) Evento sembrado. registros_desastre_usuario -> eventos_desastre es RESTRICT (V31):
--    primero sus registros (de cualquier usuario que lo haya vivido), luego el evento.
DELETE FROM registros_desastre_usuario r
USING eventos_desastre e
WHERE r.id_desastre = e.id_desastre
  AND e.mensaje_notificacion IN (
        'Resguarda las macetas de terraza para evitar exceso de lluvia y danos.',
        'Resguarda las macetas de terraza para evitar exceso de lluvia y daños.');

DELETE FROM eventos_desastre
WHERE mensaje_notificacion IN (
        'Resguarda las macetas de terraza para evitar exceso de lluvia y danos.',
        'Resguarda las macetas de terraza para evitar exceso de lluvia y daños.');

-- 2) Datos de los usuarios demo. Todo cuelga de usuarios con ON DELETE CASCADE, pero se borra
--    explicito y de hojas a raiz para que el orden quede a la vista.
DELETE FROM registros_desastre_usuario
WHERE id_planta_usuario IN (SELECT id_planta_usuario FROM plantas_usuario
                            WHERE id_usuario IN (SELECT id_usuario FROM usuarios
                     WHERE lower(email) IN ('johan@plantia.test', 'lucia@plantia.test', 'admin@plantia.test')));
DELETE FROM logros_usuario     WHERE id_usuario IN (SELECT id_usuario FROM usuarios
                     WHERE lower(email) IN ('johan@plantia.test', 'lucia@plantia.test', 'admin@plantia.test'));
DELETE FROM historial_progreso WHERE id_usuario IN (SELECT id_usuario FROM usuarios
                     WHERE lower(email) IN ('johan@plantia.test', 'lucia@plantia.test', 'admin@plantia.test'));
DELETE FROM misiones_usuario   WHERE id_usuario IN (SELECT id_usuario FROM usuarios
                     WHERE lower(email) IN ('johan@plantia.test', 'lucia@plantia.test', 'admin@plantia.test'));
DELETE FROM inventario_usuario WHERE id_usuario IN (SELECT id_usuario FROM usuarios
                     WHERE lower(email) IN ('johan@plantia.test', 'lucia@plantia.test', 'admin@plantia.test'));
DELETE FROM coleccion_usuario  WHERE id_usuario IN (SELECT id_usuario FROM usuarios
                     WHERE lower(email) IN ('johan@plantia.test', 'lucia@plantia.test', 'admin@plantia.test'));
DELETE FROM escaneos_historial WHERE id_usuario IN (SELECT id_usuario FROM usuarios
                     WHERE lower(email) IN ('johan@plantia.test', 'lucia@plantia.test', 'admin@plantia.test'));
DELETE FROM plantas_usuario    WHERE id_usuario IN (SELECT id_usuario FROM usuarios
                     WHERE lower(email) IN ('johan@plantia.test', 'lucia@plantia.test', 'admin@plantia.test'));

-- Lo que el admin demo creo (otros eventos, fondos, tipos de desastre) se conserva: esas FK son
-- ON DELETE SET NULL y solo pierden el autor.
DELETE FROM usuarios
WHERE lower(email) IN ('johan@plantia.test', 'lucia@plantia.test', 'admin@plantia.test');

-- 3) Articulos de relleno de V3 que despues se retiraron (inactivos, con asset_url '/assets/...'
--    que no existe). Solo si nadie los tiene ni los usa. 'Abono orgánico' (V3) sigue activo y a la
--    venta: es parte de la tienda y se queda.
DELETE FROM items_tienda i
WHERE i.activo = FALSE
  AND (i.nombre, i.tipo, i.asset_url) IN (('Maceta de barro', 'MACETA', '/assets/maceta_barro.png'),
                                          ('Fondo atardecer', 'FONDO',  '/assets/fondo_atardecer.png'))
  AND NOT EXISTS (SELECT 1 FROM inventario_usuario iu WHERE iu.id_item = i.id_item)
  AND NOT EXISTS (SELECT 1 FROM plantas_usuario pu WHERE pu.id_maceta = i.id_item)
  AND NOT EXISTS (SELECT 1 FROM usuarios u WHERE u.id_invernadero = i.id_item);
