import { useState } from 'react'

const ANCHO = 640
const ALTO = 220
const MARGEN = { izq: 8, der: 8, arriba: 16, abajo: 26 }

// Grafico de lineas con area (SVG, sin librerias): usuarios activos y escaneos por dia
export default function Grafico({ datos }) {
  const [activo, setActivo] = useState(null)

  const maximo = Math.max(...datos.flatMap((d) => [d.usuarios, d.escaneos])) * 1.1
  const ancho = ANCHO - MARGEN.izq - MARGEN.der
  const alto = ALTO - MARGEN.arriba - MARGEN.abajo
  const x = (i) => MARGEN.izq + (i / (datos.length - 1)) * ancho
  const y = (v) => MARGEN.arriba + alto - (v / maximo) * alto

  const linea = (campo) => datos.map((d, i) => `${i ? 'L' : 'M'}${x(i).toFixed(1)} ${y(d[campo]).toFixed(1)}`).join(' ')
  const area = (campo) => `${linea(campo)} L${x(datos.length - 1)} ${MARGEN.arriba + alto} L${x(0)} ${MARGEN.arriba + alto} Z`

  const mover = (e) => {
    const caja = e.currentTarget.getBoundingClientRect()
    const posicion = ((e.clientX - caja.left) / caja.width) * ANCHO
    const i = Math.round(((posicion - MARGEN.izq) / ancho) * (datos.length - 1))
    setActivo(Math.max(0, Math.min(datos.length - 1, i)))
  }

  const punto = activo === null ? null : datos[activo]
  const etiqueta = (f) => f.toLocaleDateString('es-PE', { day: 'numeric', month: 'short' })

  return (
    <div className="grafico">
      <div className="grafico-leyenda">
        <span><i style={{ background: 'var(--verde)' }} /> Usuarios activos</span>
        <span><i style={{ background: 'var(--azul)' }} /> Escaneos IA</span>
      </div>

      <div className="grafico-caja">
        <svg viewBox={`0 0 ${ANCHO} ${ALTO}`} onMouseMove={mover} onMouseLeave={() => setActivo(null)} role="img" aria-label="Actividad de usuarios y escaneos de los últimos días">
          <defs>
            <linearGradient id="relleno-verde" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#22c55e" stopOpacity="0.35" />
              <stop offset="100%" stopColor="#22c55e" stopOpacity="0" />
            </linearGradient>
            <linearGradient id="relleno-azul" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#3b82f6" stopOpacity="0.28" />
              <stop offset="100%" stopColor="#3b82f6" stopOpacity="0" />
            </linearGradient>
          </defs>

          {[0.25, 0.5, 0.75, 1].map((p) => (
            <line key={p} x1={MARGEN.izq} x2={ANCHO - MARGEN.der} y1={MARGEN.arriba + alto * (1 - p)} y2={MARGEN.arriba + alto * (1 - p)} className="grafico-guia" />
          ))}

          <path d={area('usuarios')} fill="url(#relleno-verde)" />
          <path d={area('escaneos')} fill="url(#relleno-azul)" />
          <path d={linea('usuarios')} className="grafico-linea" stroke="#22c55e" pathLength="1" />
          <path d={linea('escaneos')} className="grafico-linea" stroke="#3b82f6" pathLength="1" />

          {[0, Math.floor(datos.length / 2), datos.length - 1].map((i) => (
            <text key={i} x={x(i)} y={ALTO - 6} textAnchor={i === 0 ? 'start' : i === datos.length - 1 ? 'end' : 'middle'} className="grafico-eje">{etiqueta(datos[i].fecha)}</text>
          ))}

          {punto && (
            <>
              <line x1={x(activo)} x2={x(activo)} y1={MARGEN.arriba} y2={MARGEN.arriba + alto} className="grafico-cursor" />
              <circle cx={x(activo)} cy={y(punto.usuarios)} r="5" fill="#22c55e" className="grafico-punto" />
              <circle cx={x(activo)} cy={y(punto.escaneos)} r="5" fill="#3b82f6" className="grafico-punto" />
            </>
          )}
        </svg>

        {punto && (
          <div className="grafico-dato" style={{ left: `${(x(activo) / ANCHO) * 100}%` }}>
            <b>{etiqueta(punto.fecha)}</b>
            <span><i style={{ background: 'var(--verde)' }} /> {punto.usuarios.toLocaleString('es-PE')} usuarios</span>
            <span><i style={{ background: 'var(--azul)' }} /> {punto.escaneos.toLocaleString('es-PE')} escaneos</span>
          </div>
        )}
      </div>
    </div>
  )
}
