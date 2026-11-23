import { useCallback, useEffect, useRef, useState } from 'react'
import EncabezadoAdmin from '../../../components/admin/EncabezadoAdmin'
import IconoAdmin from '../../../components/admin/IconoAdmin'
import { AVISO_EVENTOS, cancelarEvento, crearEvento, listarEventos, listarRegiones } from '../../../services/admin/eventos'
import { CONFIRMAR_CANCELAR } from '../../../services/admin/respuestas.js'
import { listarTiposEvento } from '../../../services/admin/tiposEvento'
import EfectoEvento from './EfectoEvento'
import EventoEnCurso from './EventoEnCurso'
import FormularioTipo from './FormularioTipo'
import HistorialEventos from './HistorialEventos'
import ModalLanzar from './ModalLanzar'
import TarjetaTipo from './TarjetaTipo'
import { PENDIENTE } from './util'
import './eventos.css'

const MS_AVISO = 3500

// Eventos y desastres: tipos de evento (con su formulario), evento en curso e historial en una sola seccion
export default function Eventos() {
  const [estado, setEstado] = useState({ cargando: true, eventos: [], tipos: [], error: '' })
  const [regiones, setRegiones] = useState([{ codigo: 'GLOBAL', nombre: 'Global' }])
  const [vista, setVista] = useState({ modo: 'lista', tipo: null }) // lista | formulario
  const [aLanzar, setALanzar] = useState(null)
  const [cancelando, setCancelando] = useState(false)
  const [aviso, setAviso] = useState('')
  const [verEfecto, setVerEfecto] = useState(null)
  const temporizador = useRef(null)

  const cargar = useCallback(async () => {
    const [eventos, tipos] = await Promise.allSettled([listarEventos(), listarTiposEvento()])
    if (eventos.status === 'rejected') {
      setEstado({ cargando: false, eventos: [], tipos: [], error: 'No se pudo leer Django. ¿Está encendido en el puerto 8000?' })
      return
    }
    setEstado({
      cargando: false,
      eventos: eventos.value,
      tipos: tipos.status === 'fulfilled' ? tipos.value : [],
      error: tipos.status === 'rejected' ? `No se pudieron leer los tipos de evento: ${tipos.reason?.message || 'error de Django'}` : '',
    })
  }, [])

  useEffect(() => { cargar(); listarRegiones().then(setRegiones) }, [cargar])
  useEffect(() => () => clearTimeout(temporizador.current), [])

  const avisar = (texto) => {
    setAviso(texto)
    clearTimeout(temporizador.current)
    temporizador.current = setTimeout(() => setAviso(''), MS_AVISO)
  }
  const avisarMenu = () => window.dispatchEvent(new Event(AVISO_EVENTOS))

  const lanzar = async (datos) => {
    await crearEvento(datos)
    setALanzar(null)
    await cargar()
    avisarMenu()
    avisar('Evento lanzado: los usuarios ya lo reciben')
  }

  const cancelar = async (evento) => {
    // QA-07: antes cancelaba sin preguntar (y ademas danaba las plantas no prevenidas)
    const seguro = window.confirm(CONFIRMAR_CANCELAR)
    if (!seguro) return
    setCancelando(true)
    try {
      await cancelarEvento(evento.id_desastre)
      await cargar()
      avisarMenu()
      avisar('Emergencia cancelada: ninguna planta recibió daño')
    } catch (err) {
      avisar(err.message || 'No se pudo cancelar el evento')
    } finally {
      setCancelando(false)
    }
  }

  const guardado = (tipo) => {
    setEstado((e) => ({ ...e, tipos: e.tipos.map((x) => (x.id_tipo_desastre === tipo.id_tipo_desastre ? tipo : x)) }))
    setVista({ modo: 'lista', tipo: null })
    avisar(`${tipo.nombre} guardado`)
  }

  const { cargando, eventos, tipos, error } = estado
  const activo = eventos.find((e) => e.estado === 'ACTIVO' && new Date(e.fecha_fin) > new Date())

  if (vista.modo === 'formulario') {
    return (
      <>
        <FormularioTipo
          key={vista.tipo?.id_tipo_desastre ?? 'nuevo'}
          tipo={vista.tipo}
          regiones={regiones}
          onVolver={() => setVista({ modo: 'lista', tipo: null })}
          onGuardado={guardado}
          onError={avisar}
        />
        {aviso && <div className="ad-toast" role="status">{aviso}</div>}
      </>
    )
  }

  return (
    <>
      <EncabezadoAdmin titulo="Eventos y desastres" subtitulo="Crea y edita tipos de evento, lánzalos a la comunidad y revisa el historial. Solo puede haber uno activo a la vez.">
        <button type="button" className="ad-btn" onClick={() => setVista({ modo: 'formulario', tipo: null })}><IconoAdmin n="plus" />Nuevo tipo de evento</button>
      </EncabezadoAdmin>

      {cargando && <div className="ad-cargando"><span className="home-giro" aria-label="Cargando" /></div>}

      {!cargando && error && (
        <div className="home-error" role="alert">
          <span>{error}</span>
          <button onClick={cargar}>Reintentar</button>
        </div>
      )}

      {!cargando && (
        <div className="ev-cols">
          <section className="ad-card ad-c">
            <div className="ev-tipo-fila">
              <h3>Tipos de evento</h3>
              <small className="ad-small">{tipos.length} {tipos.length === 1 ? 'tipo' : 'tipos'} · {activo ? <b className="ev-en-curso-t">1 activo en curso</b> : 'ninguno en curso'}</small>
            </div>
            {activo && <div className="ad-aviso"><IconoAdmin n="alert" /><span>Ya hay un evento en curso. Solo puede haber uno a la vez: cancélalo o espera a que termine.</span></div>}
            <div className="ev-rejilla">
              {tipos.map((t) => (
                <TarjetaTipo key={t.id_tipo_desastre} tipo={t} hayActivo={Boolean(activo)} onLanzar={setALanzar} onEditar={(x) => setVista({ modo: 'formulario', tipo: x })} />
              ))}
              <button type="button" className="ev-nuevo" onClick={() => setVista({ modo: 'formulario', tipo: null })}>
                <span><IconoAdmin n="plus" /></span>
                Crear nuevo tipo de evento
                <small className="ad-small">Con imagen, texto y salud por severidad · {PENDIENTE}</small>
              </button>
            </div>
          </section>

          <div className="ev-lado">
            <EventoEnCurso evento={activo} tipos={tipos} cancelando={cancelando} onCancelar={() => cancelar(activo)} />
            <HistorialEventos eventos={eventos} tipos={tipos} onVerEfecto={setVerEfecto} />
          </div>
        </div>
      )}

      {aLanzar && <ModalLanzar tipo={aLanzar} regiones={regiones} onCerrar={() => setALanzar(null)} onLanzar={lanzar} />}
      {verEfecto && <EfectoEvento key={verEfecto.id_desastre} evento={verEfecto} tipos={tipos} onCerrar={() => setVerEfecto(null)} />}
      {aviso && <div className="ad-toast" role="status">{aviso}</div>}
    </>
  )
}
