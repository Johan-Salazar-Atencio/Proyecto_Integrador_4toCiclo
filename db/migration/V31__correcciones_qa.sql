-- V31: correcciones del QA del 22/11 (aditiva; no cambia migraciones ya aplicadas).
--
-- QA-12: la apariencia que dio la IA (forma y colores) se guarda con el escaneo. Al digitalizar, Spring la toma
--        de aqui y ya no del cuerpo que manda el cliente (antes el cliente elegia la forma y con ello los dias
--        de crecimiento, y podia guardar megas por planta). Los escaneos viejos quedan en NULL (dibujo por defecto).
ALTER TABLE escaneos_historial ADD COLUMN IF NOT EXISTS apariencia TEXT;

-- QA-19: el panel de administracion (Django) tiene su propia sesion. Antes guardaba su token en
--        usuarios.session_token, el mismo que la app: entrar en uno cerraba la sesion del otro.
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS admin_session_token VARCHAR(64);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS admin_session_expira TIMESTAMP;

-- QA-21: borrar un articulo, una mision o un evento ya no se lleva el historial de los usuarios (compras,
--        misiones cumplidas, desastres afrontados que cuentan para logros). El admin los desactiva o cancela.
ALTER TABLE inventario_usuario DROP CONSTRAINT IF EXISTS inventario_usuario_id_item_fkey;
ALTER TABLE inventario_usuario ADD CONSTRAINT inventario_usuario_id_item_fkey
    FOREIGN KEY (id_item) REFERENCES items_tienda(id_item) ON DELETE RESTRICT;

ALTER TABLE misiones_usuario DROP CONSTRAINT IF EXISTS misiones_usuario_id_mision_fkey;
ALTER TABLE misiones_usuario ADD CONSTRAINT misiones_usuario_id_mision_fkey
    FOREIGN KEY (id_mision) REFERENCES misiones(id_mision) ON DELETE RESTRICT;

ALTER TABLE registros_desastre_usuario DROP CONSTRAINT IF EXISTS registros_desastre_usuario_id_desastre_fkey;
ALTER TABLE registros_desastre_usuario ADD CONSTRAINT registros_desastre_usuario_id_desastre_fkey
    FOREIGN KEY (id_desastre) REFERENCES eventos_desastre(id_desastre) ON DELETE RESTRICT;
