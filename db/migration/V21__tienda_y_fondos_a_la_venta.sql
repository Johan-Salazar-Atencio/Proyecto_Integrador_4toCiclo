-- Tienda: el admin puede activar y desactivar productos, y se venden fondos nuevos en pixel art.
-- Las macetas quedan para mas adelante (se desactivan los datos de ejemplo de V3).
ALTER TABLE items_tienda ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE items_tienda ADD COLUMN descripcion VARCHAR(200);

-- Cada usuario compra un articulo una sola vez (el abono se usa en el momento y no pasa por aqui)
CREATE UNIQUE INDEX ux_inventario_usuario_item ON inventario_usuario(id_usuario, id_item);

UPDATE items_tienda SET activo = FALSE WHERE tipo = 'MACETA' OR (tipo = 'FONDO' AND id_fondo IS NULL);
UPDATE items_tienda SET descripcion = 'Suma un dia de crecimiento a tu planta (maximo uno al dia).'
WHERE tipo = 'ABONO';

-- Fondos extra: tira de 96 cuadros de 96x96 (scripts/arte/fondos_extra.py), no predeterminados
INSERT INTO fondos_escena (momento, nombre, imagen_url, total_frames, ancho_px, alto_px, ms_por_frame, es_predeterminado)
SELECT v.momento, v.nombre, v.imagen_url, 96, 96, 96, 120, FALSE
FROM (VALUES
    ('DIA',   'Campo nevado',     '/files/arte/fondos/nevado.png'),
    ('TARDE', 'Desierto dorado',  '/files/arte/fondos/desierto.png'),
    ('DIA',   'Cerezos en flor',  '/files/arte/fondos/sakura.png'),
    ('NOCHE', 'Ciudad cyberpunk', '/files/arte/fondos/cyberpunk.png')
) AS v(momento, nombre, imagen_url)
WHERE NOT EXISTS (SELECT 1 FROM fondos_escena f WHERE f.imagen_url = v.imagen_url);

-- Un articulo de tienda por fondo; asset_url es la vista previa (primer cuadro)
INSERT INTO items_tienda (nombre, tipo, precio_monedas, nivel_requerido, asset_url, id_fondo, descripcion)
SELECT f.nombre, 'FONDO', v.precio, v.nivel, replace(f.imagen_url, '.png', '_vista.png'), f.id_fondo, v.descripcion
FROM (VALUES
    ('/files/arte/fondos/nevado.png',    100, 1, 'Nieve cayendo sobre pinos. Reemplaza tu fondo de dia.'),
    ('/files/arte/fondos/desierto.png',  150, 2, 'Dunas doradas y cactus. Reemplaza tu fondo de tarde.'),
    ('/files/arte/fondos/sakura.png',    180, 2, 'Petalos de cerezo al viento. Reemplaza tu fondo de dia.'),
    ('/files/arte/fondos/cyberpunk.png', 250, 3, 'Neones, lluvia y autos voladores. Reemplaza tu fondo de noche.')
) AS v(imagen_url, precio, nivel, descripcion)
JOIN fondos_escena f ON f.imagen_url = v.imagen_url
WHERE NOT EXISTS (SELECT 1 FROM items_tienda i WHERE i.id_fondo = f.id_fondo);
