import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useVirtualizer } from '@tanstack/react-virtual'
import { RUTAS } from '../../../routes/rutas'
import { listarColeccion } from '../../../services/coleccion'
import { urlImagen } from '../../../services/home'
import { listarPlantasDigitales, seleccionarPlanta } from '../../../services/jardin'
import ColeccionVirtual from './ColeccionVirtual'
import './coleccion.css'

export default function Coleccion() {
  const [pestana, setPestana] = useState('virtuales')
  const [aviso, setAviso] = useState('')
  const [escaneadas, setEscaneadas] = useState([])
  const [digitales, setDigitales] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const irA = useNavigate()

  const cargar = useCallback(async () => {
    setCargando(true)
    setError('')
    try {
      const [coleccion, plantas] = await Promise.all([listarColeccion(), listarPlantasDigitales()])
      setEscaneadas(coleccion)
      setDigitales(plantas || [])
    } catch (err) {
      setError(err instanceof TypeError ? 'No se pudo conectar con el servidor.' : err.message)
    } finally {
      setCargando(false)
    }
  }, [])

  useEffect(() => { cargar() }, [cargar])

  const abrirDigital = async (planta) => {
    try {
      await seleccionarPlanta(planta.id)
      irA(RUTAS.JARDIN)
    } catch (err) {
      setError(err.message || 'No se pudo abrir la planta.')
    }
  }

  // Un cambio en una planta (apodo, baul) devuelto por el servidor se refleja en la lista sin volver a pedirla
  const actualizarPlanta = (nueva) => setDigitales((lista) => lista.map((p) => (p.id === nueva.id ? nueva : p)))
  const avisar = (texto) => {
    setAviso(texto)
    setTimeout(() => setAviso(''), 2500)
  }

  const lista = pestana === 'escaneadas' ? escaneadas : digitales

  // Virtualizacion de la lista de escaneadas (web). Antes se renderizaban
  // todos los items con .map(); ahora solo los visibles + buffer.
  const parentRef = useRef(null)
  const rowVirtualizer = useVirtualizer({
    count: pestana === 'escaneadas' ? escaneadas.length : 0,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 110,
    overscan: 4,
  })

  return (
    <div className="coleccion">
      <header className="coleccion-cabecera aparece">
        <div className="coleccion-pestanas coleccion-segmentos" role="tablist">
          <button role="tab" aria-selected={pestana === 'virtuales'} className={pestana === 'virtuales' ? 'activa' : ''}
            onClick={() => setPestana('virtuales')}>Virtuales · {digitales.length}</button>
          <button role="tab" aria-selected={pestana === 'escaneadas'} className={pestana === 'escaneadas' ? 'activa' : ''}
            onClick={() => setPestana('escaneadas')}>Escaneos · {escaneadas.length}</button>
        </div>
      </header>

      {cargando && <div className="home-cargando"><span className="home-giro" aria-label="Cargando" /></div>}

      {!cargando && error && (
        <div className="home-error" role="alert">
          <span>{error}</span>
          <button onClick={cargar}>Reintentar</button>
        </div>
      )}

      {!cargando && !error && (
        pestana === 'escaneadas' && lista.length === 0 ? (
          <div className="coleccion-vacio aparece">
            <span>🌱</span>
            <h2>Aún no tienes plantas</h2>
            <p>{pestana === 'escaneadas' ? 'Escanea tu primera planta para empezar tu colección.' : 'Digitaliza una planta escaneada para cuidarla en tu jardín.'}</p>
            <Link to={RUTAS.ESCANEAR} className="boton boton-principal">📷 Escanear</Link>
          </div>
        ) : (
          pestana === 'escaneadas' ? (
            <div
              ref={parentRef}
              className="coleccion-virtual"
              style={{ height: 520, overflow: 'auto' }}
            >
              <div style={{ height: rowVirtualizer.getTotalSize(), width: '100%', position: 'relative' }}>
                {rowVirtualizer.getVirtualItems().map((virtualItem) => {
                  const p = escaneadas[virtualItem.index]
                  return (
                    <Link
                      key={p.id}
                      to={`${RUTAS.COLECCION}/${p.id}`}
                      className="planta-card aparece"
                      style={{
                        position: 'absolute',
                        top: 0,
                        left: 0,
                        width: '100%',
                        transform: `translateY(${virtualItem.start}px)`,
                      }}
                    >
                      <Foto url={p.fotoUrl} nombre={p.nombreComun} />
                      <div className="planta-card-datos">
                        <b>{p.nombreComun}</b>
                        <small>{p.nombreCientifico}</small>
                        <div className="planta-card-chips">
                          {p.vecesVista > 1 && <span>×{p.vecesVista}</span>}
                          {p.esToxicaMascotas && <span className="chip-alerta">⚠️ Tóxica</span>}
                        </div>
                      </div>
                    </Link>
                  )
                })}
              </div>
            </div>
          ) : (
            <ColeccionVirtual digitales={digitales} onCambio={actualizarPlanta} onAbrir={abrirDigital} onAviso={avisar} />
          )
        )
      )}
      {aviso && <div className="jardin-aviso" role="status">{aviso}</div>}
    </div>
  )
}

function Foto({ url, nombre }) {
  return (
    <div className="planta-card-foto">
      <span>🪴</span>
      {url && <img src={url} alt={nombre} loading="lazy" onError={(e) => { e.currentTarget.style.display = 'none' }} />}
    </div>
  )
}