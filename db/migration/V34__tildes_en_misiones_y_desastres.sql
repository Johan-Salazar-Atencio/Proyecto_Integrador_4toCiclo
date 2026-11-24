-- V34: tildes en misiones, tipos de desastre y fondos sembrados por V13, V16 y V17.
-- Aditiva y prudente: solo cambia una fila si su texto sigue IDENTICO al sembrado; si el admin ya lo edito, se respeta.
-- V23 busca la mision por titulo ('Explorador Botanico') pero ya se ejecuto antes, asi que renombrar aqui es seguro.

UPDATE misiones SET titulo = 'Explorador Botánico' WHERE titulo = 'Explorador Botanico';
UPDATE misiones SET descripcion = 'Digitaliza 1 planta nueva a tu colección'
WHERE descripcion = 'Digitaliza 1 planta nueva a tu coleccion';

UPDATE tipos_desastre SET descripcion = 'Frío intenso que congela hojas y raíces.'
WHERE descripcion = 'Frio intenso que congela hojas y raices.';

UPDATE fondos_escena SET nombre = 'Campo de día' WHERE nombre = 'Campo de dia';
