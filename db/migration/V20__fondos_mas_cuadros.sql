-- Fondos mas calmados: 96 cuadros de 120 ms (ciclo de unos 11 s) en vez de 8 de 250 ms.
-- Las tiras PNG se regeneran con scripts/arte/generar_arte.py.
UPDATE fondos_escena SET total_frames = 96, ms_por_frame = 120
WHERE imagen_url IN ('/files/arte/fondos/dia.png', '/files/arte/fondos/tarde.png', '/files/arte/fondos/noche.png');
