import { useMemo } from 'react'

// Invernadero en pixel art de la propuesta E (el mismo dibujo que la app Kotlin: FondoInvernadero.kt).
// Cada pagina es una cuadricula de 68 x 102: techo de vidrio (0 a 26), dos modulos con su repisa de 33 cada uno
// (26 a 92) y el zocalo de ladrillo (92 a 102). Las plantas no se dibujan aqui: se colocan encima de cada hueco.
export const ANCHO = 68
export const ALTO = 102

/** Esquina izquierda y centro de cada columna, y borde superior de la repisa de cada fila (unidades de la cuadricula). */
export const centroColumna = (columna) => 5 + columna * 22 + 7
export const repisa = (fila) => 26 + 33 * fila + 27

const CIELO = {
  DIA: { s: ['#5BC8FF', '#93DCFF', '#CBF0FF'], lejos: '#9BDDAA', cerca: '#52C17A', arbol: '#2F9E4F', astro: '#FFE066', tinte: '#E9FFF8', ta: 0.14 },
  TARDE: { s: ['#FF8E72', '#FFB878', '#FFE0A0'], lejos: '#C48A9A', cerca: '#6FA05F', arbol: '#3F7F4A', astro: '#FFF3B8', tinte: '#FFD9B0', ta: 0.18 },
  NOCHE: { s: ['#0A1238', '#16245C', '#2A3A86'], lejos: '#1B3A5E', cerca: '#12402F', arbol: '#0C2D22', astro: '#F6F1D2', tinte: '#9CB8FF', ta: 0.1 },
}

// Marco (de dia, de noche) y madera de la repisa de cada tema
const MARCO = {
  VICTORIANO: { d: ['#F4F7F5', '#B9C7BF'], n: ['#A9B9CC', '#6D8099'], w: ['#E0A767', '#A8703A'] },
  SELVA: { d: ['#8A5A34', '#5E3A1E'], n: ['#6E4A2E', '#47301D'], w: ['#A8744C', '#7E5233'] },
  LUCES: { d: ['#3A4747', '#1F2A2A'], n: ['#2B3636', '#151D1D'], w: ['#C8883F', '#94612A'] },
}
const marcoDe = (tema) => MARCO[tema] || MARCO.SELVA

// Recolecta rectangulos y poligonos con su llave
function lienzo() {
  const el = []
  let n = 0
  const r = (x, y, w, h, fill, op) => { el.push(<rect key={n++} x={x} y={y} width={w} height={h} fill={fill} opacity={op} />) }
  const poly = (puntos, fill, op) => { el.push(<polygon key={n++} points={puntos} fill={fill} opacity={op} />) }
  return { el, r, poly, agrega: (nodo) => { el.push(<g key={n++}>{nodo}</g>) } }
}

function liana(L, x, largo, c1, c2) {
  for (let k = 0; k < largo; k++) {
    L.r(x + (k % 3 === 1 ? 1 : 0), 2 + k, 1, 1, c1)
    if (k % 3 === 2) {
      L.r(x - 1, 2 + k, 1, 1, c2)
      L.r(x + 2, 2 + k, 1, 1, c2)
    }
  }
  L.r(x - 1, 2 + largo, 3, 1, c2)
}

function helecho(L, x) {
  L.r(x + 1, 2, 1, 3, '#999999'); L.r(x, 5, 3, 2, '#E8743B'); L.r(x - 2, 7, 7, 1, '#2F9E4F')
  L.r(x - 3, 8, 2, 2, '#3FBF63'); L.r(x + 4, 8, 2, 2, '#3FBF63'); L.r(x - 1, 8, 6, 1, '#2F9E4F'); L.r(x + 1, 9, 1, 2, '#2F9E4F')
}

function guirnalda(L, noche) {
  const colores = ['#FFD43B', '#FF7EB6', '#6FE3FF', '#FFB878']
  for (let x = 3, j = 0; x < 63; x += 5, j++) {
    const y = 3 + Math.round(2 * Math.sin(((x - 3) / 60) * Math.PI))
    L.r(x, y, 5, 1, '#111111')
    L.r(x + 1, y, 4, 4, colores[j % 4], noche ? 0.4 : 0.15)
    L.r(x + 2, y + 1, 2, 2, colores[j % 4])
  }
}

