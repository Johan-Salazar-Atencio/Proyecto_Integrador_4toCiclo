import { cabecerasAdmin } from './auth'

// Logros del juego. Solo los administra el admin; "quitar" = desactivar (no se borran).
//   GET   /django/api/admin/logros/            lista todos (activos e inactivos), con "desbloqueos"
//   POST  /django/api/admin/logros/            crea uno (la respuesta puede traer "advertencia")
//   PATCH /django/api/admin/logros/{id}/       edita o desactiva ({ activo: false }); el "codigo" no se cambia
//   GET   /django/api/admin/logros/opciones/   condiciones (con explicacion y parametro), rarezas e iconos sugeridos

export const RAREZAS = [
  { valor: 'COMUN', texto: 'Común', color: 'var(--texto-suave)' },
  { valor: 'RARA', texto: 'Rara', color: 'var(--agua)' },
  { valor: 'EPICA', texto: 'Épica', color: 'var(--abono)' },
  { valor: 'LEGENDARIA', texto: 'Legendaria', color: 'var(--luz)' },
]
export const textoRareza = (v) => RAREZAS.find((r) => r.valor === v)?.texto || v
export const colorRareza = (v) => RAREZAS.find((r) => r.valor === v)?.color || 'var(--texto-suave)'

// Convierte el error 400 de DRF ({campo: ["..."]} o {error: "..."}) en un texto legible
function mensajeError(datos, estado) {
  if (!datos || typeof datos !== 'object') return `Error ${estado}`
  if (datos.error || datos.detail || datos.mensaje) return datos.error || datos.detail || datos.mensaje
  const partes = Object.entries(datos).map(([campo, msg]) => {
    const texto = Array.isArray(msg) ? msg.join(' ') : typeof msg === 'string' ? msg : JSON.stringify(msg)
    return campo === 'non_field_errors' ? texto : `${campo}: ${texto}`
  })
  return partes.join(' · ') || `Error ${estado}`
}

async function pedir(ruta, opciones = {}) {
  let respuesta
  try {
    respuesta = await fetch(`/django/api/admin/logros/${ruta}`, {
      ...opciones,
      headers: { 'Content-Type': 'application/json', ...cabecerasAdmin() },
    })
  } catch {
    throw new Error('No se pudo conectar con Django.')
  }
  const texto = await respuesta.text()
  let datos = null
  try { datos = texto ? JSON.parse(texto) : null } catch { /* el proxy devolvio HTML */ }
  if (!respuesta.ok) {
    const error = new Error(mensajeError(datos, respuesta.status))
    error.estado = respuesta.status
    throw error
  }
  return datos
}

export const listarLogros = () => pedir('')

export const obtenerOpciones = () => pedir('opciones/')

export const crearLogro = (datos) => pedir('', { method: 'POST', body: JSON.stringify(datos) })

export const editarLogro = (id, cambios) => pedir(`${id}/`, { method: 'PATCH', body: JSON.stringify(cambios) })

export const cambiarEstadoLogro = (id, activo) => editarLogro(id, { activo })

// Normaliza /opciones/ (tolera nombres de campo distintos mientras el contrato final no este cerrado)
export function normalizarOpciones(o) {
  const lista = (x) => (Array.isArray(x) ? x : [])
  const condiciones = lista(o?.condiciones).map((c) => (typeof c === 'string' ? { codigo: c, texto: c, explicacion: '', requiereParametro: false, valores: [] } : {
    codigo: c.codigo ?? c.valor ?? c.clave,
    texto: c.texto ?? c.nombre ?? c.titulo ?? c.codigo ?? c.valor,
    explicacion: c.explicacion ?? c.descripcion ?? '',
    requiereParametro: Boolean(c.requiere_parametro ?? c.requiereParametro ?? c.pide_parametro ?? c.parametro_requerido),
    valores: lista(c.valores_permitidos ?? c.valoresPermitidos ?? c.valores),
  }))
  const rarezas = lista(o?.rarezas).map((r) => (typeof r === 'string' ? r : r.codigo ?? r.valor))
  const iconos = lista(o?.iconos ?? o?.iconos_sugeridos).map((i) => (typeof i === 'string' ? i : i.codigo ?? i.valor))
  return { condiciones, rarezas: rarezas.length ? rarezas : RAREZAS.map((r) => r.valor), iconos }
}
