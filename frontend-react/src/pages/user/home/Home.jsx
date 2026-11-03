import AlertaClima from './components/AlertaClima'
import Accesos from './components/Accesos'
import MisionDiaria from './components/MisionDiaria'
import TarjetaPlanta from './components/TarjetaPlanta'
import TarjetaUsuario from './components/TarjetaUsuario'
import useHome from './useHome'
import './home.css'

export default function Home() {
  const { cargando, usuario, alerta, planta, mision, error, recargar } = useHome()

  if (cargando) {
    return <div className="home-cargando"><span className="home-giro" aria-label="Cargando" /></div>
  }

  return (
    <div className="home">
      {error && (
        <div className="home-error" role="alert">
          <span>{error}</span>
          <button onClick={recargar}>Reintentar</button>
        </div>
      )}

      {usuario && <TarjetaUsuario usuario={usuario} />}
      {alerta && <AlertaClima alerta={alerta} />}
      {planta && <TarjetaPlanta planta={planta} />}
      <Accesos />
      {mision && <MisionDiaria mision={mision} />}
    </div>
  )
}
