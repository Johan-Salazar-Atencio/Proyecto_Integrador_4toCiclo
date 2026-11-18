import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useVirtualizer } from '@tanstack/react-virtual'
import { RUTAS } from '../../../routes/rutas'
import { listarColeccion } from '../../../services/coleccion'
import { urlImagen } from '../../../services/home'
import { listarPlantasDigitales, seleccionarPlanta } from '../../../services/jardin'
import ColeccionVirtual from './ColeccionVirtual'
import Icono from './Iconos'
import './coleccion.css'

export default function Coleccion() {
  const [pestana, setPestana] = useState('virtuales')
  const [consulta, setConsulta] = useState('')
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

  const buscada = consulta.trim().toLowerCase()
  const escaneadasVistas = buscada
    ? escaneadas.filter((p) => `${p.nombreComun} ${p.nombreCientifico}`.toLowerCase().includes(buscada))
    : escaneadas
  const lista = pestana === 'escaneadas' ? escaneadasVistas : digitales

  // Virtualizacion de la lista de escaneadas (web). Antes se renderizaban
  // todos los items con .map(); ahora solo los visibles + buffer.
  const parentRef = useRef(null)
  const rowVirtualizer = useVirtualizer({
    count: pestana === 'escaneadas' ? escaneadasVistas.length : 0,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 110,
    overscan: 4,
  })

  return (
    <div className="coleccion">
      <header className="coleccion-cabecera aparece">
        <h2>Mi colección</h2>
        <div className="coleccion-controles">
          <div className="coleccion-pestanas" role="tablist">
            <button role="tab" aria-selected={pestana === 'escaneadas'} className={pestana === 'escaneadas' ? 'activa' : ''}
              onClick={() => setPestana('escaneadas')}>Escaneos <b>{escaneadas.length}</b></button>
            <button role="tab" aria-selected={pestana === 'virtuales'} className={pestana === 'virtuales' ? 'activa' : ''}
              onClick={() => setPestana('virtuales')}>Invernadero <b>{digitales.length}</b></button>
          </div>
          <label className="coleccion-buscar">
            <Icono nombre="search" tam={18} />
            <input value={consulta} onChange={(e) => setConsulta(e.target.value)} placeholder="Buscar planta" aria-label="Buscar planta" />
            {consulta && <button type="button" onClick={() => setConsulta('')} aria-label="Limpiar búsqueda"><Icono nombre="close" tam={16} /></button>}
          </label>
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
            <span className="coleccion-vacio-icono"><Icono nombre={buscada ? 'search' : 'cam'} tam={34} /></span>
            <h2>{buscada ? 'Ninguna planta coincide' : 'Aún no tienes plantas'}</h2>
            <p>{buscada ? 'Prueba con otro nombre.' : 'Escanea tu primera planta para empezar tu colección.'}</p>
            {!buscada && <Link to={RUTAS.ESCANEAR} className="boton boton-principal"><Icono nombre="cam" /> Escanear</Link>}
          </div>
        ) : (
          pestana === 'escaneadas' ? (
            <div
              ref={parentRef}
              className="coleccion-virtual"
              style={{ overflow: 'auto' }}
            >
              <div style={{ height: rowVirtualizer.getTotalSize(), width: '100%', position: 'relative' }}>
                {rowVirtualizer.getVirtualItems().map((virtualItem) => {
                  const p = escaneadasVistas[virtualItem.index]
                  return (
                    <Link
                      key={p.id}
                      to={`${RUTAS.COLECCION}/${p.id}`}
                      className="planta-card"
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
                          {p.esToxicaMascotas && <span className="chip-alerta"><Icono nombre="warn" tam={12} /> Tóxica</span>}
                        </div>
                      </div>
                    </Link>
                  )
                })}
              </div>
            </div>
          ) : (
            <ColeccionVirtual digitales={digitales} consulta={consulta} onCambio={actualizarPlanta} onAbrir={abrirDigital} onAviso={avisar} />
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
      <Icono nombre="leaf" tam={28} />
      {url && <img src={url} alt={nombre} loading="lazy" onError={(e) => { e.currentTarget.style.display = 'none' }} />}
    </div>
  )
}