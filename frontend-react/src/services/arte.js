import { llamar } from './api'

// Fondos animados y animaciones de desastre (GET /fondos y /animaciones). Casi no cambian:
// se piden una vez y se reutilizan; si falla, la escena se queda con su fondo de color.
let cache = null

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

// DIA de 6 a 16, TARDE de 17 a 18 y NOCHE el resto (igual que el movil)
export function momentoDelDia(hora) {
  if (hora >= 6 && hora <= 16) return 'DIA'
  if (hora >= 17 && hora <= 18) return 'TARDE'
  return 'NOCHE'
}

// Elige el fondo del momento (el predeterminado primero) y la animacion del desastre
// (tipo y fase; si falta la fase pedida usa otra del mismo tipo). null si no hay arte.
export function elegirEscena(arte, hora, tipo, fase) {
  if (!arte || !arte.fondos?.length) return null
  const momento = momentoDelDia(hora)
  const delMomento = arte.fondos.filter((f) => f.momento === momento).sort((a, b) => Number(b.isDefault) - Number(a.isDefault))
  const fondo = delMomento[0] || arte.fondos[0]
  const delTipo = tipo ? arte.animaciones.filter((a) => a.disasterType === tipo) : []
  const animacion = delTipo.find((a) => a.phase === fase) || delTipo[0] || null
  return { fondo, animacion }
}
