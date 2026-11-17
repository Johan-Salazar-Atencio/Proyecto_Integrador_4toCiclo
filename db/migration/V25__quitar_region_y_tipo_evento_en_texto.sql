-- V25: se quitan las columnas de texto que V13 dejo duplicadas con las llaves.
--   usuarios.region, eventos_desastre.region y eventos_desastre.tipo_evento ya no existen:
--   la region sale de regiones (id_region) y el tipo de tipos_desastre (id_tipo_desastre).
-- Spring y Django leen y escriben solo las llaves.

-- Completar las llaves que pudieran haber quedado vacias antes de borrar el texto
UPDATE usuarios SET id_region = (SELECT id_region FROM regiones WHERE codigo = 'GLOBAL') WHERE id_region IS NULL;
UPDATE eventos_desastre SET id_region = (SELECT id_region FROM regiones WHERE codigo = 'GLOBAL') WHERE id_region IS NULL;
UPDATE eventos_desastre e SET id_tipo_desastre = t.id_tipo_desastre
FROM tipos_desastre t WHERE e.id_tipo_desastre IS NULL AND t.codigo = e.tipo_evento;

-- El trigger de transicion de V13 lee las columnas que se van a borrar
DROP TRIGGER IF EXISTS trg_usuarios_llaves_v13 ON usuarios;
DROP TRIGGER IF EXISTS trg_eventos_llaves_v13 ON eventos_desastre;
DROP FUNCTION IF EXISTS fn_sincronizar_llaves_v13();

ALTER TABLE usuarios DROP COLUMN region;
ALTER TABLE eventos_desastre DROP COLUMN region;
ALTER TABLE eventos_desastre DROP COLUMN tipo_evento;

-- Quien inserte sin region (un registro nuevo, un evento sin zona) queda en GLOBAL
CREATE OR REPLACE FUNCTION fn_region_global() RETURNS trigger AS $$
BEGIN
    IF NEW.id_region IS NULL THEN
        SELECT id_region INTO NEW.id_region FROM regiones WHERE codigo = 'GLOBAL';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_usuarios_region_global BEFORE INSERT ON usuarios
    FOR EACH ROW EXECUTE FUNCTION fn_region_global();
CREATE TRIGGER trg_eventos_region_global BEFORE INSERT ON eventos_desastre
    FOR EACH ROW EXECUTE FUNCTION fn_region_global();
