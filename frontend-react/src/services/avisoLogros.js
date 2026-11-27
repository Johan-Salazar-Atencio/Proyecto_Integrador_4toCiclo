// Logros recien ganados que llegan dentro de la respuesta de una accion (escanear, cuidar, abonar, comprar...).
// Spring los manda en `progreso.logrosNuevos` (PlantSummaryDto, ScanResultDto) o en `logrosNuevos` si la respuesta es
// el propio ProgresoDto. api.js los avisa con este evento y el celebrador del Layout los muestra al momento, como el
// movil (GestorLogros.kt). Va aparte de logros.js para que api.js no lo importe en circulo.
export const AVISO_LOGROS = 'plantia:logros'

/** Lista de logros nuevos de una respuesta, sin ids invalidos; [] si no trae ninguno. */
export function logrosDeRespuesta(datos) {
  const lista = datos?.logrosNuevos ?? datos?.progreso?.logrosNuevos
  return Array.isArray(lista) ? lista.filter((l) => l && l.idLogro > 0) : []
}

export function avisarLogros(logros, destino = globalThis) {
  if (!logros?.length || typeof destino?.dispatchEvent !== 'function' || typeof CustomEvent !== 'function') return
  destino.dispatchEvent(new CustomEvent(AVISO_LOGROS, { detail: logros }))
}
