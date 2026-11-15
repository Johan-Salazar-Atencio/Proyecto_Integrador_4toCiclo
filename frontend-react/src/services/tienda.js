import { llamar } from './api'
import { olvidarArte } from './arte'

// Tienda contra Spring (/api/v1/tienda). Lo que se vende y los precios los decide el admin.
//   GET  /tienda                -> { monedas, nivel, items: [{ id, nombre, tipo, descripcion, precio, nivelRequerido,
//                                   previewUrl, momento, comprado, equipado, bloqueado }] }
//   POST /tienda/{id}/comprar     compra un fondo (402 sin monedas, 409 si ya lo tiene o falta nivel)
//   POST /tienda/{id}/equipar     lo pone; quita el que habia en el mismo momento del dia
//   POST /tienda/{id}/desequipar  vuelve al fondo de siempre
//   POST /plantas/{id}/abono-tienda  compra y usa un abono: +1 dia de crecimiento (maximo 1 al dia por planta)

export const obtenerTienda = () => llamar('/tienda')

export const comprarItem = (id) => llamar(`/tienda/${id}/comprar`, { metodo: 'POST' })

// Equipar o quitar un fondo cambia la escena del jardin: se vuelve a pedir el arte
export async function equiparItem(id) {
  const tienda = await llamar(`/tienda/${id}/equipar`, { metodo: 'POST' })
  olvidarArte()
  return tienda
}

export async function desequiparItem(id) {
  const tienda = await llamar(`/tienda/${id}/desequipar`, { metodo: 'POST' })
  olvidarArte()
  return tienda
}

export const comprarAbono = (idPlanta) => llamar(`/plantas/${idPlanta}/abono-tienda`, { metodo: 'POST' })

export const MOMENTOS = {
  DIA: { icono: '☀️', texto: 'Día' },
  TARDE: { icono: '🌇', texto: 'Tarde' },
  NOCHE: { icono: '🌙', texto: 'Noche' },
}
