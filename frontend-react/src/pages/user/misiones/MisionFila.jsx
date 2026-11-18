import { Link } from 'react-router-dom'
import { RUTAS } from '../../../routes/rutas'
import Icono from '../../../components/Icono'

// A donde lleva una mision: el escaner o las plantas virtuales (donde se cuida y se previene)
const IR_A_ESCANEAR = ['ESCANEO', 'DIGITALIZAR', 'COLECCION']
export const destinoMision = (m) => (m.kind === 'EVENT' || !IR_A_ESCANEAR.includes(m.type) ? RUTAS.COLECCION : RUTAS.ESCANEAR)

// Fila de una mision (Inicio, ventana de misiones y pagina /misiones): icono, titulo con barra de avance y contador.
// Completa: check, titulo tachado y la recompensa debajo. La del evento lleva la etiqueta EVENTO.
export default function MisionFila({ mision, enlace = true, onIr }) {
  const evento = mision.kind === 'EVENT'
  const hecha = Boolean(mision.isCompleted)
  const porcentaje = Math.round(Math.min(1, Math.max(0, mision.progress || 0)) * 100)
  const sub = hecha || !porcentaje
    ? (evento && !hecha ? 'Entra a cada planta virtual y toca «Prevenir evento»' : mision.reward ? `Recompensa: ${mision.reward}` : '')
    : ''

  return (
    <div className={`mi ${hecha ? 'mi-hecha' : ''} ${evento ? 'mi-evento' : ''}`}>
      <span className="mi-icono"><Icono nombre={hecha ? 'check' : evento ? 'alert' : 'target'} /></span>
      <div className="mi-texto">
        {evento && <span className="mi-etiqueta">EVENTO</span>}
        <b>{mision.title}</b>
        {sub ? (
          <small>{sub}</small>
        ) : !hecha ? (
          <div className="barra" role="progressbar" aria-valuenow={porcentaje} aria-valuemin={0} aria-valuemax={100}>
            <div className="barra-relleno" style={{ width: `${porcentaje}%` }} />
          </div>
        ) : null}
      </div>
      <span className="mi-contador">{mision.currentCount}/{mision.targetCount}</span>
      {enlace && !hecha && <Link to={destinoMision(mision)} className="mi-ir" onClick={onIr} aria-label={`Ir a: ${mision.title}`}>Ir</Link>}
    </div>
  )
}
