import { cicloDe } from '../../../components/plantaPixelSprites'

// Crecimiento por dias con cuidado (lo calcula Spring: planta.diasCuidados de planta.diasTotales).
// El dibujo avanza con 6 sprites a lo largo de esos dias; las 3 fases del servidor son
// inicial, crecimiento y madura. Las dos ultimas cambian de nombre segun como madura la especie
// (frutos, flores o solo follaje). La marchita no depende del crecimiento sino de la salud.
const NOMBRES_BASE = ['Semilla', 'Brote', 'Planta joven', 'Planta adulta']
const NOMBRES_FINALES = {
  fruto: ['Con frutos', 'Cosecha'],
  flor: ['Con botones', 'En flor'],
  follaje: ['Frondosa', 'Esplendor'],
}

export const ETAPA_MARCHITA = 6
export const NOMBRE_MARCHITA = 'Marchita'

export function etapasDe(planta) {
  return [...NOMBRES_BASE, ...NOMBRES_FINALES[cicloDe(planta.apariencia)]].map((nombre) => ({ nombre }))
}

// Por debajo de esta salud la planta se marchita (y deja de crecer hasta recuperarla)
export const SALUD_MARCHITA = 30

export const estaMarchita = (planta) => planta.healthPercent < SALUD_MARCHITA

// Mismo avance que indiceSprite del movil. Sin datos del servidor (total 0) se ve la adulta.
export function indiceSprite(dias, total) {
  if (!(total > 0)) return 3
  if (!(dias > 0)) return 0
  const avance = dias / total
  if (avance >= 1) return 5
  if (avance >= 0.8) return 4
  if (avance >= 0.6) return 3
  if (avance >= 0.34) return 2
  return 1
}

// Fase 1 a 3 como la calcula Spring: la segunda empieza al 35 % de los dias y la tercera al completarlos
export function faseNumero(dias, total) {
  if (!(total > 0)) return 1
  if (dias >= total) return 3
  return dias >= Math.ceil(total * 0.35) ? 2 : 1
}

// { indice, nombre, marchita, dias, total, fase } — `indice` es el sprite (0 a 5, o 6 si esta marchita)
export function etapaDe(planta) {
  const dias = planta.diasCuidados || 0
  const total = planta.diasTotales || 0
  const marchita = estaMarchita(planta)
  const indice = marchita ? ETAPA_MARCHITA : indiceSprite(dias, total)
  return {
    indice,
    nombre: marchita ? NOMBRE_MARCHITA : etapasDe(planta)[indice].nombre,
    marchita,
    dias,
    total,
    fase: faseNumero(dias, total),
  }
}
