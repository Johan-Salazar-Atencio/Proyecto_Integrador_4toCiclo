import { Route, Routes } from 'react-router-dom'
import { RUTAS } from './routes/rutas'
import Inicio from './pages/user/Inicio'

export default function App() {
  return (
    <Routes>
      <Route path={RUTAS.INICIO} element={<Inicio />} />
    </Routes>
  )
}
