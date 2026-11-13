-- ============================================================
-- PROYECTO PLANTIA - Version V10: invalida los tokens de sesion fijos
-- V8 dejo a los usuarios 1 y 2 con tokens conocidos ('dev-default-token-user-N')
-- que viajaban escritos en el codigo del movil. Como Flyway aplica V8 en todos
-- los entornos, cualquiera podia suplantarlos. Se borran: cada usuario obtiene
-- un token nuevo al iniciar sesion (POST /api/v1/auth/login).
-- ============================================================
UPDATE usuarios SET session_token = NULL
WHERE session_token LIKE 'dev-default-token-%';
