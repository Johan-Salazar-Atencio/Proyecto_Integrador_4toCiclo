-- ============================================================
-- PROYECTO PLANTIA - Version V8: token de sesion por defecto
-- Los usuarios seed (1,2) quedan con un token conocido para que la app
-- movil, que arranca con el usuario por defecto (id=1) sin login,
-- siga funcionando. SOLO para desarrollo; en produccion cada usuario
-- obtiene su token al iniciar sesion.
-- ============================================================
UPDATE usuarios SET session_token = 'dev-default-token-user-1'
WHERE id_usuario = 1;

UPDATE usuarios SET session_token = 'dev-default-token-user-2'
WHERE id_usuario = 2;
