-- V23: economia de hojas, prevencion de eventos, recuperacion y muerte de plantas.
-- Aditiva: no borra datos. La moneda del juego se guarda como "monedas" y en la app se llama "hojas".

-- Valores del juego que el admin puede ajustar sin tocar el codigo
CREATE TABLE parametros_juego (
    clave        VARCHAR(50) PRIMARY KEY,
    valor        INT NOT NULL,
    descripcion  VARCHAR(200)
);

INSERT INTO parametros_juego (clave, valor, descripcion) VALUES
    ('COSTO_REVIVIR',        250, 'Hojas que cuesta revivir una planta muerta'),
    ('HORAS_RECUPERACION',    24, 'Horas para abonar una planta golpeada por un evento antes de que muera'),
    ('ABONO_RECUPERACION',    45, 'Salud que suma cada abono mientras la planta se recupera'),
    ('SALUD_REVIVIDA',        50, 'Salud con la que vuelve una planta revivida'),
    ('MAX_ESCANEOS_REPETIDOS', 3, 'Escaneos de una especie ya vista que dan hojas y XP por dia');

-- Plantas: ventana de recuperacion y desastre que la golpeo (para mostrar su animacion una sola vez)
ALTER TABLE plantas_usuario ADD COLUMN recuperacion_hasta TIMESTAMP;
ALTER TABLE plantas_usuario ADD COLUMN golpe_tipo VARCHAR(50);

-- Tipos de evento: imagen, consejo y salud con la que queda una planta que no se previno.
-- La descripcion (ya existente) pasa a ser el texto informativo del evento.
ALTER TABLE tipos_desastre ADD COLUMN imagen_url VARCHAR(255);
ALTER TABLE tipos_desastre ADD COLUMN consejo VARCHAR(250);
ALTER TABLE tipos_desastre ADD COLUMN salud_final_bajo INT NOT NULL DEFAULT 40 CHECK (salud_final_bajo BETWEEN 1 AND 100);
ALTER TABLE tipos_desastre ADD COLUMN salud_final_moderado INT NOT NULL DEFAULT 20 CHECK (salud_final_moderado BETWEEN 1 AND 100);
ALTER TABLE tipos_desastre ADD COLUMN salud_final_critico INT NOT NULL DEFAULT 10 CHECK (salud_final_critico BETWEEN 1 AND 100);

UPDATE tipos_desastre SET
    descripcion = 'Las olas de calor suelen dañar los cultivos y ponen en riesgo a quienes trabajan la tierra en el campo peruano: el suelo se seca, el agua escasea y las cosechas se pierden. Riega el doble hoy y protege tus plantas, como lo hacen los agricultores.',
    consejo = 'Cubrir el suelo con paja o rastrojo ayuda a conservar la humedad.'
WHERE codigo = 'OLA_DE_CALOR';
UPDATE tipos_desastre SET
    descripcion = 'Los huaycos son flujos de agua, lodo y piedras que bajan por las quebradas en temporada de lluvias. Arrasan terrenos de cultivo, caminos y canales de riego en el campo peruano y pueden dejar a familias agricultoras sin su cosecha. Pon tu planta a salvo antes de que llegue.',
    consejo = 'Las zanjas y los muros de contención desvían el agua y el lodo.'
WHERE codigo = 'HUAYCO';
UPDATE tipos_desastre SET
    descripcion = 'Las heladas ocurren cuando la temperatura baja tanto que el agua dentro de las plantas se congela. En las zonas altas del Perú pueden quemar las hojas de cultivos como la papa o el maíz y arruinar una campaña entera. Protege tu planta antes de que caiga la noche.',
    consejo = 'Cubrir los cultivos con mantas o plásticos por la noche los protege del frío.'
WHERE codigo = 'HELADA';
UPDATE tipos_desastre SET
    descripcion = 'Las plagas son insectos u otros organismos que se alimentan de los cultivos. Si no se detectan a tiempo, pueden destruir parcelas completas y afectar el sustento de quienes viven del campo. Actúa temprano para proteger tu planta.',
    consejo = 'Revisar las hojas todos los días permite detectar una plaga a tiempo.'
WHERE codigo = 'PLAGA';

-- Reglas: cada cuidado y cada escaneo tienen su propia regla. Las antiguas se conservan inactivas
-- porque el historial de progreso las referencia.
UPDATE reglas_progreso SET activa = FALSE WHERE motivo IN ('ESCANEO', 'ESPECIE_NUEVA', 'CUIDAR');

INSERT INTO reglas_progreso (motivo, xp, monedas, tope_diario) VALUES
    ('ESCANEO_NUEVA',    70, 20, NULL),
    ('ESCANEO_REPETIDO', 10,  3, 3),
    ('REGAR',             2,  1, 5),
    ('SOL',               2,  1, 5),
    ('ABONAR',            3,  1, 3),
    ('EVENTO_COMPLETO',  80, 40, NULL);

UPDATE reglas_progreso SET xp = 30, monedas = 10 WHERE motivo = 'DIGITALIZAR';
UPDATE reglas_progreso SET xp = 10, monedas = 3  WHERE motivo = 'RACHA';
-- REFUGIO ahora es "prevenir el evento": premio por cada planta prevenida cuando el evento termina
UPDATE reglas_progreso SET xp = 10, monedas = 3  WHERE motivo = 'REFUGIO';

-- Misiones: un tipo nuevo (CUIDADO cuenta riego, luz y abono) y recompensas ajustadas a la economia
ALTER TABLE misiones DROP CONSTRAINT IF EXISTS misiones_tipo_check;
ALTER TABLE misiones ADD CONSTRAINT misiones_tipo_check
    CHECK (tipo IN ('RIEGO', 'ESCANEO', 'SOL', 'ABONO', 'COLECCION', 'REFUGIO', 'DIGITALIZAR', 'CUIDADO'));

UPDATE misiones SET recompensa_monedas = 8,  recompensa_xp = 15, orden = 1 WHERE titulo = 'Riego Consciente';
UPDATE misiones SET recompensa_monedas = 15, recompensa_xp = 30, orden = 3 WHERE titulo = 'Explorador Botanico';
UPDATE misiones SET recompensa_monedas = 20, recompensa_xp = 40, orden = 4 WHERE titulo = 'Jardinero Digital';
-- La mision del evento sale del propio evento activo (tarjeta roja); "A Resguardo" queda desactivada
UPDATE misiones SET activa = FALSE WHERE titulo = 'A Resguardo';

INSERT INTO misiones (titulo, descripcion, tipo, meta, recompensa_monedas, recompensa_xp, predeterminada, orden)
SELECT 'Cuidado Completo', 'Riega, da luz y abona una planta hoy', 'CUIDADO', 3, 12, 25, TRUE, 2
WHERE NOT EXISTS (SELECT 1 FROM misiones WHERE titulo = 'Cuidado Completo');
