import { useEffect, useState } from 'react'
import Icono from '../../../../components/Icono'
import escena1 from '../../../../assets/escenas/sin_evento_1.webp'
import escena2 from '../../../../assets/escenas/sin_evento_2.webp'
import escena3 from '../../../../assets/escenas/sin_evento_3.webp'
import escena4 from '../../../../assets/escenas/sin_evento_4.webp'

// Consejos fijos del juego (los mismos textos del movil; no son datos del usuario); cambian cada 6 segundos junto con la escena
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

// Las mismas escenas del campo peruano que el movil (ilustraciones: dia, lluvia en los andenes, atardecer y noche)
const ESCENAS = [escena1, escena2, escena3, escena4]
const CADA_MS = 6000

// Tarjeta de Inicio cuando no hay ningun evento: carrusel de escenas que pasan solas (o con los puntos) y un consejo
// que cambia con cada escena, como en el movil
export default function SinEvento() {
  const [paso, setPaso] = useState(0)
  // Si la persona elige una escena con los puntos, la cuenta de 6 segundos vuelve a empezar desde ahi
  useEffect(() => {
    const id = setTimeout(() => setPaso((p) => p + 1), CADA_MS)
    return () => clearTimeout(id)
  }, [paso])
  const activa = paso % ESCENAS.length

  return (
    <section className="tarjeta calmo aparece" style={{ animationDelay: '0.05s' }}>
      <div className="calmo-escenas">
        {ESCENAS.map((src, i) => (
          <img key={src} className={`calmo-escena ${i === activa ? 'activa' : ''}`} src={src} alt="" aria-hidden="true" loading={i === 0 ? 'eager' : 'lazy'} decoding="async" />
        ))}
        <div className="calmo-puntos" role="group" aria-label="Escenas del campo">
          {ESCENAS.map((src, i) => (
            <button key={src} type="button" className={i === activa ? 'activa' : ''} onClick={() => setPaso(i + Math.floor(paso / ESCENAS.length) * ESCENAS.length)} aria-label={`Escena ${i + 1}`} aria-pressed={i === activa} />
          ))}
        </div>
      </div>
      <div className="calmo-cuerpo">
        <div className="calmo-titulo">
          <span className="calmo-sol"><Icono nombre="sun" /></span>
          <div>
            <h2>No hay eventos por el momento</h2>
            <p>Sigue cuidando tus plantas y mantente alerta a los posibles eventos.</p>
          </div>
        </div>
        <div className="calmo-consejo" role="status">
          <Icono nombre="bulb" />
          <span>{CONSEJOS[paso % CONSEJOS.length]}</span>
        </div>
      </div>
    </section>
  )
}
