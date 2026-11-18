import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { RUTAS } from '../../../routes/rutas'
import { obtenerMisiones } from '../../../services/home'
import Icono from '../../../components/Icono'
import useRestante, { formatoCuenta } from '../useRestante'
import MisionFila from './MisionFila'
import '../home/home.css'
import './misiones.css'

// Mision del evento activo: tarjeta destacada con la cuenta regresiva del evento
function MisionEvento({ mision }) {
  const restante = useRestante(mision.endsInSeconds)
  const porcentaje = Math.round(Math.min(1, Math.max(0, mision.progress || 0)) * 100)
  return (
    <section className="tarjeta mision-evento aparece">
      <div className="mision-evento-cabecera">
        <b><Icono nombre="alert" tam={16} /> EVENTO ACTIVO</b>
        {restante != null && <b>Termina en {formatoCuenta(restante)}</b>}
      </div>
      <h2>{mision.title}</h2>
      <div className="barra" role="progressbar" aria-valuenow={porcentaje} aria-valuemin={0} aria-valuemax={100}>
        <div className="barra-relleno" style={{ width: `${porcentaje}%` }} />
      </div>
      <p>{mision.currentCount} de {mision.targetCount} plantas protegidas · {mision.reward}</p>
      {!mision.isCompleted && <small>Entra a cada planta virtual y toca «Prevenir evento».</small>}
      {mision.isCompleted
        ? <span className="mision-evento-lista">Evento prevenido</span>
        : <Link to={RUTAS.COLECCION} className="mision-evento-boton">Ir a mis plantas</Link>}
    </section>
  )
}

// Todas las misiones de hoy (GET /misiones); la del evento activo va primero y destacada
export default function MisionesUsuario() {
  const [estado, setEstado] = useState({ cargando: true, misiones: [], error: '' })
  const cargar = () => {
    setEstado((e) => ({ ...e, cargando: true, error: '' }))
    obtenerMisiones()
      .then((misiones) => setEstado({ cargando: false, misiones: misiones || [], error: '' }))
      .catch(() => setEstado({ cargando: false, misiones: [], error: 'No se pudieron cargar las misiones.' }))
  }
  useEffect(cargar, [])

  if (estado.cargando) return <div className="home-cargando"><span className="home-giro" aria-label="Cargando" /></div>
  if (estado.error) {
    return (
      <div className="coleccion-vacio aparece">
        <span>😕</span>
        <h2>{estado.error}</h2>
        <button className="boton boton-suave" onClick={cargar}>Reintentar</button>
      </div>
    )
  }

  const evento = estado.misiones.filter((m) => m.kind === 'EVENT')
  const diarias = estado.misiones.filter((m) => m.kind !== 'EVENT')
  const hechas = diarias.filter((m) => m.isCompleted).length
  return (
    <div className="misiones-usuario">
      {evento.map((m) => <MisionEvento key={m.id} mision={m} />)}
      <section className="tarjeta ms aparece">
        <div className="ms-cabecera">
          <div>
            <h2>Misiones de hoy</h2>
            {diarias.length > 0 && <small>{hechas} de {diarias.length} completadas</small>}
          </div>
        </div>
        {diarias.length === 0 && <p className="misiones-vacio">Hoy no hay misiones diarias.</p>}
        <div className="ms-lista">
          {diarias.map((m) => <MisionFila key={m.id} mision={m} />)}
        </div>
      </section>
    </div>
  )
}
