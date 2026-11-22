import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import IconoLogro from '../../../components/IconoLogro'
import { RUTAS_ADMIN } from '../../../routes/rutas'
import {
  cambiarEstadoLogro, colorRareza, crearLogro, editarLogro, listarLogros, normalizarOpciones, obtenerOpciones, textoRareza,
} from '../../../services/admin/logros'
import { useSesionAdmin } from '../../../store/SesionAdmin'
import FormularioLogro from './FormularioLogro'
import { Aviso, BotonIcono, Chip, Icono, Interruptor } from '../misiones/kit'
import './logros.css'

function FilaLogro({ l, condiciones, seleccionado, onEditar, onAlternar, ocupado }) {
  const nombreCondicion = condiciones.find((c) => c.codigo === l.condicion)?.texto || l.condicion
  return (
    <article className={`lg-fila ${seleccionado ? 'ak-tr-sel' : ''} ${l.activo ? '' : 'ak-apagada'}`}>
      <div className="lg-titulo">
        <IconoLogro clave={l.icono} className="lg-ico" />
        <div>
          <b>{l.titulo}</b>
          <small>{l.codigo}{l.secreto ? ' · secreto' : ''}</small>
        </div>
      </div>
      <div className="lg-celda" data-etiqueta="Rareza"><Chip color={colorRareza(l.rareza)}>{textoRareza(l.rareza)}</Chip></div>
      <div className="lg-celda lg-cond" data-etiqueta="Condición">
        {nombreCondicion}{l.parametro ? ` · ${l.parametro}` : ''}
      </div>
      <div className="lg-celda" data-etiqueta="Meta">{l.meta}</div>
      <div className="lg-celda" data-etiqueta="Recompensa">
        <span className="ak-hojas"><Icono nombre="leaf" tam={15} />{l.recompensa_monedas}</span> · {l.recompensa_xp} XP
      </div>
      <div className="lg-celda" data-etiqueta="Desbloqueos">{l.desbloqueos ?? 0}</div>
      <div className="lg-celda lg-acciones">
        <Interruptor activo={l.activo} disabled={ocupado} onChange={() => onAlternar(l)} etiqueta={`${l.titulo}: activo`} />
        <BotonIcono icono="edit" etiqueta={`Editar ${l.titulo}`} onClick={() => onEditar(l)} />
      </div>
    </article>
  )
}

function Cabecera() {
  return (
    <div className="lg-cab-tabla" aria-hidden="true">
      <span>Logro</span><span>Rareza</span><span>Condición</span><span>Meta</span><span>Recompensa</span><span>Desbloqueos</span><span>Estado</span>
    </div>
  )
}

export default function Logros() {
  const { cerrar } = useSesionAdmin()
  const irA = useNavigate()
  const [estado, setEstado] = useState({ cargando: true, logros: [], opciones: null, error: '' })
  const [panel, setPanel] = useState(null) // null = cerrado, {} = nuevo, logro = editar
  const [error, setError] = useState('')
  const [aviso, setAviso] = useState('')
  const [ocupado, setOcupado] = useState(false)

  // 401 = sesion del admin caducada: se vuelve al acceso como en el resto del panel
  const manejar = useCallback((err) => {
    if (err.estado === 401) {
      cerrar()
      irA(RUTAS_ADMIN.LOGIN, { replace: true })
      return true
    }
    return false
  }, [cerrar, irA])

  const cargar = useCallback(async () => {
    try {
      const [logros, opciones] = await Promise.all([listarLogros(), obtenerOpciones()])
      setEstado({ cargando: false, logros: Array.isArray(logros) ? logros : [], opciones: normalizarOpciones(opciones), error: '' })
    } catch (err) {
      if (manejar(err)) return
      setEstado((e) => ({ ...e, cargando: false, error: err.estado ? `No se pudieron cargar los logros (error ${err.estado}). ¿Django está encendido?` : err.message || 'No se pudo leer Django.' }))
    }
  }, [manejar])

  useEffect(() => { cargar() }, [cargar])

  const guardar = async (datos) => {
    let respuesta
    try {
      respuesta = panel?.id_logro ? await editarLogro(panel.id_logro, datos) : await crearLogro(datos)
    } catch (err) {
      if (manejar(err)) return
      throw err
    }
    setAviso(respuesta?.advertencia || '')
    setPanel(null)
    await cargar()
  }

  const alternar = async (logro) => {
    setError('')
    setAviso('')
    setOcupado(true)
    try {
      await cambiarEstadoLogro(logro.id_logro, !logro.activo)
      await cargar()
    } catch (err) {
      if (!manejar(err)) setError(err.message)
    } finally {
      setOcupado(false)
    }
  }

  const { cargando, logros, opciones, error: errorCarga } = estado
  if (cargando) return <div className="ak-cargando"><span className="home-giro" aria-label="Cargando" /></div>

  const activos = logros.filter((l) => l.activo)
  const inactivos = logros.filter((l) => !l.activo)
  const fila = (l) => (
    <FilaLogro key={l.id_logro} l={l} condiciones={opciones?.condiciones || []} seleccionado={panel?.id_logro === l.id_logro}
      ocupado={ocupado} onEditar={setPanel} onAlternar={alternar} />
  )

  return (
    <div className="ak-pagina">
      <div className="ak-cab">
        <div><h2 className="ak-h2">Logros</h2><p>Crea, edita o desactiva los logros que los usuarios desbloquean jugando. No se borran: se desactivan.</p></div>
        <button className="ak-btn lg-tactil" onClick={() => setPanel({})} disabled={!opciones || opciones.condiciones.length === 0}>
          <Icono nombre="plus" tam={18} />Nuevo logro
        </button>
      </div>
      <Aviso error={errorCarga} onReintentar={() => { setEstado((e) => ({ ...e, cargando: true, error: '' })); cargar() }} />
      <Aviso error={error} />
      {aviso && <div className="lg-adv" role="status"><Icono nombre="alert" tam={18} /><span><b>Advertencia:</b> {aviso}</span><button type="button" className="ak-ib" aria-label="Cerrar aviso" onClick={() => setAviso('')}><Icono nombre="x" /></button></div>}

      {!opciones ? null : <div className={`ak-cols ${panel ? 'ak-con-panel' : ''}`}>
        <section className="ak-card lg-lista">
          <h3>Activos <span>· {activos.length}</span></h3>
          <div className="lg-tabla">
            <Cabecera />
            {activos.length === 0 ? <p className="ak-vacio">No hay logros activos.</p> : activos.map(fila)}
          </div>
          <h3>Desactivados <span>· {inactivos.length}</span></h3>
          <div className="lg-tabla">
            {inactivos.length === 0 ? <p className="ak-vacio">No hay logros desactivados.</p> : inactivos.map(fila)}
          </div>
        </section>

        {panel && (
          <FormularioLogro
            key={panel.id_logro || 'nuevo'}
            logro={panel.id_logro ? panel : null}
            opciones={opciones}
            onGuardar={guardar}
            onCancelar={() => setPanel(null)}
          />
        )}
      </div>}
    </div>
  )
}
