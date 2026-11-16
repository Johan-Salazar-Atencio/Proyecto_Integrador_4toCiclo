import { urlImagen } from '../../../../services/home'
import useRestante, { formatoCuenta } from '../../useRestante'

// Evento climatico activo (GET /eventos/activo; no aparece si no hay evento): su imagen, el recuadro con la
// cuenta regresiva y la descripcion de lo que pasa en el campo peruano
export default function AlertaClima({ alerta }) {
  const grave = alerta.severity === 'HIGH' || alerta.severity === 'CRITICAL'
  const imagen = urlImagen(alerta.imageUrl)
  const restante = useRestante(alerta.endsInSeconds)

  return (
    <section className={`tarjeta alerta-evento aparece ${grave ? 'alerta-grave' : ''}`} style={{ animationDelay: '0.05s' }}>
      {imagen && <img className="alerta-imagen" src={imagen} alt={alerta.eventName || alerta.title} onError={(e) => { e.currentTarget.style.display = 'none' }} />}
      <div className="alerta-cuerpo">
        <div className="alerta-recuadro">
          <span className="alerta-icono">⚠️</span>
          <div className="alerta-texto">
            <h2>{alerta.eventName || alerta.title}</h2>
            {alerta.description && <p>{alerta.description}</p>}
          </div>
          <div className="alerta-tiempo">
            <small>Termina en</small>
            <b>{restante != null ? formatoCuenta(restante) : alerta.timeRemaining}</b>
          </div>
        </div>
        {alerta.info && <p className="alerta-info">{alerta.info}</p>}
      </div>
    </section>
  )
}
