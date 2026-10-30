import { Navigate, Route, Routes } from 'react-router-dom'
import { RUTAS } from './routes/rutas'
import { useSesion } from './store/Sesion'
import Auth from './pages/user/auth/Auth'
import Home from './pages/user/home/Home'
import Terminos from './pages/user/legal/Terminos'
import Welcome from './pages/user/welcome/Welcome'

// Las paginas con sesion redirigen a la bienvenida si no hay usuario
function Protegida({ children }) {
  const { usuario } = useSesion()
  return usuario ? children : <Navigate to={RUTAS.WELCOME} replace />
}

export default function App() {
  return (
    <Routes>
      <Route path={RUTAS.WELCOME} element={<Welcome />} />
      <Route path={RUTAS.ACCESO} element={<Auth />} />
      <Route path={RUTAS.TERMINOS} element={<Terminos />} />
      <Route path={RUTAS.HOME} element={<Protegida><Home /></Protegida>} />
      <Route path="*" element={<Navigate to={RUTAS.WELCOME} replace />} />
    </Routes>
  )
}
