import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import '../../../components/admin/admin-ui.css'
import { RUTAS, RUTAS_ADMIN } from '../../../routes/rutas'
import { iniciarSesionAdmin } from '../../../services/admin/auth'
import { useSesionAdmin } from '../../../store/SesionAdmin'
import './login.css'

// Planta pixel art de la ilustracion (cuadricula de 16 x 16)
function PlantaPixel() {
  return (
    <svg className="login-planta" viewBox="0 0 16 16" shapeRendering="crispEdges" aria-hidden="true">
      <rect x="7" y="5" width="2" height="7" fill="var(--tallo)" />
      <rect x="6" y="1" width="4" height="1" fill="var(--hoja)" />
      <rect x="5" y="2" width="6" height="3" fill="var(--hoja)" />
      <rect x="9" y="3" width="2" height="2" fill="var(--hoja-2)" />
      <rect x="2" y="5" width="5" height="2" fill="var(--hoja)" />
      <rect x="1" y="6" width="2" height="2" fill="var(--hoja)" />
      <rect x="3" y="7" width="4" height="1" fill="var(--hoja-2)" />
      <rect x="9" y="6" width="5" height="2" fill="var(--hoja)" />
      <rect x="13" y="7" width="2" height="2" fill="var(--hoja)" />
      <rect x="9" y="8" width="4" height="1" fill="var(--hoja-2)" />
      <rect x="4" y="12" width="8" height="1" fill="var(--maceta-2)" />
      <rect x="5" y="13" width="6" height="3" fill="var(--maceta)" />
      <rect x="6" y="14" width="1" height="1" fill="#2a1608" />
      <rect x="9" y="14" width="1" height="1" fill="#2a1608" />
    </svg>
  )
}

export default function LoginAdmin() {
  const { admin, iniciar } = useSesionAdmin()
  const irA = useNavigate()
  const [datos, setDatos] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)

  if (admin) return <Navigate to={RUTAS_ADMIN.DASHBOARD} replace />

  const cambiar = (campo) => (e) => setDatos({ ...datos, [campo]: e.target.value })

  const enviar = async (e) => {
    e.preventDefault()
    setCargando(true)
    setError('')
    try {
      iniciar(await iniciarSesionAdmin(datos.email, datos.password))
      irA(RUTAS_ADMIN.DASHBOARD, { replace: true })
    } catch (err) {
      setError(err.estado === 401 ? 'Correo o contraseña incorrectos, o la cuenta no es de administrador.' : err.message || 'No se pudo iniciar sesión.')
    } finally {
      setCargando(false)
    }
  }

  return (
    <main className="login-admin">
      <section className="login-arte" aria-hidden="true">
        <i className="login-nube" style={{ left: '14%', top: '34%' }} />
        <i className="login-nube" style={{ right: '16%', top: '52%' }} />
        <h2>Cuida el campo, cuida el juego.</h2>
        <div className="login-cesped" />
        <PlantaPixel />
      </section>

      <section className="login-lado">
        <form className="login-caja aparece" onSubmit={enviar} noValidate>
          <span className="admin-etiqueta">Admin Studio</span>
          <h1>Panel de administración</h1>
          <p className="login-sub">Acceso solo para el equipo de Plantia.</p>

          {error && <div className="ad-error" role="alert">{error}</div>}

          <div className="ad-fld">
            <label htmlFor="admin-correo">Correo</label>
            <input id="admin-correo" className="ad-in" type="email" placeholder="admin@plantia.app" value={datos.email} onChange={cambiar('email')} autoComplete="username" />
          </div>
          <div className="ad-fld">
            <label htmlFor="admin-clave">Contraseña</label>
            <input id="admin-clave" className="ad-in" type="password" placeholder="••••••••••" value={datos.password} onChange={cambiar('password')} autoComplete="current-password" />
          </div>

          <button type="submit" className="ad-btn login-entrar" disabled={cargando}>{cargando ? 'Entrando…' : 'Entrar al panel'}</button>

          <p className="login-aviso">Acceso real contra Django: solo usuarios con rol ADMIN.</p>
          <Link to={RUTAS.WELCOME} className="login-volver">← Volver a Plantia</Link>
        </form>
      </section>
    </main>
  )
}
