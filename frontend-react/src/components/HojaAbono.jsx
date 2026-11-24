import { estiloAbono } from './abonoEstilo'
import './abono.css'

// Disco con 1, 2 o 3 hojas del color del abono (suave lima, organico tierra, reforzado violeta)
export default function HojaAbono({ codigo, tam = 52, className = '' }) {
  const { color, hojas } = estiloAbono(codigo)
  return (
    <span className={`abono-disco ${className}`} style={{ '--abono-color': color, width: tam, height: tam }} aria-hidden="true">
      <svg viewBox="0 0 48 48" width={tam * 0.8} height={tam * 0.8}>
        {Array.from({ length: hojas }, (_, i) => {
          const x = hojas === 1 ? 24 : 24 + (i - (hojas - 1) / 2) * 12
          const y = hojas === 1 ? 26 : 28 - (i % 2 === 0 ? 0 : 5)
          return (
            <g key={i} transform={`translate(${x} ${y})`}>
              <path d="M0 11C-9 10-10-4 0-13 10-4 9 10 0 11Z" fill={color} />
              <path d="M0 10V-5" stroke="#fff" strokeOpacity=".55" strokeWidth="1.6" strokeLinecap="round" />
            </g>
          )
        })}
      </svg>
    </span>
  )
}
