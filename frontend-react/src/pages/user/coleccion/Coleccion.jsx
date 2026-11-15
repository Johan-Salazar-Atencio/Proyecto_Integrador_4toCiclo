import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useVirtualizer } from '@tanstack/react-virtual'
import { RUTAS } from '../../../routes/rutas'
import { listarColeccion } from '../../../services/coleccion'
import { urlImagen } from '../../../services/home'
import { listarPlantasDigitales, seleccionarPlanta } from '../../../services/jardin'
import './coleccion.css'

export default function Coleccion() {
  const [pestana, setPestana] = useState('escaneadas')
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
        <div>
          <h2>Mis plantas</h2>
          <p>{escaneadas.length + digitales.length} / ∞</p>
        </div>
        <div className="coleccion-pestanas" role="tablist">
          <button role="tab" aria-selected={pestana === 'escaneadas'} className={pestana === 'escaneadas' ? 'activa' : ''}
            onClick={() => setPestana('escaneadas')}>Escaneadas <b>{escaneadas.length}</b></button>
          <button role="tab" aria-selected={pestana === 'digitales'} className={pestana === 'digitales' ? 'activa' : ''}
            onClick={() => setPestana('digitales')}>Digitales <b>{digitales.length}</b></button>
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
        lista.length === 0 ? (
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
            <div className="coleccion-grilla">
              {digitales.map((p, i) => (
                <button key={p.id} className="planta-card aparece" style={{ animationDelay: `${i * 0.05}s` }} onClick={() => abrirDigital(p)}>
                  <Foto url={urlImagen(p.fotoUrl)} nombre={p.name} />
                  <div className="planta-card-datos">
                    <b>{p.name}</b>
                    <small>{p.species}</small>
                    <div className="planta-card-chips">
                      {p.activa && <span className="chip-activo">Activo</span>}
                    </div>
                  </div>
                </button>
              ))}
              <Link to={RUTAS.ESCANEAR} className="planta-card planta-card-mas aparece">
                <span>🔍</span>
                <b>¡Descubre más plantas!</b>
              </Link>
            </div>
          )
        )
      )}
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