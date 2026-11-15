// Sprites de la planta en pixel art (32x32), generados segun la APARIENCIA que da la IA al
// escanear: forma de crecimiento, color de hoja y si tiene flores o frutos (y de que color).
//
// Las etapas no se dibujan pixel a pixel: se componen con "sellos" y figuras (hoja, roseta,
// hoja de monstera, fruto, flor) sobre un lienzo de letras. Cada letra es un color; las de
// hoja/fruto/flor se calculan del color de la especie (ver paletaDe). '.' = vacio.
export const LADO = 32

// ---------------------------------------------------------------------------------------
// Apariencia
// ---------------------------------------------------------------------------------------

// Plantas digitalizadas antes de existir la apariencia (o si la IA no la envia)
export const APARIENCIA_DEFECTO = {
  forma: 'arbusto',
  colorHoja: '#4f9a52',
  tieneFlor: false,
  colorFlor: null,
  tieneFruto: true,
  colorFruto: '#d9473b',
}

// Forma que da la IA -> dibujo disponible (el mas parecido mientras no haya uno propio)
const DIBUJO_DE_FORMA = {
  arbusto: 'arbusto',
  arbol: 'arbusto',
  helecho: 'arbusto',
  suculenta: 'suculenta',
  cactus: 'suculenta',
  roseta: 'suculenta',
  monstera: 'monstera',
  trepadora: 'monstera',
  palmera: 'monstera',
}

export function aparienciaDe(apariencia) {
  return { ...APARIENCIA_DEFECTO, ...(apariencia || {}) }
}

const dibujoDe = (ap) => DIBUJO_DE_FORMA[ap.forma] || 'arbusto'

// Como "madura" la planta en sus dos ultimas etapas: dando fruto, floreciendo o solo con mas hojas
export function cicloDe(apariencia) {
  const ap = aparienciaDe(apariencia)
  const dibujo = dibujoDe(ap)
  if (dibujo === 'monstera') return 'follaje'
  if (dibujo === 'suculenta') return ap.tieneFlor || ap.tieneFruto ? 'flor' : 'follaje'
  if (ap.tieneFruto) return 'fruto'
  return ap.tieneFlor ? 'flor' : 'follaje'
}

// ---------------------------------------------------------------------------------------
// Colores
// ---------------------------------------------------------------------------------------

const PALETA_FIJA = {
  // tierra y semilla
  x: '#3e1f1f', d: '#5a2e2e', D: '#7a3b33', E: '#9a5a48', B: '#b07a45',
  // fruto sin madurar
  j: '#3d7a2f', f: '#7dcf63', F: '#c4f09a',
  // centro de la flor
  c: '#f4c430',
  // marchita
  u: '#5c3418', w: '#8a4b2a', W: '#c27a3a', Y: '#e0b070', S: '#5e3820', T: '#7d5232',
}

function mezclar(hex, hacia, t) {
  const a = parseInt(hex.slice(1), 16)
  const b = parseInt(hacia.slice(1), 16)
  const canal = (n) => Math.round(((a >> n) & 255) * (1 - t) + ((b >> n) & 255) * t)
  return `#${((canal(16) << 16) | (canal(8) << 8) | canal(0)).toString(16).padStart(6, '0')}`
}
const oscuro = (hex, t) => mezclar(hex, '#000000', t)
const claro = (hex, t) => mezclar(hex, '#ffffff', t)

export function paletaDe(apariencia) {
  const ap = aparienciaDe(apariencia)
  const hoja = ap.colorHoja || APARIENCIA_DEFECTO.colorHoja
  const fruto = ap.colorFruto || ap.colorFlor || APARIENCIA_DEFECTO.colorFruto
  const flor = ap.colorFlor || ap.colorFruto || '#f472b6'
  return {
    ...PALETA_FIJA,
    // hoja: contorno, sombra, base, brillo; tallo: oscuro y medio
    o: oscuro(hoja, 0.55), g: oscuro(hoja, 0.25), G: hoja, H: claro(hoja, 0.45),
    s: oscuro(hoja, 0.5), q: oscuro(hoja, 0.3),
    // fruto maduro
    k: oscuro(fruto, 0.45), r: fruto, R: claro(fruto, 0.55),
    // flor
    K: oscuro(flor, 0.35), p: flor, P: claro(flor, 0.5),
  }
}

// ---------------------------------------------------------------------------------------
// Lienzo y herramientas
// ---------------------------------------------------------------------------------------

const SUELO = 27 // primera fila de tierra
const CENTRO = 15.5

const lienzo = () => Array.from({ length: LADO }, () => Array(LADO).fill('.'))
const espejo = (filas) => filas.map((f) => [...f].reverse().join(''))
const voltear = (filas) => [...filas].reverse()

