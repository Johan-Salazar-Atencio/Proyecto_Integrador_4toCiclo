import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { RUTAS } from '../../../routes/rutas'
import { obtenerEventoActivo, urlImagen } from '../../../services/home'
import { cuidarPlanta, golpeVisto, obtenerPlantaJardin, prevenirPlanta, revivirPlanta } from '../../../services/jardin'
import { comprarAbono, obtenerTienda } from '../../../services/tienda'
import Digitalizacion from '../../../components/Digitalizacion'
import PlantaPixel from '../../../components/PlantaPixel'
import EscenaPixel from '../../../components/EscenaPixel'
import FondosPlanta from '../../../components/FondosPlanta'
import { ACCIONES, mensajeCuidado } from './acciones'
import { consejo, estadoAnimo, nivelBarra } from './animo'
import { etapaDe } from './etapas'
import useRestante, { formatoCuenta } from '../useRestante'
import './jardin.css'

const ESTADISTICAS = [
  { campo: 'healthPercent', texto: '❤️ Salud' },
  { campo: 'humidityPercent', texto: '💧 Agua' },
  { campo: 'lightPercent', texto: '☀️ Luz' },
]
const MS_AVISO = 2500
const MS_SONDEO_EVENTO = 20000
const MS_GOLPE = 3500

// Frase del golpe: el sujeto depende del evento que arraso con la planta
const TEXTO_GOLPE = { HUAYCO: 'El huayco', HELADA: 'La helada', OLA_DE_CALOR: 'La ola de calor', PLAGA: 'La plaga' }
const textoDelGolpe = (tipo) => `¡${TEXTO_GOLPE[tipo] || 'El evento'} arrasó con tu planta!`

// "ganó medio día", "ganó un día" o "ganó 3 días" segun lo que subio diasCuidados (igual que en el movil)
function diasGanados(antes, despues) {
  const dias = (despues.diasCuidados || 0) - (antes.diasCuidados || 0)
  if (dias <= 0) return 'ganó medio día'
  return dias === 1 ? 'ganó un día' : `ganó ${dias} días`
}

// Cuenta regresiva de la recuperacion: solo el abono salva a la planta golpeada antes de que se acabe el tiempo
function Recuperacion({ segundos }) {
  const restante = useRestante(segundos)
  return (
    <section className="jardin-recuperacion aparece" role="alert">
      <b>⚠️ En recuperación{restante != null ? ` · ${formatoCuenta(restante)}` : ''}</b>
      <p>Un evento la golpeó. Solo el abono la recupera: abónala antes de que se acabe el tiempo o morirá.</p>
    </section>
  )
}

