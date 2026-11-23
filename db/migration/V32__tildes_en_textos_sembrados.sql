-- V32: tildes en los textos que ven los usuarios y que sembraron V3, V19, V21, V22 y V26 (QA-61).
-- Aditiva y prudente: solo cambia una fila si su texto sigue IDENTICO al sembrado; si el admin ya lo edito, se respeta.

-- Abonos de la tienda
UPDATE items_tienda SET nombre = 'Abono orgánico' WHERE nombre = 'Abono organico';
UPDATE items_tienda SET descripcion = 'Suma un día de crecimiento a tu planta (máximo uno al día).'
WHERE descripcion = 'Suma un dia de crecimiento a tu planta (maximo uno al dia).';
UPDATE items_tienda SET descripcion = 'Suma 1 día de crecimiento a tu planta (máximo un abono al día por planta).'
WHERE descripcion = 'Suma 1 dia de crecimiento a tu planta (maximo un abono al dia por planta).';
UPDATE items_tienda SET descripcion = 'Suma medio día de crecimiento. Dos abonos suaves hacen 1 día.'
WHERE descripcion = 'Suma medio dia de crecimiento. Dos abonos suaves hacen 1 dia.';
UPDATE items_tienda SET descripcion = 'Suma 3 días de crecimiento de golpe. El más caro y el que más rinde.'
WHERE descripcion = 'Suma 3 dias de crecimiento de golpe. El mas caro y el que mas rinde.';

-- Fondos a la venta
UPDATE items_tienda SET descripcion = 'Nieve cayendo sobre pinos. Reemplaza tu fondo de día.'
WHERE descripcion = 'Nieve cayendo sobre pinos. Reemplaza tu fondo de dia.';
UPDATE items_tienda SET descripcion = 'Pétalos de cerezo al viento. Reemplaza tu fondo de día.'
WHERE descripcion = 'Petalos de cerezo al viento. Reemplaza tu fondo de dia.';

-- Temas de fondo: el texto vive en temas_fondo y, copiado por V22, tambien en items_tienda (los dos se ven en la app)
UPDATE temas_fondo SET descripcion = 'El fondo de siempre: un campo verde que cambia con el día.'
WHERE descripcion = 'El fondo de siempre: un campo verde que cambia con el dia.';
UPDATE items_tienda SET descripcion = 'El fondo de siempre: un campo verde que cambia con el día.'
WHERE descripcion = 'El fondo de siempre: un campo verde que cambia con el dia.';
UPDATE temas_fondo SET descripcion = 'Nieve cayendo sobre pinos, de día, de tarde y de noche.'
WHERE descripcion = 'Nieve cayendo sobre pinos, de dia, de tarde y de noche.';
UPDATE items_tienda SET descripcion = 'Nieve cayendo sobre pinos, de día, de tarde y de noche.'
WHERE descripcion = 'Nieve cayendo sobre pinos, de dia, de tarde y de noche.';
UPDATE temas_fondo SET descripcion = 'Pétalos de cerezo al viento, del día a la noche.'
WHERE descripcion = 'Petalos de cerezo al viento, del dia a la noche.';
UPDATE items_tienda SET descripcion = 'Pétalos de cerezo al viento, del día a la noche.'
WHERE descripcion = 'Petalos de cerezo al viento, del dia a la noche.';
UPDATE temas_fondo SET descripcion = 'Neones, lluvia y autos voladores. Cambia con el día, la tarde y la noche.'
WHERE descripcion = 'Neones, lluvia y autos voladores. Cambia con el dia, la tarde y la noche.';
UPDATE items_tienda SET descripcion = 'Neones, lluvia y autos voladores. Cambia con el día, la tarde y la noche.'
WHERE descripcion = 'Neones, lluvia y autos voladores. Cambia con el dia, la tarde y la noche.';

-- Macetas e invernaderos (V24)
UPDATE items_tienda SET descripcion = 'Cerámica azul.' WHERE descripcion = 'Ceramica azul.';
UPDATE items_tienda SET descripcion = 'Cerámica color crema.' WHERE descripcion = 'Ceramica color crema.';
UPDATE items_tienda SET descripcion = 'Cerámica rosada.' WHERE descripcion = 'Ceramica rosada.';
UPDATE items_tienda SET descripcion = 'Marco de madera, lianas, orquídeas y un guacamayo.'
WHERE descripcion = 'Marco de madera, lianas, orquideas y un guacamayo.';

-- Evento de demostracion (Spring lo reconoce por este texto en produccion: DatosDemoGuard acepta los dos)
UPDATE eventos_desastre SET mensaje_notificacion = 'Resguarda las macetas de terraza para evitar exceso de lluvia y daños.'
WHERE mensaje_notificacion = 'Resguarda las macetas de terraza para evitar exceso de lluvia y danos.';
