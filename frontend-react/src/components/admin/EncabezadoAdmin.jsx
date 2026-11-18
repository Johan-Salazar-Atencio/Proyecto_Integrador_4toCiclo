// Encabezado de cada seccion del panel: titulo, subtitulo y acciones a la derecha
export default function EncabezadoAdmin({ titulo, subtitulo, children }) {
  return (
    <div className="ad-hd">
      <div>
        <h2>{titulo}</h2>
        {subtitulo && <p>{subtitulo}</p>}
      </div>
      {children && <div className="ad-hd-acciones">{children}</div>}
    </div>
  )
}
