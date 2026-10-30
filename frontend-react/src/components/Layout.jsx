import { useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { RUTAS } from '../routes/rutas'
import { useSesion } from '../store/Sesion'
import Boton from './Boton'
import './layout.css'

const MENU = [
  { ruta: RUTAS.HOME, icono: '🏠', texto: 'Inicio' },
  { ruta: RUTAS.ESCANEAR, icono: '📷', texto: 'Escanear' },
  { ruta: RUTAS.JARDIN, icono: '🪴', texto: 'Jardín' },
  { ruta: RUTAS.COLECCION, icono: '📚', texto: 'Colección' },
  { ruta: RUTAS.PERFIL, icono: '👤', texto: 'Perfil' },
]

const TITULOS = { [RUTAS.FICHA]: 'Ficha de la planta', [RUTAS.CATALOGO]: 'Enciclopedia' }

// Marco de las paginas con sesion: menu lateral en pantallas anchas, barra inferior en moviles
export default function Layout() {
  const { usuario, cerrar } = useSesion()
  const [confirmando, setConfirmando] = useState(false)
  const irA = useNavigate()
  const { pathname } = useLocation()
  const titulo = MENU.find((m) => m.ruta === pathname)?.texto || TITULOS[pathname] || 'Plantia'

  const salir = () => {
    cerrar()
    irA(RUTAS.WELCOME, { replace: true })
  }

  return (
    <div className="layout">
      <aside className="layout-menu">
        <div className="layout-marca">🌱 Plantia</div>
        <nav>
          {MENU.map((item) => (
            <NavLink key={item.ruta} to={item.ruta} className="layout-enlace">
              <span className="layout-icono">{item.icono}</span>
              <span>{item.texto}</span>
            </NavLink>
          ))}
        </nav>
        <button className="layout-salir" onClick={() => setConfirmando(true)}>
          <span className="layout-icono">🚪</span>
          <span>Cerrar sesión</span>
        </button>
      </aside>

      <div className="layout-cuerpo">
        <header className="layout-barra">
          <h1>{titulo}</h1>
          <div className="layout-barra-derecha">
            <button className="layout-salir-movil" onClick={() => setConfirmando(true)} aria-label="Cerrar sesión">🚪</button>
            <span className="layout-avatar" title={usuario?.nombre}>{usuario?.nombre?.[0]?.toUpperCase() || '?'}</span>
          </div>
        </header>
        <main className="layout-contenido">
          <Outlet />
        </main>
      </div>

      {confirmando && (
        <div className="layout-fondo" onClick={() => setConfirmando(false)}>
          <div className="layout-dialogo aparece" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
            <h2>¿Cerrar sesión?</h2>
            <p>Volverás a la pantalla de bienvenida.</p>
            <div className="layout-dialogo-botones">
              <Boton variante="suave" onClick={() => setConfirmando(false)}>Cancelar</Boton>
              <Boton onClick={salir}>Cerrar sesión</Boton>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
