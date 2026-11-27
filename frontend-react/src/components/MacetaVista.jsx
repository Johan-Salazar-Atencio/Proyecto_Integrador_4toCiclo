import { useEffect, useRef } from 'react'
import { LADO, macetaVacia } from './plantaPixelSprites'

// Recorte del lienzo de 64x64 donde cae la maceta (con su contorno y su sombra): columnas 17 a 46, filas 44 a 63
export const MACETA_X = 17
export const MACETA_Y = 44
export const MACETA_ANCHO = 30
export const MACETA_ALTO = 20

// Maceta en pixel art con el mismo motor y sombreado que las plantas (la que se ve en el jardin), sin planta.
// Se amplia por un factor ENTERO (`escala`) para que todos los pixeles midan lo mismo.
export default function MacetaVista({ codigo, escala = 4, forma = 'arbusto', titulo, className = '' }) {
  const lienzo = useRef(null)
  useEffect(() => {
    const ctx = lienzo.current.getContext('2d')
    const todo = macetaVacia({ forma }, codigo)
    const recorte = ctx.createImageData(MACETA_ANCHO, MACETA_ALTO)
    for (let y = 0; y < MACETA_ALTO; y++) {
      const desde = ((MACETA_Y + y) * LADO + MACETA_X) * 4
      recorte.data.set(todo.subarray(desde, desde + MACETA_ANCHO * 4), y * MACETA_ANCHO * 4)
    }
    ctx.putImageData(recorte, 0, 0)
  }, [codigo, forma])
  return (
    <canvas
      ref={lienzo}
      className={`maceta-vista ${className}`}
      width={MACETA_ANCHO}
      height={MACETA_ALTO}
      style={{ width: MACETA_ANCHO * escala, height: MACETA_ALTO * escala, imageRendering: 'pixelated' }}
      role={titulo ? 'img' : undefined}
      aria-label={titulo}
      aria-hidden={titulo ? undefined : true}
    />
  )
}
