// Las hojas y el XP de la barra superior solo se refrescaban al cambiar de pagina: tras abonar o comprar
// seguia el saldo viejo. Cada escritura que sale bien (POST, PUT, PATCH, DELETE) avisa y la barra se refresca.
export const AVISO_SALDO = 'plantia:saldo'

export const cambiaElSaldo = (metodo = 'GET') => metodo.toUpperCase() !== 'GET'

export function avisarSaldo(destino = globalThis) {
  if (typeof destino?.dispatchEvent === 'function' && typeof Event === 'function') {
    destino.dispatchEvent(new Event(AVISO_SALDO))
  }
}
