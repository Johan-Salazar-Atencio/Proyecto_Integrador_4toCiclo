import { useState } from 'react'
import Digitalizacion from '../../components/Digitalizacion'
import PlantaPixel from '../../components/PlantaPixel'

// Galeria de las plantas pixel art del jardin: todas las formas y etapas, y una grande para
// probar los efectos de cuidado y el cambio de etapa.
const ETAPAS = ['Semilla', 'Brote', 'Joven', 'Adulta', 'Etapa 5', 'Etapa 6', 'Marchita']
const PLANTAS = [
  ['Monstera', { forma: 'monstera', colorHoja: '#2f7d4a' }],
  ['Arbusto con fruto', { forma: 'arbusto', colorHoja: '#4f9a52', tieneFruto: true, colorFruto: '#d9473b' }],
  ['Árbol con flor', { forma: 'arbol', colorHoja: '#3f8a4a', tieneFlor: true, colorFlor: '#f6c1d5' }],
  ['Árbol con fruto', { forma: 'arbol', colorHoja: '#3b8446', tieneFruto: true, colorFruto: '#f0a12a' }],
  ['Helecho', { forma: 'helecho', colorHoja: '#4a9a4a' }],
  ['Palmera', { forma: 'palmera', colorHoja: '#3d8b3d' }],
  ['Suculenta con flor', { forma: 'suculenta', colorHoja: '#7fb08a', tieneFlor: true, colorFlor: '#f472b6' }],
  ['Roseta', { forma: 'roseta', colorHoja: '#6f9f8f' }],
  ['Cactus con flor', { forma: 'cactus', colorHoja: '#5f9e4a', tieneFlor: true, colorFlor: '#f25f8a' }],
  ['Trepadora', { forma: 'trepadora', colorHoja: '#4d9c50' }],
]
// "Foto" de prueba para la secuencia de digitalizar (en la app es la foto del escaneo)
function fotoDePrueba() {
  const c = document.createElement('canvas')
  c.width = c.height = 320
  const g = c.getContext('2d')
  const cielo = g.createLinearGradient(0, 0, 0, 320)
  cielo.addColorStop(0, '#cfe6f5')
  cielo.addColorStop(1, '#e9dcc4')
  g.fillStyle = cielo
  g.fillRect(0, 0, 320, 320)
  g.fillStyle = '#b5683f'
  g.fillRect(110, 230, 100, 80)
  for (let i = 0; i < 9; i++) {
    g.fillStyle = i % 2 ? '#2f7d4a' : '#4fa863'
    g.beginPath()
    g.ellipse(160 + Math.cos(i * 0.7) * 70, 150 + Math.sin(i * 1.3) * 60, 52, 30, i, 0, 6.3)
    g.fill()
  }
  return c.toDataURL()
}

const FONDO = 'linear-gradient(180deg, #dceefb 0%, #dff3e6 100%)'

export default function DemoPlanta() {
  const [planta, setPlanta] = useState(0)
  const [etapa, setEtapa] = useState(3)
  const [efecto, setEfecto] = useState(null)
  const [foto, setFoto] = useState(null)
  const lanzar = (tipo) => setEfecto({ tipo, id: Date.now() })

  return (
    <div style={{ padding: 24, maxWidth: 1100, margin: '0 auto' }}>
      <h1>Plantas del jardín (pixel art)</h1>

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 24, alignItems: 'center', margin: '16px 0 32px' }}>
        <div style={{ width: 256, height: 256, background: FONDO, borderRadius: 28 }}>
          {foto
            ? <Digitalizacion foto={foto} apariencia={PLANTAS[planta][1]} alTerminar={() => { setFoto(null); lanzar('CRECER') }} />
            : <PlantaPixel etapa={etapa} apariencia={PLANTAS[planta][1]} efecto={efecto} titulo={PLANTAS[planta][0]} />}
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <select value={planta} onChange={(e) => setPlanta(Number(e.target.value))}>
            {PLANTAS.map(([nombre], i) => <option key={nombre} value={i}>{nombre}</option>)}
          </select>
          <label>
            {ETAPAS[etapa]}{' '}
            <input type="range" min="0" max="6" value={etapa} onChange={(e) => setEtapa(Number(e.target.value))} />
          </label>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="boton boton-suave" onClick={() => lanzar('REGAR')}>💧 Regar</button>
            <button className="boton boton-suave" onClick={() => lanzar('SOL')}>☀️ Sol</button>
            <button className="boton boton-suave" onClick={() => lanzar('ABONAR')}>🌿 Abonar</button>
          </div>
          <button className="boton boton-principal" onClick={() => { setEtapa(0); setFoto(fotoDePrueba()) }}>🪴 Digitalizar</button>
        </div>
      </div>

      {PLANTAS.map(([nombre, apariencia]) => (
        <section key={nombre} style={{ marginBottom: 20 }}>
          <h2 style={{ fontSize: '1rem', marginBottom: 8 }}>{nombre}</h2>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            {ETAPAS.map((texto, i) => (
              <div key={texto} style={{ width: 128, height: 128, background: FONDO, borderRadius: 16 }}>
                <PlantaPixel etapa={i} apariencia={apariencia} titulo={`${nombre}: ${texto}`} />
              </div>
            ))}
            {/* Marchita y muerta en su propia etapa (brote y adulta), para comparar */}
            {[[1, 'marchita', 'Brote marchito'], [5, 'marchita', 'Madura marchita'], [1, 'muerta', 'Brote muerto'], [5, 'muerta', 'Madura muerta']].map(([e, estado, texto]) => (
              <div key={texto} style={{ width: 128, height: 128, background: FONDO, borderRadius: 16 }}>
                <PlantaPixel etapa={e} estado={estado} apariencia={apariencia} titulo={`${nombre}: ${texto}`} />
              </div>
            ))}
          </div>
        </section>
      ))}
    </div>
  )
}
