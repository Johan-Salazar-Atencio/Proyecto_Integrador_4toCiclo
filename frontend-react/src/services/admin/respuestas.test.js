// Pruebas del panel admin (QA-33): `npm test`, sin navegador.
import assert from 'node:assert/strict'
import { test } from 'node:test'
import { CONFIRMAR_CANCELAR, cambiosDeEspecie, cuerpoEspecieNueva, errorDeRespuesta } from './respuestas.js'

test('la especie nueva recorta los nombres y omite la imagen vacía', () => {
  const form = { nombreComun: ' Menta ', nombreCientifico: ' Mentha spicata ', riegoDias: 3, solHoras: 5, tempMin: 10, tempMax: 30, toxica: false, publicada: true, imagen: '  ' }
  assert.deepEqual(cuerpoEspecieNueva(form), { nombreComun: 'Menta', nombreCientifico: 'Mentha spicata', riegoDias: 3, solHoras: 5, tempMin: 10, tempMax: 30, toxica: false, publicada: true })
  assert.equal(cuerpoEspecieNueva({ ...form, imagen: ' /sprites/menta.png ' }).imagen, '/sprites/menta.png')
})

const respuesta = (status, cuerpo) => ({
  status,
  json: async () => { if (cuerpo === undefined) throw new Error('sin json'); return cuerpo },
})

test('el error muestra el texto de Django y no el JSON crudo', async () => {
  assert.equal((await errorDeRespuesta(respuesta(409, { error: 'Ya existe otra especie' }))).message, 'Ya existe otra especie')
})

test('sin cuerpo JSON o con un error raro se muestra el codigo', async () => {
  assert.equal((await errorDeRespuesta(respuesta(500))).message, 'Error 500')
  assert.equal((await errorDeRespuesta(respuesta(400, [1, 2]))).message, 'Error 400')
  assert.equal((await errorDeRespuesta(respuesta(400, { error: { x: 1 } }))).message, 'Error 400')
  assert.equal((await errorDeRespuesta(respuesta(400, null))).message, 'Error 400')
})

test('al guardar una especie solo se envian los campos cambiados', () => {
  const especie = { nombreComun: 'Rosa', riegoDias: 3, imagen: '', publicada: true }
  const form = { ...especie, nombreComun: 'Rosa china', publicada: false }
  assert.deepEqual(cambiosDeEspecie(form, especie, Object.keys(especie)), { nombreComun: 'Rosa china', publicada: false })
  assert.deepEqual(cambiosDeEspecie(especie, especie, Object.keys(especie)), {})
})

test('la confirmacion de cancelar explica que no hay danos ni premios', () => {
  assert.match(CONFIRMAR_CANCELAR, /ninguna planta recibe daño ni premio/)
  assert.match(CONFIRMAR_CANCELAR, /No se puede deshacer/)
})
