import React from 'react'
import './planta-vector.css'

/**
 * Prototipo de animacion vectorial (SVG) de una Monstera deliciosa.
 * Objetivo: parecido >= 70% a la planta original (plant-monstera.png)
 * usando vectores (paths) + CSS animations. Sin imagenes raster.
 *
 * Notas:
 * - Virtualizacion: este componente solo deberia renderizarse para items
 *   visibles (ver @tanstack/react-virtual en Catalogo/Coleccion).
 * - Respeta prefers-reduced-motion (ver CSS).
 */
export default function PlantaVectorAnimada({ tamaño = 220, salud = 1 }) {
  // salud 0..1 modula intensidad del balanceo y color
  const sway = 1.2 + (1 - salud) * 1.5
  return (
    <svg
      className="planta-vector"
      width={tamaño}
      height={tamaño}
      viewBox="0 0 220 260"
      role="img"
      aria-label="Monstera deliciosa animada"
      style={{ '--sway': `${sway}deg` }}
    >
      <defs>
        <linearGradient id="hojaGrad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#4caf50" />
          <stop offset="100%" stopColor="#2e7d32" />
        </linearGradient>
        <linearGradient id="macetaGrad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#bcaaa4" />
          <stop offset="100%" stopColor="#8d6e63" />
        </linearGradient>
      </defs>

      {/* Maceta */}
      <g className="maceta">
        <path d="M70 210 L62 250 Q110 260 158 250 L150 210 Z" fill="url(#macetaGrad)" stroke="#6d4c41" strokeWidth="2" />
        <ellipse cx="110" cy="210" rx="42" ry="8" fill="#5d4037" />
      </g>

      {/* Tallo principal */}
      <path className="tallo" d="M110 210 Q108 150 110 90" stroke="#33691e" strokeWidth="6" fill="none" strokeLinecap="round" />

      {/* Hoja 1 (izquierda grande) */}
      <g className="hoja hoja-1" style={{ transformOrigin: '110px 120px' }}>
        <path
          d="M110 120
             C70 110, 40 90, 30 60
             C28 40, 45 25, 70 30
             C85 33, 95 45, 100 60
             C105 75, 108 95, 110 120 Z"
          fill="url(#hojaGrad)" stroke="#1b5e20" strokeWidth="1.5"
          fillRule="evenodd"
        />
        {/* Fenestracion (agujero) */}
        <ellipse cx="62" cy="70" rx="9" ry="14" fill="#fafafa" opacity="0.85" />
        <ellipse cx="80" cy="52" rx="7" ry="10" fill="#fafafa" opacity="0.85" />
      </g>

      {/* Hoja 2 (derecha grande) */}
      <g className="hoja hoja-2" style={{ transformOrigin: '110px 110px' }}>
        <path
          d="M110 110
             C150 100, 180 80, 190 50
             C192 30, 175 15, 150 20
             C135 23, 125 35, 120 50
             C115 65, 112 85, 110 110 Z"
          fill="url(#hojaGrad)" stroke="#1b5e20" strokeWidth="1.5"
        />
        <ellipse cx="158" cy="60" rx="9" ry="14" fill="#fafafa" opacity="0.85" />
        <ellipse cx="140" cy="42" rx="7" ry="10" fill="#fafafa" opacity="0.85" />
      </g>

      {/* Hoja 3 (superior central, mas joven) */}
      <g className="hoja hoja-3" style={{ transformOrigin: '110px 95px' }}>
        <path
          d="M110 95
             C95 85, 90 60, 100 40
             C105 25, 120 20, 130 35
             C138 48, 132 70, 125 85
             C120 92, 115 95, 110 95 Z"
          fill="#66bb6a" stroke="#2e7d32" strokeWidth="1.2"
        />
        <ellipse cx="115" cy="52" rx="5" ry="8" fill="#fafafa" opacity="0.7" />
      </g>

      {/* Hoja 4 (izquierda baja) */}
      <g className="hoja hoja-4" style={{ transformOrigin: '105px 160px' }}>
        <path
          d="M105 160
             C80 155, 55 145, 45 125
             C42 112, 52 102, 70 105
             C82 107, 92 115, 98 128
             C102 140, 104 150, 105 160 Z"
          fill="url(#hojaGrad)" stroke="#1b5e20" strokeWidth="1.3"
        />
        <ellipse cx="62" cy="125" rx="6" ry="9" fill="#fafafa" opacity="0.8" />
      </g>

      {/* Hoja 5 (derecha baja) */}
      <g className="hoja hoja-5" style={{ transformOrigin: '115px 165px' }}>
        <path
          d="M115 165
             C140 160, 165 150, 175 130
             C178 117, 168 107, 150 110
             C138 112, 128 120, 122 133
             C118 145, 116 155, 115 165 Z"
          fill="url(#hojaGrad)" stroke="#1b5e20" strokeWidth="1.3"
        />
        <ellipse cx="162" cy="130" rx="6" ry="9" fill="#fafafa" opacity="0.8" />
      </g>
    </svg>
  )
}