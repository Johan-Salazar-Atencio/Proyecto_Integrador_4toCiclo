import { useEffect, useState } from 'react'
import Icono from '../../../../components/Icono'

// Consejos de ejemplo (los mismos textos del movil); cambian cada 6 segundos junto con la escena
export const CONSEJOS = [
  'Si hay una ola de calor, refresca bien a tus plantas: riégalas temprano o al atardecer.',
  'Una helada puede quemar las hojas: cubre tus plantas por la noche.',
  'Revisa las hojas todos los días: detectar una plaga a tiempo salva a la planta.',
  'Regar de más también daña: deja que la tierra se seque un poco entre riegos.',
  'Cubrir la tierra con paja o rastrojo ayuda a conservar la humedad.',
  'Los huaycos arrastran terrenos de cultivo: las zanjas desvían el agua y el lodo.',
  'Una planta con buena luz y buena agua mantiene su salud alta.',
  'Un abono a tiempo ayuda a que tu planta crezca más rápido.',
  'Escanear una especie nueva da más hojas y XP que repetir una ya vista.',
  'Algunas plantas son tóxicas para las mascotas: revisa su ficha antes de dejarlas al alcance.',
]

// Escenas del campo hechas con CSS: amanecer, dia y noche
const ESCENAS = ['amanecer', 'dia', 'noche']
const CADA_MS = 6000

// Tarjeta de Inicio cuando no hay ningun evento: escenas que rotan solas y un consejo que cambia cada 6 segundos
export default function SinEvento() {
  const [paso, setPaso] = useState(0)
  useEffect(() => {
    const id = setInterval(() => setPaso((p) => p + 1), CADA_MS)
    return () => clearInterval(id)
  }, [])
  const activa = paso % ESCENAS.length

  return (
    <section className="tarjeta calmo aparece" style={{ animationDelay: '0.05s' }}>
      <div className="calmo-escenas" aria-hidden="true">
        {ESCENAS.map((nombre, i) => (
          <div key={nombre} className={`calmo-escena calmo-${nombre} ${i === activa ? 'activa' : ''}`}>
            <div className="calmo-astro" />
            <div className="calmo-colinas" />
            <div className="calmo-cesped" />
          </div>
        ))}
        <div className="calmo-puntos">
          {ESCENAS.map((nombre, i) => <i key={nombre} className={i === activa ? 'activa' : ''} />)}
        </div>
      </div>
      <div className="calmo-cuerpo">
        <h2>No hay eventos por el momento</h2>
        <p>Tu campo está tranquilo. Te avisaremos apenas haya un desastre en tu región.</p>
        <div className="calmo-consejo" role="status">
          <Icono nombre="bulb" />
          <span>{CONSEJOS[paso % CONSEJOS.length]}</span>
        </div>
      </div>
    </section>
  )
}
