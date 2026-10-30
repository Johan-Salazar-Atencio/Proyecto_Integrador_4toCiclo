import { Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import { RUTAS } from './routes/rutas'
import { useSesion } from './store/Sesion'
import Auth from './pages/user/auth/Auth'
import Home from './pages/user/home/Home'
import Terminos from './pages/user/legal/Terminos'
import Pronto from './pages/user/pronto/Pronto'
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

      <Route element={<Protegida><Layout /></Protegida>}>
        <Route path={RUTAS.HOME} element={<Home />} />
        <Route path={RUTAS.ESCANEAR} element={<Pronto titulo="Escanear" />} />
        <Route path={RUTAS.JARDIN} element={<Pronto titulo="Jardín virtual" />} />
        <Route path={RUTAS.COLECCION} element={<Pronto titulo="Colección" />} />
        <Route path={RUTAS.CATALOGO} element={<Pronto titulo="Enciclopedia" />} />
        <Route path={RUTAS.PERFIL} element={<Pronto titulo="Perfil" />} />
      </Route>

      <Route path="*" element={<Navigate to={RUTAS.WELCOME} replace />} />
    </Routes>
  )
}
