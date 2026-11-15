import { cicloDe } from '../../../components/plantaPixelSprites'

// Etapas de crecimiento de la planta del jardin. `desde` = cuidados acumulados para llegar a ella.
// Las dos ultimas cambian de nombre segun como madura la especie (frutos, flores o solo follaje).
// La marchita no depende del crecimiento sino de la salud.
const UMBRALES = [0, 2, 5, 9, 14, 20]
const NOMBRES_BASE = ['Semilla', 'Brote', 'Planta joven', 'Planta adulta']
const NOMBRES_FINALES = {
  fruto: ['Con frutos', 'Cosecha'],
  flor: ['Con botones', 'En flor'],
  follaje: ['Frondosa', 'Esplendor'],
}

export function etapasDe(planta) {
  const nombres = [...NOMBRES_BASE, ...NOMBRES_FINALES[cicloDe(planta.apariencia)]]
  return nombres.map((nombre, i) => ({ nombre, desde: UMBRALES[i] }))
}

export const ETAPA_MARCHITA = UMBRALES.length
export const NOMBRE_MARCHITA = 'Marchita'

// Por debajo de esta salud la planta se marchita (y deja de crecer hasta recuperarla)
export const SALUD_MARCHITA = 30

export const estaMarchita = (planta) => planta.healthPercent < SALUD_MARCHITA

// { indice, nombre, siguiente, faltan } — `siguiente` es null en la ultima etapa o si esta marchita
export function etapaDe(planta) {
  if (estaMarchita(planta)) {
    return { indice: ETAPA_MARCHITA, nombre: NOMBRE_MARCHITA, siguiente: null, faltan: 0 }
  }
  const etapas = etapasDe(planta)
  const crecimiento = planta.crecimiento || 0
  let indice = 0
  while (indice + 1 < etapas.length && crecimiento >= etapas[indice + 1].desde) indice++
  const siguiente = etapas[indice + 1] || null
  return {
    indice,
    nombre: etapas[indice].nombre,
    siguiente: siguiente?.nombre || null,
    faltan: siguiente ? siguiente.desde - crecimiento : 0,
  }
}
