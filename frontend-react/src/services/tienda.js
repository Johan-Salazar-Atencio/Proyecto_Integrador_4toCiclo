import { llamar } from './api'
import { olvidarArte } from './arte'

// Tienda contra Spring (/api/v1). Lo que se vende y los precios los decide el admin.
//   GET  /tienda                -> { monedas, nivel, items: [{ id, nombre, tipo, descripcion, precio, nivelRequerido,
//                                   previewUrl, variantes: [{ momento, vistaUrl }], comprado, bloqueado, cantidad }] }
//   POST /tienda/{id}/comprar     compra un item (402 sin monedas, 409 si ya lo tiene o falta nivel)
//
// Los abonos son inventario: la tienda los vende y la planta los usa.
//   GET  /abonos                      -> { monedas, abonos: [{ idItem, codigo, nombre, descripcion, precio, mediosDias, cantidad }] }
//   POST /abonos/{idItem}/comprar     suma 1 (o { cantidad } hasta 10) al contador y descuenta hojas; devuelve el mismo JSON
//   POST /plantas/{id}/abono { idItem }  usa 1 del contador: medio dia, 1 o 3 dias de crecimiento (maximo 1 al dia por planta)
//
// Los fondos son temas con una escena de dia, otra de tarde y otra de noche. Se ponen a una planta desde el jardin:
//   GET  /plantas/{id}/fondos      -> [{ id, nombre, descripcion, predeterminado, enUso, variantes }]
//   PUT  /plantas/{id}/fondo       { idTema } (null = el de siempre)

export const obtenerTienda = () => llamar('/tienda')

export const comprarItem = (id) => llamar(`/tienda/${id}/comprar`, { metodo: 'POST' })

export const obtenerAbonos = () => llamar('/abonos')

// Compra un abono: suma al contador (no lo usa)
export const comprarAbono = (idItem) => llamar(`/abonos/${idItem}/comprar`, { metodo: 'POST' })

// Usa un abono del contador en la planta
export const usarAbono = (idPlanta, idItem) =>
  llamar(`/plantas/${idPlanta}/abono`, { metodo: 'POST', cuerpo: { idItem } })

export const listarFondosPlanta = (idPlanta) => llamar(`/plantas/${idPlanta}/fondos`)

// Cambiar el fondo de la planta cambia la escena del jardin: se vuelve a pedir el arte
export async function ponerFondoPlanta(idPlanta, idTema) {
  const temas = await llamar(`/plantas/${idPlanta}/fondo`, { metodo: 'PUT', cuerpo: { idTema } })
  olvidarArte()
  return temas
}

export const MOMENTOS = {
  DIA: { icono: '☀️', texto: 'Día' },
  TARDE: { icono: '🌇', texto: 'Tarde' },
  NOCHE: { icono: '🌙', texto: 'Noche' },
}
