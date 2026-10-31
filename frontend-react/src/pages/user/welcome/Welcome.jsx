import { Link, useNavigate } from 'react-router-dom'
import Boton from '../../../components/Boton'
import Brote from '../../../components/Brote'
import { RUTAS } from '../../../routes/rutas'
import './welcome.css'

const ETIQUETAS = ['🌿 Escanea', '🪴 Cuida', '🏡 Colecciona']

export default function Welcome() {
  const irA = useNavigate()

  return (
    <main className="welcome">
      <div className="welcome-marca aparece">🌱 Plantia</div>

      <section className="welcome-centro">
        <div className="welcome-escena aparece" style={{ animationDelay: '0.1s' }}>
          <span className="welcome-globo">✨ ¡Hola, jardinero!</span>
          <Brote tamano={190} />
        </div>

        <h1 className="aparece" style={{ animationDelay: '0.2s' }}>
          Tu jardín <span>cobra vida</span>
        </h1>
        <p className="aparece" style={{ animationDelay: '0.3s' }}>
          Escanea una planta, identifícala con IA y cuídala como a una mascota.
        </p>

        <ul className="welcome-etiquetas aparece" style={{ animationDelay: '0.4s' }}>
          {ETIQUETAS.map((texto) => <li key={texto}>{texto}</li>)}
        </ul>

        <div className="welcome-acciones aparece" style={{ animationDelay: '0.5s' }}>
          <Boton onClick={() => irA(`${RUTAS.ACCESO}?modo=registro`)}>Crear mi Invernadero →</Boton>
          <p>
            ¿Ya tienes cuenta? <Link to={`${RUTAS.ACCESO}?modo=login`}><b>Iniciar sesión</b></Link>
          </p>
        </div>
      </section>
    </main>
  )
}
