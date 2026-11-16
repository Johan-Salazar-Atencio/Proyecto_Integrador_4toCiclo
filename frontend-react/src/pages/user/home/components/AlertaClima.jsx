import { useEffect, useState } from 'react'
import { urlImagen } from '../../../../services/home'

const dos = (n) => String(n).padStart(2, '0')
const formato = (s) => `${dos(Math.floor(s / 3600))}:${dos(Math.floor(s / 60) % 60)}:${dos(s % 60)}`

// Cuenta regresiva en vivo a partir de los segundos que manda el servidor (null si no manda)
function useRestante(segundos) {
  const [fin] = useState(() => (segundos != null ? Date.now() + segundos * 1000 : null))
  const [ahora, setAhora] = useState(Date.now())
  useEffect(() => {
    if (fin == null) return undefined
    const id = setInterval(() => setAhora(Date.now()), 1000)
    return () => clearInterval(id)
  }, [fin])
  return fin == null ? null : Math.max(0, Math.round((fin - ahora) / 1000))
}

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
            <b>{restante != null ? formato(restante) : alerta.timeRemaining}</b>
          </div>
        </div>
        {alerta.info && <p className="alerta-info">{alerta.info}</p>}
      </div>
    </section>
  )
}
