import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import Boton from '../../../components/Boton'
import Campo from '../../../components/Campo'
import { RUTAS } from '../../../routes/rutas'
import { iniciarSesion, registrarse } from '../../../services/auth'
import { useSesion } from '../../../store/Sesion'
import { coincidenPasswords, esCorreoValido, esNombreValido, esPasswordValida, LARGO_MINIMO_PASSWORD } from './validacion'
import './auth.css'

export default function Auth() {
  const [params, setParams] = useSearchParams()
  const esRegistro = params.get('modo') === 'registro'

  const [datos, setDatos] = useState({ nombre: '', email: '', password: '', confirmar: '', terminos: false })
  const [tocado, setTocado] = useState({})
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)
  const { iniciar } = useSesion()
  const irA = useNavigate()

  const cambiar = (campo) => (e) =>
    setDatos({ ...datos, [campo]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })
  const marcar = (campo) => () => setTocado({ ...tocado, [campo]: true })
  const cambiarModo = (modo) => { setParams({ modo }); setError('') }

  const errores = {
    nombre: esRegistro && tocado.nombre && !esNombreValido(datos.nombre) ? 'Escribe al menos 2 letras' : '',
    email: tocado.email && !esCorreoValido(datos.email) ? 'Correo no válido' : '',
    password: esRegistro && tocado.password && !esPasswordValida(datos.password) ? `Mínimo ${LARGO_MINIMO_PASSWORD} caracteres` : '',
    confirmar: esRegistro && tocado.confirmar && !coincidenPasswords(datos.password, datos.confirmar) ? 'Las contraseñas no coinciden' : '',
  }

  const valido = esRegistro
    ? esNombreValido(datos.nombre) && esCorreoValido(datos.email) && esPasswordValida(datos.password)
      && coincidenPasswords(datos.password, datos.confirmar) && datos.terminos
    : esCorreoValido(datos.email) && datos.password.length > 0

  const enviar = async (e) => {
    e.preventDefault()
    if (!valido) return
    setCargando(true)
    setError('')
    try {
      const usuario = esRegistro
        ? await registrarse(datos.nombre, datos.email, datos.password)
        : await iniciarSesion(datos.email, datos.password)
      iniciar(usuario)
      irA(RUTAS.HOME, { replace: true })
    } catch (err) {
      setError(mensajeDeError(err, esRegistro))
    } finally {
      setCargando(false)
    }
  }

  return (
    <main className="auth">
      <form className="auth-tarjeta aparece" onSubmit={enviar} noValidate>
        <Link to={RUTAS.WELCOME} className="auth-volver">← Volver</Link>
        <h1>{esRegistro ? 'Crea tu cuenta' : 'Bienvenido de vuelta'}</h1>

        <div className="auth-pestanas" role="tablist">
          <button type="button" role="tab" aria-selected={!esRegistro} className={!esRegistro ? 'activa' : ''}
            onClick={() => cambiarModo('login')}>Iniciar sesión</button>
          <button type="button" role="tab" aria-selected={esRegistro} className={esRegistro ? 'activa' : ''}
            onClick={() => cambiarModo('registro')}>Registrarse</button>
        </div>

        {error && <div className="auth-error" role="alert">{error}</div>}

        <div className="auth-campos">
          {esRegistro && (
            <Campo etiqueta="Nombre" value={datos.nombre} onChange={cambiar('nombre')} onBlur={marcar('nombre')}
              error={errores.nombre} autoComplete="name" />
          )}
          <Campo etiqueta="Correo" tipo="email" value={datos.email} onChange={cambiar('email')} onBlur={marcar('email')}
            error={errores.email} autoComplete="email" />
          <Campo etiqueta="Contraseña" tipo="password" value={datos.password} onChange={cambiar('password')}
            onBlur={marcar('password')} error={errores.password}
            autoComplete={esRegistro ? 'new-password' : 'current-password'} />
          {esRegistro && (
            <Campo etiqueta="Confirmar contraseña" tipo="password" value={datos.confirmar} onChange={cambiar('confirmar')}
              onBlur={marcar('confirmar')} error={errores.confirmar} autoComplete="new-password" />
          )}
        </div>

        {esRegistro && (
          <label className="auth-terminos">
            <input type="checkbox" checked={datos.terminos} onChange={cambiar('terminos')} />
            <span>Acepto los <Link to={RUTAS.TERMINOS}>términos y privacidad</Link></span>
          </label>
        )}

        <Boton type="submit" cargando={cargando} disabled={!valido}>
          {esRegistro ? 'Crear cuenta' : 'Entrar'}
        </Boton>
      </form>
    </main>
  )
}

function mensajeDeError(err, esRegistro) {
  if (err.estado === 409) return 'Ese correo ya está registrado.'
  if (err.estado === 401) return 'Correo o contraseña incorrectos.'
  if (err instanceof TypeError || err.estado >= 500) return 'No se pudo conectar con el servidor. Inténtalo de nuevo.'
  return err.message || (esRegistro ? 'No se pudo crear la cuenta.' : 'No se pudo iniciar sesión.')
}
