// Motor de la "medalla que brota" (propuesta D), portado del prototipo aprobado por Johan ("D + aviso al terminar").
// Rejilla de pixeles logicos (1 celda = P px en pantalla) y un tic cada 80 ms, como PlantaPixel.jsx de Jose:
// crece un tallo en la maceta, en su punta se dibuja el marco de la medalla, se llena, cae el sello (temblor, anillo
// de destellos y hojas) y sube la tarjeta. Mismos tiempos, particulas y rarezas que el movil (MotorMedalla.kt).
// No toca el DOM: la pantalla le da la geometria con colocar(), llama a tic() y pinta con dibujarMedalla().

export const TIC_MS = 80
/** Un toque antes de este tiempo salta al final; despues, cierra. */
export const SALTO_MS = 1000
export const MAX_PARTICULAS = 160
const RING = 6
const FILL = RING + 2
const ESCALA_SELLO = [1.9, 1.5, 1.2, 1]

export const CONTORNO = '#2c2028'
// Color, luz y sombra de cada rareza (fijos: son el pixel art, no cambian con el tema)
export const RZ = [
  { hex: '#a3e635', luz: '#ecfccb', osc: '#365314', cols: ['#a3e635', '#ecfccb', '#ffffff'] },
  { hex: '#38bdf8', luz: '#e0f2fe', osc: '#0c4a6e', cols: ['#38bdf8', '#e0f2fe', '#ffffff'] },
  { hex: '#c084fc', luz: '#f3e8ff', osc: '#4c1d95', cols: ['#c084fc', '#f3e8ff', '#fbcfe8'] },
  { hex: '#fbbf24', luz: '#fef3c7', osc: '#78350f', cols: ['#fbbf24', '#fef3c7', '#ffffff', '#fb923c'] },
]
const HOJAS = [
  ['#a4d65e', '#7fc241'],
  ['#a4d65e', '#7fc241', '#d8f1ff', '#58b4f0'],
  ['#a4d65e', '#7fc241', '#f3e8ff', '#c084fc'],
  ['#a4d65e', '#7fc241', '#fef3c7', '#fbbf24'],
]

export const NIVEL_RAREZA = { COMUN: 0, RARA: 1, EPICA: 2, LEGENDARIA: 3 }
export const nivelRareza = (rareza) => NIVEL_RAREZA[String(rareza || '').trim().toUpperCase()] ?? 0

const clamp = (v, a, b) => Math.max(a, Math.min(b, v))

const CIR = {}
/** Puntos [x, y] de un circulo de radio r, desde arriba en sentido horario y sin repetir. */
export function circulo(r) {
  if (CIR[r]) return CIR[r]
  const out = []
  const vis = new Set()
  const n = Math.ceil(r * 14)
  for (let i = 0; i < n; i++) {
    const a = -Math.PI / 2 + (i / n) * Math.PI * 2
    const x = Math.round(Math.cos(a) * r)
    const y = Math.round(Math.sin(a) * r)
    const k = x * 1000 + y
    if (!vis.has(k)) { vis.add(k); out.push([x, y]) }
  }
  return (CIR[r] = out)
}

export class MotorMedalla {
  /**
   * @param {number} rareza 0 comun, 1 rara, 2 epica, 3 legendaria
   * @param {boolean} estatico animaciones reducidas: se calcula el cuadro final de una vez
   * @param {() => void} alTerminar se llama UNA vez, cuando la animacion llega a su fin (o se adelanta)
   * @param {() => number} azar fuente de aleatorios (para pruebas)
   */
  constructor(rareza, estatico = false, alTerminar = () => {}, azar = Math.random) {
    const r = clamp(rareza | 0, 0, 3)
    this.r = r
    this.estatico = estatico
    this.alTerminar = alTerminar
    this.azar = azar
    this.H = [10, 12, 14, 16][r]
    this.tg = 2 + Math.ceil(this.H / 3)
    this.R = [11, 12, 13, 14][r]
    this.ST = FILL + 1
    this.fin = this.tg + this.ST + 10
    this.t = 0
    this.h = 0
    this.done = false
    this.tarjeta = estatico
    this.shake = 0
    this.shk = 1
    this.shx = 0
    this.shy = 0
    this.peak = 0
    this.parts = []
    this.cx = 0
    this.bot = 0
    this.gw = 0
    this.gh = 0
  }

