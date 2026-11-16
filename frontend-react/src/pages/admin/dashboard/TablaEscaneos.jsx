const inicial = (nombre) => nombre.trim()[0]?.toUpperCase() || '?'
const hace = (iso) => {
  const min = Math.max(0, Math.round((Date.now() - new Date(iso)) / 60000))
  if (min < 1) return 'ahora'
  if (min < 60) return `hace ${min} min`
  if (min < 1440) return `hace ${Math.round(min / 60)} h`
  return `hace ${Math.round(min / 1440)} d`
}
const nivel = (c) => (c == null ? ['Sin dato', 'barra-roja'] : c >= 95 ? ['Alta confianza', ''] : c >= 85 ? ['Media', 'barra-ambar'] : ['Revisar', 'barra-roja'])

// Ultimos escaneos procesados por la IA (datos reales)
export default function TablaEscaneos({ escaneos }) {
  return (
    <div className="tabla-escaneos">
      <table>
        <thead>
          <tr><th>Usuario</th><th>Especie identificada</th><th>Precisión IA</th><th>Cuándo</th><th>Estado</th></tr>
        </thead>
        <tbody>
          {escaneos.length === 0 && <tr><td colSpan={5}>Todavía no hay escaneos: aparecerán aquí cuando los usuarios escaneen plantas.</td></tr>}
          {escaneos.map((e) => {
            const [texto, clase] = nivel(e.confianza)
            return (
              <tr key={e.id}>
                <td><span className="tabla-avatar">{inicial(e.usuario)}</span> {e.usuario}</td>
                <td><b>{e.especie}</b><small>{e.cientifico}</small></td>
                <td className="tabla-precision">
                  <span><b>{e.confianza == null ? '—' : `${e.confianza.toFixed(1)}%`}</b> <small>{texto}</small></span>
                  <div className="barra"><div className={`barra-relleno ${clase}`} style={{ width: `${e.confianza ?? 0}%` }} /></div>
                </td>
                <td>{hace(e.fecha)}</td>
                <td><span className={`tabla-estado ${(e.confianza ?? 0) >= 85 ? 'tabla-validado' : 'tabla-revision'}`}>{(e.confianza ?? 0) >= 85 ? '✅ Validado' : '⏳ En revisión'}</span></td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
