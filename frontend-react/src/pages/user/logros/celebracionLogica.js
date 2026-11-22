// Reglas de la celebracion de logros (sin DOM, para poder probarlas con `node --test`).
// Son las mismas del movil (GestorLogros.kt / ControlCelebracion.kt):
//   - el mas raro primero; a igual rareza, por id;
//   - con 1–3 logros se celebran todos; desde el 4.º se celebran los 2 mas raros y el resto va en una tarjeta resumen;
//   - el aviso de arriba sale al TERMINAR la animacion y se va a los 4 s (6 s con animaciones reducidas) o con un toque;
//   - un logro se marca visto al CERRAR su medalla (o el resumen), nunca antes;
//   - POST /logros/vistos acepta como maximo 100 ids por llamada.
import { nivelRareza } from './motorMedalla.js'

export const LOTE_VISTOS = 100
export const CELEBRADOS_ANTES_DEL_RESUMEN = 2
export const MIN_RESUMEN = 2
export const DURACION_AVISO_MS = 4000
export const DURACION_AVISO_REDUCIDO_MS = 6000

export const duracionAviso = (reducido) => (reducido ? DURACION_AVISO_REDUCIDO_MS : DURACION_AVISO_MS)

/** Legendaria antes que epica, rara y comun; a igual rareza, por id. Sin repetidos ni ids invalidos. */
export function ordenarLogros(logros) {
  const vistos = new Set()
  return (logros || [])
    .filter((l) => l && l.idLogro > 0 && !vistos.has(l.idLogro) && vistos.add(l.idLogro))
    .sort((a, b) => nivelRareza(b.rareza) - nivelRareza(a.rareza) || a.idLogro - b.idLogro)
}

/**
 * Que toca mostrar en la tanda [lista] cuando ya se cerraron [hechos]:
 *   { tipo: 'una', logro, numero, total } | { tipo: 'resumen', logros } | null
 * [mostrandoId]: la medalla que ya esta en pantalla; aunque lleguen mas logros no se cambia por el resumen.
 */
export function pasoCelebracion(lista, hechos, mostrandoId = null) {
  const resto = lista.slice(hechos)
  if (resto.length === 0) return null
  const resumen = hechos >= CELEBRADOS_ANTES_DEL_RESUMEN && resto.length >= MIN_RESUMEN
  if (resumen && resto[0].idLogro !== mostrandoId) return { tipo: 'resumen', logros: resto }
  return { tipo: 'una', logro: resto[0], numero: hechos + 1, total: lista.length }
}

/** Agrega a la tanda los que llegan despues sin mover el que ya se muestra; el resto se reordena por rareza. */
export function sumarATanda(lista, hechos, nuevos) {
  const conocidos = new Set(lista.map((l) => l.idLogro))
  const frescos = ordenarLogros(nuevos).filter((l) => !conocidos.has(l.idLogro))
  if (frescos.length === 0) return lista
  const fijos = lista.slice(0, hechos + 1)
  return [...fijos, ...ordenarLogros([...lista.slice(hechos + 1), ...frescos])]
}

/** Parte los ids en lotes de [tam] como maximo. */
export function enLotes(ids, tam = LOTE_VISTOS) {
  const salida = []
  for (let i = 0; i < ids.length; i += tam) salida.push(ids.slice(i, i + tam))
  return salida
}

/** "+60 XP", "+15 hojas" (solo lo que no es cero). */
export function partesRecompensa(xp, hojas) {
  return [xp > 0 ? `+${xp} XP` : '', hojas > 0 ? `+${hojas} ${hojas === 1 ? 'hoja' : 'hojas'}` : ''].filter(Boolean)
}

/** Suma de XP y hojas de varios logros (tarjeta resumen). */
export function sumarRecompensas(logros) {
  return logros.reduce((a, l) => ({ xp: a.xp + (l.recompensaXp || 0), hojas: a.hojas + (l.recompensaMonedas || 0) }), { xp: 0, hojas: 0 })
}
