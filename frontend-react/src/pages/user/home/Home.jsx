import { useNavigate } from 'react-router-dom'
import Boton from '../../../components/Boton'
import { RUTAS } from '../../../routes/rutas'
import { useSesion } from '../../../store/Sesion'

// Pagina provisional: el inicio real llega en el commit 3
export default function Home() {
  const { usuario, cerrar } = useSesion()
  const irA = useNavigate()

  return (
    <main className="aparece" style={{ padding: 32, textAlign: 'center' }}>
      <h1>Hola, {usuario?.nombre}</h1>
      <p style={{ color: 'var(--texto-suave)', margin: '8px 0 20px' }}>Sesión iniciada.</p>
      <Boton variante="suave" onClick={() => { cerrar(); irA(RUTAS.WELCOME, { replace: true }) }}>Cerrar sesión</Boton>
    </main>
  )
}
