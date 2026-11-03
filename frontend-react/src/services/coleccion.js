import { llamar } from './api'
import { urlImagen } from './home'

// GET /coleccion -> [{ id, idCatalogo, nombreComun, nombreCientifico, fotoUrl, vecesVista, primeraVez, ultimaVez, esToxicaMascotas }]
export async function listarColeccion() {
  const plantas = (await llamar('/coleccion')) || []
  return plantas
    // Se descartan registros sin nombre o resultados fallidos de la IA (TODO(backend): que lo filtre el servidor)
    .filter((p) => p.nombreComun?.trim() && !p.nombreComun.toLowerCase().startsWith('no identificado'))
    .map((p) => ({ ...p, fotoUrl: urlImagen(p.fotoUrl) }))
}

// GET /coleccion/{id} -> { item, cuidados }. Se devuelve con la forma de un escaneo (sin % de IA).
export async function detalleColeccion(id) {
  const detalle = await llamar(`/coleccion/${id}`)
  if (!detalle?.item) throw new Error('Planta no encontrada')
  const { item, cuidados } = detalle
  return {
    idEscaneo: 0,
    idCatalogo: item.idCatalogo,
    nombreComun: item.nombreComun,
    nombreCientifico: item.nombreCientifico,
    confianza: null,
    imagenUrl: urlImagen(item.fotoUrl),
    cuidados: cuidados || [],
  }
}
