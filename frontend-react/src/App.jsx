import { Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import LayoutAdmin from './components/admin/LayoutAdmin'
import { RUTAS, RUTAS_ADMIN } from './routes/rutas'
import { useSesion } from './store/Sesion'
import { useSesionAdmin } from './store/SesionAdmin'
import LoginAdmin from './pages/admin/login/LoginAdmin'
import CatalogoAdmin from './pages/admin/catalogo/CatalogoAdmin'
import Eventos from './pages/admin/eventos/Eventos'
import Misiones from './pages/admin/misiones/Misiones'
import Logros from './pages/admin/logros/Logros'
import TiendaAdmin from './pages/admin/tienda/TiendaAdmin'
import Dashboard from './pages/admin/dashboard/Dashboard'
import JuegoAdmin from './pages/admin/juego/JuegoAdmin'
import Auth from './pages/user/auth/Auth'
import Catalogo from './pages/user/catalogo/Catalogo'
import DetalleCatalogo from './pages/user/catalogo/DetalleCatalogo'
import Coleccion from './pages/user/coleccion/Coleccion'
import DetalleColeccion from './pages/user/coleccion/DetalleColeccion'
import Escaneo from './pages/user/escaneo/Escaneo'
import Home from './pages/user/home/Home'
import DemoPlanta from './pages/user/DemoPlanta'
import Historial from './pages/user/historial/Historial'
import Jardin from './pages/user/jardin/Jardin'
import LogrosUsuario from './pages/user/logros/LogrosUsuario'
import MisionesUsuario from './pages/user/misiones/MisionesUsuario'
import Terminos from './pages/user/legal/Terminos'
import Tienda from './pages/user/tienda/Tienda'
import EditarDatos from './pages/user/perfil/EditarDatos'
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
      <Route path={RUTAS.DEMO_PLANTA} element={<DemoPlanta />} />
      <Route path={RUTAS.ACCESO} element={<Auth />} />
      <Route path={RUTAS.TERMINOS} element={<Terminos />} />

      <Route element={<Protegida><Layout /></Protegida>}>
        <Route path={RUTAS.HOME} element={<Home />} />
        <Route path={RUTAS.ESCANEAR} element={<Escaneo />} />
        <Route path={RUTAS.FICHA} element={<Ficha />} />
        <Route path={RUTAS.JARDIN} element={<Jardin />} />
        <Route path={RUTAS.MISIONES} element={<MisionesUsuario />} />
        <Route path={RUTAS.LOGROS} element={<LogrosUsuario />} />
        <Route path={RUTAS.TIENDA} element={<Tienda />} />
        <Route path={RUTAS.COLECCION} element={<Coleccion />} />
        <Route path={`${RUTAS.COLECCION}/:id`} element={<DetalleColeccion />} />
        <Route path={RUTAS.CATALOGO} element={<Catalogo />} />
        <Route path={`${RUTAS.CATALOGO}/:id`} element={<DetalleCatalogo />} />
        <Route path={RUTAS.HISTORIAL} element={<Historial />} />
        <Route path={RUTAS.PERFIL} element={<Perfil />} />
        <Route path={RUTAS.EDITAR_DATOS} element={<EditarDatos />} />
      </Route>

      <Route path={RUTAS_ADMIN.LOGIN} element={<LoginAdmin />} />
      <Route element={<ProtegidaAdmin><LayoutAdmin /></ProtegidaAdmin>}>
        <Route path={RUTAS_ADMIN.DASHBOARD} element={<Dashboard />} />
        <Route path={RUTAS_ADMIN.EVENTOS} element={<Eventos />} />
        <Route path={RUTAS_ADMIN.MISIONES} element={<Misiones />} />
        <Route path={RUTAS_ADMIN.LOGROS} element={<Logros />} />
        <Route path={RUTAS_ADMIN.TIENDA} element={<TiendaAdmin />} />
        <Route path={RUTAS_ADMIN.JUEGO} element={<JuegoAdmin />} />
        <Route path={RUTAS_ADMIN.TIPOS_EVENTO} element={<Navigate to={RUTAS_ADMIN.EVENTOS} replace />} />
        <Route path={RUTAS_ADMIN.CATALOGO} element={<CatalogoAdmin />} />
      </Route>

      <Route path="*" element={<Navigate to={RUTAS.WELCOME} replace />} />
    </Routes>
  )
}
