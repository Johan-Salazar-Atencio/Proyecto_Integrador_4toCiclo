-- V26: varios tipos de abono (medio dia, 1 dia y 3 dias de crecimiento) y fotos en la coleccion.
-- Aditiva: no borra datos. El abono se mide en "medios dias" para poder dar medio dia (2 medios = 1 dia).

-- Cuantos medios dias de crecimiento suma cada abono (los articulos que no son abono lo ignoran)
ALTER TABLE items_tienda ADD COLUMN medios_dias INT NOT NULL DEFAULT 2 CHECK (medios_dias BETWEEN 1 AND 24);

-- Si una planta recibio un solo medio dia, queda pendiente hasta completar el dia
ALTER TABLE plantas_usuario ADD COLUMN medio_dia_pendiente BOOLEAN NOT NULL DEFAULT FALSE;

-- El abono que ya existia suma 1 dia (2 medios dias)
UPDATE items_tienda
SET medios_dias = 2,
    codigo = 'ABONO_1',
    descripcion = 'Suma 1 dia de crecimiento a tu planta (maximo un abono al dia por planta).'
WHERE tipo = 'ABONO';

-- Abonos nuevos: uno mas barato (medio dia) y uno mas caro (3 dias)
INSERT INTO items_tienda (nombre, tipo, precio_monedas, nivel_requerido, asset_url, descripcion, codigo, medios_dias)
SELECT 'Abono suave', 'ABONO', 15, 1, asset_url,
       'Suma medio dia de crecimiento. Dos abonos suaves hacen 1 dia.', 'ABONO_MEDIO', 1
FROM items_tienda WHERE codigo = 'ABONO_1' LIMIT 1;

INSERT INTO items_tienda (nombre, tipo, precio_monedas, nivel_requerido, asset_url, descripcion, codigo, medios_dias)
SELECT 'Abono reforzado', 'ABONO', 80, 1, asset_url,
       'Suma 3 dias de crecimiento de golpe. El mas caro y el que mas rinde.', 'ABONO_3', 6
FROM items_tienda WHERE codigo = 'ABONO_1' LIMIT 1;

-- Fotos: las plantas de la coleccion que se guardaron sin foto toman la del escaneo mas reciente de esa especie
UPDATE coleccion_usuario c
SET foto_url = e.imagen_foto_url
FROM (
    SELECT DISTINCT ON (id_usuario, id_catalogo) id_usuario, id_catalogo, imagen_foto_url
    FROM escaneos_historial
    WHERE id_catalogo IS NOT NULL
    ORDER BY id_usuario, id_catalogo, fecha_escaneo DESC
) e
WHERE c.foto_url IS NULL AND e.id_usuario = c.id_usuario AND e.id_catalogo = c.id_catalogo;
