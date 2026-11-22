// Iconos pixelados de los logros: el emoji actual de IconoLogro.jsx se pinta una vez en 16x16, se toma su silueta
// (sin medios tonos) y se le pone el contorno oscuro de Jose, como los sprites 16x16 + borde del prototipo.
// Se dibuja ampliado con image-rendering: pixelated.
import { emojiLogro } from '../../../components/IconoLogro'

const N = 16
const LADO = N + 2
const CONTORNO = [44, 32, 40]
const CACHE = new Map()

/** Lienzo 18x18 con el icono pixelado de la clave (las claves nuevas usan el trofeo por defecto). */
export function iconoPixel(clave) {
  const emoji = emojiLogro(clave)
  if (CACHE.has(emoji)) return CACHE.get(emoji)
  const salida = document.createElement('canvas')
  salida.width = salida.height = LADO
  try {
    const base = document.createElement('canvas')
    base.width = base.height = N
    const g = base.getContext('2d', { willReadFrequently: true })
    g.textAlign = 'center'
    g.textBaseline = 'middle'
    g.font = `${N - 1}px "Apple Color Emoji","Segoe UI Emoji","Noto Color Emoji",sans-serif`
    g.fillText(emoji, N / 2, N / 2 + 1)
    const { data } = g.getImageData(0, 0, N, N)
    const col = Array.from({ length: LADO }, () => Array(LADO).fill(null))
    for (let y = 0; y < N; y++) for (let x = 0; x < N; x++) {
      const i = (y * N + x) * 4
      if (data[i + 3] >= 110) col[y + 1][x + 1] = [data[i], data[i + 1], data[i + 2]]
    }
    const s = salida.getContext('2d')
    for (let y = 0; y < LADO; y++) for (let x = 0; x < LADO; x++) {
      let c = col[y][x]
      if (!c && ((y > 0 && col[y - 1][x]) || (y < LADO - 1 && col[y + 1][x]) || (x > 0 && col[y][x - 1]) || (x < LADO - 1 && col[y][x + 1]))) c = CONTORNO
      if (c) { s.fillStyle = `rgb(${c[0]},${c[1]},${c[2]})`; s.fillRect(x, y, 1, 1) }
    }
  } catch { /* sin canvas: se queda vacio, el texto sigue diciendo el logro */ }
  CACHE.set(emoji, salida)
  return salida
}

/** Copia el icono en un <canvas> 18x18 de la pagina. */
export function pintarIcono(lienzo, clave) {
  if (!lienzo) return
  const g = lienzo.getContext('2d')
  g.imageSmoothingEnabled = false
  g.clearRect(0, 0, lienzo.width, lienzo.height)
  g.drawImage(iconoPixel(clave), 0, 0, lienzo.width, lienzo.height)
}
