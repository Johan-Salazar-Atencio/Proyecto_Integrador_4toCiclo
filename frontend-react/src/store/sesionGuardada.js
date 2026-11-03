// Sesion guardada en el navegador (el movil usa el objeto Sesion)
const CLAVE = 'plantia.sesion'
const USUARIO_POR_DEFECTO = 1

export function leerSesion() {
  try {
    return JSON.parse(localStorage.getItem(CLAVE)) || null
  } catch {
    return null
  }
}

export function guardarSesion(sesion) {
  try {
    localStorage.setItem(CLAVE, JSON.stringify(sesion))
  } catch {
    /* sin almacenamiento: la sesion dura hasta recargar */
  }
}

export function borrarSesion() {
  try {
    localStorage.removeItem(CLAVE)
  } catch {
    /* nada que borrar */
  }
}

export function obtenerUsuarioId() {
  return leerSesion()?.id ?? USUARIO_POR_DEFECTO
}
