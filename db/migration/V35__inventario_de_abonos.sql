-- V35: los abonos pasan a ser inventario. La tienda los vende (suma al contador) y la planta los usa (descuenta 1).
-- Aditiva: no borra datos. Una fila por usuario y tipo de abono (items_tienda.tipo = 'ABONO').
CREATE TABLE abonos_usuario (
    id_abono_usuario SERIAL PRIMARY KEY,
    id_usuario       INT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    id_item          INT NOT NULL REFERENCES items_tienda(id_item) ON DELETE CASCADE,
    cantidad         INT NOT NULL DEFAULT 0 CHECK (cantidad >= 0),
    CONSTRAINT uq_abonos_usuario UNIQUE (id_usuario, id_item)
);
CREATE INDEX idx_abonos_usuario ON abonos_usuario(id_usuario);