function pintar(l, x, y, c) {
  if (c !== '.' && x >= 0 && x < LADO && y >= 0 && y < LADO) l[y][x] = c
}

function borrar(l, x, y) {
  if (x >= 0 && x < LADO && y >= 0 && y < LADO) l[y][x] = '.'
}

function sello(l, filas, x, y) {
  filas.forEach((fila, j) => [...fila].forEach((c, i) => pintar(l, x + i, y + j, c)))
}

function linea(l, x0, y0, x1, y1, c, pintor = pintar) {
  const pasos = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)) || 1
  for (let i = 0; i <= pasos; i++) {
    pintor(l, Math.round(x0 + ((x1 - x0) * i) / pasos), Math.round(y0 + ((y1 - y0) * i) / pasos), c)
  }
}

// Elipse rellena con contorno 'o', brillo arriba 'H' y sombra abajo 'g'
function elipse(l, cx, cy, rx, ry) {
  const dentro = (x, y) => ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2 <= 1
  for (let y = Math.floor(cy - ry); y <= Math.ceil(cy + ry); y++) {
    for (let x = Math.floor(cx - rx); x <= Math.ceil(cx + rx); x++) {
      if (!dentro(x, y)) continue
      const borde = !dentro(x - 1, y) || !dentro(x + 1, y) || !dentro(x, y - 1) || !dentro(x, y + 1)
      const color = borde ? 'o' : !dentro(x, y - 2) ? 'H' : !dentro(x, y + 2) ? 'g' : 'G'
      pintar(l, x, y, color)
    }
  }
}

function tierra(l) {
  const filas = [[11, 20, 'D'], [8, 23, 'D'], [6, 25, 'D'], [5, 26, 'd'], [6, 25, 'x']]
  filas.forEach(([a, b, c], j) => {
    for (let x = a; x <= b; x++) pintar(l, x, SUELO + j, c)
  })
  for (const [x, y] of [[13, 28], [19, 28], [10, 29], [22, 29], [16, 30], [8, 30], [24, 30]]) pintar(l, x, y, 'E')
}

export const TIERRA = (() => {
  const l = lienzo()
  tierra(l)
  return l
})()

// ---------------------------------------------------------------------------------------
// Sellos comunes
// ---------------------------------------------------------------------------------------

const SEMILLA = ['..H.', '.GG.', '.BB.', 'BBBB']
const YEMA = ['.oo.', 'oHGo', 'oGgo']
const COTILEDON = ['.ooo..', 'oGHGGo', '.oooo.']
const FRUTO = ['..g..', '.kkk.', 'kRrrk', 'krrrk', '.kkk.']
const FRUTO_VERDE = FRUTO.map((f) => f.replace(/k/g, 'j').replace(/r/g, 'f').replace(/R/g, 'F'))
const FLOR = ['..P..', '.pPp.', 'PpcpP', '.pPp.', '..p..']
const BOTON = ['.p.', 'gpg', '.g.']

function semilla() {
  const l = lienzo()
  sello(l, SEMILLA, 14, SUELO - 4)
  return l
}

// ---------------------------------------------------------------------------------------
// Arbusto (hojas en pares y ramas; da frutos o flores)
// ---------------------------------------------------------------------------------------

const HOJA = ['..ooo...', '.oGGGoo.', 'oGHHGGGo', 'oggggGo.', '.oooo...']
const HOJITA = ['.oo..', 'oGHGo', '.ooo.']

// (bx, by) = punto donde la hoja se une al tallo o la rama; `caida` = hoja doblada (marchita)
const hojaDer = (l, bx, by, caida) => sello(l, caida ? voltear(HOJA) : HOJA, bx, by - 2)
const hojaIzq = (l, bx, by, caida) => sello(l, caida ? voltear(espejo(HOJA)) : espejo(HOJA), bx - 7, by - 2)
const hojitaDer = (l, bx, by, caida) => sello(l, caida ? voltear(HOJITA) : HOJITA, bx, by - 1)
const hojitaIzq = (l, bx, by, caida) => sello(l, caida ? voltear(espejo(HOJITA)) : espejo(HOJITA), bx - 4, by - 1)

function tallo(l, arriba) {
  for (let y = arriba; y < SUELO; y++) {
    pintar(l, 15, y, 's')
    pintar(l, 16, y, 'q')
  }
}

