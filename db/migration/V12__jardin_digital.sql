-- ============================================================
-- PROYECTO PLANTIA - Version V12: datos del jardin digital
-- La web simulaba el jardin en localStorage porque faltaban estos datos.
--   crecimiento : cuidados utiles acumulados (define la etapa del pixel art)
--   apariencia  : JSON con forma y colores que dio la IA al escanear
--   foto_url    : foto del escaneo del que salio la planta
--   es_activa   : la planta que se muestra en "Mi jardin" (una por usuario)
-- ============================================================
ALTER TABLE plantas_usuario ADD COLUMN IF NOT EXISTS crecimiento INT NOT NULL DEFAULT 0;
ALTER TABLE plantas_usuario ADD COLUMN IF NOT EXISTS apariencia TEXT;
ALTER TABLE plantas_usuario ADD COLUMN IF NOT EXISTS foto_url VARCHAR(500);
ALTER TABLE plantas_usuario ADD COLUMN IF NOT EXISTS es_activa BOOLEAN NOT NULL DEFAULT FALSE;
