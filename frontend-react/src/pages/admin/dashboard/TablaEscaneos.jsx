const inicial = (nombre) => nombre.trim()[0]?.toUpperCase() || '?'
const nivel = (c) => (c >= 95 ? ['Alta confianza', ''] : c >= 85 ? ['Media', 'barra-ambar'] : ['Revisar', 'barra-roja'])

// Ultimos escaneos procesados por la IA (ejemplo)
export default function TablaEscaneos({ escaneos }) {
  return (
    <div className="tabla-escaneos">
      <table>
        <thead>
          <tr><th>Usuario</th><th>Especie identificada</th><th>Precisión IA</th><th>Cuándo</th><th>Estado</th></tr>
        </thead>
        <tbody>
          {escaneos.map((e) => {
            const [texto, clase] = nivel(e.confianza)
            return (
              <tr key={e.id}>
                <td><span className="tabla-avatar">{inicial(e.usuario)}</span> {e.usuario}</td>
                <td><b>{e.especie}</b><small>{e.familia}</small></td>
                <td className="tabla-precision">
                  <span><b>{e.confianza.toFixed(1)}%</b> <small>{texto}</small></span>
                  <div className="barra"><div className={`barra-relleno ${clase}`} style={{ width: `${e.confianza}%` }} /></div>
                </td>
                <td>{e.hace}</td>
                <td><span className={`tabla-estado ${e.estado === 'VALIDADO' ? 'tabla-validado' : 'tabla-revision'}`}>{e.estado === 'VALIDADO' ? '✅ Validado' : '⏳ En revisión'}</span></td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