const arbusto = {
  brote() {
    const l = lienzo()
    tallo(l, 20)
    sello(l, COTILEDON, 17, 18)
    sello(l, espejo(COTILEDON), 9, 18)
    sello(l, YEMA, 14, 17)
    return l
  },
  joven() {
    const l = lienzo()
    tallo(l, 12)
    hojaDer(l, 17, 23)
    hojaIzq(l, 14, 20)
    hojaDer(l, 17, 17)
    hojitaIzq(l, 14, 14)
    hojitaDer(l, 17, 12)
    sello(l, YEMA, 14, 9)
    return l
  },
  adulta(caida = false) {
    const l = lienzo()
    tallo(l, 5)
    linea(l, 14, 19, 10, 15, 's')
    linea(l, 17, 20, 21, 16, 'q')
    hojaDer(l, 17, 24, caida)
    hojaIzq(l, 14, 22, caida)
    hojaIzq(l, 10, 15, caida)
    hojaDer(l, 21, 16, caida)
    hojaDer(l, 17, 11, caida)
    hojaIzq(l, 14, 9, caida)
    hojitaDer(l, 17, 5, caida)
    hojitaIzq(l, 14, 6, caida)
    sello(l, YEMA, 14, 2)
    return l
  },
  // Esquina superior izquierda de cada fruto/flor (5x5)
  adornos: [[4, 16], [22, 17], [9, 10], [19, 12], [19, 3]],
  frondosa(mas) {
    const l = this.adulta()
    linea(l, 14, 25, 9, 22, 's')
    hojaIzq(l, 9, 22)
    linea(l, 17, 14, 21, 11, 'q')
    hojaDer(l, 21, 11)
    if (mas) {
      hojitaIzq(l, 10, 12)
      hojitaDer(l, 25, 20)
      hojitaDer(l, 21, 4)
    }
    return l
  },
}

// ---------------------------------------------------------------------------------------
// Suculenta (roseta de hojas carnosas; puede echar una vara con flores o hijuelos)
// ---------------------------------------------------------------------------------------

// Roseta apoyada en el suelo; `e` = escala (1 = adulta)
function roseta(l, cx, e) {
  const base = SUELO - 0.5
  elipse(l, cx, base - 9 * e, 2.6 * e, 5.5 * e) // hoja central, detras
  elipse(l, cx - 4.6 * e, base - 6.5 * e, 3 * e, 4.4 * e)
  elipse(l, cx + 4.6 * e, base - 6.5 * e, 3 * e, 4.4 * e)
  elipse(l, cx - 7.5 * e, base - 3 * e, 4.6 * e, 2.8 * e)
  elipse(l, cx + 7.5 * e, base - 3 * e, 4.6 * e, 2.8 * e)
  elipse(l, cx - 2.6 * e, base - 3.6 * e, 2.8 * e, 3.4 * e)
  elipse(l, cx + 2.6 * e, base - 3.6 * e, 2.8 * e, 3.4 * e) // hojas frontales, delante
}

const suculenta = {
  brote() {
    const l = lienzo()
    elipse(l, CENTRO, SUELO - 3.5, 1.6, 3)
    elipse(l, CENTRO - 2.6, SUELO - 1.6, 2.4, 1.5)
    elipse(l, CENTRO + 2.6, SUELO - 1.6, 2.4, 1.5)
    return l
  },
  joven() {
    const l = lienzo()
    roseta(l, CENTRO, 0.65)
    return l
  },
  adulta() {
    const l = lienzo()
    roseta(l, CENTRO, 1)
    return l
  },
  // Vara floral que sale del centro de la roseta
  vara(l) {
    linea(l, 16, 16, 17, 11, 'q')
    linea(l, 17, 11, 20, 6, 'q')
    linea(l, 20, 6, 21, 3, 'q')
    linea(l, 17, 11, 13, 7, 'q')
  },
  adornos: [[19, 0], [11, 4], [19, 6]],
  frondosa(mas) {
    const l = lienzo()
    if (mas) roseta(l, 5.5, 0.5)
    roseta(l, 26, 0.5)
    roseta(l, CENTRO, 0.9)
    return l
  },
}

// ---------------------------------------------------------------------------------------
// Monstera (peciolos largos y hojas grandes; de adulta con cortes y agujeros)
// ---------------------------------------------------------------------------------------

// Hoja acorazonada con nervio central; con `cortes` se abren ranuras en diagonal desde el
// borde hacia el nervio, como las hojas adultas de monstera
function hojaMonstera(l, cx, cy, rx, ry, cortes) {
  elipse(l, cx, cy, rx, ry)
  const x = Math.floor(cx)
  // Muesca del corazon donde entra el peciolo
  borrar(l, x, Math.ceil(cy + ry) - 1)
  linea(l, x, Math.round(cy + ry) - 2, x, Math.round(cy - ry) + 2, 'g')
  if (!cortes) return
  for (const lado of [-1, 1]) {
    for (const t of [-0.15, 0.45]) {
      const y0 = Math.round(cy + t * ry)
      const borde = x + lado * Math.round(rx)
      linea(l, borde, y0, x + lado * 2, y0 - 1, '.', borrar)
    }
  }
}

