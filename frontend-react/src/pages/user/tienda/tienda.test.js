// Cuarta vuelta (web en navegador): confirmacion de compra, saldo de la barra y reactivar macetas. `npm test`.
import assert from 'node:assert/strict'
import { test } from 'node:test'
import { textosCompra } from './confirmacion.js'
import { estiloAbono, ordenarAbonos, textoTienes } from '../../../components/abonoEstilo.js'
import { AVISO_SALDO, avisarSaldo, cambiaElSaldo } from '../../../services/saldo.js'
import { motivoSinVenta } from '../../admin/tienda/venta.js'

test('la compra pide confirmacion con el nombre y el precio', () => {
  const t = textosCompra({ nombre: 'Crema', tipo: 'MACETA', precio: 80 })
  assert.equal(t.titulo, '¿Comprar Crema?')
  assert.match(t.detalle, /80 hojas/)
  assert.equal(t.boton, 'Comprar')
  assert.equal(textosCompra(null), null)
})

test('el abono se compra, suma al contador y se usa desde el jardin', () => {
  const t = textosCompra({ nombre: 'Abono reforzado', tipo: 'ABONO', precio: 80 })
  assert.equal(t.titulo, '¿Comprar Abono reforzado?')
  assert.equal(t.boton, 'Comprar')
  assert.match(t.detalle, /80 hojas/)
  assert.match(t.detalle, /desde tu jardín/)
  assert.match(t.detalle, /uno al día/)
})

test('cada abono tiene su color como en el movil y uno desconocido sale lima', () => {
  assert.equal(estiloAbono('ABONO_MEDIO').color, '#7CB518')
  assert.equal(estiloAbono('ABONO_1').color, '#9A6B3F')
  assert.equal(estiloAbono('ABONO_3').color, '#9333EA')
  assert.deepEqual([1, 2, 3], ['ABONO_MEDIO', 'ABONO_1', 'ABONO_3'].map((c) => estiloAbono(c).hojas))
  assert.equal(estiloAbono('OTRO').color, '#7CB518')
  assert.equal(estiloAbono(undefined).color, '#7CB518')
})

test('el contador dice cuantos abonos tiene y los abonos van del mas barato al mas caro', () => {
  assert.equal(textoTienes(3), 'Tienes 3')
  assert.equal(textoTienes(undefined), 'Tienes 0')
  const orden = ordenarAbonos([{ precio: 80 }, { precio: 15 }, { precio: 30 }]).map((a) => a.precio)
  assert.deepEqual(orden, [15, 30, 80])
  assert.deepEqual(ordenarAbonos(undefined), [])
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
