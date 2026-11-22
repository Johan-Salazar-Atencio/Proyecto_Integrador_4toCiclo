import { useCallback, useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { RUTAS_ADMIN } from '../../routes/rutas'
import { estadoDjango } from '../../services/admin/auth'
import { AVISO_EVENTOS, listarEventos } from '../../services/admin/eventos'
import { useSesionAdmin } from '../../store/SesionAdmin'
import IconoAdmin from './IconoAdmin'
import './admin-ui.css'
import './layout-admin.css'

const MENU = [
  { ruta: RUTAS_ADMIN.DASHBOARD, icono: 'chart', texto: 'Dashboard' },
  { ruta: RUTAS_ADMIN.EVENTOS, icono: 'alert', texto: 'Eventos y desastres', insignia: true },
  { ruta: RUTAS_ADMIN.MISIONES, icono: 'target', texto: 'Misiones diarias' },
  { ruta: RUTAS_ADMIN.LOGROS, icono: 'trophy', texto: 'Logros' },
  { ruta: RUTAS_ADMIN.TIENDA, icono: 'bag', texto: 'Tienda' },
  { ruta: RUTAS_ADMIN.JUEGO, icono: 'leaf', texto: 'Economía del juego' },
  { ruta: RUTAS_ADMIN.CATALOGO, icono: 'book', texto: 'Catálogo y moderación' },
]

const MS_REVISION = 15000

// Marco del panel de administracion: menu lateral flotante (cajon en pantallas angostas)
export default function LayoutAdmin() {
  const { admin, cerrar } = useSesionAdmin()
  const irA = useNavigate()
  const { pathname } = useLocation()
  const [enLinea, setEnLinea] = useState(null)
  const [activos, setActivos] = useState(0)
  const [abierto, setAbierto] = useState(false)

  const revisar = useCallback(async () => {
    const ok = await estadoDjango()
    setEnLinea(ok)
    try {
      const ahora = Date.now()
      const eventos = await listarEventos()
      setActivos(eventos.filter((e) => e.estado === 'ACTIVO' && new Date(e.fecha_fin) > ahora).length)
    } catch { /* sin Django: la insignia queda como estaba */ }
  }, [])

  // Revisa cada tanto si Django responde y cuantos eventos hay activos
  useEffect(() => {
    revisar()
    const id = setInterval(revisar, MS_REVISION)
    window.addEventListener(AVISO_EVENTOS, revisar)
    return () => { clearInterval(id); window.removeEventListener(AVISO_EVENTOS, revisar) }
  }, [revisar])

  useEffect(() => { setAbierto(false) }, [pathname])

  const titulo = MENU.find((m) => pathname.startsWith(m.ruta))?.texto || 'Panel'

  const salir = () => {
    cerrar()
    irA(RUTAS_ADMIN.LOGIN, { replace: true })
  }

  return (
    <div className="admin">
      <header className="admin-barra-movil">
        <button type="button" className="ad-ib" onClick={() => setAbierto(true)} aria-label="Abrir menú"><IconoAdmin n="menu" /></button>
        <b>{titulo}</b>
        <span className="admin-wm">Plant<span>ia</span></span>
      </header>

      {abierto && <div className="admin-velo" onClick={() => setAbierto(false)} />}

      <aside className={`ad-card admin-menu ${abierto ? 'abierto' : ''}`}>
        <div className="admin-marca">
          <span className="admin-wm">Plant<span>ia</span></span>
          <span className="admin-etiqueta">Admin</span>
          <button type="button" className="ad-ib admin-cerrar-menu" onClick={() => setAbierto(false)} aria-label="Cerrar menú"><IconoAdmin n="x" /></button>
        </div>
        <nav aria-label="Secciones del panel">
          {MENU.map((item) => (
            <NavLink key={item.ruta} to={item.ruta} className="admin-enlace">
              <IconoAdmin n={item.icono} />
              <span>{item.texto}</span>
              {item.insignia && activos > 0 && <span className="admin-insignia" aria-label={`${activos} evento activo`}>{activos}</span>}
            </NavLink>
          ))}
        </nav>

        <div className="admin-yo">
          <span className={`admin-estado ${enLinea === null ? '' : enLinea ? 'admin-estado-ok' : 'admin-estado-fallo'}`}>
            <i /> API IA: {enLinea === null ? '…' : enLinea ? 'Online' : 'Sin conexión'}
          </span>
          <div className="admin-persona">
            <span className="admin-avatar">{admin?.nombre?.[0] || 'A'}</span>
            <div>
              <b>{admin?.nombre || 'Administrador'}</b>
              <small>Rol ADMIN</small>
            </div>
          </div>
          <button type="button" className="admin-salir" onClick={salir}><IconoAdmin n="out" />Cerrar sesión</button>
        </div>
      </aside>

      <main className="admin-contenido">
        <Outlet />
      </main>
    </div>
  )
}
