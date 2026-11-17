const hace = (iso) => {
  const min = Math.max(0, Math.round((Date.now() - new Date(iso)) / 60000))
  if (min < 1) return 'ahora'
  if (min < 60) return `hace ${min} min`
  if (min < 1440) return `hace ${Math.round(min / 60)} h`
  return `hace ${Math.round(min / 1440)} d`
}
const nivel = (c) => (c == null ? ['Sin dato', 'var(--texto-suave)'] : c >= 95 ? ['Alta confianza', 'var(--ok)'] : c >= 85 ? ['Media', 'var(--luz)'] : ['Revisar', 'var(--peligro)'])

const COLUMNAS = '78px minmax(0, 1fr) 118px 112px'

// Ultimos escaneos procesados por la IA (datos reales), como filas con barra de precision y chip de estado
export default function TablaEscaneos({ escaneos }) {
  if (escaneos.length === 0) {
    return <p className="historial-vacio">Todavía no hay escaneos: aparecerán aquí cuando los usuarios escaneen plantas.</p>
  }

  return (
    <div className="dash-tabla" role="table" aria-label="Últimos escaneos">
      <div className="ad-th" role="row" style={{ '--cols': COLUMNAS }}>
        <span>Cuándo</span><span>Especie</span><span>Precisión IA</span><span>Estado</span>
      </div>
      {escaneos.map((e) => {
        const [texto, color] = nivel(e.confianza)
        return (
          <div key={e.id} className="ad-tr" role="row" style={{ '--cols': COLUMNAS }}>
            <small>{hace(e.fecha)}</small>
            <div className="dash-especie">
              <b>{e.especie}</b>
              <small>{e.usuario}{e.cientifico ? ` · ${e.cientifico}` : ''}</small>
            </div>
            <div className="dash-precision">
              <div className="ad-bar" style={{ '--c': color }}><i style={{ width: `${e.confianza ?? 0}%` }} /></div>
              <small>{e.confianza == null ? '—' : `${Math.round(e.confianza)}%`}</small>
            </div>
            <span className="ad-chip" style={{ '--c': color }}>{texto}</span>
          </div>
        )
      })}
    </div>
  )
}
