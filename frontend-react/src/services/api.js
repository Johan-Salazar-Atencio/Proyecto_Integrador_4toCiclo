import { obtenerUsuarioId } from '../store/sesionGuardada'

const BASE = '/api/v1'

// Llamada JSON a Spring Boot. Agrega X-User-Id como el interceptor del movil.
export async function llamar(ruta, { metodo = 'GET', cuerpo, formData } = {}) {
  const cabeceras = { 'X-User-Id': String(obtenerUsuarioId()) }
  let body
  if (formData) {
    body = formData
  } else if (cuerpo !== undefined) {
    cabeceras['Content-Type'] = 'application/json'
    body = JSON.stringify(cuerpo)
  }

  const respuesta = await fetch(`${BASE}${ruta}`, { method: metodo, headers: cabeceras, body })
  if (respuesta.status === 204) return null

  const texto = await respuesta.text()
  const datos = texto ? JSON.parse(texto) : null
  if (!respuesta.ok) {
    const error = new Error(datos?.mensaje || datos?.message || `Error ${respuesta.status}`)
    error.estado = respuesta.status
    throw error
  }
  return datos
}
