import { useCallback, useEffect, useState } from 'react'
import {
  cambiarEstadoMision, crearMision, editarMision, listarMisiones, textoTipoMision,
} from '../../../services/admin/misiones'
import FormularioMision from './FormularioMision'
import { Aviso, BotonIcono, Chip, Icono, Interruptor, PENDIENTE } from './kit'
import './misiones.css'

const COLOR_TIPO = {
  RIEGO: 'var(--agua)', ESCANEO: 'var(--primario)', DIGITALIZAR: 'var(--primario)', REFUGIO: 'var(--evento)',
  SOL: 'var(--luz)', ABONO: 'var(--abono)', CUIDADO: 'var(--primario)', COLECCION: 'var(--abono)',
}
const COLUMNAS = '1.5fr 120px 50px 60px 56px 44px auto'

function FilaMision({ m, seleccionada, onEditar, onAlternar }) {
  return (
    <div className={`ak-tr ${seleccionada ? 'ak-tr-sel' : ''} ${m.activa ? '' : 'ak-apagada'}`} style={{ '--cols': COLUMNAS, '--ancho': '640px' }}>
      <div>
        <b>{m.titulo}</b>
        {m.descripcion && <small>{m.descripcion}</small>}
      </div>
      <Chip color={COLOR_TIPO[m.tipo]}>{textoTipoMision(m.tipo)}</Chip>
      <span>{m.meta}</span>
      <span className="ak-hojas"><Icono nombre="leaf" tam={15} />{m.recompensa_monedas}</span>
      <span>{m.recompensa_xp} XP</span>
      <Interruptor activo={m.activa} onChange={() => onAlternar(m)} etiqueta={`${m.titulo}: activa`} />
      <span className="ak-ra">
        <BotonIcono icono="edit" etiqueta={`Editar ${m.titulo}`} onClick={() => onEditar(m)} />
        <BotonIcono icono="trash" peligro etiqueta={`Eliminar ${m.titulo}`} disabled titulo={PENDIENTE} />
      </span>
    </div>
  )
}

function Cabecera() {
  return (
    <div className="ak-th" style={{ '--cols': COLUMNAS, '--ancho': '640px' }}>
      <span>Misión</span><span>Tipo</span><span>Meta</span><span>Hojas</span><span>XP</span><span>Activa</span><span style={{ width: 76 }} />
    </div>
  )
}

export default function Misiones() {
  const [estado, setEstado] = useState({ cargando: true, misiones: [], error: '' })
  const [panel, setPanel] = useState(null) // null = cerrado, {} = nueva, mision = editar
  const [error, setError] = useState('')

  const cargar = useCallback(async () => {
    try {
      setEstado({ cargando: false, misiones: await listarMisiones(), error: '' })
    } catch (err) {
      setEstado({ cargando: false, misiones: [], error: err.message || 'No se pudo leer Django.' })
    }
  }, [])

  useEffect(() => { cargar() }, [cargar])

  const guardar = async (datos) => {
    if (panel?.id_mision) await editarMision(panel.id_mision, datos)
    else await crearMision(datos)
    setPanel(null)
    await cargar()
  }

  const alternar = async (mision) => {
    setError('')
    try {
      await cambiarEstadoMision(mision.id_mision, !mision.activa)
      await cargar()
    } catch (err) {
      setError(err.message)
    }
  }

  const { cargando, misiones, error: errorCarga } = estado
  if (cargando) return <div className="ak-cargando"><span className="home-giro" aria-label="Cargando" /></div>

  const activas = misiones.filter((m) => m.activa)
  const apagadas = misiones.filter((m) => !m.activa)
  const fila = (m) => <FilaMision key={m.id_mision} m={m} seleccionada={panel?.id_mision === m.id_mision} onEditar={setPanel} onAlternar={alternar} />

  return (
    <div className="ak-pagina">
      <div className="ak-cab">
        <p>Crea, edita o desactiva las misiones que ven los usuarios cada día.</p>
        <button className="ak-btn" onClick={() => setPanel({})}><Icono nombre="plus" tam={18} />Nueva misión</button>
      </div>
      <Aviso error={errorCarga} onReintentar={cargar} />
      <Aviso error={error} />

      <div className={`ak-cols ${panel ? 'ak-con-panel' : ''}`}>
        <section className="ak-card">
          <h3>Activas <span>· {activas.length}</span></h3>
          <div className="ak-lista">
            <Cabecera />
            {activas.length === 0 ? <p className="ak-vacio">No hay misiones activas.</p> : activas.map(fila)}
          </div>
          <h3>Desactivadas <span>· {apagadas.length}</span></h3>
          <div className="ak-lista">
            {apagadas.length === 0 ? <p className="ak-vacio">No hay misiones desactivadas.</p> : apagadas.map(fila)}
          </div>
        </section>

        {panel && (
          <FormularioMision
            key={panel.id_mision || 'nueva'}
            mision={panel.id_mision ? panel : null}
            onGuardar={guardar}
            onCancelar={() => setPanel(null)}
          />
        )}
      </div>
    </div>
  )
}
