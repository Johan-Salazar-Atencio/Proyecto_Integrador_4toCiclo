-- ============================================================
-- PROYECTO PLANTIA - Version V5: Columna publicada en catálogo
-- Agrega el campo 'publicada' a plantas_catalogo
-- ============================================================
ALTER TABLE plantas_catalogo ADD COLUMN publicada BOOLEAN NOT NULL DEFAULT TRUE;