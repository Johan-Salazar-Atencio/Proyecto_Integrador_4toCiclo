import { useCallback, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { RUTAS } from '../../../routes/rutas'
import { urlImagen } from '../../../services/home'
import { cuidarPlanta, obtenerPlantaJardin } from '../../../services/jardin'
import { comprarAbono, obtenerTienda } from '../../../services/tienda'
import PlantaPixel from '../../../components/PlantaPixel'
import EscenaPixel from '../../../components/EscenaPixel'
import { ACCIONES, mensajeCuidado } from './acciones'
import { consejo, estadoAnimo, nivelBarra } from './animo'
import { etapaDe } from './etapas'
import './jardin.css'

const ESTADISTICAS = [
  { campo: 'healthPercent', texto: '❤️ Salud' },
  { campo: 'humidityPercent', texto: '💧 Agua' },
  { campo: 'lightPercent', texto: '☀️ Luz' },
]
const MS_AVISO = 2500

export default function Jardin() {
  const [estado, setEstado] = useState({ cargando: true, planta: null, error: '' })
  const [aviso, setAviso] = useState('')
  const [salto, setSalto] = useState(0)
  const [cuidando, setCuidando] = useState(false)
  const [abono, setAbono] = useState(null) // abono de la tienda (precio), si esta a la venta
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
    obtenerTienda().then((t) => setAbono(t.items.find((i) => i.tipo === 'ABONO') || null)).catch(() => setAbono(null))
  }, [])
  useEffect(() => () => clearTimeout(temporizador.current), [])

  const avisar = (texto) => {
    setAviso(texto)
    clearTimeout(temporizador.current)
    temporizador.current = setTimeout(() => setAviso(''), MS_AVISO)
  }

  const cuidar = async (accion) => {
    if (cuidando) return
    const previa = estado.planta
    const antes = etapaDe(previa)
    setCuidando(true)
    try {
      const planta = await cuidarPlanta(previa.id, accion)
      const despues = etapaDe(planta)
      setEstado((e) => ({ ...e, planta }))
      setSalto((s) => s + 1)
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

  // Abono de la tienda: cuesta monedas y suma un dia de crecimiento (maximo uno al dia por planta)
  const abonarConTienda = async () => {
    if (cuidando) return
    setCuidando(true)
    try {
      const planta = await comprarAbono(estado.planta.id)
      const antes = etapaDe(estado.planta)
      const despues = etapaDe(planta)
      setEstado((e) => ({ ...e, planta }))
      setSalto((s) => s + 1)
      avisar(despues.indice > antes.indice ? `¡${planta.name} creció! Ahora es: ${despues.nombre} 🎉` : `Abono aplicado: ${planta.name} ganó un día 🌿`)
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

  return (
    <div className="jardin">
      <section className={`jardin-marco aparece ${marchita ? 'jardin-marco-marchita' : ''}`}>
        <EscenaPixel />
        <div key={salto} className={`jardin-planta ${salto ? 'jardin-salta' : ''}`}>
          <PlantaPixel etapa={etapa.indice} apariencia={planta.apariencia} titulo={`${planta.name}: ${etapa.nombre}`} />
        </div>
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

        <div className="jardin-acciones aparece" style={{ animationDelay: '0.2s' }}>
          {Object.entries(ACCIONES).map(([clave, a]) => (
            <button key={clave} className="jardin-accion" onClick={() => cuidar(clave)} disabled={cuidando}>
              <span>{a.icono}</span>
              <b>{a.texto}</b>
              <small>{a.pista}</small>
            </button>
          ))}
        </div>

        {abono && (
          <button className="jardin-abono aparece" onClick={abonarConTienda} disabled={cuidando} style={{ animationDelay: '0.26s' }}>
            <span>🌿</span>
            <b>{abono.nombre}: +1 día de crecimiento</b>
            <small>{abono.precio} 🍃 · máximo 1 al día</small>
          </button>
        )}
        <Link className="jardin-tienda-enlace" to={RUTAS.TIENDA}>🛒 Ir a la tienda</Link>
      </div>

      {aviso && <div className="jardin-aviso" role="status">{aviso}</div>}
    </div>
  )
}
