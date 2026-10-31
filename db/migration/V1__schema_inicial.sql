-- ============================================================
-- PROYECTO PLANTIA - SCHEMA INICIAL (PostgreSQL)
-- Modulo compartido: Spring Boot (Usuario/Juego) + Django (Admin/IA)
-- Version: V1
-- ============================================================

-- 1. USUARIOS
CREATE TABLE usuarios (
    id_usuario       SERIAL PRIMARY KEY,
    nombre           VARCHAR(100) NOT NULL,
    email            VARCHAR(150) UNIQUE NOT NULL,
    password_hash    VARCHAR(255) NOT NULL,
    rol              VARCHAR(20) DEFAULT 'USER' CHECK (rol IN ('USER', 'ADMIN')),
    nivel            INT DEFAULT 1,
    experiencia_xp   INT DEFAULT 0,
    monedas          INT DEFAULT 100,
    racha_dias       INT DEFAULT 0,
    fecha_registro   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. CATALOGO BASE DE PLANTAS
CREATE TABLE plantas_catalogo (
    id_catalogo             SERIAL PRIMARY KEY,
    nombre_comun            VARCHAR(100) NOT NULL,
    nombre_cientifico       VARCHAR(150) UNIQUE NOT NULL,
    frecuencia_riego_dias   INT NOT NULL,
    frecuencia_sol_horas    INT NOT NULL,
    temperatura_min_c       INT DEFAULT 10,
    temperatura_max_c       INT DEFAULT 30,
    es_toxica_mascotas      BOOLEAN DEFAULT FALSE,
    sprite_base_url         VARCHAR(255) NOT NULL,
    etapa_semilla_url       VARCHAR(255),
    etapa_brote_url         VARCHAR(255),
    etapa_adulta_url        VARCHAR(255),
    datos_ia_raw            JSONB,
    fecha_creacion          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. HISTORIAL DE ESCANEOS CON IA
CREATE TABLE escaneos_historial (
    id_escaneo        SERIAL PRIMARY KEY,
    id_usuario        INT REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    id_catalogo       INT REFERENCES plantas_catalogo(id_catalogo) ON DELETE SET NULL,
    imagen_foto_url   VARCHAR(255) NOT NULL,
    confianza_ia      NUMERIC(5,2),
    respuesta_ia_raw  JSONB NOT NULL,
    fecha_escaneo     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. PLANTAS EN CRIANZA DEL USUARIO (TAMAGOTCHI)
CREATE TABLE plantas_usuario (
    id_planta_usuario       SERIAL PRIMARY KEY,
    id_usuario              INT REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    id_catalogo             INT REFERENCES plantas_catalogo(id_catalogo) ON DELETE RESTRICT,
    apodo                   VARCHAR(50),
    salud_porcentaje        INT DEFAULT 100 CHECK (salud_porcentaje BETWEEN 0 AND 100),
    hidratacion_porcentaje  INT DEFAULT 100 CHECK (hidratacion_porcentaje BETWEEN 0 AND 100),
    sol_porcentaje          INT DEFAULT 100 CHECK (sol_porcentaje BETWEEN 0 AND 100),
    etapa_crecimiento       VARCHAR(20) DEFAULT 'SEMILLA'
                            CHECK (etapa_crecimiento IN ('SEMILLA','BROTE','ADULTA','MUERTA')),
    modo_tiempo             VARCHAR(20) DEFAULT 'REAL'
                            CHECK (modo_tiempo IN ('REAL','ACELERADO')),
    esta_refugiada          BOOLEAN DEFAULT FALSE,
    ultimo_riego            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ultimo_sol              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ultimo_abono            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_plantado          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. EVENTOS Y DESASTRES CLIMATICOS (ADMIN)
CREATE TABLE eventos_desastre (
    id_desastre          SERIAL PRIMARY KEY,
    id_creador           INT REFERENCES usuarios(id_usuario),
    tipo_evento          VARCHAR(50) NOT NULL
                         CHECK (tipo_evento IN ('HUAYCO','HELADA','OLA_DE_CALOR','PLAGA')),
    region               VARCHAR(100) DEFAULT 'GLOBAL',
    mensaje_notificacion TEXT NOT NULL,
    fecha_inicio         TIMESTAMP NOT NULL,
    fecha_fin            TIMESTAMP NOT NULL,
    estado               VARCHAR(20) DEFAULT 'PROGRAMADO'
                         CHECK (estado IN ('PROGRAMADO','ACTIVO','FINALIZADO')),
    severidad            VARCHAR(20) DEFAULT 'MODERADO'
                         CHECK (severidad IN ('BAJO','MODERADO','CRITICO'))
);

-- 6. REGISTROS DE IMPACTO DE DESASTRES EN PLANTAS
CREATE TABLE registros_desastre_usuario (
    id_registro             SERIAL PRIMARY KEY,
    id_desastre             INT REFERENCES eventos_desastre(id_desastre) ON DELETE CASCADE,
    id_planta_usuario       INT REFERENCES plantas_usuario(id_planta_usuario) ON DELETE CASCADE,
    estado_final            VARCHAR(30) NOT NULL
                            CHECK (estado_final IN ('SALVADA','DANADA','DESTRUIDA')),
    dano_salud_aplicado     INT DEFAULT 0,
    fecha_procesado         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. CATALOGO DE LA TIENDA
CREATE TABLE items_tienda (
    id_item         SERIAL PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL,
    tipo            VARCHAR(30) NOT NULL CHECK (tipo IN ('MACETA','FONDO','ABONO')),
    precio_monedas  INT NOT NULL,
    nivel_requerido INT DEFAULT 1,
    asset_url       VARCHAR(255) NOT NULL
);

-- 8. INVENTARIO DEL USUARIO
CREATE TABLE inventario_usuario (
    id_inventario   SERIAL PRIMARY KEY,
    id_usuario      INT REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    id_item         INT REFERENCES items_tienda(id_item) ON DELETE CASCADE,
    esta_equipado   BOOLEAN DEFAULT FALSE,
    fecha_compra    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 9. LOGROS Y MEDALLAS
CREATE TABLE logros (
    id_logro            SERIAL PRIMARY KEY,
    nombre              VARCHAR(100) NOT NULL,
    descripcion         TEXT NOT NULL,
    icono_url           VARCHAR(255) NOT NULL,
    recompensa_monedas  INT DEFAULT 50
);

CREATE TABLE logros_usuario (
    id_usuario      INT REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    id_logro        INT REFERENCES logros(id_logro) ON DELETE CASCADE,
    fecha_obtenido  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_usuario, id_logro)
);

-- ============================================================
-- INDICES PARA OPTIMIZACION
-- ============================================================
CREATE INDEX idx_escaneos_usuario ON escaneos_historial(id_usuario);
CREATE INDEX idx_plantas_usuario ON plantas_usuario(id_usuario);
CREATE INDEX idx_eventos_estado ON eventos_desastre(estado);
CREATE INDEX idx_inventario_usuario ON inventario_usuario(id_usuario);
CREATE INDEX idx_plantas_catalogo_nombre ON plantas_catalogo(nombre_cientifico);
