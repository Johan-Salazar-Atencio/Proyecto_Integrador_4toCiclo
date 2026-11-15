// Exporta los sprites de la planta de la web (frontend-react/src/components/plantaPixelSprites.js)
// a un JSON que lee el movil, para que ambos dibujen lo mismo sin duplicar el dibujo.
// La fuente unica es el .js de la web; si cambia, volver a correr:
//   node scripts/arte/exportar_sprites.mjs
import { writeFileSync } from 'node:fs'
import { fileURLToPath, pathToFileURL } from 'node:url'
import path from 'node:path'
import assert from 'node:assert/strict'

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..')
const origen = pathToFileURL(path.join(raiz, 'frontend-react/src/components/plantaPixelSprites.js')).href
const { LADO, TIERRA, spritesDe, paletaDe } = await import(origen)

const FORMAS = ['arbusto', 'arbol', 'helecho', 'suculenta', 'cactus', 'roseta', 'monstera', 'trepadora', 'palmera']
// Las 4 combinaciones de flor/fruto; la clave es "<flor><fruto>" con 1 o 0 (ej. "01" = solo fruto).
// Asi el movil elige el conjunto sin repetir la logica de cicloDe.
const VARIANTES = [
  { tieneFlor: false, tieneFruto: false },
  { tieneFlor: false, tieneFruto: true },
  { tieneFlor: true, tieneFruto: false },
  { tieneFlor: true, tieneFruto: true },
]

// Colores calculados: letra -> [color base, operacion, cantidad]
const REGLAS = {
  hoja: { o: ['oscuro', 0.55], g: ['oscuro', 0.25], G: ['base', 0], H: ['claro', 0.45], s: ['oscuro', 0.5], q: ['oscuro', 0.3] },
  fruto: { k: ['oscuro', 0.45], r: ['base', 0], R: ['claro', 0.55] },
  flor: { K: ['oscuro', 0.35], p: ['base', 0], P: ['claro', 0.5] },
}

const mezclar = (hex, hacia, t) => {
  const a = parseInt(hex.slice(1), 16)
  const b = parseInt(hacia.slice(1), 16)
  const canal = (n) => Math.round(((a >> n) & 255) * (1 - t) + ((b >> n) & 255) * t)
  return `#${((canal(16) << 16) | (canal(8) << 8) | canal(0)).toString(16).padStart(6, '0')}`
}
const aplicar = ([op, t], base) => (op === 'base' ? base : mezclar(base, op === 'oscuro' ? '#000000' : '#ffffff', t))

// Comprueba que las reglas exportadas dan los mismos colores que paletaDe (si la web cambia, falla aqui)
for (const [hoja, fruto, flor] of [['#4f9a52', '#d9473b', '#f472b6'], ['#2a6b8f', '#f4c430', '#a855f7']]) {
  const p = paletaDe({ colorHoja: hoja, colorFruto: fruto, colorFlor: flor })
  for (const [grupo, base] of [['hoja', hoja], ['fruto', fruto], ['flor', flor]]) {
    for (const [letra, regla] of Object.entries(REGLAS[grupo])) assert.equal(aplicar(regla, base), p[letra], `color ${letra}`)
  }
}

const calculadas = new Set(Object.values(REGLAS).flatMap((g) => Object.keys(g)))
const fijos = Object.fromEntries(Object.entries(paletaDe({})).filter(([k]) => !calculadas.has(k)))

// Conjuntos de 7 sprites sin repetir; cada forma apunta al suyo segun flor/fruto
const conjuntos = {}
const formas = {}
for (const forma of FORMAS) {
  formas[forma] = {}
  for (const v of VARIANTES) {
    const sprites = spritesDe({ forma, ...v }).map((sp) => sp.map((f) => f.join('')))
    const id = JSON.stringify(sprites)
    let clave = Object.keys(conjuntos).find((k) => JSON.stringify(conjuntos[k]) === id)
    if (!clave) {
      clave = `s${Object.keys(conjuntos).length + 1}`
      conjuntos[clave] = sprites
    }
    formas[forma][`${+v.tieneFlor}${+v.tieneFruto}`] = clave
  }
}

const salida = {
  version: 1,
  lado: LADO,
  // Orden de los sprites en cada conjunto: semilla, brote, joven, adulta, cuarta, quinta, marchita
  etapas: ['semilla', 'brote', 'joven', 'adulta', 'cuarta', 'quinta', 'marchita'],
  tierra: TIERRA.map((f) => (Array.isArray(f) ? f.join('') : f)),
  paletaFija: fijos,
  reglasColor: REGLAS,
  // Valores cuando la IA no manda apariencia (APARIENCIA_DEFECTO de la web)
  defecto: { forma: 'arbusto', colorHoja: '#4f9a52', tieneFlor: false, tieneFruto: true, colorFruto: '#d9473b', colorFlorSinColor: '#f472b6' },
  formas,
  conjuntos,
}
const destino = path.join(raiz, 'mobile-kotlin/app/src/main/assets/planta_sprites.json')
writeFileSync(destino, JSON.stringify(salida))
console.log('Escrito', destino, `${Object.keys(conjuntos).length} conjuntos`)
