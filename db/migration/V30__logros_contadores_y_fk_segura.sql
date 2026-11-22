-- V30: endurece los logros (aditiva; V29 ya esta aplicada y no se toca).
--
-- 1) Contadores de cuidados en usuarios. RIEGOS_TOTAL, ABONOS_TOTAL y LUZ_TOTAL contaban filas de
--    historial_progreso, pero las reglas REGAR/ABONAR/SOL tienen tope diario: pasado el tope no se
--    guarda fila y el conteo quedaba corto (MANO_VERDE casi imposible). Ahora Spring suma 1 en cada
--    cuidado util (y en cada abono de tienda), con el usuario bloqueado.
-- 2) logros_usuario.id_logro pasa de ON DELETE CASCADE a RESTRICT: borrar un logro ya no puede
--    llevarse el avance de todos los usuarios. El admin solo desactiva logros.

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS riegos_total INT NOT NULL DEFAULT 0;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS abonos_total INT NOT NULL DEFAULT 0;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS luz_total    INT NOT NULL DEFAULT 0;

ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS ck_usuarios_cuidados_no_negativos;
ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_cuidados_no_negativos
    CHECK (riegos_total >= 0 AND abonos_total >= 0 AND luz_total >= 0);

-- Relleno con lo unico que existe: los cuidados que si dejaron fila en historial_progreso.
-- Es un piso (los cuidados pasados el tope diario no se pueden recuperar); nunca baja un valor.
UPDATE usuarios u
SET riegos_total = GREATEST(u.riegos_total, c.riegos),
    abonos_total = GREATEST(u.abonos_total, c.abonos),
    luz_total    = GREATEST(u.luz_total,    c.luz)
FROM (
    SELECT h.id_usuario,
           COUNT(*) FILTER (WHERE r.motivo = 'REGAR')  AS riegos,
           COUNT(*) FILTER (WHERE r.motivo = 'ABONAR') AS abonos,
           COUNT(*) FILTER (WHERE r.motivo = 'SOL')    AS luz
    FROM historial_progreso h
    JOIN reglas_progreso r ON r.id_regla = h.id_regla
    WHERE r.motivo IN ('REGAR', 'ABONAR', 'SOL')
    GROUP BY h.id_usuario
) c
WHERE c.id_usuario = u.id_usuario;

-- FK segura: el nombre lo puso Postgres en V29 (REFERENCES en linea)
ALTER TABLE logros_usuario DROP CONSTRAINT IF EXISTS logros_usuario_id_logro_fkey;
ALTER TABLE logros_usuario ADD CONSTRAINT logros_usuario_id_logro_fkey
    FOREIGN KEY (id_logro) REFERENCES logros(id_logro) ON DELETE RESTRICT;
