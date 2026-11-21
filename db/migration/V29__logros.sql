-- V29: sistema de logros configurable por datos.
-- Reemplaza el esquema viejo de logros (V1/V15) por el contrato del juego.
-- Aditiva en reglas_progreso: agrega la regla LOGRO para historial de premios.

DROP TABLE IF EXISTS logros_usuario;
DROP TABLE IF EXISTS logros;

CREATE TABLE logros (
    id_logro           SERIAL PRIMARY KEY,
    codigo             VARCHAR(50)  NOT NULL UNIQUE,
    titulo             VARCHAR(80)  NOT NULL,
    descripcion        VARCHAR(250) NOT NULL,
    icono              VARCHAR(40)  NOT NULL,
    rareza             VARCHAR(15)  NOT NULL DEFAULT 'COMUN'
                           CHECK (rareza IN ('COMUN', 'RARA', 'EPICA', 'LEGENDARIA')),
    condicion          VARCHAR(40)  NOT NULL
                           CHECK (condicion IN (
                               'ESCANEOS_TOTAL', 'ESPECIES_DISTINTAS', 'DESASTRE_AFRONTADO',
                               'DESASTRES_TIPOS_DISTINTOS', 'PLANTAS_REFUGIADAS', 'EVENTOS_SIN_PERDIDAS',
                               'RIEGOS_TOTAL', 'ABONOS_TOTAL', 'LUZ_TOTAL', 'RACHA_DIAS',
                               'NIVEL', 'COMPRAS_TIENDA', 'MACETAS_COMPRADAS', 'INVERNADEROS_COMPRADOS',
                               'FONDOS_COMPRADOS', 'MISIONES_COMPLETADAS', 'ESCANEO_HORA'
                           )),
    meta               INT          NOT NULL DEFAULT 1 CHECK (meta >= 1),
    parametro          VARCHAR(60),
    recompensa_xp      INT          NOT NULL DEFAULT 0 CHECK (recompensa_xp >= 0),
    recompensa_monedas INT          NOT NULL DEFAULT 0 CHECK (recompensa_monedas >= 0),
    secreto            BOOLEAN      NOT NULL DEFAULT FALSE,
    activo             BOOLEAN      NOT NULL DEFAULT TRUE,
    orden              INT          NOT NULL DEFAULT 0
);

CREATE TABLE logros_usuario (
    id_logro_usuario SERIAL PRIMARY KEY,
    id_usuario       INT       NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    id_logro         INT       NOT NULL REFERENCES logros(id_logro) ON DELETE CASCADE,
    progreso         INT       NOT NULL DEFAULT 0,
    desbloqueado_en  TIMESTAMP,
    notificado       BOOLEAN   NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_logro_usuario UNIQUE (id_usuario, id_logro)
);

CREATE INDEX idx_logros_usuario_usuario ON logros_usuario(id_usuario);

INSERT INTO reglas_progreso (motivo, xp, monedas, tope_diario, activa)
SELECT 'LOGRO', 0, 0, NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM reglas_progreso WHERE motivo = 'LOGRO');

