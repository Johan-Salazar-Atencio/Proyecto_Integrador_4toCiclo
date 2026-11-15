-- ============================================================
-- PROYECTO PLANTIA - Version V9: Usuario administrador de prueba
-- Permite probar el panel admin de Django y los endpoints de escritura.
-- Credenciales: admin@plantia.test / plantia123  (rol ADMIN)
-- ============================================================
INSERT INTO usuarios (nombre, email, password_hash, rol, nivel, experiencia_xp, monedas, racha_dias, region)
VALUES ('Admin Tecsup', 'admin@plantia.test', '$2a$10$n7uGz2GzFEIhdSm3KDEOYO0ZMzGL132epZ61oqWefVWQYQIufMQVy', 'ADMIN', 1, 0, 0, 0, 'GLOBAL')
ON CONFLICT (email) DO UPDATE SET rol = 'ADMIN', password_hash = EXCLUDED.password_hash;
