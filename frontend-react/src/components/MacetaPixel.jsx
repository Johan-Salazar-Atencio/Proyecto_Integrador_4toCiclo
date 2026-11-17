import { useMemo } from 'react'
import { COLUMNAS, FILAS, celdasDeMaceta } from './macetas'

// Maceta en pixel art (SVG de 16 x 8); su tamano lo da el ancho del contenedor
export default function MacetaPixel({ codigo = 'TERRACOTA', className = '', titulo }) {
  const celdas = useMemo(() => celdasDeMaceta(codigo), [codigo])
  return (
    <svg className={className} viewBox={`0 0 ${COLUMNAS} ${FILAS}`} shapeRendering="crispEdges" role={titulo ? 'img' : undefined} aria-label={titulo} aria-hidden={titulo ? undefined : true}>
      {celdas.map((c) => <rect key={`${c.x}-${c.y}`} x={c.x} y={c.y} width="1.03" height="1.03" fill={c.color} />)}
    </svg>
  )
}
