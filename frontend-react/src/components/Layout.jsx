import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { RUTAS } from '../routes/rutas'
import { useSesion } from '../store/Sesion'
import { avisarSiToca } from '../store/preferencias'
import { obtenerUsuario, urlImagen } from '../services/home'
import { AVISO_SALDO } from '../services/saldo'
import { iniciales, porcentajeXp } from '../pages/user/home/xp'
import Boton from './Boton'
import Icono from './Icono'
import './layout.css'

// Orden de la barra: en el movil el boton Escanear queda al centro
const MENU = [
  { ruta: RUTAS.HOME, icono: 'home', texto: 'Inicio' },
  { ruta: RUTAS.COLECCION, icono: 'grid', texto: 'Colección' },
  { ruta: RUTAS.ESCANEAR, icono: 'cam', texto: 'Escanear', escanear: true },
  { ruta: RUTAS.TIENDA, icono: 'bag', texto: 'Tienda' },
  { ruta: RUTAS.PERFIL, icono: 'user', texto: 'Perfil' },
]

const TITULOS = [
  [RUTAS.EDITAR_DATOS, 'Editar datos'],
  [RUTAS.FICHA, 'Ficha de la planta'],
  [RUTAS.CATALOGO, 'Enciclopedia'],
  [RUTAS.HISTORIAL, 'Historial'],
  [RUTAS.JARDIN, 'Jardín virtual'],
  [RUTAS.MISIONES, 'Misiones'],
  [RUTAS.LOGROS, 'Logros'],
  [RUTAS.TIENDA, 'Tienda'],
]

// Marco de las paginas con sesion: barra superior flotante en pantallas anchas y barra inferior flotante en moviles
export default function Layout() {
  const { usuario, cerrar } = useSesion()
  const [confirmando, setConfirmando] = useState(false)
  const [perfil, setPerfil] = useState(null)
  const irA = useNavigate()
  const { pathname } = useLocation()
  // Recordatorio diario (si se activo en el perfil): se revisa al abrir y cada minuto
  useEffect(() => {
    avisarSiToca()
    const id = setInterval(() => avisarSiToca(), 60000)
    return () => clearInterval(id)
  }, [])
  // Hojas y XP de la barra: se refrescan al cambiar de pagina y tras cada compra, cuidado o escaneo
  useEffect(() => {
    let vivo = true
    const refrescar = () => obtenerUsuario().then((u) => { if (vivo && u) setPerfil(u) }).catch(() => {})
    refrescar()
    window.addEventListener(AVISO_SALDO, refrescar)
    return () => { vivo = false; window.removeEventListener(AVISO_SALDO, refrescar) }
  }, [pathname])
  const titulo = TITULOS.find(([ruta]) => pathname.startsWith(ruta))?.[1] || MENU.find((m) => pathname.startsWith(m.ruta))?.texto || 'Plantia'

  const salir = () => {
    cerrar()
    irA(RUTAS.WELCOME, { replace: true })
  }

  const nombre = perfil?.name || usuario?.nombre
  const foto = urlImagen(perfil?.profileImageUrl)

  return (
    <div className="layout">
      <header className="layout-barra">
        <Link to={RUTAS.HOME} className="layout-marca">Plantia</Link>
        <nav className="layout-nav" aria-label="Principal">
          {MENU.map((item) => (
            <NavLink key={item.ruta} to={item.ruta} className={({ isActive }) => `layout-enlace ${item.escanear ? 'layout-escanear' : ''} ${isActive ? 'activo' : ''}`}>
              <span className="layout-icono"><Icono nombre={item.icono} /></span>
              <span className="layout-texto">{item.texto}</span>
            </NavLink>
          ))}
        </nav>
        <div className="layout-derecha">
          <Link to={RUTAS.TIENDA} className="layout-hojas" title="Ir a la tienda">
            <Icono nombre="leaf" /> <span>{perfil?.leaves ?? '–'}</span>
          </Link>
          <button className="layout-salir" onClick={() => setConfirmando(true)} aria-label="Cerrar sesión" title="Cerrar sesión">
            <Icono nombre="out" />
          </button>
          <Link to={RUTAS.PERFIL} className="layout-xp" style={{ '--p': `${porcentajeXp(perfil)}%` }} title={nombre} aria-label="Perfil">
            <b>
              {foto && <img src={foto} alt="" onError={(e) => { e.currentTarget.style.display = 'none' }} />}
              <span>{iniciales(nombre)}</span>
            </b>
          </Link>
        </div>
      </header>

      <main className="layout-contenido">
        <h1 className="solo-lectores">{titulo}</h1>
        <Outlet />
      </main>

      {confirmando && (
        <div className="layout-fondo" onClick={() => setConfirmando(false)}>
          <div className="layout-dialogo aparece" role="dialog" aria-modal="true" aria-labelledby="layout-salir-titulo" onClick={(e) => e.stopPropagation()}>
            <h2 id="layout-salir-titulo">¿Cerrar sesión?</h2>
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
