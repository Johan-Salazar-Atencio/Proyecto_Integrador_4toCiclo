import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { RUTAS } from '../../../routes/rutas'
import { listarColeccion } from '../../../services/coleccion'
import { urlImagen } from '../../../services/home'
import { activar, listarDigitales, plantaActiva } from '../../../store/jardinSimulado'
import './coleccion.css'

export default function Coleccion() {
  const [pestana, setPestana] = useState('escaneadas')
  const [escaneadas, setEscaneadas] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const irA = useNavigate()

  const digitales = listarDigitales()
  const activa = plantaActiva()?.name

  const cargar = useCallback(async () => {
    setCargando(true)
    setError('')
    try {
      setEscaneadas(await listarColeccion())
    } catch (err) {
      setError(err instanceof TypeError ? 'No se pudo conectar con el servidor.' : err.message)
    } finally {
      setCargando(false)
    }
  }, [])

  useEffect(() => { cargar() }, [cargar])

  const abrirDigital = (planta) => {
    activar(planta.name)
    irA(RUTAS.JARDIN)
  }

  const lista = pestana === 'escaneadas' ? escaneadas : digitales

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

      {pestana === 'escaneadas' && cargando && <div className="home-cargando"><span className="home-giro" aria-label="Cargando" /></div>}

      {pestana === 'escaneadas' && !cargando && error && (
        <div className="home-error" role="alert">
          <span>{error}</span>
          <button onClick={cargar}>Reintentar</button>
        </div>
      )}

      {!(pestana === 'escaneadas' && (cargando || error)) && (
        lista.length === 0 ? (
          <div className="coleccion-vacio aparece">
            <span>🌱</span>
            <h2>Aún no tienes plantas</h2>
            <p>{pestana === 'escaneadas' ? 'Escanea tu primera planta para empezar tu colección.' : 'Digitaliza una planta escaneada para cuidarla en tu jardín.'}</p>
            <Link to={RUTAS.ESCANEAR} className="boton boton-principal">📷 Escanear</Link>
          </div>
        ) : (
          <div className="coleccion-grilla">
            {lista.map((p, i) => (
              pestana === 'escaneadas' ? (
                <Link key={p.id} to={`${RUTAS.COLECCION}/${p.id}`} className="planta-card aparece" style={{ animationDelay: `${i * 0.05}s` }}>
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
              ) : (
                <button key={p.name} className="planta-card aparece" style={{ animationDelay: `${i * 0.05}s` }} onClick={() => abrirDigital(p)}>
                  <Foto url={urlImagen(p.imageUrl)} nombre={p.name} />
                  <div className="planta-card-datos">
                    <b>{p.name}</b>
                    <small>{p.species}</small>
                    <div className="planta-card-chips">
                      {p.name === activa && <span className="chip-activo">Activo</span>}
                    </div>
                  </div>
                </button>
              )
            ))}
            <Link to={RUTAS.ESCANEAR} className="planta-card planta-card-mas aparece">
              <span>🔍</span>
              <b>¡Descubre más plantas!</b>
            </Link>
          </div>
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
