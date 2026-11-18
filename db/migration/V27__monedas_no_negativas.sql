-- V27: el saldo de monedas (hojas) nunca puede quedar negativo.
-- Ultima defensa contra el doble gasto: aunque una compra se colara, la BD la rechaza
-- (Spring la devuelve como 409). Aditiva: no borra datos.

-- Por si alguna prueba o carrera anterior dejo saldos raros
UPDATE usuarios SET monedas = 0 WHERE monedas IS NULL OR monedas < 0;

ALTER TABLE usuarios
    ADD CONSTRAINT ck_usuarios_monedas_no_negativas CHECK (monedas >= 0);
