-- V28: ESCANEO_NUEVA (20 hojas + 70 XP) no tenia tope diario. Aunque ya no se aceptan especies
-- genericas, se limita a 5 especies nuevas pagadas por dia para que no se pueda farmear.
-- Aditiva: no borra datos.
UPDATE reglas_progreso SET tope_diario = 5 WHERE motivo = 'ESCANEO_NUEVA' AND tope_diario IS NULL;
