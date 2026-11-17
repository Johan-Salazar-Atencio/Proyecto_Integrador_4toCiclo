-- Crecimiento por dias con cuidado: un dia cuenta si hubo al menos un cuidado util.
-- La duracion total depende de la forma de la planta (ver CrecimientoPlanta).
ALTER TABLE plantas_usuario ADD COLUMN dias_cuidados INT NOT NULL DEFAULT 0;
ALTER TABLE plantas_usuario ADD COLUMN ultimo_dia_cuidado DATE;

-- Plantas que ya existian: dos cuidados utiles se toman como un dia (maximo 4,
-- para que ninguna salte a la fase madura sin cuidarla de nuevo).
UPDATE plantas_usuario SET dias_cuidados = LEAST(COALESCE(crecimiento, 0) / 2, 4);
