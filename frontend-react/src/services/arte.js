import { llamar } from './api'

// Fondos animados y animaciones de desastre (GET /fondos y /animaciones). Casi no cambian:
// se piden una vez y se reutilizan; si falla, la escena se queda con su fondo de color.
let cache = null

// Tras equipar o quitar un fondo en la tienda: la proxima escena pide el arte de nuevo
export function olvidarArte() {
  cache = null
}

export async function obtenerArte() {
  if (cache) return cache
  try {
    const [fondos, animaciones] = await Promise.all([llamar('/fondos'), llamar('/animaciones')])
    cache = { fondos: fondos || [], animaciones: animaciones || [] }
    return cache
  } catch {
    return null
  }
}

// DIA de 6:00 a 11:59, TARDE de 12:00 a 17:59 y NOCHE de 18:00 a 5:59 (igual que el movil)
export function momentoDelDia(hora) {
  if (hora >= 6 && hora <= 11) return 'DIA'
  if (hora >= 12 && hora <= 17) return 'TARDE'
  return 'NOCHE'
}

// Elige el fondo del momento (el predeterminado primero) y la animacion del desastre
// (tipo y fase; si falta la fase pedida usa otra del mismo tipo). null si no hay arte.
export function elegirEscena(arte, hora, tipo, fase) {
  if (!arte || !arte.fondos?.length) return null
  const momento = momentoDelDia(hora)
  // El fondo equipado de la tienda va primero; despues el predeterminado
  const delMomento = arte.fondos
    .filter((f) => f.momento === momento)
    .sort((a, b) => Number(b.equipado) - Number(a.equipado) || Number(b.isDefault) - Number(a.isDefault))
  const fondo = delMomento[0] || arte.fondos[0]
  const delTipo = tipo ? arte.animaciones.filter((a) => a.disasterType === tipo) : []
  const animacion = delTipo.find((a) => a.phase === fase) || delTipo[0] || null
  return { fondo, animacion }
}
