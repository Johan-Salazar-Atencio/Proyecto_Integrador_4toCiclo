import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { RUTAS } from '../../../routes/rutas'
import { obtenerUsuario } from '../../../services/home'
import { useSesion } from '../../../store/Sesion'
import './perfil.css'

// Perfil del usuario (GET /usuarios/me)
export default function Perfil() {
  const { usuario: sesion, cerrar } = useSesion()
  const irA = useNavigate()
  const [estado, setEstado] = useState({ cargando: true, datos: null, error: '' })

  const cargar = useCallback(async () => {
    setEstado({ cargando: true, datos: null, error: '' })
    try {
      const datos = await obtenerUsuario()
      setEstado({ cargando: false, datos, error: datos ? '' : 'No existe un usuario con ese id' })
    } catch {
      setEstado({ cargando: false, datos: null, error: 'No se pudo cargar el perfil' })
    }
  }, [])

  useEffect(() => { cargar() }, [cargar])

  const { datos } = estado
  const nombre = datos?.name || sesion?.nombre || 'Jardinero'
  const faltan = datos ? Math.max(0, datos.experienceToNextLevel - datos.experience) : 0

  return (
    <div className="perfil">
      <section className="perfil-cabecera tarjeta aparece">
        <span className="perfil-avatar">{nombre[0]?.toUpperCase()}</span>
        <h2>{nombre}</h2>
        {sesion?.email && <p>{sesion.email}</p>}
        {datos && <span className="perfil-nivel">Nivel {datos.level} · {datos.badge}</span>}

        {estado.cargando && <span className="home-giro" aria-label="Cargando" />}
        {estado.error && (
          <div className="home-error" role="alert">
            <span>{estado.error}</span>
            <button onClick={cargar}>Reintentar</button>
          </div>
        )}

        {datos && (
          <div className="perfil-datos">
            <Dato valor={datos.leaves} texto="🍃 Hojas" />
            <Dato valor={datos.experience} texto="⭐ XP" />
            <Dato valor={faltan} texto="🎯 Faltan" />
          </div>
        )}
      </section>

      <nav className="perfil-enlaces aparece" style={{ animationDelay: '0.1s' }}>
        <Link to={RUTAS.HISTORIAL} className="tarjeta"><span>🕘</span> Historial de escaneos <i>›</i></Link>
        <Link to={RUTAS.CATALOGO} className="tarjeta"><span>📖</span> Enciclopedia <i>›</i></Link>
        <Link to={RUTAS.TERMINOS} className="tarjeta"><span>📄</span> Términos y privacidad <i>›</i></Link>
        <button className="tarjeta perfil-salir" onClick={() => { cerrar(); irA(RUTAS.WELCOME, { replace: true }) }}>
          <span>🚪</span> Cerrar sesión
        </button>
      </nav>
    </div>
  )
}

function Dato({ valor, texto }) {
  return (
    <div className="perfil-dato">
      <b>{valor}</b>
      <small>{texto}</small>
    </div>
  )
}
