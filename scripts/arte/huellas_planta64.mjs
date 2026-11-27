// Huellas (CRC32 de los pixeles RGBA) de muchos fotogramas del motor de plantas de la web
// (frontend-react/src/components/plantaPixelSprites.js). El movil porta ese motor a Kotlin (MotorPlanta64.kt) y
// su test PlantaPixel64Test comprueba que da exactamente los mismos pixeles. Si cambia el motor de la web:
//   node scripts/arte/huellas_planta64.mjs
// y portar el mismo cambio a MotorPlanta64.kt hasta que el test vuelva a pasar.
import { mkdtempSync, readFileSync, writeFileSync, copyFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { fileURLToPath, pathToFileURL } from 'node:url'
import path from 'node:path'
import { crc32 } from 'node:zlib'

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..')
const componentes = path.join(raiz, 'frontend-react/src/components')
// El motor importa './macetas' sin extension (lo resuelve Vite): se copia a una carpeta temporal como .mjs
const tmp = mkdtempSync(path.join(tmpdir(), 'planta64-'))
copyFileSync(path.join(componentes, 'macetas.js'), path.join(tmp, 'macetas.mjs'))
const fuente = readFileSync(path.join(componentes, 'plantaPixelSprites.js'), 'utf8').replace("from './macetas'", "from './macetas.mjs'")
writeFileSync(path.join(tmp, 'motor.mjs'), fuente)
const { fotograma, macetaVacia } = await import(pathToFileURL(path.join(tmp, 'motor.mjs')).href)

const FORMAS = ['arbusto', 'arbol', 'helecho', 'suculenta', 'roseta', 'cactus', 'monstera', 'trepadora', 'palmera']
const COLORES = [
  { colorHoja: '#2e7d32', tieneFlor: true, colorFlor: '#ffeb3b', tieneFruto: true, colorFruto: '#f44336' },
  { colorHoja: '#7fb69a', tieneFlor: true, colorFlor: '#d32f2f', tieneFruto: false, colorFruto: null },
  { colorHoja: '#8fa361', tieneFlor: false, colorFlor: null, tieneFruto: false, colorFruto: null },
  { colorHoja: 'abc', tieneFlor: false, colorFlor: null, tieneFruto: true, colorFruto: '#FFE135' },
]
const v = (x) => (x == null ? '' : String(x))
const lineas = []
const huella = (d) => crc32(Buffer.from(d.buffer, d.byteOffset, d.byteLength)).toString(16).padStart(8, '0')
const caso = (ap, etapa, k, maceta, estado) => {
  const clave = ap ? [ap.forma, ap.colorHoja, ap.tieneFlor, ap.colorFlor, ap.tieneFruto, ap.colorFruto].map(v).join('|') : '-|||||'
  lineas.push(`${clave}|${etapa}|${k}|${v(maceta)}|${v(estado)}=${huella(fotograma(ap, etapa, k, maceta, estado))}`)
}

for (const forma of FORMAS) {
  for (const colores of COLORES) {
    const ap = { forma, ...colores }
    for (let etapa = 0; etapa <= 6; etapa++) {
      for (const k of [0, 4, 9]) for (const estado of [null, 'marchita', 'muerta']) caso(ap, etapa, k, null, estado)
    }
  }
  const ap = { forma, ...COLORES[0] }
  for (const maceta of ['NINGUNA', 'AZUL', 'ROSA', 'DORADA', 'XYZ']) for (const etapa of [3, 5]) caso(ap, etapa, 2, maceta, null)
  lineas.push(`vacia|${forma}|AZUL=${huella(macetaVacia({ forma }, 'AZUL'))}`)
  lineas.push(`vacia|${forma}|=${huella(macetaVacia({ forma }))}`)
}
// Sin apariencia (planta vieja) y con forma desconocida
for (let etapa = 0; etapa <= 6; etapa++) caso(null, etapa, 1, null, null)
caso({ forma: 'baobab', colorHoja: 'no-es-color', tieneFlor: true, colorFlor: '#123', tieneFruto: false, colorFruto: null }, 4, 7, null, null)

const destino = path.join(raiz, 'mobile-kotlin/app/src/test/resources/planta64_huellas.txt')
writeFileSync(destino, `${lineas.join('\n')}\n`)
console.log(`${lineas.length} huellas en ${path.relative(raiz, destino)}`)
