// Acceso del administrador contra Django (endpoint real).
//
// POST /django/api/admin/login { email, password }
//   -> 200 { id, nombre, email, rol: "ADMIN" }
//   -> 401 credenciales invalidas, 403 si el usuario no es ADMIN
//
// La sesion se guarda en sessionStorage (se borra al cerrar el navegador).

const CLAVE = 'plantia.admin'

export function leerAdmin() {
  try { return JSON.parse(localStorage.getItem(CLAVE) || 'null') } catch { return null }
}

export function guardarAdmin(admin) {
  try { localStorage.setItem(CLAVE, JSON.stringify(admin)) } catch { /* sin almacenamiento */ }
}

export function borrarAdmin() {
  try { localStorage.removeItem(CLAVE) } catch { /* nada que borrar */ }
}

export function obtenerAdminId() {
  return leerAdmin()?.id ?? null
}

export async function iniciarSesionAdmin(email, password) {
  const respuesta = await fetch('/django/api/admin/login/', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: email.trim(), password }),
  })
  const texto = await respuesta.text()
  const datos = texto ? JSON.parse(texto) : null
  if (!respuesta.ok) {
    const error = new Error(datos?.error || datos?.mensaje || `Error ${respuesta.status}`)
    error.estado = respuesta.status
    throw error
  }
  guardarAdmin(datos)
  return datos
}

export function cerrarSesionAdmin() {
  borrarAdmin()
}

// Estado del motor de IA / Django para el indicador de la barra superior
export async function estadoDjango() {
  try {
    const respuesta = await fetch('/django/api/health')
    return respuesta.ok
  } catch {
    return false
  }
}
