import { useNavigate } from 'react-router-dom'
import Boton from '../../../components/Boton'
import './terminos.css'

const SECCIONES = [
  ['Qué es Plantia', 'Plantia es un proyecto académico de Tecsup para identificar y cuidar plantas. No es un servicio comercial.'],
  ['Qué datos guardamos', 'Tu nombre, tu correo, tu contraseña (guardada cifrada) y las fotos de las plantas que escaneas.'],
  ['Para qué los usamos', 'Para tu cuenta, tu colección y tu jardín. No los vendemos ni los compartimos con terceros.'],
  ['Inteligencia artificial', 'Las fotos se envían a un servicio de IA externo para identificar la planta. Si el proyecto cambia de proveedor, este aviso sigue siendo válido.'],
  ['Identificación orientativa', 'La IA puede equivocarse. Antes de acercar una planta a una mascota o a un niño, confírmalo con un especialista, sobre todo su toxicidad.'],
  ['Tu cuenta', 'Puedes pedir que se borre tu cuenta. Te pedimos usar la app de forma responsable.'],
  ['Cambios', 'Estos términos pueden actualizarse durante el curso. Te avisaremos dentro de la app.'],
]

export default function Terminos() {
  const irA = useNavigate()

  return (
    <main className="terminos">
      <article className="terminos-tarjeta aparece">
        <h1>Términos y privacidad</h1>
        <p className="terminos-aviso">Texto de ejemplo para un proyecto académico, sin valor legal.</p>

        <ol>
          {SECCIONES.map(([titulo, texto]) => (
            <li key={titulo}>
              <h2>{titulo}</h2>
              <p>{texto}</p>
            </li>
          ))}
        </ol>

        <Boton onClick={() => irA(-1)}>Entendido</Boton>
      </article>
    </main>
  )
}
