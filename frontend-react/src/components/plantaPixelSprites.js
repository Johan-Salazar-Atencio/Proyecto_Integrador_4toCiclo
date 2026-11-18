// Plantas en pixel art (64x64) para el jardin, generadas segun la APARIENCIA que da la IA al
// escanear: forma de crecimiento, color de hoja y si tiene flores o frutos (y de que color).
//
// Nada se dibuja pixel a pixel: cada planta se compone con piezas parametricas (hoja, tallo,
// cilindro, fronda, esfera) sobre una maceta, con rampas de 6 tonos que viran a frio en la
// sombra y a calido en la luz, luz desde arriba a la izquierda y contorno oscuro. Como las
// piezas son parametricas, el balanceo no rota el dibujo: se redibuja cada fotograma con las
// hojas un poco desplazadas, y la planta marchita dobla de verdad sus hojas.
import { paletaDeMaceta } from './macetas'

export const LADO = 64
export const FOTOGRAMAS = 12

const SUELO = 47 // fila donde nace la planta (tierra de la maceta)
const CX = 32
const LUZ = [-0.55, -0.83]
const CONTORNO = [44, 32, 40]

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

const HEX = /^#?([0-9a-f]{6}|[0-9a-f]{3})$/i

// '#rrggbb' valido o el valor por defecto (la IA o el cliente pueden mandar cualquier cosa)
function hex(valor, defecto) {
  const m = HEX.exec(String(valor ?? '').trim())
  if (!m) return defecto
  const h = m[1].length === 3 ? [...m[1]].map((c) => c + c).join('') : m[1]
  return `#${h.toLowerCase()}`
}

export function aparienciaDe(apariencia) {
  if (!apariencia || typeof apariencia !== 'object') return APARIENCIA_DEFECTO
  return {
    forma: FORMAS[apariencia.forma] ? apariencia.forma : 'arbusto',
    colorHoja: hex(apariencia.colorHoja, APARIENCIA_DEFECTO.colorHoja),
    tieneFlor: Boolean(apariencia.tieneFlor),
    colorFlor: hex(apariencia.colorFlor, null),
    tieneFruto: Boolean(apariencia.tieneFruto),
    colorFruto: hex(apariencia.colorFruto, null),
  }
}

const SOLO_FOLLAJE = new Set(['monstera', 'helecho', 'palmera', 'trepadora'])
const SOLO_FLOR = new Set(['suculenta', 'roseta', 'cactus'])

// Como "madura" la planta en sus dos ultimas etapas: dando fruto, floreciendo o solo con mas hojas
export function cicloDe(apariencia) {
  const ap = aparienciaDe(apariencia)
  if (SOLO_FOLLAJE.has(ap.forma)) return 'follaje'
  if (SOLO_FLOR.has(ap.forma)) return ap.tieneFlor || ap.tieneFruto ? 'flor' : 'follaje'
  if (ap.tieneFruto) return 'fruto'
  return ap.tieneFlor ? 'flor' : 'follaje'
}

// ---------------------------------------------------------------------------------------
// Color
// ---------------------------------------------------------------------------------------

const aRgb = (h) => {
  const n = parseInt(h.slice(1), 16)
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255]
}

function aHsl([r, g, b]) {
  r /= 255; g /= 255; b /= 255
  const max = Math.max(r, g, b)
  const min = Math.min(r, g, b)
  const l = (max + min) / 2
  if (max === min) return [0, 0, l]
  const d = max - min
  const s = l > 0.5 ? d / (2 - max - min) : d / (max + min)
  let h
  if (max === r) h = (g - b) / d + (g < b ? 6 : 0)
  else if (max === g) h = (b - r) / d + 2
  else h = (r - g) / d + 4
  return [h * 60, s, l]
}

function deHsl(h, s, l) {
  s = Math.max(0, Math.min(1, s))
  l = Math.max(0, Math.min(1, l))
  const c = (1 - Math.abs(2 * l - 1)) * s
  const x = c * (1 - Math.abs(((h / 60) % 2) - 1))
  const m = l - c / 2
  const [r, g, b] = h < 60 ? [c, x, 0] : h < 120 ? [x, c, 0] : h < 180 ? [0, c, x] : h < 240 ? [0, x, c] : h < 300 ? [x, 0, c] : [c, 0, x]
  return [Math.round((r + m) * 255), Math.round((g + m) * 255), Math.round((b + m) * 255)]
}

// Acerca el tono `h` al `objetivo` por el camino corto del circulo cromatico
const virar = (h, objetivo, t) => (h + ((((objetivo - h + 540) % 360) - 180) * t) + 360) % 360

function mezclar(a, b, t) {
  const [r1, g1, b1] = aRgb(a)
  const [r2, g2, b2] = aRgb(b)
  const c = (x, y) => Math.round(x * (1 - t) + y * t).toString(16).padStart(2, '0')
  return `#${c(r1, r2)}${c(g1, g2)}${c(b1, b2)}`
}

// Rampa de 6 tonos (0 = contorno ... 5 = brillo). La sombra vira a azul y la luz a amarillo.
function rampa(color, { hoja = false } = {}) {
  let [h, s, l] = aHsl(aRgb(color))
  if (hoja) {
    l = Math.max(0.3, Math.min(0.5, l))
    s = Math.max(0.22, Math.min(0.6, s))
  }
  const fria = Math.max(s, 0.16)
  return [
    deHsl(virar(h, 245, 0.34), fria * 1.05, l * 0.3),
    deHsl(virar(h, 230, 0.22), fria, l * 0.52),
    deHsl(virar(h, 215, 0.1), s, l * 0.76),
    deHsl(h, s, l),
    deHsl(virar(h, 62, 0.16), s * 0.95, l + (1 - l) * 0.24),
    deHsl(virar(h, 58, 0.28), s * 0.85, l + (1 - l) * 0.5),
  ]
}

// ---------------------------------------------------------------------------------------
// Lienzo
// ---------------------------------------------------------------------------------------

// d = RGBA; m = mascara de "solido" (lo que recibe contorno)
const lienzo = () => ({ d: new Uint8ClampedArray(LADO * LADO * 4), m: new Uint8Array(LADO * LADO) })

