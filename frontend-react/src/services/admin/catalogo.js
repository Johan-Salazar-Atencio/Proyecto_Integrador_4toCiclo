import { obtenerAdminId } from './auth'

// Catalogo de especies para el panel. Lectura y escritura son REALES contra Django.
// GET  /django/api/catalog/              -> lista de especies
// GET  /django/api/catalog/{id}/cuidados -> cuidados de una especie (Spring, via proxy /api)
// PATCH /django/api/admin/catalogo/{id}/ { ...campos, publicada }  (requiere X-Admin-Id)

// Misma forma sin importar si viene de Django (snake_case) o de Spring (camelCase)
const normalizar = (p) => ({
  id: p.id_catalogo ?? p.id,
  nombreComun: p.nombre_comun ?? p.nombreComun,
  nombreCientifico: p.nombre_cientifico ?? p.nombreCientifico,
  riegoDias: p.frecuencia_riego_dias ?? p.frecuenciaRiegoDias,
  solHoras: p.frecuencia_sol_horas ?? p.frecuenciaSolHoras,
  tempMin: p.temperatura_min_c ?? p.temperaturaMinC,
  tempMax: p.temperatura_max_c ?? p.temperaturaMaxC,
  toxica: Boolean(p.es_toxica_mascotas ?? p.esToxicaMascotas),
  publicada: p.publicada ?? true,
})

// Mapeo inverso: campos del formulario (camelCase) -> columnas Django (snake_case)
const aDjango = (campos) => {
  const out = {}
  if (campos.nombreComun !== undefined) out.nombre_comun = campos.nombreComun
  if (campos.nombreCientifico !== undefined) out.nombre_cientifico = campos.nombreCientifico
  if (campos.riegoDias !== undefined) out.frecuencia_riego_dias = campos.riegoDias
  if (campos.solHoras !== undefined) out.frecuencia_sol_horas = campos.solHoras
  if (campos.tempMin !== undefined) out.temperatura_min_c = campos.tempMin
  if (campos.tempMax !== undefined) out.temperatura_max_c = campos.tempMax
  if (campos.toxica !== undefined) out.es_toxica_mascotas = campos.toxica
  if (campos.publicada !== undefined) out.publicada = campos.publicada
  return out
}

export async function listarEspecies() {
  const respuesta = await fetch('/django/api/catalog/')
  if (!respuesta.ok) throw new Error(`Django respondio ${respuesta.status}`)
  const lista = await respuesta.json()
  const especies = lista
    .map(normalizar)
    .sort((a, b) => a.nombreComun.localeCompare(b.nombreComun, 'es'))
  return { especies, origen: 'django' }
}

// Secciones de cuidado de una especie (lectura real, Spring)
export async function cuidadosDe(id) {
  try {
    const respuesta = await fetch(`/api/v1/catalogo/${id}/cuidados`, { headers: { 'X-User-Id': '1' } })
    if (!respuesta.ok) return []
    return await respuesta.json()
  } catch {
    return []
  }
}

// REAL: guarda los cambios de una especie en la BD (Django)
export async function guardarEspecie(id, campos) {
  const adminId = obtenerAdminId()
  const respuesta = await fetch(`/django/api/admin/catalogo/${id}/`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', 'X-Admin-Id': String(adminId ?? '') },
    body: JSON.stringify(aDjango(campos)),
  })
  if (!respuesta.ok) {
    const texto = await respuesta.text()
    throw new Error(texto || `Error ${respuesta.status}`)
  }
  return respuesta.json()
}

// Ya no hay cambios locales: la BD es la fuente de verdad
export function descartarCambios() {
  // no-op: recargar la lista desde el servidor
}
