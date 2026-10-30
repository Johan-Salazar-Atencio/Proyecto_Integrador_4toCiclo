import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import Boton from '../../../components/Boton'
import Campo from '../../../components/Campo'
import { RUTAS, RUTAS_ADMIN } from '../../../routes/rutas'
import { iniciarSesionAdmin } from '../../../services/admin/auth'
import { useSesionAdmin } from '../../../store/SesionAdmin'
import './login.css'

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
      setError(err.estado === 403 ? 'Tu cuenta no tiene permisos de administrador.' : err.message || 'No se pudo iniciar sesión.')
    } finally {
      setCargando(false)
    }
  }

  return (
    <main className="login-admin">
      <form className="login-admin-tarjeta aparece" onSubmit={enviar} noValidate>
        <div className="login-admin-marca">
          <span>🌱</span>
          <div>
            <b>Plantia</b>
            <small>ADMIN STUDIO</small>
          </div>
        </div>

        <h1>Panel de administración</h1>
        <p className="login-admin-sub">Acceso solo para el equipo de Plantia.</p>

        {error && <div className="auth-error" role="alert">{error}</div>}

        <Campo etiqueta="Correo" tipo="email" value={datos.email} onChange={cambiar('email')} autoComplete="username" />
        <Campo etiqueta="Contraseña" tipo="password" value={datos.password} onChange={cambiar('password')} autoComplete="current-password" />

        <Boton type="submit" cargando={cargando}>Entrar al panel</Boton>

        <p className="login-admin-aviso">Modo de prueba: el acceso aún no está conectado con Django, entra cualquier correo.</p>
        <Link to={RUTAS.WELCOME} className="login-admin-volver">← Volver a Plantia</Link>
      </form>
    </main>
  )
}
