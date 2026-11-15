-- ============================================================
-- PROYECTO PLANTIA - Version V11: sesiones con hash y caducidad
-- session_token pasa a guardar el SHA-256 (hex, 64 caracteres) del token, no
-- el token: si alguien lee la BD no puede usar las sesiones. Los tokens que
-- habia estaban en texto plano y dejan de ser validos (hay que iniciar sesion).
-- session_expira: fecha tras la cual el token ya no se acepta.
-- ============================================================
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS session_expira TIMESTAMP;
UPDATE usuarios SET session_token = NULL, session_expira = NULL;
