import { useNavigate } from 'react-router-dom'
import { RUTAS } from '../../../../routes/rutas'
import Brote from '../../../../components/Brote'
import Icono from '../../../../components/Icono'

// Inicio sin plantas: en vez de un hueco, una planta grande como la de la bienvenida y la invitacion a escanear la primera
export default function SinPlantas() {
  const irA = useNavigate()
  return (
    <section className="tarjeta sin-plantas aparece" style={{ animationDelay: '0.1s' }}>
      <div className="sin-plantas-escena">
        <span className="sin-plantas-globo">✨ ¡Hola, jardinero!</span>
        <Brote tamano={260} />
      </div>
      <h2>Tu jardín <span>está esperando</span></h2>
      <p>Empieza a digitalizar tus plantas para ver tu jardín aquí. Escanea la primera para identificarla y empezar a cuidarla.</p>
      <button className="sin-plantas-boton" onClick={() => irA(RUTAS.ESCANEAR)}>
        <Icono nombre="cam" /> Escanear una planta
      </button>
    </section>
  )
}
