// Pruebas de los tipos de evento del panel admin: `npm test`, sin navegador.
import assert from 'node:assert/strict'
import { test } from 'node:test'
import { cambiarActivoTipo, crearTipoEvento, peticionTipo } from './tiposEvento.js'

test('sin imagen el tipo viaja como JSON', () => {
  const [ruta, op] = peticionTipo('POST', '/x/', { nombre: 'Sequía' }, null)
  assert.equal(ruta, '/x/')
  assert.equal(op.method, 'POST')
  assert.equal(op.headers['Content-Type'], 'application/json')
  assert.deepEqual(JSON.parse(op.body), { nombre: 'Sequía' })
})

test('con imagen viaja como formulario con archivo y sin Content-Type propio', () => {
  const archivo = new Blob(['x'], { type: 'image/png' })
  const [, op] = peticionTipo('PATCH', '/x/1/', { nombre: 'Sequía', consejo: null }, archivo)
  assert.ok(op.body instanceof FormData)
  assert.equal(op.body.get('nombre'), 'Sequía')
  assert.equal(op.body.get('consejo'), '')
  assert.ok(op.body.get('imagen'))
  assert.equal(op.headers['Content-Type'], undefined)
})

test('el interruptor manda solo activo con PATCH al tipo', async () => {
  const llamadas = []
  globalThis.fetch = async (ruta, op) => {
    llamadas.push([ruta, op])
    return { ok: true, text: async () => JSON.stringify({ id_tipo_desastre: 4, activo: false }) }
  }
  const tipo = await cambiarActivoTipo(4, false)
  assert.equal(tipo.activo, false)
  assert.equal(llamadas[0][0], '/django/api/admin/tipos-desastre/4/')
  assert.equal(llamadas[0][1].method, 'PATCH')
  assert.deepEqual(JSON.parse(llamadas[0][1].body), { activo: false })
})

test('crear un tipo usa POST y muestra el error de Django si falla', async () => {
  globalThis.fetch = async (ruta, op) => ({ ok: false, status: 409, text: async () => JSON.stringify({ error: 'Ya existe un tipo de evento con ese nombre (SEQUIA)' }), ruta, op })
  await assert.rejects(() => crearTipoEvento({ nombre: 'Sequía' }, null), /Ya existe un tipo de evento/)
})
