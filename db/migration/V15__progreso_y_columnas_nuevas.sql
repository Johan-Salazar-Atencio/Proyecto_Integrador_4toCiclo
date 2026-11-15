-- V15: reglas e historial de progreso (XP, monedas) y columnas nuevas para racha,
-- refugio, resultado de desastres, misiones y logros. Aditiva: no cambia ni borra datos.

-- Reglas editables: cuanto XP y monedas da cada accion.
-- tope_diario = maximo de veces por dia que cuenta la accion (NULL = sin tope).
CREATE TABLE reglas_progreso (
    id_regla     SERIAL PRIMARY KEY,
    motivo       VARCHAR(30) UNIQUE NOT NULL,
    xp           INT NOT NULL DEFAULT 0,
    monedas      INT NOT NULL DEFAULT 0,
    tope_diario  INT CHECK (tope_diario IS NULL OR tope_diario > 0),
    activa       BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO reglas_progreso (motivo, xp, monedas, tope_diario) VALUES
    ('ESCANEO',       20, 0, NULL),
    ('ESPECIE_NUEVA', 50, 0, NULL),
    ('DIGITALIZAR',   30, 0, NULL),
    ('CUIDAR',         5, 0, 10),
    ('MISION',         0, 0, NULL),
    ('REFUGIO',       40, 0, NULL),
    ('RACHA',         10, 0, 1);

-- Un renglon por cada vez que un usuario gana XP o monedas
CREATE TABLE historial_progreso (
    id_movimiento     SERIAL PRIMARY KEY,
    id_usuario        INT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    id_regla          INT NOT NULL REFERENCES reglas_progreso(id_regla),
    xp_ganado         INT NOT NULL DEFAULT 0,
    monedas_ganadas   INT NOT NULL DEFAULT 0,
    nivel_resultante  INT NOT NULL,
    id_referencia     INT,
    fecha             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_historial_usuario_fecha ON historial_progreso(id_usuario, fecha);
CREATE INDEX idx_historial_regla_dia ON historial_progreso(id_usuario, id_regla, fecha);

-- Usuarios: racha diaria y foto de perfil
ALTER TABLE usuarios ADD COLUMN ultima_actividad DATE;
ALTER TABLE usuarios ADD COLUMN foto_url VARCHAR(500);

-- Plantas: desde cuando esta refugiada
ALTER TABLE plantas_usuario ADD COLUMN refugiada_desde TIMESTAMP;

-- Resultado de un desastre por planta: una sola fila por evento y planta
ALTER TABLE registros_desastre_usuario ADD COLUMN estaba_refugiada BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE registros_desastre_usuario ADD COLUMN xp_otorgado INT NOT NULL DEFAULT 0;
ALTER TABLE registros_desastre_usuario
    ADD CONSTRAINT uq_registro_evento_planta UNIQUE (id_desastre, id_planta_usuario);
CREATE INDEX idx_registros_planta ON registros_desastre_usuario(id_planta_usuario);

-- Misiones: XP de recompensa y dos tipos nuevos
ALTER TABLE misiones ADD COLUMN recompensa_xp INT NOT NULL DEFAULT 0;
ALTER TABLE misiones DROP CONSTRAINT IF EXISTS misiones_tipo_check;
ALTER TABLE misiones ADD CONSTRAINT misiones_tipo_check
    CHECK (tipo IN ('RIEGO', 'ESCANEO', 'SOL', 'ABONO', 'COLECCION', 'REFUGIO', 'DIGITALIZAR'));

-- Logros: que condicion los desbloquea y cuanto hay que alcanzar
ALTER TABLE logros ADD COLUMN tipo_condicion VARCHAR(30);
ALTER TABLE logros ADD COLUMN meta INT;