export default function Jardin() {
  const [estado, setEstado] = useState({ cargando: true, planta: null, error: '' })
  const [aviso, setAviso] = useState('')
  const [efecto, setEfecto] = useState(null)
  const [cuidando, setCuidando] = useState(false)
  const [fondosAbierto, setFondosAbierto] = useState(false)
  const [claveEscena, setClaveEscena] = useState(0) // cambia al poner otro fondo: la escena se vuelve a armar
  const [abonos, setAbonos] = useState([]) // abonos de la tienda (medio dia, 1 y 3 dias), del mas barato al mas caro
  const [evento, setEvento] = useState(null) // evento activo (o null): con el aparece "Prevenir evento"
  const [golpe, setGolpe] = useState(null) // tipo de evento que arraso con la planta, mientras se muestra
  const [escudo, setEscudo] = useState(0) // sube cada vez que se previene: dispara el escudo
  // Se llega desde "Digitalizar": antes de la planta se ve como la foto se vuelve semilla
  const lugar = useLocation()
  const irA = useNavigate()
  const [llegando, setLlegando] = useState(Boolean(lugar.state?.digitalizada))
  const temporizador = useRef(null)

  // Planta del jardin guardada en el servidor (GET /plantas/activa, 204 = sin planta)
  const cargar = useCallback(async () => {
    setEstado({ cargando: true, planta: null, error: '' })
    try {
      setEstado({ cargando: false, planta: await obtenerPlantaJardin(), error: '' })
    } catch (err) {
      setEstado({ cargando: false, planta: null, error: err instanceof TypeError ? 'No se pudo conectar con el servidor.' : err.message })
    }
  }, [])

  useEffect(() => { cargar() }, [cargar])
  useEffect(() => {
    obtenerTienda()
      .then((t) => setAbonos(t.items.filter((i) => i.tipo === 'ABONO').sort((a, b) => a.precio - b.precio)))
      .catch(() => setAbonos([]))
  }, [])
  useEffect(() => () => clearTimeout(temporizador.current), [])

  // Un evento que lanza el admin aparece solo: se consulta mientras el jardin esta a la vista
  useEffect(() => {
    let vivo = true
    const consultar = () => obtenerEventoActivo().then((e) => { if (vivo) setEvento(e) }).catch(() => {})
    consultar()
    const id = setInterval(consultar, MS_SONDEO_EVENTO)
    return () => { vivo = false; clearInterval(id) }
  }, [])

  // Si un evento golpeo a la planta, se muestra una sola vez como arraso con ella y se marca como visto
  const golpeTipo = estado.planta?.golpeTipo
  const plantaId = estado.planta?.id
  useEffect(() => {
    if (!golpeTipo) return undefined
    setGolpe(golpeTipo)
    const id = setTimeout(() => {
      setGolpe(null)
      golpeVisto(plantaId).then((p) => setEstado((e) => ({ ...e, planta: p }))).catch(() => {})
    }, MS_GOLPE)
    return () => clearTimeout(id)
  }, [golpeTipo, plantaId])

  const avisar = (texto) => {
    setAviso(texto)
    clearTimeout(temporizador.current)
    temporizador.current = setTimeout(() => setAviso(''), MS_AVISO)
  }

  const alDigitalizar = () => {
    setLlegando(false)
    setEfecto({ tipo: 'CRECER', id: Date.now() })
    avisar(`¡${estado.planta.name} ya vive en tu jardín! 🌱`)
    // Sin esto, recargar la pagina repetiria la secuencia
    irA(lugar.pathname, { replace: true, state: null })
  }

  const cuidar = async (accion) => {
    if (cuidando || llegando) return
    const previa = estado.planta
    const antes = etapaDe(previa)
    setCuidando(true)
    try {
      const planta = await cuidarPlanta(previa.id, accion)
      const despues = etapaDe(planta)
      setEstado((e) => ({ ...e, planta }))
      setEfecto({ tipo: accion, id: Date.now() })
      if (antes.marchita && !despues.marchita) {
        avisar(`¡${planta.name} se recuperó! 🌱`)
      } else if (despues.indice > antes.indice && !despues.marchita) {
        avisar(`¡${planta.name} creció! Ahora es: ${despues.nombre} 🎉`)
      } else {
        avisar(mensajeCuidado(previa, planta, accion))
      }
    } catch (err) {
      avisar(err.message || 'No se pudo cuidar la planta.')
    } finally {
      setCuidando(false)
    }
  }

  // Prevenir el evento: deja a esta planta a resguardo (una por una)
  const prevenir = async () => {
    if (cuidando) return
    setCuidando(true)
    try {
      const planta = await prevenirPlanta(estado.planta.id)
      setEstado((e) => ({ ...e, planta }))
      setEscudo((n) => n + 1)
      avisar(`${planta.name} está a resguardo 🛡️`)
    } catch (err) {
      avisar(err.message || 'No se pudo prevenir el evento.')
    } finally {
      setCuidando(false)
    }
  }

  // Revivir: cuesta hojas y la planta sigue creciendo desde donde iba
  const revivir = async () => {
    if (cuidando) return
    setCuidando(true)
    try {
      const planta = await revivirPlanta(estado.planta.id)
      setEstado((e) => ({ ...e, planta }))
      setEfecto({ tipo: 'CRECER', id: Date.now() })
      avisar(`¡${planta.name} revivió! 🌱`)
    } catch (err) {
      avisar(err.estado === 402 ? 'No te alcanzan las hojas para revivirla.' : err.message || 'No se pudo revivir la planta.')
    } finally {
      setCuidando(false)
    }
  }

  // Abono de la tienda: cuesta monedas y suma medio dia, 1 o 3 dias de crecimiento (maximo uno al dia por planta)
  const abonarConTienda = async (abono) => {
    if (cuidando) return
    setCuidando(true)
    try {
      const planta = await comprarAbono(estado.planta.id, abono.id)
      const antes = etapaDe(estado.planta)
      const despues = etapaDe(planta)
      setEstado((e) => ({ ...e, planta }))
      setEfecto({ tipo: 'ABONAR', id: Date.now() })
      avisar(despues.indice > antes.indice
        ? `¡${planta.name} creció! Ahora es: ${despues.nombre} 🎉`
        : `${abono.nombre} aplicado: ${planta.name} ${diasGanados(estado.planta, planta)} 🌿`)
    } catch (err) {
      avisar(err.message || 'No se pudo usar el abono.')
    } finally {
      setCuidando(false)
    }
  }

  if (estado.cargando) return <div className="home-cargando"><span className="home-giro" aria-label="Cargando" /></div>

  if (estado.error) {
    return (
      <div className="coleccion-vacio aparece">
        <span>😕</span>
        <h2>{estado.error}</h2>
        <button className="boton boton-suave" onClick={cargar}>Reintentar</button>
      </div>
    )
  }

  const planta = estado.planta
  if (!planta) {
    return (
      <div className="coleccion-vacio aparece">
        <span>🌱</span>
        <h2>Aún no tienes una planta en tu jardín</h2>
        <p>Escanea una planta y pulsa Digitalizar para cuidarla aquí.</p>
        <Link to={RUTAS.ESCANEAR} className="boton boton-principal">📷 Escanear</Link>
      </div>
    )
  }

  const animo = estadoAnimo(planta)
  const foto = urlImagen(planta.fotoUrl)
  const etapa = etapaDe(planta)
  const marchita = etapa.marchita
  const muerta = planta.fase === 'MUERTA'

  return (
    <div className="jardin">
      <section className={`jardin-marco aparece ${marchita ? 'jardin-marco-marchita' : ''}`}>
        <EscenaPixel key={`escena-${claveEscena}`} />
        <div className="jardin-planta">
          {llegando
            ? <Digitalizacion foto={foto} apariencia={planta.apariencia} maceta={planta.macetaCodigo} alTerminar={alDigitalizar} />
            : <PlantaPixel etapa={etapa.indice} apariencia={planta.apariencia} maceta={planta.macetaCodigo} efecto={efecto} titulo={`${planta.name}: ${etapa.nombre}`} />}
        </div>
        {escudo > 0 && <span key={`escudo-${escudo}`} className="jardin-escudo" aria-hidden="true">🛡️</span>}
        {golpe && <div className="jardin-golpe" role="alert">{textoDelGolpe(golpe)}</div>}
        {planta.refugiada && evento && <span className="jardin-resguardo">🛡️ A resguardo</span>}
        <button className="jardin-fondos-boton" onClick={() => setFondosAbierto(true)} aria-label={`Fondos de ${planta.name}`} title="Fondos de esta planta">🖼️</button>
        <div
          className={`jardin-chip ${animo.clase}`}
          aria-label={`Ánimo ${animo.texto}${etapa.total > 0 ? `, día ${etapa.dias} de ${etapa.total}, fase ${etapa.fase} de 3` : ''}${marchita ? ', marchita' : ''}`}
        >
          <span className="jardin-chip-cara">{animo.cara}</span>
          {etapa.total > 0 && (
            <b>{marchita ? 'Marchita' : etapa.dias >= etapa.total ? 'Madura' : `Día ${etapa.dias}/${etapa.total}`}</b>
          )}
          {etapa.total > 0 && !marchita && (
            <span className="jardin-puntos" aria-hidden="true">
              {[1, 2, 3].map((n) => <i key={n} className={n <= etapa.fase ? 'punto-hecho' : ''} />)}
            </span>
          )}
        </div>
      </section>

      <div className="jardin-panel">
        <section className="tarjeta aparece" style={{ animationDelay: '0.08s' }}>
          <div className="jardin-titulo">
            {foto && <img className="jardin-foto" src={foto} alt="" onError={(e) => { e.currentTarget.style.display = 'none' }} />}
            <div>
              <h2>{planta.name}</h2>
              <p className="jardin-especie">{planta.species}</p>
            </div>
          </div>
          <div className="jardin-barras">
            {ESTADISTICAS.map(({ campo, texto }) => (
              <div key={campo} className="planta-estadistica">
                <span>{texto}</span>
                <div className="barra"><div className={`barra-relleno ${nivelBarra(planta[campo])}`} style={{ width: `${planta[campo]}%` }} /></div>
                <b>{planta[campo]}%</b>
              </div>
            ))}
          </div>
          {planta.lastWatered && <small className="jardin-riego">Último riego: {planta.lastWatered}</small>}
        </section>

        <section className="jardin-consejo aparece" style={{ animationDelay: '0.14s' }}>
          <span>💡</span>
          <p>{consejo(planta)}</p>
        </section>

        {muerta ? (
          <section className="jardin-muerta aparece">
            <h2>💀 {planta.name} murió</h2>
            <p>No se abonó a tiempo después del evento. Puedes revivirla y sigue creciendo desde donde iba.</p>
            <button className="jardin-revivir" onClick={revivir} disabled={cuidando}>
              {planta.costoRevivir != null ? `Revivir · ${planta.costoRevivir} 🍃` : 'Revivir'}
            </button>
          </section>
        ) : (
          <>
            {planta.enRecuperacion && <Recuperacion segundos={planta.recuperacionSegundos} />}
            <div className="jardin-acciones aparece" style={{ animationDelay: '0.2s' }}>
              {Object.entries(ACCIONES).map(([clave, a]) => (
                <button key={clave} className="jardin-accion" onClick={() => cuidar(clave)} disabled={cuidando || llegando}>
                  <span>{a.icono}</span>
                  <b>{a.texto}</b>
                  <small>{a.pista}</small>
                </button>
              ))}
            </div>
            {(evento || planta.refugiada) && (
              <button
                className={`jardin-prevenir ${planta.refugiada ? 'jardin-prevenir-hecho' : ''}`}
                onClick={prevenir}
                disabled={cuidando || planta.refugiada}
              >
                {planta.refugiada ? '🛡️ Evento prevenido' : `🛡️ Prevenir evento${evento?.eventName ? `: ${evento.eventName}` : ''}`}
              </button>
            )}
            {abonos.length > 0 && (
              <div className="jardin-abonos aparece" style={{ animationDelay: '0.26s' }}>
                <p className="jardin-abonos-titulo">🌿 Abonos · máximo 1 al día</p>
                {abonos.map((abono) => (
                  <button
                    key={abono.id}
                    className="jardin-abono"
                    onClick={() => abonarConTienda(abono)}
                    disabled={cuidando || abono.bloqueado}
                    title={abono.bloqueado ? `Se desbloquea en el nivel ${abono.nivelRequerido}` : undefined}
                  >
                    <b>{abono.nombre}</b>
                    <span className="jardin-abono-desc">{abono.descripcion}</span>
                    <small>{abono.bloqueado ? `🔒 Nivel ${abono.nivelRequerido}` : `${abono.precio} 🍃`}</small>
                  </button>
                ))}
              </div>
            )}
          </>
        )}
        <Link className="jardin-tienda-enlace" to={RUTAS.TIENDA}>🛒 Ir a la tienda</Link>
      </div>

      {fondosAbierto && (
        <FondosPlanta
          planta={planta}
          onCerrar={() => setFondosAbierto(false)}
          onCambio={(tema) => {
            setClaveEscena((k) => k + 1)
            avisar(`${planta.name} ahora usa «${tema.nombre}»`)
          }}
        />
      )}

      {aviso && <div className="jardin-aviso" role="status">{aviso}</div>}
    </div>
  )
}
