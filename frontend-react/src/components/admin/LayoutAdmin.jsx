import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { RUTAS_ADMIN } from '../../routes/rutas'
import { estadoDjango } from '../../services/admin/auth'
import { useSesionAdmin } from '../../store/SesionAdmin'
import './layout-admin.css'

const MENU = [
  { ruta: RUTAS_ADMIN.DASHBOARD, icono: '📊', texto: 'Dashboard' },
  { ruta: RUTAS_ADMIN.EVENTOS, icono: '⚠️', texto: 'Lanzador de desastres' },
  { ruta: RUTAS_ADMIN.CATALOGO, icono: '📖', texto: 'Catálogo y moderación' },
]

const MS_REVISION = 15000

// Marco del panel de administracion: menu lateral, barra superior con el estado de Django
export default function LayoutAdmin() {
  const { admin, cerrar } = useSesionAdmin()
  const irA = useNavigate()
  const { pathname } = useLocation()
  const [enLinea, setEnLinea] = useState(null)

  // Revisa cada tanto si Django responde (indicador "API IA")
  useEffect(() => {
    let vivo = true
    const revisar = async () => { const ok = await estadoDjango(); if (vivo) setEnLinea(ok) }
    revisar()
    const id = setInterval(revisar, MS_REVISION)
    return () => { vivo = false; clearInterval(id) }
  }, [])

  const titulo = MENU.find((m) => pathname.startsWith(m.ruta))?.texto || 'Panel'

  const salir = () => {
    cerrar()
    irA(RUTAS_ADMIN.LOGIN, { replace: true })
  }

  return (
    <div className="admin">
      <aside className="admin-menu">
        <div className="admin-marca">
          <span>🌱</span>
          <div>
            <b>Plantia</b>
            <small>ADMIN STUDIO</small>
          </div>
        </div>
        <p className="admin-seccion">ADMINISTRACIÓN</p>
        <nav>
          {MENU.map((item) => (
            <NavLink key={item.ruta} to={item.ruta} className="admin-enlace">
              <span>{item.icono}</span>
              <span>{item.texto}</span>
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="admin-cuerpo">
        <header className="admin-barra">
          <h1>{titulo}</h1>
          <div className="admin-barra-derecha">
            <span className={`admin-estado ${enLinea === null ? '' : enLinea ? 'admin-estado-ok' : 'admin-estado-fallo'}`}>
              <i /> API IA: {enLinea === null ? '…' : enLinea ? 'Online' : 'Sin conexión'}
            </span>
            <div className="admin-persona">
              <span className="admin-avatar">{admin?.nombre?.[0] || 'A'}</span>
              <div>
                <b>{admin?.nombre}</b>
                <small>Master Operator</small>
              </div>
            </div>
            <button className="admin-salir" onClick={salir}>Salir</button>
          </div>
        </header>
        <main className="admin-contenido">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
