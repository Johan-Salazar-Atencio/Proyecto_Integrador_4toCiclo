import { useCallback, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { RUTAS } from '../../../routes/rutas'
import { obtenerPlantaActiva, urlImagen } from '../../../services/home'
import { ACCIONES, aplicarAjustes, aplicarCuidado, plantaActiva } from '../../../store/jardinSimulado'
import { consejo, estadoAnimo, nivelBarra } from './animo'
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
  const temporizador = useRef(null)

  // Primero las plantas digitales; si no hay, la planta activa del servidor (GET /plantas/activa, 204 = sin planta)
  const cargar = useCallback(async () => {
    setEstado({ cargando: true, planta: null, error: '' })
    const digital = plantaActiva()
    if (digital) return setEstado({ cargando: false, planta: digital, error: '' })
    try {
      const base = await obtenerPlantaActiva()
      setEstado({ cargando: false, planta: base ? aplicarAjustes(base) : null, error: '' })
    } catch (err) {
      setEstado({ cargando: false, planta: null, error: err instanceof TypeError ? 'No se pudo conectar con el servidor.' : err.message })
    }
  }, [])

  useEffect(() => { cargar() }, [cargar])
  useEffect(() => () => clearTimeout(temporizador.current), [])

  const cuidar = (accion) => {
    const { planta, mensaje } = aplicarCuidado(estado.planta, accion)
    setEstado({ ...estado, planta })
    setSalto((s) => s + 1)
    setAviso(mensaje)
    clearTimeout(temporizador.current)
    temporizador.current = setTimeout(() => setAviso(''), MS_AVISO)
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
  const foto = urlImagen(planta.imageUrl)

  return (
    <div className="jardin">
      <section className="jardin-marco aparece">
        <div key={salto} className={`jardin-planta ${salto ? 'jardin-salta' : ''}`}>
          <span className="jardin-respaldo">🪴</span>
          {foto && <img src={foto} alt={planta.name} onError={(e) => { e.currentTarget.style.display = 'none' }} />}
        </div>
        <span className={`jardin-animo ${animo.clase}`}>{animo.cara} {animo.texto}</span>
      </section>

      <div className="jardin-panel">
        <section className="tarjeta aparece" style={{ animationDelay: '0.08s' }}>
          <h2>{planta.name}</h2>
          <p className="jardin-especie">{planta.species}</p>
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
            <button key={clave} className="jardin-accion" onClick={() => cuidar(clave)}>
              <span>{a.icono}</span>
              <b>{a.texto}</b>
              <small>{a.pista}</small>
            </button>
          ))}
        </div>
      </div>

      {aviso && <div className="jardin-aviso" role="status">{aviso}</div>}
    </div>
  )
}