  colocar(cx, bot, gw, gh) { this.cx = cx; this.bot = bot; this.gw = gw; this.gh = gh }

  get sueloY() { return this.bot - 12 }
  get my() { return this.sueloY - this.H - this.R - 1 }
  /** Tics desde que cae el sello, para el cuadro que se pinta (negativo = aun no cae). */
  get desdeSello() { return this.t - 1 - this.tg - this.ST }
  get iconoVisible() { return this.desdeSello >= 0 }
  get escalaIcono() { return this.desdeSello < 0 ? 1 : ESCALA_SELLO[Math.min(3, this.desdeSello)] }
  get alfaIcono() { return this.desdeSello === 0 ? 0.7 : 1 }

  rnd(a, b) { return a + this.azar() * (b - a) }
  pick(l) { return l[Math.floor(this.azar() * l.length)] }

  nueva(p) {
    if (this.parts.length >= MAX_PARTICULAS) return
    this.parts.push({ vx: 0, vy: 0, g: 0, w: 1, h: 1, ...p, total: p.vida })
  }

  anillo(cx, cy, n, cols, vel) {
    for (let i = 0; i < n; i++) {
      const a = (i / n) * 6.283
      this.nueva({ k: 'dest', x: cx + Math.cos(a) * 8, y: cy + Math.sin(a) * 8, vida: 11, color: this.pick(cols), vx: Math.cos(a) * vel, vy: Math.sin(a) * vel * 0.9 })
    }
  }

  logica(t) {
    const r = this.r
    this.h = clamp((t - 2) * 3, 0, this.H)
    const e = t - this.tg - this.ST
    if (e === 0) {
      this.shake = 5
      this.shk = r >= 2 ? 2 : 1
      this.anillo(this.cx, this.my, [8, 10, 14, 18][r], RZ[r].cols, 2.2)
      const n = [10, 16, 24, 36][r]
      for (let i = 0; i < n; i++) {
        const a = this.rnd(-2.9, -0.25)
        this.nueva({ k: 'hoja', x: this.cx, y: this.my, vx: Math.cos(a) * this.rnd(0.8, 2.4), vy: Math.sin(a) * this.rnd(1.6, 3.4), g: 0.22, w: 3, h: 2, vida: 34, color: this.pick(HOJAS[r]) })
      }
    }
    if (e === 2) this.tarjeta = true
    // temblor
    if (this.shake > 0) {
      const m = this.shk
      this.shx = this.shake % 2 ? m : -m
      this.shy = this.shake % 3 === 0 ? -m : m
      this.shake--
    } else { this.shx = 0; this.shy = 0 }
  }

  tic() {
    const t = this.t++
    this.logica(t)
    for (const p of this.parts) { p.x += p.vx; p.y += p.vy; p.vy += p.g; p.vida-- }
    this.parts = this.parts.filter((p) => p.vida > 0 && p.y < this.gh + 6 && p.y > -14)
    this.peak = Math.max(this.peak, this.parts.length)
    if (this.t >= this.fin && !this.done) {
      this.done = true
      this.alTerminar()
    }
  }

  /** Toque antes de que acabe: salta al final (las particulas ya soltadas siguen su camino). */
  adelantar() { let n = 0; while (!this.done && n++ < 200) this.tic() }

  /** Animaciones reducidas: el cuadro final, sin particulas. */
  completarEstatico() { let n = 0; while (!this.done && n++ < 300) this.tic(); this.parts = [] }

  /** Sin nada que mover se apaga el bucle; los adornos de epica/legendaria siguen como maximo ~10 s. */
  debeParar() {
    return this.estatico || (this.t > this.fin + 70 && !this.parts.length && this.r < 2) || this.t > this.fin + 130
  }
}

// ---------- Dibujo (en celdas: el canvas mide gw x gh y el CSS lo amplia sin suavizar) ----------