// Paisaje que se ve por la ventana: cielo, astro, nubes o estrellas, montes y arboles
function paisaje(L, momento) {
  const p = CIELO[momento]
  L.r(2, 0, 64, 22, p.s[0]); L.r(2, 22, 64, 18, p.s[1]); L.r(2, 40, 64, 14, p.s[2])
  L.r(46, 8, 6, 6, p.astro); L.r(45, 9, 8, 4, p.astro); L.r(47, 7, 4, 8, p.astro)
  if (momento === 'NOCHE') {
    ;[[8, 5], [22, 12], [36, 6], [60, 10], [14, 24], [30, 28], [56, 26], [40, 20], [6, 34], [40, 36], [62, 32], [26, 4]].forEach(([x, y]) => L.r(x, y, 1, 1, '#fff'))
  } else {
    L.r(8, 12, 10, 2, '#fff', 0.9); L.r(10, 10, 6, 2, '#fff', 0.9); L.r(26, 24, 9, 2, '#fff', 0.85)
    L.r(28, 22, 5, 2, '#fff', 0.85); L.r(52, 30, 8, 2, '#fff', 0.8); L.r(54, 28, 4, 2, '#fff', 0.8)
  }
  L.r(2, 46, 64, 20, p.lejos); L.r(4, 42, 16, 4, p.lejos); L.r(6, 40, 8, 2, p.lejos); L.r(36, 40, 22, 6, p.lejos); L.r(42, 38, 10, 2, p.lejos)
  L.r(2, 54, 64, 12, p.cerca); L.r(2, 52, 26, 2, p.cerca); L.r(44, 52, 22, 2, p.cerca)
  ;[[6, 46], [22, 49], [40, 45], [56, 48]].forEach(([x, y]) => {
    L.r(x, y, 6, 8, p.arbol); L.r(x + 1, y - 2, 4, 2, p.arbol); L.r(x + 2, y + 8, 1, 5, '#5E3A1E')
  })
  L.r(2, 0, 64, 66, p.tinte, p.ta)
}

// Un modulo: ventanal con su marco, rayos de luz, adornos del tema y la repisa donde se apoyan las macetas
function modulo(L, tema, momento, indice, hueco) {
  const noche = momento === 'NOCHE'
  const m = marcoDe(tema)
  const f = noche ? m.n : m.d
  const madera = m.w
  L.poly('24,0 29,0 17,20 17,15', '#fff', 0.2)
  L.poly('56,0 60,0 51,12 51,8', '#fff', 0.18)
  L.poly('7,0 11,0 3,16 3,11', '#fff', 0.18)
  L.r(16, 0, 1, 27, f[0]); L.r(17, 0, 1, 27, f[1], 0.6); L.r(50, 0, 1, 27, f[0]); L.r(51, 0, 1, 27, f[1], 0.6)
  L.r(2, 13, 64, 1, f[0]); L.r(2, 14, 64, 1, f[1], 0.5)
  L.r(0, 0, 2, 33, f[0]); L.r(2, 0, 1, 33, f[1]); L.r(66, 0, 2, 33, f[0]); L.r(65, 0, 1, 33, f[1])
  L.r(0, 0, 68, 2, f[0]); L.r(0, 2, 68, 1, f[1])

  if (tema === 'VICTORIANO') {
    helecho(L, indice % 2 ? 56 : 8)
    L.r(63, 8, 2, 2, '#FF7EB6'); L.r(64, 13, 2, 2, '#FF7EB6'); L.r(63, 19, 2, 2, '#FF7EB6'); L.r(1, 6, 1, 18, '#3FBF63', 0.8)
  } else if (tema === 'LUCES') {
    guirnalda(L, noche)
    const g = noche ? 0.35 : 0.12
    L.r(33, 2, 1, 5, '#111111'); L.r(28, 5, 12, 12, '#FFD43B', g); L.r(31, 7, 5, 6, '#2A2A2A'); L.r(32, 8, 3, 4, '#FFD43B')
    if (momento !== 'DIA') {
      const dx = (indice * 3) % 7
      ;[[10, 18], [56, 10], [24, 22], [44, 19]].forEach(([x, y]) => { L.r(x + dx, y, 1, 1, '#FFF59A'); L.r(x - 1 + dx, y - 1, 3, 3, '#FFF59A', 0.25) })
    }
  } else {
    liana(L, 4, 9 + (indice % 3) * 2, '#2F9E4F', '#3FBF63'); liana(L, 11, 6, '#1F7A3A', '#2F9E4F')
    liana(L, 55, 8, '#1F7A3A', '#3FBF63'); liana(L, 62, 11, '#2F9E4F', '#3FBF63')
    L.r(5, 13, 2, 2, '#FF7EB6'); L.r(60, 15, 2, 2, '#FFD43B'); L.r(12, 10, 2, 2, '#FF7EB6')
    if (indice === 0) {
      // Guacamayo posado
      L.r(40, 9, 4, 4, '#E5484D'); L.r(43, 8, 3, 3, '#E5484D'); L.r(46, 9, 1, 1, '#FFB020')
      L.r(40, 10, 3, 2, '#3B82F6'); L.r(37, 11, 3, 1, '#3B82F6'); L.r(44, 9, 1, 1, '#fff')
    }
  }

  // El "+" del primer hueco libre
  if (hueco != null) {
    const x = 5 + hueco * 22
    const cx = x + 7
    L.r(cx - 3, 19, 7, 1, f[0], 0.8); L.r(cx, 16, 1, 7, f[0], 0.8)
    L.r(x, 12, 2, 1, f[0], 0.6); L.r(x + 12, 12, 2, 1, f[0], 0.6); L.r(x, 24, 2, 1, f[0], 0.6); L.r(x + 12, 24, 2, 1, f[0], 0.6)
  }

  // Repisa
  L.r(0, 27, 68, 4, madera[0]); L.r(0, 31, 68, 2, madera[1]); L.r(0, 27, 68, 1, '#fff', 0.25)
  for (let gx = 4; gx < 66; gx += 9) L.r(gx, 29, 3, 1, madera[1], 0.5)
}