INSERT INTO logros (codigo, titulo, descripcion, icono, rareza, condicion, meta, parametro, recompensa_xp, recompensa_monedas, secreto, orden) VALUES
    ('PRIMERA_PLANTA',        'Mi primera planta',      'Escanea tu primera planta.',                                  'semilla',     'COMUN',      'ESCANEOS_TOTAL',             1,   NULL,           20,  10, FALSE,  1),
    ('ESCANEOS_10',           'Aprendiz',               'Escanea 10 plantas.',                                         'semilla',     'COMUN',      'ESCANEOS_TOTAL',             10,  NULL,           50,  20, FALSE,  2),
    ('ESCANEOS_30',           'Explorador',             'Escanea 30 plantas.',                                         'semilla',     'RARA',       'ESCANEOS_TOTAL',             30,  NULL,          100,  40, FALSE,  3),
    ('ESCANEOS_50',           'Botánico',               'Escanea 50 plantas.',                                         'semilla',     'EPICA',      'ESCANEOS_TOTAL',             50,  NULL,          200,  80, FALSE,  4),
    ('ESCANEOS_100',          'Maestro botánico',       'Escanea 100 plantas.',                                        'semilla',     'LEGENDARIA', 'ESCANEOS_TOTAL',             100, NULL,          400, 150, FALSE,  5),
    ('ESPECIES_5',            'Coleccionista',          'Escanea 5 especies distintas.',                               'hoja',        'COMUN',      'ESPECIES_DISTINTAS',         5,   NULL,           50,  20, FALSE,  6),
    ('ESPECIES_15',           'Gran coleccionista',     'Escanea 15 especies distintas.',                              'hoja',        'RARA',       'ESPECIES_DISTINTAS',         15,  NULL,          120,  50, FALSE,  7),
    ('ESPECIES_30',           'Enciclopedia viva',      'Escanea 30 especies distintas.',                              'hoja',        'EPICA',      'ESPECIES_DISTINTAS',         30,  NULL,          250, 100, FALSE,  8),
    ('PRIMER_HUAYCO',         'Frente al huayco',       'Afronta tu primer huayco.',                                   'huayco',      'RARA',       'DESASTRE_AFRONTADO',         1,   'HUAYCO',       60,  25, FALSE,  9),
    ('PRIMERA_HELADA',        'Noche de helada',        'Afronta tu primera helada.',                                  'copo',        'RARA',       'DESASTRE_AFRONTADO',         1,   'HELADA',       60,  25, FALSE, 10),
    ('PRIMERA_OLA_CALOR',     'Bajo el sol ardiente',   'Afronta tu primera ola de calor.',                            'sol',         'RARA',       'DESASTRE_AFRONTADO',         1,   'OLA_DE_CALOR', 60,  25, FALSE, 11),
    ('PRIMERA_PLAGA',         'Plaga en el cultivo',    'Afronta tu primera plaga.',                                   'bicho',       'RARA',       'DESASTRE_AFRONTADO',         1,   'PLAGA',        60,  25, FALSE, 12),
    ('SUPERVIVIENTE_CAMPO',   'Superviviente del campo','Afronta los 4 tipos de desastre.',                            'escudo',      'EPICA',      'DESASTRES_TIPOS_DISTINTOS',  4,   NULL,          250, 100, FALSE, 13),
    ('PREVISOR',              'Previsor',               'Refugia una planta antes de un evento.',                      'refugio',     'COMUN',      'PLANTAS_REFUGIADAS',         1,   NULL,           40,  15, FALSE, 14),
    ('ESCUDO_VERDE',          'Escudo verde',           'Refugia 10 plantas antes de un evento.',                      'refugio',     'EPICA',      'PLANTAS_REFUGIADAS',         10,  NULL,          200,  80, FALSE, 15),
    ('RESILIENTE',            'Resiliente',             'Termina 5 eventos sin perder ninguna planta.',                'corazon',     'EPICA',      'EVENTOS_SIN_PERDIDAS',       5,   NULL,          220,  90, FALSE, 16),
    ('PRIMERA_GOTA',          'Primera gota',           'Riega una planta por primera vez.',                           'gota',        'COMUN',      'RIEGOS_TOTAL',               1,   NULL,           10,   5, FALSE, 17),
    ('MANO_VERDE',            'Mano verde',             'Riega plantas 50 veces.',                                     'gota',        'RARA',       'RIEGOS_TOTAL',               50,  NULL,          100,  40, FALSE, 18),
    ('BUEN_ABONO',            'Buen abono',             'Abona una planta por primera vez.',                           'abono',       'COMUN',      'ABONOS_TOTAL',               1,   NULL,           15,   5, FALSE, 19),
    ('SOL_MANANA',            'Sol de la mañana',       'Da luz a una planta por primera vez.',                        'luz',         'COMUN',      'LUZ_TOTAL',                  1,   NULL,           15,   5, FALSE, 20),
    ('RACHA_3',               'Constante',              'Mantén una racha de 3 días.',                                 'racha',       'COMUN',      'RACHA_DIAS',                 3,   NULL,           30,  10, FALSE, 21),
    ('RACHA_7',               'Una semana verde',       'Mantén una racha de 7 días.',                                 'racha',       'RARA',       'RACHA_DIAS',                 7,   NULL,           80,  30, FALSE, 22),
    ('RACHA_30',              'Jardinero fiel',         'Mantén una racha de 30 días.',                                'racha',       'LEGENDARIA', 'RACHA_DIAS',                 30,  NULL,          400, 150, FALSE, 23),
    ('NIVEL_5',               'Brote joven',            'Alcanza el nivel 5.',                                         'nivel',       'COMUN',      'NIVEL',                      5,   NULL,           50,  20, FALSE, 24),
    ('NIVEL_10',              'Árbol firme',            'Alcanza el nivel 10.',                                        'nivel',       'RARA',       'NIVEL',                      10,  NULL,          150,  60, FALSE, 25),
    ('PRIMERA_COMPRA',        'Primera compra',         'Compra algo en la tienda.',                                   'tienda',      'COMUN',      'COMPRAS_TIENDA',             1,   NULL,           15,   0, FALSE, 26),
    ('PRIMERA_MACETA',        'Mi primera maceta',      'Compra tu primera maceta.',                                   'maceta',      'COMUN',      'MACETAS_COMPRADAS',          1,   NULL,           20,  10, FALSE, 27),
    ('PRIMER_INVERNADERO',    'Bajo techo',             'Compra tu primer invernadero.',                               'invernadero', 'RARA',       'INVERNADEROS_COMPRADOS',     1,   NULL,           60,  25, FALSE, 28),
    ('DECORADOR',             'Decorador',              'Compra tu primer fondo de escena.',                           'fondo',       'COMUN',      'FONDOS_COMPRADOS',           1,   NULL,           20,  10, FALSE, 29),
    ('MISION_1',              'Primera misión',         'Completa tu primera misión.',                                 'mision',      'COMUN',      'MISIONES_COMPLETADAS',       1,   NULL,           20,  10, FALSE, 30),
    ('MISION_10',             'Cumplidor',              'Completa 10 misiones.',                                       'mision',      'RARA',       'MISIONES_COMPLETADAS',       10,  NULL,          100,  40, FALSE, 31),
    ('MISION_50',             'Incansable',             'Completa 50 misiones.',                                       'mision',      'EPICA',      'MISIONES_COMPLETADAS',       50,  NULL,          250, 100, FALSE, 32),
    ('MADRUGADOR',            'Madrugador',             'Escanea una planta antes de las 6:00.',                       'amanecer',    'RARA',       'ESCANEO_HORA',               1,   'MADRUGADA',    50,  20, TRUE,  33),
    ('NOCTURNO',              'Búho nocturno',          'Escanea una planta desde las 23:00.',                         'luna',        'RARA',       'ESCANEO_HORA',               1,   'NOCHE',        50,  20, TRUE,  34);