function dibujarMaceta(ctx, m, t) {
  const R_ = (x, y, w, h, c) => { ctx.fillStyle = c; ctx.fillRect(Math.round(x), Math.round(y), w, h) }
  const { cx, bot, sueloY, h, H } = { cx: m.cx, bot: m.bot, sueloY: m.sueloY, h: m.h, H: m.H }
  const fila = (y, ancho, c) => R_(cx - Math.floor(ancho / 2), y, ancho, 1, c)
  for (let i = 0; i < 3; i++) fila(bot - 12 + i + 1, 24, CONTORNO)
  for (let i = 0; i < 7; i++) fila(bot - 9 + i + 1, 22 - i, CONTORNO)
  for (let i = 0; i < 3; i++) fila(bot - 12 + i + 1, 22, '#e79a6e')
  for (let i = 0; i < 7; i++) {
    const a = 20 - i
    fila(bot - 9 + i + 1, a, '#d9825b')
    R_(cx - Math.floor(a / 2), bot - 9 + i + 1, 2, 1, '#e79a6e')
    R_(cx + Math.floor(a / 2) - 2, bot - 9 + i + 1, 2, 1, '#b86a45')
  }
  fila(sueloY, 18, '#5b4636'); fila(sueloY + 1, 18, '#6e5441')
  if (h > 0) { R_(cx - 1, sueloY - h, 2, h, '#7fc241'); R_(cx, sueloY - h, 1, h, '#4e8c34') }
  const vaiven = m.estatico ? 0 : [0, 0, 1, 1][(t >> 1) & 3]
  for (let k = 0; k < 4; k++) {
    const alt = 5 + k * 5
    if (h < alt || alt > H) continue
    const y = sueloY - alt
    const dy = k % 2 ? vaiven : -vaiven
    for (const s of [-1, 1]) {
      const x0 = cx + s
      const L = (dx, w, yy, c) => R_(s < 0 ? x0 - dx - w + 1 : x0 + dx, y + yy + dy, w, 1, c)
      L(1, 5, 0, CONTORNO); L(2, 6, 1, CONTORNO)
      L(1, 4, 1, '#a4d65e'); L(2, 5, 2, '#7fc241'); L(4, 3, 3, '#4e8c34')
    }
  }
  if (!m.iconoVisible && h > 0) R_(cx - 1, sueloY - h - 2, 2, 2, '#bfe27a')
}

function destelloFijo(ctx, x, y, c) {
  ctx.fillStyle = c; x = Math.round(x); y = Math.round(y)
  ctx.fillRect(x, y, 1, 1); ctx.fillRect(x - 1, y, 3, 1); ctx.fillRect(x, y - 1, 1, 3)
}

