import { cabecerasAdmin } from './auth'

// Misiones diarias (tabla misiones). Solo las administra el admin; "quitar" = desactivar.
//   GET   /django/api/admin/misiones/        lista todas (activas e inactivas)
//   POST  /django/api/admin/misiones/        crea una
//   PATCH /django/api/admin/misiones/{id}/   edita o desactiva ({ activa: false })

export const TIPOS_MISION = [
  { valor: 'RIEGO', icono: '💧', texto: 'Regar plantas' },
  { valor: 'ESCANEO', icono: '📷', texto: 'Escanear plantas' },
  { valor: 'DIGITALIZAR', icono: '🪴', texto: 'Digitalizar plantas nuevas' },
  { valor: 'REFUGIO', icono: '🏠', texto: 'Refugiar en un desastre' },
  { valor: 'SOL', icono: '☀️', texto: 'Dar sol' },
  { valor: 'ABONO', icono: '🌿', texto: 'Abonar' },
  { valor: 'COLECCION', icono: '📚', texto: 'Ampliar la colección' },
]

export const textoTipoMision = (valor) => TIPOS_MISION.find((t) => t.valor === valor)?.texto || valor
export const iconoTipoMision = (valor) => TIPOS_MISION.find((t) => t.valor === valor)?.icono || '🎯'

async function pedir(ruta, opciones = {}) {
  const respuesta = await fetch(`/django/api/admin/misiones/${ruta}`, {
    ...opciones,
    headers: { 'Content-Type': 'application/json', ...cabecerasAdmin() },
  })
  const texto = await respuesta.text()
  const datos = texto ? JSON.parse(texto) : null
  if (!respuesta.ok) throw new Error(datos?.error || `Error ${respuesta.status}`)
  return datos
}

export const listarMisiones = () => pedir('')

export const crearMision = (datos) => pedir('', { method: 'POST', body: JSON.stringify(datos) })

export const editarMision = (id, cambios) => pedir(`${id}/`, { method: 'PATCH', body: JSON.stringify(cambios) })

export const cambiarEstadoMision = (id, activa) => editarMision(id, { activa })
