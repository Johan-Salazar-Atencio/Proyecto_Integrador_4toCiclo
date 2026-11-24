import { useCallback, useEffect, useMemo, useState } from 'react'
import { guardarEspecie, listarEspecies } from '../../../services/admin/catalogo'
import PanelEspecie from './PanelEspecie'
import PlantaPixel from '../../../components/PlantaPixel'
import { aparienciaDeEspecie } from '../../../services/admin/catalogo'
import { Aviso, BotonIcono, Chip, Icono, Interruptor } from '../misiones/kit'
import './catalogo-admin.css'

const FILTROS = [
  ['todas', 'Todas'],
  ['publicadas', 'Publicadas'],
  ['ocultas', 'No publicadas'],
  ['toxicas', 'Tóxicas'],
]
const COLUMNAS = '48px minmax(0, 1.35fr) 90px 80px 44px 36px'
const NUEVA = 'nueva'

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
  const creando = seleccionada === NUEVA
  const elegida = especies.find((e) => e.id === seleccionada)

  if (cargando) return <div className="ak-cargando"><span className="home-giro" aria-label="Cargando" /></div>

  const publicadas = especies.filter((e) => e.publicada).length

  return (
    <div className="ak-pagina">
      <div className="ak-cab">
        <div><h2 className="ak-h2">Catálogo y moderación</h2><p>Cada vez que un usuario escanea una planta, la IA la identifica y la guarda aquí. Desde esta pantalla moderas ese catálogo: corriges sus datos, ocultas las que no deben salir en los escaneos o agregas una especie a mano.</p></div>
        <button className="ak-btn" onClick={() => setSeleccionada(NUEVA)}><Icono nombre="plus" tam={18} />Nueva especie</button>
      </div>
      <Aviso error={errorCarga} onReintentar={cargar} />
      <Aviso error={error} />

      <div className="ak-cnt">
        <div><small>Total especies</small><b>{especies.length}</b></div>
        <div><small>Publicadas</small><b style={{ color: 'var(--ok)' }}>{publicadas}</b></div>
        <div><small>No publicadas</small><b>{especies.length - publicadas}</b></div>
        <div><small>Escaneos de la IA</small><b>{especies.reduce((t, e) => t + e.escaneos, 0)}</b></div>
        <div><small>Tóxicas</small><b style={{ color: 'var(--peligro)' }}>{especies.filter((e) => e.toxica).length}</b></div>
      </div>

      <div className={`ak-cols ${elegida || creando ? 'ak-con-panel' : ''}`}>
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
            <div className="ak-th" style={{ '--cols': COLUMNAS, '--ancho': '560px' }}>
              <span /><span>Especie</span><span>Cuidado</span><span>Escaneos</span><span>Pub.</span><span />
            </div>
            {visibles.length === 0 ? <p className="ak-vacio">{especies.length === 0 ? 'Todavía no hay especies: aparecerán cuando un usuario escanee una planta, o agrega una con «Nueva especie».' : 'No hay especies con ese filtro.'}</p> : visibles.map((e) => (
              <div key={e.id} className={`ak-tr ${e.id === seleccionada ? 'ak-tr-sel' : ''} ${e.publicada ? '' : 'ak-apagada'}`} style={{ '--cols': COLUMNAS, '--ancho': '560px' }}>
                <span className="ak-cat-mini"><PlantaPixel etapa={2} apariencia={aparienciaDeEspecie(e)} titulo={e.nombreComun} animar={false} sinSuelo /></span>
                <div className="ak-cat-nombre">
                  <b>{e.nombreComun}</b> {e.toxica && <Chip color="var(--peligro)">⚠ Tóxica</Chip>}
                  <small><i>{e.nombreCientifico}</i></small>
                </div>
                <small>Riego {e.riegoDias} d</small>
                <small title="Veces que la IA identificó esta especie"><b>{e.escaneos}</b> {e.escaneos === 1 ? 'escaneo' : 'escaneos'}</small>
                <Interruptor activo={e.publicada} onChange={() => publicar(e)} etiqueta={`${e.nombreComun}: publicada`} />
                <span className="ak-ra">
                  <BotonIcono icono="edit" etiqueta={`Editar ${e.nombreComun}`} onClick={() => setSeleccionada(e.id)} />
                </span>
              </div>
            ))}
          </div>
        </section>

        {creando && (
          <div className="ak-cat-panel-wrap">
            <PanelEspecie key="nueva" especie={null} onCerrar={() => setSeleccionada(null)} onCreada={async (id) => { await cargar(); setSeleccionada(id) }} />
          </div>
        )}
        {elegida && (
          <div className="ak-cat-panel-wrap">
            <PanelEspecie key={elegida.id} especie={elegida} onCerrar={() => setSeleccionada(null)} onCambio={cargar} />
          </div>
        )}
      </div>
    </div>
  )
}
