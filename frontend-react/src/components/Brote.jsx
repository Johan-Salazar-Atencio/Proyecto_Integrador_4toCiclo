import './brote.css'

// Brote con carita dibujado en SVG; "respira" en bucle (como BroteEnMaceta del movil)
export default function Brote({ tamano = 180 }) {
  return (
    <svg className="brote" width={tamano} height={tamano} viewBox="0 0 200 200" role="img" aria-label="Brote de Plantia">
      <ellipse cx="100" cy="186" rx="52" ry="7" fill="rgba(0,0,0,0.08)" />
      <g className="brote-planta">
        <path d="M100 118 C100 92 100 80 100 66" stroke="#16a34a" strokeWidth="7" strokeLinecap="round" fill="none" />
        <path d="M100 76 C70 76 50 58 52 36 C78 34 98 50 100 76Z" fill="#4ade80" />
        <path d="M100 70 C128 70 150 52 148 30 C122 28 102 44 100 70Z" fill="#22c55e" />
      </g>
      <path d="M52 118 H148 L138 176 Q137 184 128 184 H72 Q63 184 62 176Z" fill="#f59e0b" />
      <rect x="46" y="110" width="108" height="16" rx="8" fill="#d97706" />
      <circle cx="84" cy="150" r="5" fill="#1f2937" />
      <circle cx="116" cy="150" r="5" fill="#1f2937" />
      <circle cx="75" cy="160" r="6" fill="#fca5a5" opacity="0.7" />
      <circle cx="125" cy="160" r="6" fill="#fca5a5" opacity="0.7" />
      <path d="M92 160 Q100 168 108 160" stroke="#1f2937" strokeWidth="3.5" strokeLinecap="round" fill="none" />
    </svg>
  )
}
