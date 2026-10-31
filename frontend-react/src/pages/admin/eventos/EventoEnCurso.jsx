import { useEffect, useState } from 'react'
import { etiquetaTipo, iconoTipo } from '../../../services/admin/eventos'

const dos = (n) => String(n).padStart(2, '0')

// Cuenta regresiva hasta `fin` (se actualiza cada segundo)
function useRestante(fin) {
  const [ahora, setAhora] = useState(Date.now())
  useEffect(() => {
    const id = setInterval(() => setAhora(Date.now()), 1000)
    return () => clearInterval(id)
  }, [])
  const ms = Math.max(0, new Date(fin).getTime() - ahora)
  const total = Math.floor(ms / 1000)
  return { vencido: ms === 0, h: Math.floor(total / 3600), m: Math.floor((total % 3600) / 60), s: total % 60 }
}

// Tarjeta del evento activo con cuenta regresiva y boton para cancelarlo
export default function EventoEnCurso({ evento, onCancelar, cancelando }) {
  const { vencido, h, m, s } = useRestante(evento.fecha_fin)

  return (
    <section className={`tarjeta en-curso aparece ${evento.severidad === 'CRITICO' ? 'en-curso-critico' : ''}`}>
      <div className="en-curso-info">
        <div className="en-curso-etiquetas">
          <span className="en-curso-vivo"><i /> Evento en curso</span>
          <span className="en-curso-sev">{evento.severidad}</span>
          {evento.simulado && <span className="dashboard-ejemplo">simulado</span>}
        </div>
        <h2>{iconoTipo(evento.tipo_evento)} {etiquetaTipo(evento.tipo_evento)} · {evento.region}</h2>
        <p>{evento.mensaje_notificacion}</p>
      </div>

      <div className="en-curso-lado">
        <div className="en-curso-reloj" aria-label="Tiempo restante">
          <small>{vencido ? 'VENTANA VENCIDA' : 'TIEMPO RESTANTE'}</small>
          <b>{dos(h)}<i>:</i>{dos(m)}<i>:</i>{dos(s)}</b>
        </div>
        <button className="en-curso-cancelar" onClick={onCancelar} disabled={cancelando}>
          {cancelando ? 'Cancelando…' : '✖ Cancelar emergencia'}
        </button>
      </div>
    </section>
  )
}
