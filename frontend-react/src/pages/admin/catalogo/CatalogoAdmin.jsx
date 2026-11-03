import { useCallback, useEffect, useMemo, useState } from 'react'
import { listarEspecies } from '../../../services/admin/catalogo'
import PanelEspecie from './PanelEspecie'
import './catalogo-admin.css'

const FILTROS = [
  ['todas', 'Todas'],
  ['toxicas', 'Tóxicas'],
  ['ocultas', 'No publicadas'],
]

export default function CatalogoAdmin() {
  const [estado, setEstado] = useState({ cargando: true, especies: [], origen: 'django', error: '' })
  const [texto, setTexto] = useState('')
  const [filtro, setFiltro] = useState('todas')
  const [seleccionada, setSeleccionada] = useState(null)

  const cargar = useCallback(async () => {
    try {
      const { especies, origen } = await listarEspecies()
      setEstado({ cargando: false, especies, origen, error: '' })
    } catch {
      setEstado({ cargando: false, especies: [], origen: 'django', error: 'No se pudo leer el catálogo.' })
    }
  }, [])

  useEffect(() => { cargar() }, [cargar])

  const visibles = useMemo(() => {
    const t = texto.trim().toLowerCase()
    return estado.especies.filter((e) =>
      (!t || e.nombreComun.toLowerCase().includes(t) || e.nombreCientifico.toLowerCase().includes(t)) &&
      (filtro === 'todas' || (filtro === 'toxicas' && e.toxica) || (filtro === 'ocultas' && !e.publicada)))
  }, [estado.especies, texto, filtro])

  const { cargando, especies, origen, error } = estado
  const elegida = especies.find((e) => e.id === seleccionada)

  if (cargando) return <div className="home-cargando"><span className="home-giro" aria-label="Cargando" /></div>

  return (
    <div className="cat-adm">
      {error && (
        <div className="home-error" role="alert"><span>{error}</span><button onClick={cargar}>Reintentar</button></div>
      )}

      <section className="cat-resumen aparece">
        <span className="cat-dato">Total especies <b>{especies.length}</b></span>
        <span className="cat-dato">Publicadas <b>{especies.filter((e) => e.publicada).length}</b></span>
        <span className="cat-dato">Tóxicas <b>{especies.filter((e) => e.toxica).length}</b></span>
        {origen === 'spring' && <span className="dashboard-ejemplo" title="Django /api/catalog/ responde 500">leído de Spring</span>}
      </section>

      <div className={`cat-columnas ${elegida ? 'cat-con-panel' : ''}`}>
        <section className="tarjeta aparece" style={{ animationDelay: '0.06s' }}>
          <div className="cat-filtros">
            <input type="search" placeholder="🔍 Buscar por nombre común o científico…" value={texto} onChange={(e) => setTexto(e.target.value)} />
            <div className="cat-chips">
              {FILTROS.map(([valor, etiqueta]) => (
                <button key={valor} className={filtro === valor ? 'activa' : ''} aria-pressed={filtro === valor} onClick={() => setFiltro(valor)}>{etiqueta}</button>
              ))}
            </div>
          </div>

          {visibles.length === 0 ? (
            <p className="historial-vacio">No hay especies con ese filtro.</p>
          ) : (
            <div className="tabla-escaneos">
              <table className="cat-tabla">
                <thead><tr><th>Especie</th><th>Taxonomía</th><th>Cuidado</th><th>Estado</th></tr></thead>
                <tbody>
                  {visibles.map((e) => (
                    <tr key={e.id} className={e.id === seleccionada ? 'cat-fila-activa' : ''} onClick={() => setSeleccionada(e.id)} tabIndex={0}
                      onKeyDown={(ev) => ev.key === 'Enter' && setSeleccionada(e.id)}>
                      <td><b>{e.nombreComun}</b><small>#ESP-{String(e.id).padStart(4, '0')}</small></td>
                      <td className="cat-cientifico">{e.nombreCientifico}</td>
                      <td>💧 Cada {e.riegoDias} d · ☀️ {e.solHoras} h</td>
                      <td>
                        <span className={`tabla-estado ${e.publicada ? 'tabla-validado' : 'est-final'}`}>{e.publicada ? 'Publicada' : 'Oculta'}</span>
                        {e.toxica && <span className="tabla-estado tabla-revision cat-toxica">⚠️ Tóxica</span>}
                        {e.editada && <span className="dashboard-ejemplo">editada</span>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {elegida && <PanelEspecie key={elegida.id} especie={elegida} onCerrar={() => setSeleccionada(null)} onCambio={cargar} />}
      </div>
    </div>
  )
}
