import { useCallback, useEffect, useMemo, useState } from 'react'
import { guardarEspecie, listarEspecies } from '../../../services/admin/catalogo'
import PanelEspecie from './PanelEspecie'
import { Aviso, BotonIcono, Chip, Icono, Interruptor, PENDIENTE } from '../misiones/kit'
import './catalogo-admin.css'

const FILTROS = [
  ['todas', 'Todas'],
  ['publicadas', 'Publicadas'],
  ['ocultas', 'No publicadas'],
  ['toxicas', 'Tóxicas'],
]
const COLUMNAS = '1.35fr 90px 44px auto'

export default function CatalogoAdmin() {
  const [estado, setEstado] = useState({ cargando: true, especies: [], error: '' })
  const [texto, setTexto] = useState('')
  const [filtro, setFiltro] = useState('todas')
  const [seleccionada, setSeleccionada] = useState(null)
  const [error, setError] = useState('')

  const cargar = useCallback(async () => {
    try {
      const { especies } = await listarEspecies()
      setEstado({ cargando: false, especies, error: '' })
    } catch {
      setEstado({ cargando: false, especies: [], error: 'No se pudo leer el catálogo.' })
    }
  }, [])

  useEffect(() => { cargar() }, [cargar])

  const visibles = useMemo(() => {
    const t = texto.trim().toLowerCase()
    return estado.especies.filter((e) =>
      (!t || e.nombreComun.toLowerCase().includes(t) || e.nombreCientifico.toLowerCase().includes(t)) &&
      (filtro === 'todas' || (filtro === 'toxicas' && e.toxica) || (filtro === 'ocultas' && !e.publicada) || (filtro === 'publicadas' && e.publicada)))
  }, [estado.especies, texto, filtro])

  const publicar = async (especie) => {
    setError('')
    try {
      await guardarEspecie(especie.id, { publicada: !especie.publicada })
      await cargar()
    } catch (err) {
      setError(err.message || 'No se pudo cambiar el estado.')
    }
  }

  const { cargando, especies, error: errorCarga } = estado
  const elegida = especies.find((e) => e.id === seleccionada)

  if (cargando) return <div className="ak-cargando"><span className="home-giro" aria-label="Cargando" /></div>

  const publicadas = especies.filter((e) => e.publicada).length

  return (
    <div className="ak-pagina">
      <div className="ak-cab">
        <p>Edita, publica u oculta las especies que identifica la IA.</p>
        <button className="ak-btn" disabled title={PENDIENTE}><Icono nombre="plus" tam={18} />Nueva especie</button>
      </div>
      <Aviso error={errorCarga} onReintentar={cargar} />
      <Aviso error={error} />

      <div className="ak-cnt">
        <div><small>Total especies</small><b>{especies.length}</b></div>
        <div><small>Publicadas</small><b style={{ color: 'var(--ok)' }}>{publicadas}</b></div>
        <div><small>No publicadas</small><b>{especies.length - publicadas}</b></div>
        <div><small>Tóxicas</small><b style={{ color: 'var(--peligro)' }}>{especies.filter((e) => e.toxica).length}</b></div>
      </div>

      <div className={`ak-cols ${elegida ? 'ak-con-panel' : ''}`}>
        <section className="ak-card">
          <label className="ak-cat-busca">
            <Icono nombre="search" />
            <input type="search" placeholder="Buscar por nombre común o científico…" value={texto} onChange={(e) => setTexto(e.target.value)} aria-label="Buscar especie" />
          </label>
          <div className="ak-chips" role="group" aria-label="Filtrar especies">
            {FILTROS.map(([valor, etiqueta]) => (
              <button key={valor} className={filtro === valor ? 'on' : ''} aria-pressed={filtro === valor} onClick={() => setFiltro(valor)}>{etiqueta}</button>
            ))}
          </div>

          <div className="ak-lista">
            <div className="ak-th" style={{ '--cols': COLUMNAS, '--ancho': '440px' }}>
              <span>Especie</span><span>Cuidado</span><span>Pub.</span><span style={{ width: 76 }} />
            </div>
            {visibles.length === 0 ? <p className="ak-vacio">No hay especies con ese filtro.</p> : visibles.map((e) => (
              <div key={e.id} className={`ak-tr ${e.id === seleccionada ? 'ak-tr-sel' : ''} ${e.publicada ? '' : 'ak-apagada'}`} style={{ '--cols': COLUMNAS, '--ancho': '440px' }}>
                <div className="ak-cat-nombre">
                  <b>{e.nombreComun}</b> {e.toxica && <Chip color="var(--peligro)">⚠ Tóxica</Chip>}
                  <small><i>{e.nombreCientifico}</i></small>
                </div>
                <small>Riego {e.riegoDias} d</small>
                <Interruptor activo={e.publicada} onChange={() => publicar(e)} etiqueta={`${e.nombreComun}: publicada`} />
                <span className="ak-ra">
                  <BotonIcono icono="edit" etiqueta={`Editar ${e.nombreComun}`} onClick={() => setSeleccionada(e.id)} />
                </span>
              </div>
            ))}
          </div>
        </section>

        {elegida && (
          <div className="ak-cat-panel-wrap">
            <PanelEspecie key={elegida.id} especie={elegida} onCerrar={() => setSeleccionada(null)} onCambio={cargar} />
          </div>
        )}
      </div>
    </div>
  )
}