function dibujarMarco(ctx, m, t) {
  const R_ = (x, y, w, h, c) => { ctx.fillStyle = c; ctx.fillRect(Math.round(x), Math.round(y), w, h) }
  const cx = m.cx, cy = m.my, R = m.R, r = m.r, rz = RZ[r], ST = m.ST
  const pts = circulo(R), n = pts.length
  const reveal = clamp(Math.floor((n * (t + 1)) / RING), 0, n)
  // relleno que sube desde abajo
  const filas = clamp(Math.floor(((t - RING + 1) * (2 * R)) / 3), 0, 2 * R)
  if (filas > 0) {
    ctx.fillStyle = rz.osc
    for (let i = 0; i < filas; i++) { const y = R - i - 1, h = Math.floor(Math.sqrt(Math.max(0, (R - 2) * (R - 2) - y * y) + R * 0.6)); ctx.fillRect(cx - h, cy + y, h * 2 + 1, 1) }
  }
  // marco punto a punto, con la punta en blanco mientras avanza
  ctx.fillStyle = CONTORNO; for (let i = 0; i < reveal; i++) ctx.fillRect(cx + pts[i][0] - 1, cy + pts[i][1] - 1, 4, 4)
  ctx.fillStyle = rz.hex; for (let i = 0; i < reveal; i++) ctx.fillRect(cx + pts[i][0], cy + pts[i][1], 2, 2)
  if (reveal < n) { ctx.fillStyle = '#fff'; for (let i = Math.max(0, reveal - 3); i < reveal; i++) ctx.fillRect(cx + pts[i][0], cy + pts[i][1], 2, 2) }
  if (r >= 2 && reveal > 0) {   // segundo marco que se dibuja al reves
    const p2 = circulo(R + 3), k = Math.floor(p2.length * clamp((t + 1) / RING, 0, 1))
    ctx.fillStyle = rz.luz; for (let i = 0; i < k; i += 2) ctx.fillRect(cx + p2[p2.length - 1 - i][0], cy + p2[p2.length - 1 - i][1], 1, 1)
  }
  if (t < ST) return
  if (r >= 1) for (const [dx, dy] of [[0, -R], [R, 0], [0, R], [-R, 0]]) { R_(cx + dx - 1, cy + dy - 1, 4, 4, CONTORNO); R_(cx + dx, cy + dy, 2, 2, '#ffffff') }
  // onda de choque del sello (doble en epica y legendaria)
  const e = t - ST
  for (const [d, c] of [[0, rz.hex], [2, rz.luz]]) {
    if (d && r < 2) continue
    const k = e - d; if (k < 0 || k > 7) continue
    const rr = R + 4 + k * 4
    ctx.globalAlpha = 1 - k / 8; ctx.fillStyle = c
    for (const p of circulo(rr)) ctx.fillRect(cx + p[0], cy + p[1], 1, 1)
    ctx.globalAlpha = 1
  }
  if (r >= 2) {   // alas con aleteo
    const aleteo = m.estatico ? 0 : ((t >> 2) & 1)
    for (const s of [-1, 1]) for (let j = 0; j < 4; j++) { const len = 11 - j * 2, x = s < 0 ? cx - R - 2 - len : cx + R + 3, y = cy - 9 + j * 4 - aleteo; R_(x - 1, y - 1, len + 2, 5, CONTORNO); R_(x, y, len, 3, j % 2 ? rz.luz : '#ffffff') }
  }
  if (r === 3) {   // corona que cae y estrellas orbitando
    const dc = [-10, -5, -2, 0][clamp(e - 3, 0, 3)], y0 = cy - R - 5 + dc
    if (e >= 3) { R_(cx - 7, y0 - 1, 15, 6, CONTORNO); for (const dx of [-6, 0, 5]) R_(cx + dx - 1, y0 - 5, 4, 5, CONTORNO); R_(cx - 6, y0, 13, 3, '#fbbf24'); for (const dx of [-5, 0, 5]) R_(cx + dx, y0 - 4, 2, 4, '#fbbf24'); R_(cx - 1, y0 + 1, 2, 1, '#e11d48'); R_(cx - 5, y0, 1, 1, '#fff') }
    if (!m.estatico) for (let i = 0; i < 6; i++) { const a = t * 0.15 + (i * Math.PI) / 3; destelloFijo(ctx, cx + Math.cos(a) * (R + 9), cy + Math.sin(a) * (R + 8), i % 2 ? '#fef3c7' : '#fbbf24') }
  }
  if (r >= 2 && !m.estatico) {   // brillo que cruza la medalla cada ~2,2 s
    const k = (t - ST) % 28
    if (k < 8) {
      ctx.globalAlpha = 0.55; ctx.fillStyle = '#fff'
      const off = -R + k * ((2 * R) / 7)
      for (let y = -R + 3; y < R - 3; y++) { const x = Math.round(off - y * 0.6 + R * 0.3), lim = Math.floor(Math.sqrt((R - 3) * (R - 3) - y * y)); if (Math.abs(x) < lim) ctx.fillRect(cx + x, cy + y, 2, 1) }
      ctx.globalAlpha = 1
    }
  }
}

function dibujarParticulas(ctx, m) {
  for (const p of m.parts) {
    const x = Math.round(p.x), y = Math.round(p.y)
    ctx.fillStyle = p.color
    if (p.k === 'dest') {
      ctx.fillRect(x, y, 1, 1)
      if (Math.abs(p.vida - p.total / 2) < p.total / 4) { ctx.fillRect(x - 1, y, 3, 1); ctx.fillRect(x, y - 1, 1, 3) }
    } else ctx.fillRect(x, y, p.w, p.h)
  }
}

/** Pinta el cuadro del ultimo tic. [icono] es un canvas 18x18 (o null). */
export function dibujarMedalla(ctx, m, icono) {
  ctx.clearRect(0, 0, m.gw, m.gh)
  const t = m.t - 1
  if (t < 0) return
  dibujarMaceta(ctx, m, t)
  if (t >= m.tg) dibujarMarco(ctx, m, t - m.tg)
  dibujarParticulas(ctx, m)
  if (m.iconoVisible && icono) {
    // el icono (18x18) cae como un sello: escala 1,9 -> 1 centrado en la medalla
    const lado = 18 * m.escalaIcono
    ctx.globalAlpha = m.alfaIcono
    ctx.drawImage(icono, Math.round(m.cx - lado / 2), Math.round(m.my - lado / 2), Math.round(lado), Math.round(lado))
    ctx.globalAlpha = 1
  }
}
