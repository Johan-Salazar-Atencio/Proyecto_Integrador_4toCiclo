import { useEffect, useState } from 'react'
import IconoAdmin from '../../../components/admin/IconoAdmin'
import { SEV, imagenDe, nombreDe } from './util'

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

// Evento activo con cuenta regresiva y boton para cancelarlo; sin evento, aviso de que no hay ninguno
export default function EventoEnCurso({ evento, tipos, cancelando, onCancelar }) {
  if (!evento) {
    return (
      <section className="ad-card ad-c">
        <h3>Evento en curso</h3>
        <div className="ad-vacio">
          <IconoAdmin n="check" />
          <b>No hay ningún evento activo</b>
          <small className="ad-small">Elige un tipo de evento y presiona Lanzar.</small>
        </div>
      </section>
    )
  }
  return <Activo evento={evento} tipos={tipos} cancelando={cancelando} onCancelar={onCancelar} />
}

function Activo({ evento, tipos, cancelando, onCancelar }) {
  const { vencido, h, m, s } = useRestante(evento.fecha_fin)
  const tipo = tipos.find((t) => t.codigo === evento.tipo_evento)
  const foto = imagenDe(tipo)
  const sev = SEV[evento.severidad] || { texto: evento.severidad, color: 'var(--texto-suave)' }

  return (
    <section className="ad-card ad-c ev-encurso">
      <div className="ev-tipo-fila">
        <h3>Evento en curso</h3>
        <span className="ad-chip" style={{ '--c': sev.color }}>{sev.texto.toUpperCase()}</span>
      </div>
      <div className="ev-encurso-info">
        {foto ? <img src={foto} alt={nombreDe(tipos, evento.tipo_evento)} /> : <div className="ev-sinimg"><IconoAdmin n="img" /></div>}
        <div>
          <b className="ev-encurso-nombre">{nombreDe(tipos, evento.tipo_evento)}</b>
          <small className="ad-small">{evento.region}{evento.simulado ? ' · simulado' : ''} · #EVT-{evento.id_desastre}</small>
          <small className="ad-small ev-reloj-t">{vencido ? 'VENTANA VENCIDA' : 'TIEMPO RESTANTE'}</small>
          <div className="ev-reloj" aria-label="Tiempo restante">{dos(h)}:{dos(m)}:{dos(s)}</div>
        </div>
      </div>
      {evento.mensaje_notificacion && <p className="ev-mensaje">{evento.mensaje_notificacion}</p>}
      <button type="button" className="ad-btn ad-btn-d" onClick={onCancelar} disabled={cancelando}>{cancelando ? 'Cancelando…' : 'Cancelar emergencia'}</button>
    </section>
  )
}
