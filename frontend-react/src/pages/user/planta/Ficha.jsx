import { Navigate, useLocation } from 'react-router-dom'
import { RUTAS } from '../../../routes/rutas'
import FichaPlanta from './FichaPlanta'

// Ficha de la planta recien escaneada. El resultado llega por el estado de la ruta;
// si se recarga la pagina se pierde y se vuelve a Escanear.
export default function Ficha() {
  const { state } = useLocation()
  if (!state?.resultado) return <Navigate to={RUTAS.ESCANEAR} replace />
  return <FichaPlanta resultado={state.resultado} />
}