function pon(l, x, y, c, solido = 1, alfa = 255) {
  if (x < 0 || x >= LADO || y < 0 || y >= LADO) return
  const i = (y * LADO + x) * 4
  l.d[i] = c[0]; l.d[i + 1] = c[1]; l.d[i + 2] = c[2]; l.d[i + 3] = alfa
  l.m[y * LADO + x] = solido
}

const hay = (l, x, y) => x >= 0 && x < LADO && y >= 0 && y < LADO && l.d[(y * LADO + x) * 4 + 3] > 0

// Contorno oscuro alrededor de todo lo solido (solo sobre pixeles vacios)
function contornear(l) {
  const marca = []
  for (let y = 0; y < LADO; y++) {
    for (let x = 0; x < LADO; x++) {
      if (hay(l, x, y)) continue
      const s = (xx, yy) => xx >= 0 && xx < LADO && yy >= 0 && yy < LADO && l.m[yy * LADO + xx] === 1
      if (s(x - 1, y) || s(x + 1, y) || s(x, y - 1) || s(x, y + 1)) marca.push([x, y])
    }
  }
  for (const [x, y] of marca) pon(l, x, y, CONTORNO, 2)
}

// Azar con semilla: cada fotograma repite exactamente las mismas decisiones
function azar(semilla) {
  let a = semilla >>> 0
  return () => {
    a = (a + 0x6d2b79f5) | 0
    let t = Math.imul(a ^ (a >>> 15), 1 | a)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

// ---------------------------------------------------------------------------------------
// Piezas
// ---------------------------------------------------------------------------------------

const fase = (P) => (2 * Math.PI * P.k) / FOTOGRAMAS

// Viento: desplaza un punto segun su altura sobre la maceta (la base no se mueve)
function mec(P, x, y) {
  const h = Math.max(0, SUELO - y) / 44
  const f = fase(P)
  let dx = P.amp * (h ** 1.4 * Math.sin(f) + 0.25 * h * Math.sin(2 * f + 1.3))
  if (P.marchita) dx = dx * 0.3 + P.caida * h * h
  return [x + dx, y + Math.abs(dx) * 0.12]
}

const bez = (x0, y0, cx, cy, x1, y1) => (t) => [
  (1 - t) ** 2 * x0 + 2 * (1 - t) * t * cx + t * t * x1,
  (1 - t) ** 2 * y0 + 2 * (1 - t) * t * cy + t * t * y1,
]

function muestrear(curva, n) {
  const pts = []
  for (let i = 0; i <= n; i++) pts.push(curva(i / n))
  return pts
}

// Punto de la polilinea mas cercano a (x, y): distancia, parametro 0..1, lado y direccion
function cercano(pts, x, y) {
  const n = pts.length - 1
  const r = { d: 1e9, t: 0, lado: 1, dx: 0, dy: -1, atras: false, tNeg: 0, dNeg: 0 }
  for (let i = 0; i < n; i++) {
    const [ax, ay] = pts[i]
    const vx = pts[i + 1][0] - ax
    const vy = pts[i + 1][1] - ay
    const l2 = vx * vx + vy * vy || 1e-6
    const u = ((x - ax) * vx + (y - ay) * vy) / l2
    const uc = Math.max(0, Math.min(1, u))
    const d2 = (x - ax - vx * uc) ** 2 + (y - ay - vy * uc) ** 2
    if (d2 < r.d) {
      const cruz = vx * (y - ay) - vy * (x - ax)
      const largo = Math.sqrt(l2)
      r.d = d2; r.t = (i + uc) / n; r.lado = cruz >= 0 ? 1 : -1
      r.dx = vx / largo; r.dy = vy / largo
      r.atras = i === 0 && u < 0
      r.tNeg = u / n; r.dNeg = Math.abs(cruz) / largo
    }
  }
  r.d = Math.sqrt(r.d)
  return r
}

// Ancho relativo (0..1) de la hoja a lo largo del nervio (t: 0 = base, 1 = punta)
const PERFIL = {
  ovada: (t) => (t <= 0 || t >= 1 ? 0 : Math.sin(Math.PI * t ** 0.75) ** 0.8),
  lanza: (t) => (t <= 0 || t >= 1 ? 0 : Math.sin(Math.PI * t ** 0.9) ** 1.15),
  gorda: (t) => (t <= 0 || t >= 1 ? 0 : Math.sin(Math.PI * t ** 0.62) ** 0.5),
  corazon: (t) => {
    if (t >= 1 || t <= -0.14) return 0
    if (t > 0.2) return Math.cos(((t - 0.2) / 0.8) * (Math.PI / 2)) ** 0.85
    return Math.sqrt(Math.max(0, 1 - ((0.2 - t) / 0.34) ** 2))
  },
}

// Hoja: nace en (bx, by), apunta a `ang` grados (0 = arriba, negativo = izquierda) y mide
// `len` de largo y `ancho` de medio ancho. Con `cortes` se abren ranuras de monstera.
function hoja(P, bx, by, ang, len, ancho, o = {}) {
  const { perfil = 'ovada', capa = 0, cortes = 0, agujeros = false, punta = null, curva = 0.14, dientes = 0 } = o
  let R = o.rampa || P.R.hoja
  const desfase = P.rng() * 6.283
  let a = ang
  let L = len
  let cv = curva
  if (P.marchita) {
    const sentido = a === 0 ? (P.rng() < 0.5 ? -1 : 1) : Math.sign(a)
    a = Math.max(-168, Math.min(168, a + sentido * (36 + P.rng() * 24)))
    cv = curva + 0.14
    L = len * 0.94
    if (R === P.R.hoja && P.rng() < 0.3) R = P.R.amarilla
  } else {
    a += P.aleteo * Math.sin(fase(P) + desfase)
  }
  const [x0, y0] = mec(P, bx, by)
  const rad = (a * Math.PI) / 180
  const dx = Math.sin(rad)
  const dy = -Math.cos(rad)
  const x1 = x0 + dx * L
  const y1 = y0 + dy * L
  let nx = -dy
  let ny = dx
  if (ny > 0) { nx = -nx; ny = -ny }
  const k = cv * L * Math.min(1, Math.abs(dx) * 1.5)
  const pts = muestrear(bez(x0, y0, (x0 + x1) / 2 + nx * k, (y0 + y1) / 2 + ny * k, x1, y1), 14)
  const ext = perfil === 'corazon' ? 0.14 : 0
  const w = PERFIL[perfil]
  const fino = ancho < 2.3
  const nervio = o.nervio ?? ancho >= 5
  const memo = new Map()

  const info = (x, y) => {
    const clave = y * 256 + x
    if (memo.has(clave)) return memo.get(clave)
    const q = cercano(pts, x, y)
    let t = q.t
    let d = q.d
    let p = null
    if (ext && q.atras) { t = q.tNeg; d = q.dNeg }
    // Borde dentado (frondas): el ancho alterna cada `dientes` pixeles a lo largo del nervio
    const diente = dientes && Math.floor((t * L) / dientes) % 2 === 0 ? 0.6 : 1
    const hw = w(t) * ancho * diente
    const dentro = (!q.atras || ext) && (d <= hw || (d <= 0.5 && t > 0.03 && t < 0.97))
    if (dentro && !(ext && t < 0.05 && d < 0.75)) {
      const s = hw > 0 ? Math.min(1, d / hw) : 0
      let hueco = false
      if (cortes && t > 0.2 && t < 0.88) {
        for (let i = 0; i < cortes && !hueco; i++) {
          const ti = 0.27 + ((i + 0.5) * 0.56) / cortes
          if (s > 0.36 && Math.abs(t - ti - 0.1 * (s - 0.36)) < 0.55 / L) hueco = true
          if (agujeros && s > 0.13 && s < 0.34 && Math.abs(t - ti + 0.07) < 0.8 / L) hueco = true
        }
      }
      p = { t, s, d, lado: q.lado, dx: q.dx, dy: q.dy, hueco }
    }
    memo.set(clave, p)
    return p
  }

  const m = Math.ceil(ancho) + 2
  const xs = pts.map((p) => p[0])
  const ys = pts.map((p) => p[1])
  for (let y = Math.floor(Math.min(...ys)) - m; y <= Math.ceil(Math.max(...ys)) + m; y++) {
    for (let x = Math.floor(Math.min(...xs)) - m; x <= Math.ceil(Math.max(...xs)) + m; x++) {
      const p = info(x, y)
      if (!p) continue
      const borde = !info(x - 1, y) || !info(x + 1, y) || !info(x, y - 1) || !info(x, y + 1)
      const luz = (-p.dy * LUZ[0] + p.dx * LUZ[1]) * p.lado > 0
      let tono
      if (p.hueco) tono = 0
      else if (fino) tono = p.t > 0.8 ? 4 : luz ? 4 : 3
      else if (borde) tono = o.marcado ? (luz ? 1 : 0) : luz ? 2 : 1
      else if (nervio && p.d < 0.6 && p.t > 0.06 && p.t < 0.82) tono = o.nervioTono ?? 5
      else if (luz) tono = p.s > 0.3 && p.s < 0.6 && p.t > 0.26 && p.t < 0.46 && ancho >= 4 ? 5 : 4
      else tono = p.s > 0.6 ? 2 : 3
      tono = Math.max(p.hueco ? 0 : 1, tono - capa)
      pon(P.l, x, y, punta && p.t > 0.88 ? punta[tono] : R[tono])
    }
  }
  return [x1, y1]
}

// Tallo o rama: curva de 1 o 2 pixeles de grosor
function curva(P, x0, y0, cx, cy, x1, y1, o = {}) {
  const { grosor = 1, capa = 0 } = o
  const R = P.marchita && !o.rampa ? P.R.talloSeco : o.rampa || P.R.tallo
  const a = mec(P, x0, y0)
  const c = mec(P, cx, cy)
  const b = mec(P, x1, y1)
  const n = Math.ceil((Math.abs(x1 - x0) + Math.abs(y1 - y0) + Math.abs(cx - x0)) * 1.5) + 2
  const horizontal = Math.abs(x1 - x0) > Math.abs(y1 - y0)
  for (const [x, y] of muestrear(bez(a[0], a[1], c[0], c[1], b[0], b[1]), n)) {
    const px = Math.round(x)
    const py = Math.round(y)
    if (o.tope != null && py > o.tope) continue
    if (grosor === 1) {
      pon(P.l, px, py, R[Math.max(1, 2 - capa)])
    } else if (horizontal) {
      pon(P.l, px, py, R[3])
      pon(P.l, px, py + 1, R[1])
    } else {
      pon(P.l, px, py, R[3])
      pon(P.l, px + 1, py, R[1])
    }
  }
}

// Cilindro con extremos redondos y sombreado de volumen (cactus, troncos, tutor)
function cilindro(P, x0, y0, x1, y1, r0, r1, o = {}) {
  const { costillas = false, anillos = false, tope = LADO } = o
  const R = o.rampa || P.R.hoja
  const a = mec(P, x0, y0)
  const b = mec(P, x1, y1)
  const vx = b[0] - a[0]
  const vy = b[1] - a[1]
  const l2 = vx * vx + vy * vy || 1e-6
  const largo = Math.sqrt(l2)
  const rm = Math.max(r0, r1) + 1
  const dato = (x, y) => {
    const u = Math.max(0, Math.min(1, ((x - a[0]) * vx + (y - a[1]) * vy) / l2))
    const d = Math.hypot(x - a[0] - vx * u, y - a[1] - vy * u)
    const r = r0 + (r1 - r0) * u
    return y <= tope && d <= r + 0.15 ? { u, d, r } : null
  }
  for (let y = Math.floor(Math.min(a[1], b[1]) - rm); y <= Math.ceil(Math.max(a[1], b[1]) + rm); y++) {
    for (let x = Math.floor(Math.min(a[0], b[0]) - rm); x <= Math.ceil(Math.max(a[0], b[0]) + rm); x++) {
      const p = dato(x, y)
      if (!p) continue
      const lado = vx * (y - a[1]) - vy * (x - a[0]) >= 0 ? 1 : -1
      const luz = ((-vy / largo) * LUZ[0] + (vx / largo) * LUZ[1]) * lado > 0
      const s = Math.min(1, p.d / p.r)
      const mm = luz ? s : -s
      const borde = !dato(x - 1, y) || !dato(x + 1, y) || !dato(x, y - 1) || (y < tope && !dato(x, y + 1))
      let tono
      if (borde) tono = luz ? 1 : 0
      else tono = mm > 0.82 ? 3 : mm > 0.56 ? 4 : mm > 0.38 ? 5 : mm > 0.12 ? 4 : mm > -0.3 ? 3 : mm > -0.7 ? 2 : 1
      const lateral = Math.round(s * p.r * lado)
      const eje = Math.round(p.u * largo)
      let color = R[tono]
      if (!borde) {
        if (costillas && ((lateral % 3) + 3) % 3 === 1) {
          color = eje % 4 === 1 && o.espinas ? o.espinas : R[Math.max(1, tono - 1)]
        }
        if (anillos && eje % 3 === 0) color = R[Math.max(1, tono - 1)]
      }
      pon(P.l, x, y, color)
    }
  }
}

// Esfera sombreada (frutos, botones, copa de arbol)
function disco(P, cx, cy, r, R, o = {}) {
  const [x0, y0] = o.fijo ? [cx, cy] : mec(P, cx, cy)
  const dentro = (x, y) => (x - x0) ** 2 + (y - y0) ** 2 <= r * r
  for (let y = Math.floor(y0 - r); y <= Math.ceil(y0 + r); y++) {
    for (let x = Math.floor(x0 - r); x <= Math.ceil(x0 + r); x++) {
      if (!dentro(x, y) || (o.tope != null && y > o.tope)) continue
      const luz = -((x - x0) * 0.6 + (y - y0) * 0.75) / r
      const borde = !dentro(x - 1, y) || !dentro(x + 1, y) || !dentro(x, y - 1) || !dentro(x, y + 1)
      let tono = luz > 0.5 ? 5 : luz > 0.15 ? 4 : luz > -0.3 ? 3 : luz > -0.62 ? 2 : 1
      if (borde && o.borde !== false) tono = luz > 0 ? Math.min(tono, 2) : 0
      pon(P.l, x, y, R[Math.max(0, tono - (o.capa || 0))])
    }
  }
}

function flor(P, x, y) {
  const [cx, cy] = mec(P, x, y)
  const R = P.R.flor
  for (let i = 0; i < 5; i++) {
    const a = ((-90 + i * 72) * Math.PI) / 180
    const px = cx + Math.cos(a) * 2.1
    const py = cy + Math.sin(a) * 2.1
    for (let yy = Math.floor(py - 1.6); yy <= Math.ceil(py + 1.6); yy++) {
      for (let xx = Math.floor(px - 1.6); xx <= Math.ceil(px + 1.6); xx++) {
        if ((xx - px) ** 2 + (yy - py) ** 2 > 2.3) continue
        const luz = -((xx - cx) * 0.6 + (yy - cy) * 0.75)
        pon(P.l, xx, yy, R[luz > 1.4 ? 5 : luz > -0.4 ? 4 : luz > -1.8 ? 3 : 2])
      }
    }
  }
  const ox = Math.round(cx)
  const oy = Math.round(cy)
  pon(P.l, ox, oy, P.R.centro[4]); pon(P.l, ox - 1, oy, P.R.centro[5])
  pon(P.l, ox, oy + 1, P.R.centro[2]); pon(P.l, ox - 1, oy + 1, P.R.centro[3])
}

function boton(P, x, y) {
  disco(P, x, y, 1.5, P.R.flor)
  const [cx, cy] = mec(P, x, y)
  pon(P.l, Math.round(cx), Math.round(cy + 2), P.R.hoja[2])
  pon(P.l, Math.round(cx - 1), Math.round(cy + 1), P.R.hoja[1])
}

function fruto(P, x, y, maduro) {
  disco(P, x, y, maduro ? 2.7 : 1.9, maduro ? P.R.fruto : P.R.verde)
  const [cx, cy] = mec(P, x, y)
  pon(P.l, Math.round(cx), Math.round(cy - (maduro ? 3 : 2)), P.R.hoja[1])
}

// ---------------------------------------------------------------------------------------
// Maceta
// ---------------------------------------------------------------------------------------

const TONO_MACETA = (u) => (u < 0.08 ? 2 : u < 0.2 ? 4 : u < 0.3 ? 5 : u < 0.44 ? 4 : u < 0.64 ? 3 : u < 0.84 ? 2 : 1)

function maceta(l, estilo) {
  const R = rampa(estilo.color)
  const A = rampa(estilo.acento || '#efe3c8')
  const T = rampa('#5b3a2a')
  // Tono segun la posicion horizontal, con tramado en los cambios de banda
  const tono = (x, y, x0, x1, extra = 0) => {
    const u = (x - x0) / (x1 - x0)
    const sig = TONO_MACETA(Math.min(1, u + 0.6 / (x1 - x0)))
    const base = (x + y) % 2 === 0 ? sig : TONO_MACETA(u)
    return Math.max(1, Math.min(5, base + extra))
  }
  const fila = (y, x0, x1, extra, rampaFila = R) => {
    for (let x = x0; x <= x1; x++) {
      const borde = x === x0 ? 1 : x === x1 ? 0 : null
      pon(l, x, y, rampaFila[borde ?? tono(x, y, x0, x1, extra)])
    }
  }
  // Borde trasero y tierra
  for (let x = 21; x <= 42; x++) pon(l, x, 45, R[1])
  for (const [y, x0, x1] of [[46, 19, 44], [47, 18, 45]]) {
    for (let x = x0; x <= x1; x++) {
      const dentro = x >= x0 + 2 && x <= x1 - 2
      const mota = (x * 7 + y * 13) % 5 === 0
      pon(l, x, y, dentro ? T[mota ? 3 : y === 46 ? 1 : 2] : R[x < CX ? 4 : 2])
    }
  }
  // Borde delantero, sombra bajo el borde y cuerpo
  fila(48, 18, 45, 1)
  fila(49, 18, 45, 1)
  fila(50, 18, 45, 0)
  fila(51, 20, 43, -2)
  for (let y = 52; y <= 61; y++) {
    const dentro = Math.floor((y - 52) / 3.4)
    const banda = estilo.banda && (y === 55 || y === 56)
    fila(y, 20 + dentro, 43 - dentro, y >= 60 ? -1 : 0, banda ? A : R)
  }
  for (let x = 23; x <= 40; x++) pon(l, x, 61, R[x > 36 ? 0 : 1])
}

function sombra(l) {
  for (const [y, x0, x1, alfa] of [[62, 19, 44, 70], [63, 23, 40, 38]]) {
    for (let x = x0; x <= x1; x++) if (!hay(l, x, y)) pon(l, x, y, [30, 22, 34], 0, alfa)
  }
}

// ---------------------------------------------------------------------------------------
// Plantas (e = etapa 1..5; devuelven los puntos donde van frutos o flores)
// ---------------------------------------------------------------------------------------

function semilla(P) {
  disco(P, CX - 1.5, SUELO - 1.4, 2.4, P.R.semilla, { fijo: true })
  pon(P.l, CX - 1, SUELO - 4, P.R.semilla[1])
  curva(P, CX, SUELO - 3, CX + 1, SUELO - 5, CX + 1.5, SUELO - 7, { rampa: P.R.brote })
  hoja(P, CX + 1.5, SUELO - 7, 42, 3.4, 1.6, { rampa: P.R.brote })
  return []
}

// Rama lateral del arbusto: hojas alternas y un penacho en la punta
function rama(P, x0, y0, ang, len, largo, ancho, puntos) {
  const rad = (ang * Math.PI) / 180
  const x1 = x0 + Math.sin(rad) * len
  const y1 = y0 - Math.cos(rad) * len
  const lado = Math.sign(ang)
  const en = (t) => [x0 + (x1 - x0) * t, y0 + (y1 - y0) * t]
  hoja(P, ...en(0.6), ang + lado * 34, largo, ancho, { capa: 1 })
  hoja(P, ...en(0.9), ang - lado * 40, largo, ancho, { capa: 1 })
  curva(P, x0, y0, (x0 + x1) / 2, (y0 + y1) / 2 - len * 0.1, x1, y1)
  hoja(P, ...en(0.45), ang + lado * 60, largo * 0.9, ancho)
  hoja(P, ...en(0.7), ang - lado * 52, largo * 0.85, ancho)
  hoja(P, x1, y1, ang + lado * 30, largo, ancho)
  hoja(P, x1, y1, ang - lado * 26, largo * 0.9, ancho)
  hoja(P, x1, y1, ang, largo * 0.7, ancho * 0.8, { rampa: P.R.brote })
  puntos.push([x1 + lado * 2, y1 + 4], [(x0 + x1) / 2 + lado, (y0 + y1) / 2 + 4])
}

function arbusto(P, e) {
  const alto = [0, 7, 14, 20, 24, 26][e]
  const largo = [0, 7, 9, 9.5, 10, 10.5][e]
  const ancho = [0, 2.6, 3.2, 3.5, 3.7, 3.9][e]
  const eje = bez(CX, SUELO, CX - 2, SUELO - alto * 0.5, CX + 0.5, SUELO - alto)
  const [tx, ty] = eje(1)
  const puntos = []
  const ramas = e <= 2 ? [] : e === 3 ? [[0.3, -52, 12], [0.44, 50, 12]] : [[0.22, -58, 15], [0.32, 56, 16], [0.54, -38, 12], [0.64, 40, 11]]
  if (e >= 2) {
    for (const f of [0.55, 0.85]) {
      const [x, y] = eje(f)
      hoja(P, x, y, -(34 + P.rng() * 16), largo, ancho, { capa: 1 })
      hoja(P, x, y, 36 + P.rng() * 16, largo, ancho, { capa: 1 })
    }
  }
  curva(P, CX, SUELO, CX - 2, SUELO - alto * 0.5, CX + 0.5, SUELO - alto, { grosor: e >= 3 ? 2 : 1 })
  for (let i = ramas.length - 1; i >= 0; i--) {
    const [f, ang, len] = ramas[i]
    const [x, y] = eje(f)
    rama(P, x, y, ang, len + (e === 5 ? 1 : 0), largo, ancho, puntos)
  }
  if (e === 2) {
    for (const [f, ang] of [[0.45, 78], [0.75, 62]]) {
      const [x, y] = eje(f)
      hoja(P, x, y, -ang, largo, ancho)
      hoja(P, x + 1, y - 1, ang, largo, ancho)
    }
  }
  if (e >= 2) {
    hoja(P, tx, ty + 2, -56, largo * 0.95, ancho)
    hoja(P, tx, ty + 2, 54, largo * 0.95, ancho)
  }
  hoja(P, tx, ty, e === 1 ? -62 : -22, largo * 0.8, ancho * 0.85, e === 1 ? {} : { rampa: P.R.brote })
  hoja(P, tx, ty, e === 1 ? 60 : 20, largo * 0.8, ancho * 0.85, e === 1 ? {} : { rampa: P.R.brote })
  puntos.push([tx + 3, ty + 5])
  return puntos.slice(0, 7)
}

const MONSTERA = [
  null,
  [[-3, -7, -50, 7.5, 3.8, 0, 0], [3, -10, 40, 8, 4, 0, 0]],
  [[0, -22, 6, 10, 5, 1, 0], [-5, -12, -82, 11, 5.4, 0, 0], [5, -15, 70, 11.5, 5.6, 0, 0]],
  [[0, -29, 4, 13, 6.6, 1, 2], [-6, -13, -112, 14, 7, 0, 2], [6, -15, 108, 14.5, 7.2, 0, 2], [-4, -25, -52, 14, 7, 0, 3], [5, -27, 46, 13.5, 6.8, 0, 3]],
  [[0, -31, 4, 14, 7, 1, 3], [-11, -22, -78, 12, 6, 1, 2], [11, -23, 76, 12, 6, 1, 2], [-6, -13, -114, 15, 7.6, 0, 3, 1], [6, -15, 110, 15.5, 7.8, 0, 3, 1], [-4, -26, -50, 15, 7.5, 0, 3, 1], [5, -28, 44, 14.5, 7.3, 0, 3, 1], [-1, -9, -152, 8, 4.4, 0, 0]],
]
MONSTERA[5] = [...MONSTERA[4], [2, -11, 146, 8.5, 4.6, 0, 0]]

function monstera(P, e) {
  const hojas = MONSTERA[e]
  const peciolo = ([dx, dy, , , , capa]) =>
    curva(P, CX + dx * 0.15, SUELO, CX + dx * 0.3, SUELO + dy * 0.6, CX + dx, SUELO + dy, { capa })
  const pintar = ([dx, dy, ang, len, ancho, capa, cortes, agujeros]) =>
    hoja(P, CX + dx, SUELO + dy, ang, len, ancho, { perfil: 'corazon', capa, cortes, agujeros: Boolean(agujeros), curva: 0.08, nervio: ancho >= 5 })
  hojas.filter((h) => h[5] === 1).forEach((h) => { peciolo(h); pintar(h) })
  hojas.filter((h) => h[5] === 0).forEach(peciolo)
  if (e === 5) {
    // Hoja nueva aun enrollada
    curva(P, CX + 1, SUELO, CX + 3, SUELO - 12, CX + 2, SUELO - 22)
    hoja(P, CX + 2, SUELO - 22, 10, 11, 1.8, { perfil: 'lanza', rampa: P.R.brote, nervio: false })
  }
  hojas.filter((h) => h[5] === 0).forEach(pintar)
  return []
}

function roseta(P, x, y, s) {
  const o = { perfil: 'gorda', curva: 0.03, punta: P.marchita ? null : P.R.punta, marcado: true, nervio: false }
  for (const a of [-24, 24]) hoja(P, x, y - 3 * s, a, 15.5 * s, 4.2 * s, { ...o, capa: 1 })
  hoja(P, x, y - 3 * s, 0, 16.5 * s, 4.4 * s, { ...o, capa: 1 })
  for (const lado of [-1, 1]) hoja(P, x + lado * s, y - 1.5 * s, lado * 52, 15 * s, 4.8 * s, o)
  for (const lado of [-1, 1]) hoja(P, x + lado * 2 * s, y, lado * 82, 13.5 * s, 4.8 * s, o)
  for (const lado of [-1, 1]) hoja(P, x + lado * 1.5, y, lado * 30, 10.5 * s, 4.6 * s, o)
  hoja(P, x, y, 0, 7 * s, 4 * s, o)
}

function suculenta(P, e, ciclo) {
  const florece = ciclo !== 'follaje' && e >= 4
  const s = florece ? 1.02 : [0, 0.42, 0.7, 1, 1.08, 1.12][e]
  if (florece) {
    curva(P, CX, SUELO - 8, CX + 3, SUELO - 22, CX + 7, SUELO - 30, { rampa: P.R.punta })
    curva(P, CX + 3.5, SUELO - 22, CX + 8, SUELO - 24, CX + 10, SUELO - 26, { rampa: P.R.punta })
    curva(P, CX + 2.5, SUELO - 18, CX - 1, SUELO - 22, CX - 3, SUELO - 24, { rampa: P.R.punta })
  } else if (e >= 4) {
    roseta(P, 23, SUELO, 0.36)
    if (e >= 5) roseta(P, 41.5, SUELO, 0.38)
  }
  roseta(P, CX, SUELO, s)
  return florece ? [[CX + 7, SUELO - 32], [CX + 11, SUELO - 27], [CX - 4, SUELO - 25]] : []
}

const ESPINA = [244, 236, 208]

function cactus(P, e, ciclo) {
  const h = [0, 5, 12, 20, 23, 25][e]
  const r = [0, 3.4, 4.6, 5.8, 6.2, 6.4][e]
  const o = { costillas: true, espinas: P.marchita ? null : ESPINA, tope: SUELO }
  const puntos = [[CX, SUELO - h - r - 1]]
  if (e >= 3) {
    const y = SUELO - h * 0.42
    const alto = e >= 4 ? 9 : 6
    cilindro(P, CX - r + 1, y, CX - r - 4.5, y, 2.4, 2.4, o)
    cilindro(P, CX - r - 4.5, y, CX - r - 4.5, y - alto, 2.6, 2.4, o)
    puntos.push([CX - r - 4.5, y - alto - 4])
  }
  if (e >= 4) {
    const y = SUELO - h * 0.62
    const alto = e >= 5 ? 8 : 6
    cilindro(P, CX + r - 1, y, CX + r + 4, y, 2.2, 2.2, o)
    cilindro(P, CX + r + 4, y, CX + r + 4, y - alto, 2.4, 2.2, o)
    puntos.push([CX + r + 4, y - alto - 4])
  }
  cilindro(P, CX, SUELO, CX, SUELO - h, r, r * 0.94, o)
  if (ciclo === 'follaje' && e >= 5) cilindro(P, CX + 8, SUELO, CX + 8, SUELO - 3, 2.6, 2.6, o)
  return puntos
}

function helecho(P, e) {
  const n = [0, 3, 5, 7, 9, 11][e]
  const len = [0, 9, 15, 21, 24, 26][e]
  const ancho = [0, 2, 2.6, 3.3, 3.6, 3.8][e]
  const frondas = []
  for (let i = 0; i < n; i++) {
    const a = -88 + (176 * (i + 0.5)) / n + (P.rng() - 0.5) * 10
    frondas.push({ a, largo: len * (0.82 + 0.3 * Math.cos((a * Math.PI) / 180)) + P.rng() * 2, capa: i % 2 })
  }
  for (const capa of [1, 0]) {
    for (const f of frondas.filter((x) => x.capa === capa)) {
      hoja(P, CX + f.a / 30, SUELO, f.a, f.largo, ancho, { perfil: 'lanza', capa, dientes: 2, curva: 0.3, nervio: true, nervioTono: 2 })
    }
  }
  if (e >= 5 && !P.marchita) {
    // Fronda nueva enrollada
    curva(P, CX + 1, SUELO, CX + 3, SUELO - 5, CX + 2, SUELO - 11, { rampa: P.R.brote })
    const [x, y] = mec(P, CX + 2, SUELO - 11)
    for (const [dx, dy] of [[1, -1], [2, -2], [1, -3], [0, -2]]) pon(P.l, Math.round(x + dx), Math.round(y + dy), P.R.brote[4])
  }
  return []
}

function palmera(P, e) {
  const fr = { perfil: 'lanza', dientes: 2, curva: 0.34, nervio: true, nervioTono: 2 }
  if (e <= 2) {
    const hojas = e === 1 ? [[-16, 11], [18, 9]] : [[-46, 14], [-15, 17], [17, 16], [48, 13]]
    for (const [a, len] of hojas) hoja(P, CX, SUELO, a, len, e === 1 ? 2.4 : 3, e === 1 ? { perfil: 'lanza', curva: 0.2 } : fr)
    return []
  }
  const alto = [0, 0, 0, 10, 14, 17][e]
  const n = [0, 0, 0, 6, 7, 9][e]
  const len = [0, 0, 0, 18, 20, 21][e]
  const cy = SUELO - alto - 1
  const frondas = []
  for (let i = 0; i < n; i++) {
    frondas.push({ a: -116 + (232 * (i + 0.5)) / n + (P.rng() - 0.5) * 8, largo: len + P.rng() * 2, capa: i % 2 })
  }
  for (const f of frondas.filter((x) => x.capa === 1)) hoja(P, CX + 1, cy, f.a, f.largo, 4, { ...fr, capa: 1 })
  cilindro(P, CX, SUELO, CX + 1, SUELO - alto, 2.4, 1.9, { rampa: P.R.tronco, anillos: true, tope: SUELO })
  for (const f of frondas.filter((x) => x.capa === 0)) hoja(P, CX + 1, cy, f.a, f.largo, 4, { ...fr, capa: 0 })
  disco(P, CX + 1, cy, 2, P.R.tronco)
  return []
}

function copa(P, cx, cy, r, capa) {
  const n = Math.round(r * (capa ? 1.3 : 1.7))
  if (!capa) disco(P, cx, cy, r * 0.72, P.R.hoja, { borde: false, capa: 1 })
  for (let i = 0; i < n; i++) {
    const a = (i * 360) / n + P.rng() * 30
    const rad = (a * Math.PI) / 180
    const d = r * (capa ? 0.3 : 0.15)
    const ang = a > 180 ? a - 360 : a
    hoja(P, cx + Math.sin(rad) * d, cy - Math.cos(rad) * d, ang, r * 0.98, 2.7, { capa, nervio: false })
  }
}

function arbol(P, e) {
  const alto = [0, 7, 13, 17, 19, 20][e]
  const cima = SUELO - alto
  if (e === 1) {
    curva(P, CX, SUELO, CX - 1, SUELO - 4, CX + 1, cima, { rampa: P.R.tronco })
    hoja(P, CX + 1, cima, -44, 5.5, 2.5)
    hoja(P, CX + 1, cima, 40, 6, 2.6)
    return []
  }
  if (e === 2) {
    copa(P, CX + 1, cima - 2, 5.5, 1)
    curva(P, CX, SUELO, CX - 1, SUELO - 6, CX + 1, cima, { rampa: P.R.tronco, grosor: 2 })
    copa(P, CX + 1, cima - 2, 5.5, 0)
    return []
  }
  const mas = e >= 4 ? 1 : 0
  const copas = [[CX - 8, cima - 6, 6.5 + mas], [CX + 9, cima - 7, 6.5 + mas], [CX + 1, cima - 12, 7.5 + mas]]
  for (const [x, y, r] of copas) copa(P, x, y, r, 1)
  curva(P, CX, cima + 4, CX - 5, cima + 1, CX - 8, cima - 5, { rampa: P.R.tronco, grosor: 2 })
  curva(P, CX + 1, cima + 5, CX + 6, cima + 1, CX + 9, cima - 6, { rampa: P.R.tronco, grosor: 2 })
  cilindro(P, CX, SUELO, CX + 1, cima - 2, 2.3, 1.5, { rampa: P.R.tronco, tope: SUELO })
  for (const [x, y, r] of copas) copa(P, x, y, r, 0)
  if (e >= 4) copa(P, CX + 1, cima - 4, 5.5, 0)
  return [[CX - 11, cima - 4], [CX + 12, cima - 5], [CX - 2, cima - 15], [CX + 5, cima - 9], [CX - 5, cima - 8], [CX + 9, cima - 11]]
}

function trepadora(P, e) {
  const cor = { perfil: 'corazon', curva: 0.06 }
  if (e === 1) {
    curva(P, CX, SUELO, CX - 1, SUELO - 3, CX + 1, SUELO - 6)
    hoja(P, CX, SUELO - 4, 62, 5.5, 3, cor)
    hoja(P, CX + 1, SUELO - 6, -52, 6, 3.2, cor)
    return []
  }
  const alto = [0, 0, 15, 26, 32, 36][e]
  const n = [0, 0, 4, 7, 9, 11][e]
  cilindro(P, CX, SUELO, CX, SUELO - alto, 1.7, 1.6, { rampa: P.R.tutor, anillos: true, tope: SUELO })
  const nudos = []
  for (let i = 0; i < n; i++) {
    const f = (i + 0.5) / n
    const lado = i % 2 ? 1 : -1
    nudos.push({ x: CX + lado * 1.5, y: SUELO - 2 - f * (alto - 1), lado, len: 7.8 - 2.2 * f + P.rng() })
  }
  let [px, py] = [CX, SUELO]
  for (const nudo of nudos) {
    curva(P, px, py, (px + nudo.x) / 2 + nudo.lado * 2, (py + nudo.y) / 2, nudo.x, nudo.y)
    px = nudo.x
    py = nudo.y
  }
  // Ramas que cuelgan por fuera de la maceta
  const cuelga = (lado, caida) => {
    const fin = [CX + lado * 15, SUELO + caida]
    curva(P, CX + lado * 4, SUELO, CX + lado * 14, SUELO - 6, fin[0], fin[1])
    const camino = bez(CX + lado * 4, SUELO, CX + lado * 14, SUELO - 6, fin[0], fin[1])
    for (const t of [0.5, 0.78, 1]) {
      const [x, y] = camino(t)
      hoja(P, x, y, lado * (150 + P.rng() * 20), 5.6, 3, cor)
    }
  }
  if (e >= 3) cuelga(-1, e >= 4 ? 9 : 5)
  if (e >= 4) cuelga(1, e >= 5 ? 10 : 6)
  for (let i = nudos.length - 1; i >= 0; i--) {
    const nudo = nudos[i]
    hoja(P, nudo.x, nudo.y, nudo.lado * (74 + P.rng() * 30), nudo.len, nudo.len * 0.5, cor)
  }
  return []
}

// amp = balanceo del conjunto (px en la punta); aleteo = giro propio de cada hoja (grados)
const FORMAS = {
  arbusto: { dibujo: arbusto, amp: 1.5, aleteo: 5, maceta: { color: '#c7683f' } },
  arbol: { dibujo: arbol, amp: 0.9, aleteo: 6, maceta: { color: '#4c6c9c', banda: true } },
  helecho: { dibujo: helecho, amp: 1.2, aleteo: 7, maceta: { color: '#a67d58' } },
  suculenta: { dibujo: suculenta, amp: 0.25, aleteo: 1.2, maceta: { color: '#e4d6b8', banda: true, acento: '#c7683f' } },
  roseta: { dibujo: suculenta, amp: 0.25, aleteo: 1.2, maceta: { color: '#8fa3ad' } },
  cactus: { dibujo: cactus, amp: 0, aleteo: 0, maceta: { color: '#c7683f', banda: true } },
  monstera: { dibujo: monstera, amp: 1.1, aleteo: 3.5, maceta: { color: '#e9e5dc' } },
  trepadora: { dibujo: trepadora, amp: 0.6, aleteo: 5, maceta: { color: '#d7a23f' } },
  palmera: { dibujo: palmera, amp: 1.5, aleteo: 8, maceta: { color: '#5d6672' } },
}

// ---------------------------------------------------------------------------------------
// Fotogramas
// ---------------------------------------------------------------------------------------

export const ETAPA_MARCHITA = 6

function rampas(ap, marchita) {
  const verde = ap.colorHoja
  const seco = mezclar(verde, '#b0803a', 0.74)
  return {
    hoja: rampa(marchita ? seco : verde, { hoja: true }),
    amarilla: rampa(mezclar(verde, '#c2a83e', 0.55), { hoja: true }),
    brote: rampa(mezclar(verde, '#cfe86e', 0.45), { hoja: true }),
    tallo: rampa(mezclar(verde, '#5d6b24', 0.4), { hoja: true }),
    talloSeco: rampa('#7a5a34'),
    punta: rampa(mezclar(verde, ap.colorFlor || '#e2637f', 0.5)),
    flor: rampa(ap.colorFlor || ap.colorFruto || '#f08ab0'),
    centro: rampa('#f2c230'),
    fruto: rampa(ap.colorFruto || ap.colorFlor || '#d9473b'),
    verde: rampa(mezclar(verde, '#c4d64a', 0.5)),
    tronco: rampa(marchita ? '#7d6650' : '#8a5a3c'),
    tutor: rampa('#a47a52'),
    semilla: rampa('#c79a5e'),
  }
}

// Maceta con que se dibuja la planta: SIN_MACETA (la dibuja otro, como el invernadero), la comprada en la
// tienda (AZUL, ROSA... con su color, ver macetas.js) o, si no hay codigo, la propia de cada forma
export const SIN_MACETA = 'NINGUNA'

function estiloMaceta(forma, codigo) {
  if (!codigo) return forma.maceta
  return { ...forma.maceta, color: paletaDeMaceta(codigo).cuerpo }
}

function dibujar(ap, etapa, k, codigoMaceta) {
  const forma = FORMAS[ap.forma]
  const ciclo = cicloDe(ap)
  const marchita = etapa === ETAPA_MARCHITA
  const l = lienzo()
  if (codigoMaceta !== SIN_MACETA) maceta(l, estiloMaceta(forma, codigoMaceta))
  const P = {
    l, k, marchita,
    rng: azar(ap.forma.length * 7919 + etapa * 104729),
    amp: forma.amp, aleteo: forma.aleteo, caida: 3.5,
    R: rampas(ap, marchita),
  }
  if (etapa === 0) {
    P.amp = 0.8
    P.aleteo = 9
    semilla(P)
  } else {
    const e = marchita ? 3 : etapa
    const puntos = forma.dibujo(P, e, ciclo)
    if (!marchita && e >= 4 && ciclo !== 'follaje') {
      for (const [x, y] of puntos) {
        if (ciclo === 'fruto') fruto(P, x, y, e === 5)
        else if (e === 5) flor(P, x, y)
        else boton(P, x, y)
      }
    }
  }
  contornear(l)
  sombra(l)
  return l.d
}

const cache = new Map()

// La maceta de esa planta, sin nada plantado (para la secuencia de digitalizar)
export function macetaVacia(apariencia, codigoMaceta = null) {
  const { forma } = aparienciaDe(apariencia)
  const clave = `maceta|${forma}|${codigoMaceta || ''}`
  if (!cache.has(clave)) {
    const l = lienzo()
    maceta(l, estiloMaceta(FORMAS[forma], codigoMaceta))
    contornear(l)
    sombra(l)
    cache.set(clave, l.d)
  }
  return cache.get(clave)
}

// Pixeles RGBA (64x64) del fotograma `k` de la planta en esa etapa (0..6), con la maceta indicada
export function fotograma(apariencia, etapa, k = 0, codigoMaceta = null) {
  const ap = aparienciaDe(apariencia)
  const e = Math.max(0, Math.min(ETAPA_MARCHITA, etapa | 0))
  const clave = `${ap.forma}|${ap.colorHoja}|${ap.colorFlor}|${ap.colorFruto}|${cicloDe(ap)}|${e}|${codigoMaceta || ''}`
  if (!cache.has(clave)) cache.set(clave, [])
  const cuadros = cache.get(clave)
  const i = ((k % FOTOGRAMAS) + FOTOGRAMAS) % FOTOGRAMAS
  if (!cuadros[i]) cuadros[i] = dibujar(ap, e, i, codigoMaceta)
  return cuadros[i]
}
