-- ============================================================
-- PROYECTO PLANTIA - Version V6: Password real del usuario demo
-- El seed V3 guardaba 'TEMP_NO_AUTH' (texto plano), imposible de
-- validar con BCrypt: el login del movil/web fallaba con los seeds.
-- Ahora el demo (y Lucia, V4) pueden iniciar sesion con 'plantia123'.
-- ============================================================
UPDATE usuarios SET password_hash = '$2a$10$n7uGz2GzFEIhdSm3KDEOYO0ZMzGL132epZ61oqWefVWQYQIufMQVy'
WHERE email = 'johan@plantia.test';

UPDATE usuarios SET password_hash = '$2a$10$n7uGz2GzFEIhdSm3KDEOYO0ZMzGL132epZ61oqWefVWQYQIufMQVy'
WHERE email = 'lucia@plantia.test';
