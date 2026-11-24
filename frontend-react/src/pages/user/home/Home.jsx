import AlertaClima from './components/AlertaClima'
import MisionDiaria from './components/MisionDiaria'
import SinEvento from './components/SinEvento'
import SinPlantas from './components/SinPlantas'
import TarjetaPlanta from './components/TarjetaPlanta'
import TarjetaUsuario from './components/TarjetaUsuario'
import useHome from './useHome'
import './home.css'

// Inicio: en pantallas anchas la planta va a la izquierda y usuario, evento y misiones a la derecha;
// en angostas, una sola columna (usuario, planta, evento, misiones)
export default function Home() {
  const { cargando, usuario, alerta, planta, plantas, misiones, error, aviso, ocupado, mover, seleccionar, cuidar, recargar } = useHome()

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

      <div className="home-cols">
        {usuario && <div className="home-area-usuario"><TarjetaUsuario usuario={usuario} /></div>}
        {planta ? (
          <div className="home-area-planta">
            <TarjetaPlanta planta={planta} plantas={plantas} ocupado={ocupado} onMover={mover} onElegir={seleccionar} onCuidar={cuidar} />
          </div>
        ) : !error && (
          // Sin plantas el Inicio no queda vacio: invita a escanear la primera
          <div className="home-area-planta"><SinPlantas /></div>
        )}
        <div className="home-area-evento">{alerta ? <AlertaClima alerta={alerta} /> : <SinEvento />}</div>
        {misiones.length > 0 && <div className="home-area-misiones"><MisionDiaria misiones={misiones} /></div>}
      </div>

      {aviso && <div className="jardin-aviso" role="status">{aviso}</div>}
    </div>
  )
}