// Techo de vidrio a dos aguas con su cielo, vigas y adornos
function techo(L, tema, momento, idClip) {
  const p = CIELO[momento]
  const noche = momento === 'NOCHE'
  const m = marcoDe(tema)
  const f = noche ? m.n : m.d
  L.r(0, 0, 68, 26, f[1])
  const cielo = lienzo()
  cielo.r(0, 0, 68, 26, p.s[0])
  if (noche) {
    ;[[20, 12], [44, 16], [30, 18], [50, 12], [26, 10]].forEach(([x, y]) => cielo.r(x, y, 1, 1, '#fff'))
  } else {
    cielo.r(38, 14, 10, 2, '#fff', 0.85); cielo.r(40, 12, 6, 2, '#fff', 0.85); cielo.r(18, 18, 8, 2, '#fff', 0.8)
  }
  cielo.r(0, 0, 68, 26, p.tinte, p.ta)
  L.agrega(
    <>
      <clipPath id={idClip}><polygon points="4,26 34,4 64,26" /></clipPath>
      <g clipPath={`url(#${idClip})`}>{cielo.el}</g>
    </>,
  )
  L.agrega(
    <>
      <g stroke={f[0]} strokeWidth="1">
        <line x1="34" y1="4" x2="19" y2="26" /><line x1="34" y1="4" x2="49" y2="26" /><line x1="34" y1="4" x2="34" y2="26" />
      </g>
      <polygon points="4,26 34,4 64,26" fill="none" stroke={f[0]} strokeWidth="2" />
    </>,
  )
  L.r(33, 0, 2, 4, f[0]); L.r(33, 0, 2, 1, '#FFD43B'); L.r(0, 24, 68, 2, f[0])
  if (tema === 'VICTORIANO') {
    L.r(33, 6, 1, 3, '#999999'); L.r(31, 9, 5, 4, '#FFD43B', noche ? 0.9 : 0.5); L.r(32, 10, 3, 2, '#FFF3B8')
  } else if (tema === 'LUCES') {
    for (let t = 0; t <= 1; t += 0.17) {
      const x = 4 + t * 30
      const y = 26 - t * 22
      L.r(x - 1, y, 4, 4, '#FFD43B', noche ? 0.4 : 0.15); L.r(x, y + 1, 2, 2, '#FFD43B')
      L.r(68 - x - 3, y, 4, 4, '#FF7EB6', noche ? 0.4 : 0.15); L.r(68 - x - 2, y + 1, 2, 2, '#FF7EB6')
    }
  } else {
    liana(L, 3, 13, '#2F9E4F', '#3FBF63'); liana(L, 7, 8, '#1F7A3A', '#2F9E4F'); liana(L, 64, 11, '#2F9E4F', '#3FBF63'); liana(L, 60, 7, '#1F7A3A', '#3FBF63')
  }
}

// Zocalo de ladrillos del pie del invernadero
function zocalo(L, tema, momento) {
  const w = marcoDe(tema).w
  L.r(0, 0, 68, 10, w[1])
  for (let x = -4; x < 68; x += 8) { L.r(x, 0, 7, 4, w[0]); L.r(x + 4, 5, 7, 4, w[0]) }
  if (momento === 'NOCHE') L.r(0, 0, 68, 10, '#001', 0.4)
}

let contador = 0

/**
 * Pagina del invernadero elegido (SELVA, VICTORIANO o LUCES) en pixel art. `momento` es DIA, TARDE o NOCHE;
 * `huecoLibre` (0 a 5) marca con un "+" el primer lugar vacio. Ocupa todo el ancho; su proporcion es 68 : 102.
 */
export default function Invernadero({ tema = 'SELVA', momento = 'DIA', huecoLibre = null, className = '' }) {
  const idClip = useMemo(() => `techo-${contador++}`, [])
  const dibujo = useMemo(() => {
    const L = lienzo()
    techo(L, tema, momento, idClip)
    const Lm = lienzo()
    paisaje(Lm, momento)
    modulo(Lm, tema, momento, 0, huecoLibre != null && huecoLibre < 3 ? huecoLibre : null)
    const L2 = lienzo()
    modulo(L2, tema, momento, 1, huecoLibre != null && huecoLibre >= 3 ? huecoLibre - 3 : null)
    const Lz = lienzo()
    zocalo(Lz, tema, momento)
    return (
      <>
        {L.el}
        <g transform="translate(0 26)">{Lm.el}<g transform="translate(0 33)">{L2.el}</g></g>
        <g transform="translate(0 92)">{Lz.el}</g>
      </>
    )
  }, [tema, momento, huecoLibre, idClip])

  return (
    <svg className={className} viewBox={`0 0 ${ANCHO} ${ALTO}`} width="100%" shapeRendering="crispEdges" aria-hidden="true" style={{ display: 'block' }}>
      {dibujo}
    </svg>
  )
}
