import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { RUTAS } from '../../../routes/rutas'
import { escaneosRecientes } from '../../../services/historial'
import Icono from '../coleccion/Iconos'
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
        <span className="coleccion-vacio-icono"><Icono nombre="clock" tam={34} /></span>
        <h2>Aún no has escaneado plantas</h2>
        <p>Cuando identifiques una planta, aparecerá aquí.</p>
        <Link to={RUTAS.ESCANEAR} className="boton boton-principal"><Icono nombre="cam" /> Escanear</Link>
      </div>
    )
  }

  return (
    <div className="historial">
      <header className="historial-cabecera">
        <div>
          <h2>Historial de escaneos</h2>
          <p>{estado.escaneos.length} reciente{estado.escaneos.length === 1 ? '' : 's'}</p>
        </div>
        <Link to={RUTAS.PERFIL} className="historial-volver">← Perfil</Link>
      </header>
      <ul className="historial-lista">
        {estado.escaneos.map((e, i) => (
          <li key={e.id} className="aparece" style={{ animationDelay: `${Math.min(i, 10) * 0.04}s` }}>
            <span className="historial-icono" style={e.statusColor ? { color: e.statusColor } : undefined}><Icono nombre="leaf" tam={22} /></span>
            <b>{e.plantName}</b>
            <span className="historial-confianza">{e.confidence}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}
