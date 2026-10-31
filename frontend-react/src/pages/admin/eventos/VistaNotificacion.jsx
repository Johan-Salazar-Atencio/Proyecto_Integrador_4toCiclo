// Como vera el usuario la notificacion push (se actualiza mientras escribes)
export default function VistaNotificacion({ tipo, mensaje, region }) {
  return (
    <aside className="vista">
      <p className="vista-titulo">📱 Vista previa en el teléfono</p>
      <div className="vista-telefono">
        <div className="vista-notificacion" key={tipo.valor}>
          <div className="vista-cabecera">
            <span>🌱 PLANTIA · URGENTE</span>
            <small>Ahora</small>
          </div>
          <b>{tipo.icono} Alerta: {tipo.texto}</b>
          <p>{mensaje.trim() || 'Escribe el mensaje para verlo aquí…'}</p>
          <small>Se enviará a los usuarios de: {region === 'GLOBAL' ? 'todas las regiones' : region}</small>
        </div>
      </div>
    </aside>
  )
}
