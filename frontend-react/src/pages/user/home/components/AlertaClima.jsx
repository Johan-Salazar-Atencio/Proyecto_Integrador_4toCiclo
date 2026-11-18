import { urlImagen } from '../../../../services/home'
import useRestante, { formatoCuenta } from '../../useRestante'

// Evento activo (GET /eventos/activo; si no hay, Inicio muestra SinEvento): su imagen, etiqueta, nombre, descripcion,
// recuadro "Termina en" con la cuenta regresiva y la informacion de lo que pasa en el campo peruano
export default function AlertaClima({ alerta }) {
  const grave = alerta.severity === 'HIGH' || alerta.severity === 'CRITICAL'
  const imagen = urlImagen(alerta.imageUrl)
  const restante = useRestante(alerta.endsInSeconds)

  return (
    <section className={`tarjeta ev aparece ${grave ? 'ev-grave' : ''} ${imagen ? '' : 'ev-sin-imagen'}`} style={{ animationDelay: '0.05s' }}>
      {imagen && <img className="ev-imagen" src={imagen} alt={alerta.eventName || alerta.title} onError={(e) => { e.currentTarget.style.display = 'none' }} />}
      <div className="ev-cuerpo">
        <span className="ev-etiqueta">Desastre · Evento activo</span>
        <h2>{alerta.eventName || alerta.title}</h2>
        {alerta.description && <p>{alerta.description}</p>}
        {alerta.info && <p className="ev-info">{alerta.info}</p>}
        <div className="ev-cuenta">
          <small>Termina en</small>
          <b>{restante != null ? formatoCuenta(restante) : alerta.timeRemaining}</b>
        </div>
      </div>
    </section>
  )
}
