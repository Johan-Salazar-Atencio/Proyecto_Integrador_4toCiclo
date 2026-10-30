import { Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import LayoutAdmin from './components/admin/LayoutAdmin'
import { RUTAS, RUTAS_ADMIN } from './routes/rutas'
import { useSesion } from './store/Sesion'
import { useSesionAdmin } from './store/SesionAdmin'
import LoginAdmin from './pages/admin/login/LoginAdmin'
import CatalogoAdmin from './pages/admin/catalogo/CatalogoAdmin'
import Eventos from './pages/admin/eventos/Eventos'
import Dashboard from './pages/admin/dashboard/Dashboard'
import Auth from './pages/user/auth/Auth'
import Catalogo from './pages/user/catalogo/Catalogo'
import DetalleCatalogo from './pages/user/catalogo/DetalleCatalogo'
import Coleccion from './pages/user/coleccion/Coleccion'
import DetalleColeccion from './pages/user/coleccion/DetalleColeccion'
import Escaneo from './pages/user/escaneo/Escaneo'
import Home from './pages/user/home/Home'
import Historial from './pages/user/historial/Historial'
import Jardin from './pages/user/jardin/Jardin'
import Terminos from './pages/user/legal/Terminos'
import Perfil from './pages/user/perfil/Perfil'
import Ficha from './pages/user/planta/Ficha'
import Welcome from './pages/user/welcome/Welcome'

// Las paginas con sesion redirigen a la bienvenida si no hay usuario
function Protegida({ children }) {
  const { usuario } = useSesion()
  return usuario ? children : <Navigate to={RUTAS.WELCOME} replace />
}

// Lo mismo para el panel de administracion
function ProtegidaAdmin({ children }) {
  const { admin } = useSesionAdmin()
  return admin ? children : <Navigate to={RUTAS_ADMIN.LOGIN} replace />
}

export default function App() {
  return (
    <Routes>
      <Route path={RUTAS.WELCOME} element={<Welcome />} />
      <Route path={RUTAS.ACCESO} element={<Auth />} />
      <Route path={RUTAS.TERMINOS} element={<Terminos />} />

      <Route element={<Protegida><Layout /></Protegida>}>
        <Route path={RUTAS.HOME} element={<Home />} />
        <Route path={RUTAS.ESCANEAR} element={<Escaneo />} />
        <Route path={RUTAS.FICHA} element={<Ficha />} />
        <Route path={RUTAS.JARDIN} element={<Jardin />} />
        <Route path={RUTAS.COLECCION} element={<Coleccion />} />
        <Route path={`${RUTAS.COLECCION}/:id`} element={<DetalleColeccion />} />
        <Route path={RUTAS.CATALOGO} element={<Catalogo />} />
        <Route path={`${RUTAS.CATALOGO}/:id`} element={<DetalleCatalogo />} />
        <Route path={RUTAS.HISTORIAL} element={<Historial />} />
        <Route path={RUTAS.PERFIL} element={<Perfil />} />
      </Route>

      <Route path={RUTAS_ADMIN.LOGIN} element={<LoginAdmin />} />
      <Route element={<ProtegidaAdmin><LayoutAdmin /></ProtegidaAdmin>}>
        <Route path={RUTAS_ADMIN.DASHBOARD} element={<Dashboard />} />
        <Route path={RUTAS_ADMIN.EVENTOS} element={<Eventos />} />
        <Route path={RUTAS_ADMIN.CATALOGO} element={<CatalogoAdmin />} />
      </Route>

      <Route path="*" element={<Navigate to={RUTAS.WELCOME} replace />} />
    </Routes>
  )
}
