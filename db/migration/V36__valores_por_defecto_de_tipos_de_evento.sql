-- V36: valores por defecto de cada tipo de evento (región, duración y mensaje push), que el panel admin usa al lanzarlo.
-- Aditiva: no borra datos. Spring no lee estas columnas; solo las usa Django y el panel.
ALTER TABLE tipos_desastre ADD COLUMN region_defecto VARCHAR(100) NOT NULL DEFAULT 'GLOBAL';
ALTER TABLE tipos_desastre ADD COLUMN duracion_defecto_min INT NOT NULL DEFAULT 30 CHECK (duracion_defecto_min BETWEEN 5 AND 4320);
ALTER TABLE tipos_desastre ADD COLUMN mensaje_push VARCHAR(200);
