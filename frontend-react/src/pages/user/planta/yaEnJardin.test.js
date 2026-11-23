// QA-04: avisos de "ya esta en tu jardin" (sin navegador): `npm test`.
import assert from 'node:assert/strict'
import { test } from 'node:test'
import { avisoAntes, avisoDespues, plantaDeLaEspecie } from './yaEnJardin.js'

const plantas = [
  { id: 1, name: 'Mi Rosita', species: 'Rosa qaensis', fase: 'BROTE', enBaul: false },
  { id: 2, name: 'Cactus', species: 'Cactus qaensis', fase: 'MUERTA', enBaul: false },
  { id: 3, name: 'Monstera', species: 'Monstera qaensis', fase: 'SEMILLA', enBaul: true },
]

test('encuentra la planta de la misma especie sin distinguir mayusculas ni espacios', () => {
  assert.equal(plantaDeLaEspecie(plantas, { nombreCientifico: ' rosa QAENSIS ' }).id, 1)
  assert.equal(plantaDeLaEspecie(plantas, { nombreCientifico: 'Ficus qaensis' }), null)
  assert.equal(plantaDeLaEspecie(plantas, { nombreCientifico: '' }), null)
  assert.equal(plantaDeLaEspecie(null, { nombreCientifico: 'Rosa qaensis' }), null)
})

test('el aviso previo dice que no se reinicia, que no revive y que esta en el baul', () => {
  assert.match(avisoAntes(plantas[0]), /no la reinicia/)
  assert.match(avisoAntes(plantas[0]), /Mi Rosita/)
  assert.match(avisoAntes(plantas[1]), /no la revive/)
  assert.match(avisoAntes(plantas[2]), /baúl/)
  assert.equal(avisoAntes(null), '')
})

test('el aviso del jardin solo aparece si Spring respondio yaEnJardin', () => {
  assert.match(avisoDespues({ name: 'Mi Rosita', yaEnJardin: true }), /conserva su progreso/)
  assert.equal(avisoDespues({ name: 'Nueva' }), '')
  assert.equal(avisoDespues(null), '')
})
