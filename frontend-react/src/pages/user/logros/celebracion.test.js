// Pruebas de la celebracion de logros (sin navegador): `npm test` (usa node --test, sin dependencias).
import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  duracionAviso, enLotes, ordenarLogros, partesRecompensa, pasoCelebracion, sumarATanda, sumarRecompensas,
} from './celebracionLogica.js'
import { circulo, MotorMedalla, nivelRareza } from './motorMedalla.js'

const L = (idLogro, rareza = 'COMUN', extra = {}) => ({ idLogro, rareza, titulo: `Logro ${idLogro}`, recompensaXp: 10, recompensaMonedas: 5, ...extra })
const ids = (l) => l.map((x) => x.idLogro)

// Recorre una tanda como lo haria el usuario cerrando cada cosa; devuelve lo que vio y lo que se marco visto
function recorrer(lista) {
  const vistas = []
  const marcados = []
  let hechos = 0
  let paso
  while ((paso = pasoCelebracion(lista, hechos))) {
    if (paso.tipo === 'una') { vistas.push(paso.logro.idLogro); marcados.push([paso.logro.idLogro]); hechos++ } else {
      vistas.push(`resumen:${ids(paso.logros).join(',')}`); marcados.push(ids(paso.logros)); hechos += paso.logros.length
    }
  }
  return { vistas, marcados }
}

test('ordena el mas raro primero y a igual rareza por id, sin repetidos ni ids invalidos', () => {
  const lista = ordenarLogros([L(5), L(2, 'RARA'), L(9, 'LEGENDARIA'), L(1, 'EPICA'), L(3, 'RARA'), L(2, 'RARA'), L(0), null])
  assert.deepEqual(ids(lista), [9, 1, 2, 3, 5])
})

test('con 1 a 3 logros se celebran todos', () => {
  assert.deepEqual(recorrer(ordenarLogros([L(1), L(2, 'EPICA'), L(3, 'RARA')])).vistas, [2, 3, 1])
  assert.deepEqual(recorrer(ordenarLogros([L(1)])).vistas, [1])
})

test('desde el 4.º se celebran los 2 mas raros y luego el resumen', () => {
  const lista = ordenarLogros([L(1), L(2, 'RARA'), L(3, 'EPICA'), L(4, 'RARA'), L(5)])
  const { vistas, marcados } = recorrer(lista)
  assert.deepEqual(vistas, [3, 2, 'resumen:4,1,5'])
  assert.deepEqual(marcados, [[3], [2], [4, 1, 5]])
  assert.deepEqual(recorrer(ordenarLogros([L(1), L(2), L(3), L(4)])).vistas, [1, 2, 'resumen:3,4'])
})

test('la medalla que ya se muestra no se cambia por el resumen si llegan mas', () => {
  let lista = ordenarLogros([L(1), L(2), L(3)])
  assert.equal(pasoCelebracion(lista, 2).logro.idLogro, 3)
  lista = sumarATanda(lista, 2, [L(4), L(5)])
  assert.equal(pasoCelebracion(lista, 2, 3).tipo, 'una')
  assert.equal(pasoCelebracion(lista, 2, 3).total, 5)
  assert.deepEqual(ids(pasoCelebracion(lista, 3, 3).logros), [4, 5])
})

test('sumar a la tanda no mueve el actual y reordena el resto', () => {
  const lista = ordenarLogros([L(1), L(2)])
  const nueva = sumarATanda(lista, 0, [L(3, 'LEGENDARIA'), L(2)])
  assert.deepEqual(ids(nueva), [1, 3, 2])
})

test('los vistos se parten en lotes de 100 como maximo', () => {
  const lotes = enLotes(Array.from({ length: 250 }, (_, i) => i + 1))
  assert.deepEqual(lotes.map((l) => l.length), [100, 100, 50])
  assert.deepEqual(lotes.flat().length, 250)
  assert.deepEqual(enLotes([]), [])
})

test('el aviso dura 4 s, o 6 s con animaciones reducidas', () => {
  assert.equal(duracionAviso(false), 4000)
  assert.equal(duracionAviso(true), 6000)
})

test('recompensas: solo lo que no es cero y en singular si es 1', () => {
  assert.deepEqual(partesRecompensa(60, 15), ['+60 XP', '+15 hojas'])
  assert.deepEqual(partesRecompensa(0, 1), ['+1 hoja'])
  assert.deepEqual(sumarRecompensas([L(1), L(2)]), { xp: 20, hojas: 10 })
})

// ---- Motor (mismos numeros que el prototipo y que MotorMedalla.kt) ----
const motor = (r, estatico = false, alTerminar = () => {}) => {
  let s = 42
  const azar = () => ((s = (s * 16807) % 2147483647) / 2147483647)
  const m = new MotorMedalla(r, estatico, alTerminar, azar)
  m.colocar(60, 140, 120, 260)
  return m
}

test('la duracion crece con la rareza (~2,0 a 2,2 s)', () => {
  assert.deepEqual([0, 1, 2, 3].map((r) => motor(r).fin), [25, 25, 26, 27])
})

test('avisa que termino una sola vez y justo al final (ahi baja el aviso)', () => {
  let veces = 0
  const m = motor(0, false, () => veces++)
  for (let i = 0; i < m.fin - 1; i++) m.tic()
  assert.equal(veces, 0)
  m.tic()
  assert.equal(veces, 1)
  for (let i = 0; i < 50; i++) m.tic()
  m.adelantar()
  assert.equal(veces, 1)
})

test('un toque antes de 1 s salta al final', () => {
  let veces = 0
  const m = motor(3, false, () => veces++)
  m.tic()
  m.adelantar()
  assert.equal(m.done, true)
  assert.equal(m.tarjeta, true)
  assert.equal(veces, 1)
})

test('animaciones reducidas: cuadro final sin particulas y el bucle no arranca', () => {
  let veces = 0
  const m = motor(2, true, () => veces++)
  assert.equal(m.tarjeta, true)
  m.completarEstatico()
  assert.equal(m.parts.length, 0)
  assert.equal(m.iconoVisible, true)
  assert.equal(m.debeParar(), true)
  assert.equal(veces, 1)
})

test('picos de particulas al sellar: 18 / 26 / 38 / 54', () => {
  assert.deepEqual([0, 1, 2, 3].map((r) => { const m = motor(r); for (let i = 0; i < m.fin + 40; i++) m.tic(); return m.peak }), [18, 26, 38, 54])
})

test('el sello cae grande (1,9) y se asienta en 3 tics', () => {
  const m = motor(1)
  for (let i = 0; i <= m.tg + m.ST; i++) m.tic()
  assert.equal(m.escalaIcono, 1.9)
  assert.equal(m.alfaIcono, 0.7)
  for (let i = 0; i < 3; i++) m.tic()
  assert.equal(m.escalaIcono, 1)
})

test('rareza desconocida cuenta como comun y el circulo no repite puntos', () => {
  assert.equal(nivelRareza('rara'), 1)
  assert.equal(nivelRareza('???'), 0)
  const c = circulo(11)
  assert.equal(new Set(c.map(([x, y]) => `${x},${y}`)).size, c.length)
  assert.deepEqual(c[0], [0, -11])
})
