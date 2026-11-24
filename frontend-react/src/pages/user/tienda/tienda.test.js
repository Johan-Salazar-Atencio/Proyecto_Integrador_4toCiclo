// Cuarta vuelta (web en navegador): confirmacion de compra, saldo de la barra y reactivar macetas. `npm test`.
import assert from 'node:assert/strict'
import { test } from 'node:test'
import { textosCompra } from './confirmacion.js'
import { AVISO_SALDO, avisarSaldo, cambiaElSaldo } from '../../../services/saldo.js'
import { motivoSinVenta } from '../../admin/tienda/venta.js'

test('la compra pide confirmacion con el nombre y el precio', () => {
  const t = textosCompra({ nombre: 'Crema', tipo: 'MACETA', precio: 80 })
  assert.equal(t.titulo, '¿Comprar Crema?')
  assert.match(t.detalle, /80 hojas/)
  assert.equal(t.boton, 'Comprar')
  assert.equal(textosCompra(null), null)
})

test('el abono avisa que se aplica a la planta y del maximo diario', () => {
  const t = textosCompra({ nombre: 'Abono reforzado', tipo: 'ABONO', precio: 80 })
  assert.equal(t.titulo, '¿Usar Abono reforzado en tu planta?')
  assert.match(t.detalle, /80 hojas/)
  assert.match(t.detalle, /un abono al día/)
})

test('solo las escrituras refrescan el saldo de la barra', () => {
  assert.equal(cambiaElSaldo('GET'), false)
  assert.equal(cambiaElSaldo(), false)
  for (const m of ['POST', 'put', 'PATCH', 'DELETE']) assert.equal(cambiaElSaldo(m), true)
})

test('avisarSaldo emite el evento que escucha la barra', () => {
  const destino = new EventTarget()
  let veces = 0
  destino.addEventListener(AVISO_SALDO, () => { veces += 1 })
  avisarSaldo(destino)
  assert.equal(veces, 1)
  assert.doesNotThrow(() => avisarSaldo(null))
})

test('una maceta desactivada se puede volver a poner a la venta', () => {
  assert.equal(motivoSinVenta({ tipo: 'MACETA', activo: false }), null)
  assert.equal(motivoSinVenta({ tipo: 'ABONO', activo: false }), null)
  assert.equal(motivoSinVenta({ tipo: 'FONDO', activo: false, variantes: [{ momento: 'DIA' }] }), null)
  assert.match(motivoSinVenta({ tipo: 'FONDO', activo: false, variantes: [] }), /escenas/)
  assert.equal(motivoSinVenta({ tipo: 'FONDO', activo: true, variantes: [] }), null)
})
