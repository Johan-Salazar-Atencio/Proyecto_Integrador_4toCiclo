import { etiquetaTipo, iconoTipo } from '../../../services/admin/eventos'

const fecha = (iso) => new Date(iso).toLocaleString('es-PE', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })

function duracion(inicio, fin) {
  const min = Math.round((new Date(fin) - new Date(inicio)) / 60000)
  const h = Math.floor(min / 60)
  const m = min % 60
  return h >= 24 ? `${Math.round(h / 24)} d` : `${h} h${m ? ` ${m} min` : ''}`
}

const ESTADOS = { ACTIVO: ['En curso', 'est-activo'], PROGRAMADO: ['Programado', 'est-programado'], FINALIZADO: ['Finalizado', 'est-final'] }

// Historial de eventos (Django, solo lectura) mas los simulados de esta sesion
export default function HistorialEventos({ eventos }) {
  if (eventos.length === 0) return <p className="historial-vacio">Aún no se ha lanzado ningún evento.</p>

  return (
    <div className="tabla-escaneos">
      <table>
        <thead>
          <tr><th>Evento</th><th>Severidad</th><th>Inicio</th><th>Duración</th><th>Estado</th></tr>
        </thead>
        <tbody>
          {eventos.map((e) => {
            const [texto, clase] = ESTADOS[e.estado] || [e.estado, '']
            return (
              <tr key={e.id_desastre}>
                <td>
                  <b>{iconoTipo(e.tipo_evento)} {etiquetaTipo(e.tipo_evento)}</b>
                  <small>#EVT-{e.id_desastre} · {e.region}{e.simulado ? ' · simulado' : ''}</small>
                </td>
                <td><span className={`sev-chip sev-chip-${e.severidad.toLowerCase()}`}>{e.severidad}</span></td>
                <td>{fecha(e.fecha_inicio)}</td>
                <td>{duracion(e.fecha_inicio, e.fecha_fin)}</td>
                <td><span className={`tabla-estado ${clase}`}>{texto}</span></td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
