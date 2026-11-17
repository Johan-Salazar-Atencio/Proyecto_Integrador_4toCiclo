-- V24: baul de plantas (tope de 12 activas), macetas, invernaderos en la tienda y apodos.
-- Aditiva: no borra datos de usuarios.

-- Baul: una planta guardada queda congelada (no crece, no se desgasta, no le afectan eventos).
-- Maceta: la que la planta tiene puesta (un articulo de la tienda de tipo MACETA); null = la terracota de siempre.
ALTER TABLE plantas_usuario ADD COLUMN en_baul BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE plantas_usuario ADD COLUMN id_maceta INT REFERENCES items_tienda(id_item) ON DELETE SET NULL;
CREATE INDEX idx_plantas_usuario_baul ON plantas_usuario(id_usuario, en_baul);

-- Los articulos de pixel art se identifican por un codigo estable: el movil y la web los dibujan por ese codigo
ALTER TABLE items_tienda ADD COLUMN codigo VARCHAR(30);
CREATE UNIQUE INDEX ux_items_tienda_codigo ON items_tienda(codigo) WHERE codigo IS NOT NULL;
ALTER TABLE items_tienda DROP CONSTRAINT IF EXISTS items_tienda_tipo_check;
ALTER TABLE items_tienda ADD CONSTRAINT items_tienda_tipo_check
    CHECK (tipo IN ('MACETA', 'FONDO', 'ABONO', 'INVERNADERO'));

-- Invernadero elegido por el usuario para su coleccion virtual; null = Selva tropical, el predeterminado
ALTER TABLE usuarios ADD COLUMN id_invernadero INT REFERENCES items_tienda(id_item) ON DELETE SET NULL;

-- Macetas. Las de precio 0 ya son de todos los usuarios; las demas se compran una sola vez.
INSERT INTO items_tienda (nombre, tipo, precio_monedas, nivel_requerido, asset_url, descripcion, codigo, activo) VALUES
    ('Terracota', 'MACETA',  0, 1, '/pixel/maceta/terracota', 'La maceta de barro de toda la vida.',      'TERRACOTA', TRUE),
    ('Azul',      'MACETA',  0, 1, '/pixel/maceta/azul',      'Ceramica azul.',                            'AZUL',      TRUE),
    ('Crema',     'MACETA', 80, 1, '/pixel/maceta/crema',     'Ceramica color crema.',                     'CREMA',     TRUE),
    ('Rosa',      'MACETA', 100, 1, '/pixel/maceta/rosa',     'Ceramica rosada.',                          'ROSA',      TRUE),
    ('Dorada',    'MACETA', 150, 2, '/pixel/maceta/dorada',   'Una maceta dorada para tu mejor planta.',   'DORADA',    TRUE);

-- Invernaderos (el fondo de la coleccion virtual). La Selva tropical es gratis y es el predeterminado.
INSERT INTO items_tienda (nombre, tipo, precio_monedas, nivel_requerido, asset_url, descripcion, codigo, activo) VALUES
    ('Selva tropical',    'INVERNADERO',   0, 1, '/pixel/invernadero/selva',      'Marco de madera, lianas, orquideas y un guacamayo.',      'SELVA',      TRUE),
    ('Cristal victoriano', 'INVERNADERO', 150, 2, '/pixel/invernadero/victoriano', 'Hierro blanco, helechos colgantes y rosas.',              'VICTORIANO', TRUE),
    ('Taller de luces',   'INVERNADERO', 200, 3, '/pixel/invernadero/luces',      'Hierro negro, guirnaldas y faroles que brillan de noche.', 'LUCES',      TRUE);

-- Parametros: tope de plantas activas; el tope de escaneos repetidos vive en la regla ESCANEO_REPETIDO
DELETE FROM parametros_juego WHERE clave = 'MAX_ESCANEOS_REPETIDOS';
INSERT INTO parametros_juego (clave, valor, descripcion) VALUES
    ('MAX_PLANTAS_ACTIVAS', 12, 'Plantas virtuales activas que se pueden tener; las demas van al baul');
