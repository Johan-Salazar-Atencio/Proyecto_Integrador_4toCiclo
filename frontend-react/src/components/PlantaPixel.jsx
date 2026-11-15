import { useMemo } from 'react'
import { LADO, TIERRA, paletaDe, spritesDe, tramos } from './plantaPixelSprites'
import './planta-pixel.css'

// Planta en pixel art (SVG, 32x32) con 7 etapas de crecimiento. Su dibujo y sus colores salen
// de la `apariencia` que da la IA al escanear (ver plantaPixelSprites.js).
const MARCHITA = 6

const rects = (lista) =>
  lista.map((t) => <rect key={`${t.x}-${t.y}`} x={t.x} y={t.y} width={t.ancho} height="1" fill={t.color} />)

export default function PlantaPixel({ etapa = 0, apariencia = null, titulo, animar = true, className = '' }) {
  const indice = Math.max(0, Math.min(MARCHITA, etapa))
  const { planta, suelo } = useMemo(() => {
    const paleta = paletaDe(apariencia)
    return { planta: tramos(spritesDe(apariencia)[indice], paleta), suelo: tramos(TIERRA, paleta) }
  }, [apariencia, indice])

  return (
    <svg
      className={`planta-pixel ${animar && indice !== MARCHITA ? 'planta-pixel-viva' : ''} ${className}`}
      viewBox={`0 0 ${LADO} ${LADO}`}
      shapeRendering="crispEdges"
      role="img"
      aria-label={titulo}
    >
      {titulo && <title>{titulo}</title>}
      <g className="planta-pixel-tallo">{rects(planta)}</g>
      {rects(suelo)}
    </svg>
  )
}
