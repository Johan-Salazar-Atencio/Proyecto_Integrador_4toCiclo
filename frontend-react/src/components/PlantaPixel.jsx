import './planta-pixel.css'

// Planta en pixel art (SVG, 16x16) con 7 etapas de crecimiento. Cada letra de la cuadricula es
// un pixel de color (ver PALETA); '.' es transparente. La maceta de tierra es comun a todas.
const PALETA = {
  d: '#5a2e2e', D: '#7a3b33', // tierra
  s: '#2f5d3a', g: '#2e6b40', G: '#4f9a52', // tallo y hojas
  f: '#7dcf63', F: '#b2ec8a', // fruto verde
  r: '#d9473b', R: '#f59a84', // fruto maduro
  S: '#6b3b22', w: '#8a4b2a', W: '#c27a3a', // marchita
}

const TIERRA = [
  '......DDDD......',
  '....DDDDDDDD....',
  '...dddddddddd...',
]

const SEMILLA = [
  '.......gG.......',
]

const BROTE = [
  '.....GG.GG......',
  '....G..s..G.....',
  '.......s........',
  '.......s........',
]

const JOVEN = [
  '........GG......',
  '.......sGgG.....',
  '....GG.s........',
  '...GgGGs........',
  '.......s.GG.....',
  '.......sGGgG....',
  '.....GGs........',
  '....GgGs........',
  '.......s........',
]

const ADULTA = [
  '......G.GG......',
  '.......sGg......',
  '..GG...s...GG...',
  '.GgGG..s..GGgG..',
  '....ss.s.ss.....',
  '......sss.......',
  '..GG...s...GG...',
  '.GgGG..s..GGgG..',
  '....ss.s.ss.....',
  '......sss.......',
  '.......s........',
  '.......s........',
]

// Esquina superior izquierda (fila, columna) de cada fruto 2x2, sobre la cuadricula de ADULTA
const FRUTOS = [[0, 4], [2, 10], [5, 2], [6, 9], [8, 4]]

function conFrutos(base, claro, oscuro) {
  const filas = base.map((f) => f.split(''))
  for (const [y, x] of FRUTOS) {
    filas[y][x] = claro
    filas[y][x + 1] = oscuro
    filas[y + 1][x] = oscuro
    filas[y + 1][x + 1] = oscuro
  }
  return filas.map((f) => f.join(''))
}

const MARCHITA = ADULTA.map((f) => f.replace(/s/g, 'S').replace(/g/g, 'w').replace(/G/g, 'W'))

// Mismo orden que ETAPAS en pages/user/jardin/etapas.js
const SPRITES = [
  SEMILLA,
  BROTE,
  JOVEN,
  ADULTA,
  conFrutos(ADULTA, 'F', 'f'),
  conFrutos(ADULTA, 'R', 'r'),
  MARCHITA,
]

const LADO = 16

function pixeles(filas, desde) {
  return filas.flatMap((fila, y) =>
    fila.split('').flatMap((c, x) =>
      PALETA[c] ? [<rect key={`${x}-${y + desde}`} x={x} y={y + desde} width="1" height="1" fill={PALETA[c]} />] : [],
    ),
  )
}

export default function PlantaPixel({ etapa = 0, titulo, animar = true, className = '' }) {
  const planta = SPRITES[Math.max(0, Math.min(SPRITES.length - 1, etapa))]
  const marchita = etapa === SPRITES.length - 1
  // La planta se apoya justo encima de la tierra
  const desde = LADO - TIERRA.length - planta.length
  return (
    <svg
      className={`planta-pixel ${animar && !marchita ? 'planta-pixel-viva' : ''} ${className}`}
      viewBox={`0 0 ${LADO} ${LADO}`}
      shapeRendering="crispEdges"
      role="img"
      aria-label={titulo}
    >
      {titulo && <title>{titulo}</title>}
      <g className="planta-pixel-tallo">{pixeles(planta, desde)}</g>
      {pixeles(TIERRA, LADO - TIERRA.length)}
    </svg>
  )
}
