// Sesion guardada en el navegador (el movil usa el objeto Sesion)
const CLAVE = 'plantia.sesion'

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

// null sin sesion: antes se usaba el usuario 1 por defecto y la web actuaba como el
export function obtenerUsuarioId() {
  return leerSesion()?.id ?? null
}

export function obtenerToken() {
  return leerSesion()?.sessionToken ?? ''
}