function peciolo(l, x, y) {
  linea(l, 15, SUELO - 1, x, y, 's')
  linea(l, 16, SUELO - 1, x + 1, y, 'q')
}

const monstera = {
  brote() {
    const l = lienzo()
    peciolo(l, 12, 21)
    peciolo(l, 19, 19)
    hojaMonstera(l, 12, 19, 2.8, 3, false)
    hojaMonstera(l, 19.5, 17, 2.8, 3, false)
    return l
  },
  joven() {
    const l = lienzo()
    const hojas = [[16, 8, 3.8, 4.2], [23, 14, 4, 4.4], [8.5, 16, 4, 4.4]]
    hojas.forEach(([x, y, , ry]) => peciolo(l, Math.floor(x), Math.round(y + ry) - 1))
    hojas.forEach(([x, y, rx, ry]) => hojaMonstera(l, x, y, rx, ry, false))
    return l
  },
  adulta(extra = 0) {
    const l = lienzo()
    const hojas = [
      [10.5, 7, 5, 5.5],
      [21.5, 6, 5, 5.5],
      [6.5, 16.5, 5, 5.5],
      [25.5, 16, 5, 5.5],
    ]
    if (extra >= 1) hojas.push([16, 17, 4.4, 4.6])
    hojas.forEach(([x, y, , ry]) => peciolo(l, Math.floor(x), Math.round(y + ry) - 1))
    hojas.forEach(([x, y, rx, ry]) => hojaMonstera(l, x, y, rx, ry, true))
    if (extra >= 2) {
      // Hoja nueva aun enrollada
      linea(l, 16, SUELO - 1, 16, 6, 'q')
      for (let y = 1; y <= 6; y++) {
        pintar(l, 15, y, 'o')
        pintar(l, 16, y, 'H')
        pintar(l, 17, y, 'o')
      }
      pintar(l, 16, 0, 'o')
    }
    return l
  },
  adornos: [],
  frondosa(mas) {
    return this.adulta(mas ? 2 : 1)
  },
}

const DIBUJOS = { arbusto, suculenta, monstera }

// ---------------------------------------------------------------------------------------
// Etapas
// ---------------------------------------------------------------------------------------

function conAdornos(l, posiciones, sprite, dx = 0, dy = 0) {
  for (const [x, y] of posiciones) sello(l, sprite, x + dx, y + dy)
  return l
}

const MARCHITAR = { o: 'u', g: 'w', G: 'W', H: 'Y', s: 'S', q: 'T' }

function marchita(dibujo, d) {
  // El arbusto dobla las hojas; el resto solo se seca
  const base = dibujo === 'arbusto' ? d.adulta(true) : d.adulta()
  return base.map((fila) => fila.map((c) => MARCHITAR[c] || c))
}

function construir(dibujo, ciclo) {
  const d = DIBUJOS[dibujo]
  let cuarta
  let quinta
  if (ciclo === 'fruto') {
    cuarta = conAdornos(d.adulta(), d.adornos, FRUTO_VERDE)
    quinta = conAdornos(d.adulta(), d.adornos, FRUTO)
  } else if (ciclo === 'flor') {
    const conVara = () => {
      const l = d.adulta()
      if (d.vara) d.vara(l)
      return l
    }
    cuarta = conAdornos(conVara(), d.adornos, BOTON, 1, 1)
    quinta = conAdornos(conVara(), d.adornos, FLOR)
  } else {
    cuarta = d.frondosa(false)
    quinta = d.frondosa(true)
  }
  // Mismo orden que las etapas de pages/user/jardin/etapas.js (la ultima es la marchita)
  return [semilla(), d.brote(), d.joven(), d.adulta(), cuarta, quinta, marchita(dibujo, d)]
}

const cache = new Map()

// Los 7 sprites (solo la planta, sin tierra) para una apariencia
export function spritesDe(apariencia) {
  const ap = aparienciaDe(apariencia)
  const clave = `${dibujoDe(ap)}:${cicloDe(ap)}`
  if (!cache.has(clave)) cache.set(clave, construir(dibujoDe(ap), cicloDe(ap)))
  return cache.get(clave)
}

// Une los pixeles contiguos del mismo color de cada fila: menos <rect> que dibujar
export function tramos(sprite, paleta) {
  const res = []
  sprite.forEach((fila, y) => {
    let x = 0
    while (x < LADO) {
      const c = fila[x]
      let fin = x + 1
      while (fin < LADO && fila[fin] === c) fin++
      if (c !== '.') res.push({ x, y, ancho: fin - x, color: paleta[c] })
      x = fin
    }
  })
  return res
}
