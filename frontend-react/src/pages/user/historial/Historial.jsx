import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { RUTAS } from '../../../routes/rutas'
import { escaneosRecientes } from '../../../services/historial'
import './historial.css'

// Escaneos recientes del usuario (GET /escaneos/recientes)
export default function Historial() {
  const [estado, setEstado] = useState({ cargando: true, escaneos: [], error: '' })

  const cargar = useCallback(async () => {
    setEstado({ cargando: true, escaneos: [], error: '' })
    try {
      setEstado({ cargando: false, escaneos: await escaneosRecientes(), error: '' })
    } catch (err) {
      setEstado({ cargando: false, escaneos: [], error: err instanceof TypeError ? 'No se pudo conectar con el servidor.' : err.message })
    }
  }, [])

  useEffect(() => { cargar() }, [cargar])

  if (estado.cargando) return <div className="home-cargando"><span className="home-giro" aria-label="Cargando" /></div>

  if (estado.error) {
    return (
      <div className="home-error" role="alert">
        <span>{estado.error}</span>
        <button onClick={cargar}>Reintentar</button>
      </div>
    )
  }

  if (estado.escaneos.length === 0) {
    return (
      <div className="coleccion-vacio aparece">
        <span>🕘</span>
        <h2>Aún no has escaneado plantas</h2>
        <Link to={RUTAS.ESCANEAR} className="boton boton-principal">📷 Escanear</Link>
      </div>
    )
  }

  return (
    <div className="historial">
      <Link to={RUTAS.PERFIL} className="historial-volver">← Perfil</Link>
      <ul className="historial-lista">
        {estado.escaneos.map((e, i) => (
          <li key={e.id} className="tarjeta aparece" style={{ animationDelay: `${i * 0.04}s` }}>
            <span className="historial-punto" style={{ background: e.statusColor || 'var(--verde)' }} />
            <b>{e.plantName}</b>
            <span className="historial-confianza">{e.confidence}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}
