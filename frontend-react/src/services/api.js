import { RUTAS } from '../routes/rutas'
import { borrarSesion, obtenerUsuarioId, obtenerToken } from '../store/sesionGuardada'

const BASE = '/api/v1'

// Llamada JSON a Spring Boot. Agrega X-User-Id y X-Session-Token como el interceptor del movil.
export async function llamar(ruta, { metodo = 'GET', cuerpo, formData, senal } = {}) {
  const cabeceras = {}
  const usuarioId = obtenerUsuarioId()
  if (usuarioId != null) cabeceras['X-User-Id'] = String(usuarioId)
  const token = obtenerToken()
  if (token) cabeceras['X-Session-Token'] = token
  let body
  if (formData) {
    body = formData
  } else if (cuerpo !== undefined) {
    cabeceras['Content-Type'] = 'application/json'
    body = JSON.stringify(cuerpo)
  }

  const respuesta = await fetch(`${BASE}${ruta}`, { method: metodo, headers: cabeceras, body, signal: senal })
  if (respuesta.status === 204) return null

  const datos = leerJson(await respuesta.text())
  if (!respuesta.ok) {
    if (respuesta.status === 401) sesionCaducada()
    // Spring responde {"error": "..."} (AuthFilter y ManejadorErrores)
    const error = new Error(datos?.error || datos?.mensaje || datos?.message || `Error ${respuesta.status}`)
    error.estado = respuesta.status
    throw error
  }
  return datos
}

// El proxy o un servidor caido pueden devolver HTML: no debe romper con un SyntaxError
function leerJson(texto) {
  if (!texto) return null
  try {
    return JSON.parse(texto)
  } catch {
    return null
  }
}

// 401 = sesion invalida o caducada: se borra y se vuelve al acceso (salvo si ya estamos fuera)
const RUTAS_PUBLICAS = [RUTAS.WELCOME, RUTAS.ACCESO, RUTAS.TERMINOS]
function sesionCaducada() {
  borrarSesion()
  if (!RUTAS_PUBLICAS.includes(window.location.pathname)) window.location.assign(RUTAS.ACCESO)
}
