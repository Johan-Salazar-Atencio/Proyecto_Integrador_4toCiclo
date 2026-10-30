import { llamar } from './api'

// Cada funcion devuelve el JSON o null si el servidor responde 204 (sin contenido)
export const obtenerUsuario = () => llamar('/usuarios/me')
export const obtenerEventoActivo = () => llamar('/eventos/activo')
export const obtenerPlantaActiva = () => llamar('/plantas/activa')
export const obtenerMisionDiaria = () => llamar('/misiones/diaria')

// Las fotos pueden venir como http://localhost:8080/files/... o relativas.
// Se dejan en el mismo origen para que el proxy de Vite las sirva.
export function urlImagen(url) {
  if (!url) return null
  const sinHost = url.replace(/^https?:\/\/(localhost|127\.0\.0\.1):\d+/, '')
  return /^https?:\/\//.test(sinHost) || sinHost.startsWith('/') ? sinHost : `/${sinHost}`
}
