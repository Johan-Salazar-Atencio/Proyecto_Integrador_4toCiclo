import { cabecerasAdmin } from './auth'
import { cuerpoEspecieNueva, errorDeRespuesta } from './respuestas.js'

// Catalogo de especies para el panel. Lectura y escritura son REALES contra Django.
// POST  /django/api/admin/catalogo/      -> crea una especie (nombres, riego y sol obligatorios)
// GET  /django/api/admin/catalogo/       -> lista de especies, publicadas o no (requiere admin; QA-20)
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
  imagen: p.sprite_base_url ?? p.spriteBaseUrl ?? '',
  publicada: p.publicada ?? true,
  escaneos: p.escaneos ?? 0, // veces que la IA identificó la especie (solo lo da el listado del admin)
})

// Dibujo de la miniatura: el catálogo no guarda el aspecto de la planta, así que se elige por el nombre
// (cactus, helecho, palmera…) y, si no, por el nombre científico: la misma especie siempre se ve igual.
const FORMAS_POR_NOMBRE = [
  [/cact|opuntia|echinocactus/i, 'cactus'], [/helecho|fern|nephrolepis/i, 'helecho'], [/palm|areca|chamaedorea/i, 'palmera'],
  [/suculenta|aloe|echeveria|crassula|sedum|haworthia/i, 'suculenta'], [/monstera|filodendro|philodendron|potos|epipremnum|hiedra|hedera/i, 'trepadora'],
  [/rosa|ficus|arbol|árbol|limon|limón|naranj/i, 'arbol'],
]
const FORMAS = ['arbusto', 'roseta', 'monstera', 'arbusto', 'trepadora', 'helecho']
export function aparienciaDeEspecie(especie) {
  const texto = `${especie.nombreComun} ${especie.nombreCientifico}`
  const porNombre = FORMAS_POR_NOMBRE.find(([patron]) => patron.test(texto))
  let suma = 0
  for (const letra of especie.nombreCientifico || '') suma = (suma * 31 + letra.charCodeAt(0)) % 9973
  const forma = porNombre ? porNombre[1] : FORMAS[suma % FORMAS.length]
  const verdes = ['#2f7d4a', '#3f8a4a', '#4f9a52', '#4a9a4a', '#5f9e4a']
  return { forma, colorHoja: verdes[suma % verdes.length], tieneFlor: suma % 3 === 0, colorFlor: '#f6c1d5' }
}

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
  if (campos.imagen !== undefined) out.sprite_base_url = campos.imagen
  if (campos.publicada !== undefined) out.publicada = campos.publicada
  return out
}

export async function listarEspecies() {
  // El catalogo publico (/django/api/catalog/) ya solo trae las publicadas: el panel usa el suyo para ver todas
  const respuesta = await fetch('/django/api/admin/catalogo/', { headers: cabecerasAdmin() })
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
  const respuesta = await fetch(`/django/api/admin/catalogo/${id}/`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...cabecerasAdmin() },
    body: JSON.stringify(aDjango(campos)),
  })
  if (!respuesta.ok) throw await errorDeRespuesta(respuesta) // {"error"} de Django, no el JSON crudo
  return respuesta.json()
}

// REAL: crea una especie nueva en la BD (Django, POST /api/admin/catalogo/). Devuelve { id_catalogo, ... }
export async function crearEspecie(form) {
  const respuesta = await fetch('/django/api/admin/catalogo/', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...cabecerasAdmin() },
    body: JSON.stringify(aDjango(cuerpoEspecieNueva(form))),
  })
  if (!respuesta.ok) throw await errorDeRespuesta(respuesta)
  return respuesta.json()
}

// Ya no hay cambios locales: la BD es la fuente de verdad
export function descartarCambios() {
  // no-op: recargar la lista desde el servidor
}
