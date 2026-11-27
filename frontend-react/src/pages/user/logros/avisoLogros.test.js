import { test } from 'node:test'
import assert from 'node:assert/strict'
import { AVISO_LOGROS, avisarLogros, logrosDeRespuesta } from '../../../services/avisoLogros.js'

test('saca los logros nuevos de progreso o del propio ProgresoDto', () => {
  assert.deepEqual(logrosDeRespuesta({ progreso: { logrosNuevos: [{ idLogro: 3 }] } }), [{ idLogro: 3 }])
  assert.deepEqual(logrosDeRespuesta({ logrosNuevos: [{ idLogro: 5 }, { idLogro: 0 }, null] }), [{ idLogro: 5 }])
})

test('sin logros nuevos devuelve una lista vacia', () => {
  for (const datos of [null, undefined, [], [{ idLogro: 1 }], {}, { progreso: {} }, { progreso: { logrosNuevos: 'x' } }]) {
    assert.deepEqual(logrosDeRespuesta(datos), [])
  }
})

test('avisa con un evento solo si hay logros', () => {
  const recibidos = []
  const destino = new EventTarget()
  destino.addEventListener(AVISO_LOGROS, (e) => recibidos.push(e.detail))
  avisarLogros([], destino)
  avisarLogros([{ idLogro: 7 }], destino)
  assert.deepEqual(recibidos, [[{ idLogro: 7 }]])
})
