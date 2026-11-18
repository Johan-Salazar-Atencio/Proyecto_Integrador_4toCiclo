import { nombreDe } from './util'

const fecha = (iso) => new Date(iso).toLocaleString('es-PE', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })

function duracion(inicio, fin) {
  const min = Math.round((new Date(fin) - new Date(inicio)) / 60000)
  const h = Math.floor(min / 60)
  const m = min % 60
  return h >= 24 ? `${Math.round(h / 24)} d` : h === 0 ? `${m} min` : `${h} h${m ? ` ${m} min` : ''}`
}

const ESTADOS = {
  ACTIVO: ['En curso', 'var(--evento)'],
  PROGRAMADO: ['Programado', 'var(--agua)'],
  FINALIZADO: ['Finalizado', 'var(--texto-suave)'],
}
const COLUMNAS = 'minmax(0, 1.1fr) 84px 64px 92px auto'

// Historial de eventos lanzados desde el panel
export default function HistorialEventos({ eventos, tipos, onVerEfecto }) {
  return (
    <section className="ad-card ad-c ev-historial">
      <h3>Historial de desastres ejecutados</h3>
      {eventos.length === 0 ? (
        <p className="historial-vacio">Aún no se ha lanzado ningún evento.</p>
      ) : (
        <div className="ev-historial-lista">
          <div className="ad-th" style={{ '--cols': COLUMNAS }}><span>Evento</span><span>Inicio</span><span>Duración</span><span>Estado</span><span /></div>
          {eventos.map((e) => {
            const [texto, color] = ESTADOS[e.estado] || [e.estado, 'var(--texto-suave)']
            return (
              <div key={e.id_desastre} className="ad-tr" style={{ '--cols': COLUMNAS }}>
                <div>
                  <b>{nombreDe(tipos, e.tipo_evento)}</b>
                  <small>#EVT-{e.id_desastre} · {e.region} · {e.severidad}</small>
                </div>
                <small>{fecha(e.fecha_inicio)}</small>
                <small>{duracion(e.fecha_inicio, e.fecha_fin)}</small>
                <span className="ad-chip" style={{ '--c': color }}>{texto}</span>
                {e.estado === 'FINALIZADO' && !e.simulado
                  ? <button type="button" className="ad-btn ad-btn-g ad-btn-sm" onClick={() => onVerEfecto(e)}>Ver efecto</button>
                  : <span />}
              </div>
            )
          })}
        </div>
      )}
    </section>
  )
}
