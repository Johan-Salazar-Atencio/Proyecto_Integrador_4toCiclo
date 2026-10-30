import { llamar } from './api'

// GET /catalogo?q=&toxica= -> [{ id, nombreComun, nombreCientifico, frecuenciaRiegoDias, frecuenciaSolHoras,
// temperaturaMinC, temperaturaMaxC, esToxicaMascotas, spriteBaseUrl }]
export function listarCatalogo({ texto = '', soloToxicas = false } = {}) {
  const params = new URLSearchParams()
  if (texto.trim()) params.set('q', texto.trim())
  if (soloToxicas) params.set('toxica', 'true')
  const consulta = params.toString()
  return llamar(`/catalogo${consulta ? `?${consulta}` : ''}`).then((r) => r || [])
}

// GET /catalogo/{id}/cuidados -> secciones de cuidado (misma forma que en un escaneo)
export const cuidadosCatalogo = (id) => llamar(`/catalogo/${id}/cuidados`).then((r) => r || [])
