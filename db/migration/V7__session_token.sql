-- ============================================================
-- PROYECTO PLANTIA - Version V7: Token de sesion por usuario
-- Cierra la suplantacion por header X-User-Id: cada login genera
-- un token aleatorio que el cliente debe enviar en X-Session-Token.
-- ============================================================
ALTER TABLE usuarios ADD COLUMN session_token VARCHAR(64);
