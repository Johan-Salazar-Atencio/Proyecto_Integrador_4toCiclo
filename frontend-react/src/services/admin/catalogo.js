import { llamar } from '../api'

// Catalogo de especies para el panel. LEER es real; GUARDAR / PUBLICAR estan SIMULADOS
// (los cambios se guardan solo en este navegador porque Django aun no tiene endpoints de escritura).
//
// Lectura: Django GET /api/catalog/ . Hoy responde 500 (AttributeError con `cuidadosseccion_set`,
// codigo de Django), asi que mientras tanto se lee el mismo catalogo de Spring GET /api/v1/catalogo.
//
// TODO(django): reemplazar `guardarEspecie` por
//   PATCH /django/api/admin/catalogo/{id_catalogo}
//         { nombre_comun, nombre_cientifico, frecuencia_riego_dias, frecuencia_sol_horas,
//           temperatura_min_c, temperatura_max_c, es_toxica_mascotas, publicada }  ->  200 + especie
// y agregar el campo `publicada` (la tabla plantas_catalogo aun no lo tiene).

const CLAVE = 'plantia.admin.catalogo'
const leerCambios = () => {
  try { return JSON.parse(localStorage.getItem(CLAVE)) || {} } catch { return {} }
}
const guardarCambios = (cambios) => {
  try { localStorage.setItem(CLAVE, JSON.stringify(cambios)) } catch { /* sin almacenamiento */ }
}

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
  publicada: true,
})

export async function listarEspecies() {
  let origen = 'django'
  let lista
  try {
    const respuesta = await fetch('/django/api/catalog/')
    if (!respuesta.ok) throw new Error(String(respuesta.status))
    lista = await respuesta.json()
  } catch {
    lista = (await llamar('/catalogo')) || []
    origen = 'spring'
  }
  const cambios = leerCambios()
  const especies = lista
    .map(normalizar)
    .map((e) => ({ ...e, ...cambios[e.id], editada: Boolean(cambios[e.id]) }))
    .sort((a, b) => a.nombreComun.localeCompare(b.nombreComun, 'es'))
  return { especies, origen }
}

// Secciones de cuidado de una especie (lectura real)
export async function cuidadosDe(id) {
  return (await llamar(`/catalogo/${id}/cuidados`)) || []
}

// SIMULADO: guarda los cambios de una especie solo en este navegador
export async function guardarEspecie(id, campos) {
  await new Promise((ok) => setTimeout(ok, 500))
  const cambios = leerCambios()
  cambios[id] = { ...cambios[id], ...campos }
  guardarCambios(cambios)
}

// SIMULADO: descarta los cambios locales de una especie
export function descartarCambios(id) {
  const cambios = leerCambios()
  delete cambios[id]
  guardarCambios(cambios)
}
