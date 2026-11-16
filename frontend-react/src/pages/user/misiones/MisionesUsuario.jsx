import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { RUTAS } from '../../../routes/rutas'
import { obtenerMisiones } from '../../../services/home'
import useRestante, { formatoCuenta } from '../useRestante'
import './misiones.css'

// A donde lleva una mision: el escaner o las plantas virtuales (donde se cuida y se previene)
const IR_A_ESCANEAR = ['ESCANEO', 'DIGITALIZAR', 'COLECCION']
const destino = (m) => (m.kind === 'EVENT' || !IR_A_ESCANEAR.includes(m.type) ? RUTAS.COLECCION : RUTAS.ESCANEAR)

function MisionEvento({ mision }) {
  const restante = useRestante(mision.endsInSeconds)
  return (
    <section className="tarjeta mision-evento aparece">
      <div className="mision-evento-cabecera">
        <b>⚠️ EVENTO ACTIVO</b>
        {restante != null && <b>Termina en {formatoCuenta(restante)}</b>}
      </div>
      <h2>{mision.title}</h2>
      <div className="barra"><div className="barra-relleno" style={{ width: `${Math.round(mision.progress * 100)}%` }} /></div>
      <p>{mision.currentCount} de {mision.targetCount} plantas protegidas · {mision.reward}</p>
      {!mision.isCompleted && <small>Entra a cada planta virtual y toca «Prevenir evento».</small>}
      {mision.isCompleted
        ? <span className="mision-evento-lista">🛡️ Evento prevenido</span>
        : <Link to={RUTAS.COLECCION} className="mision-evento-boton">🛡️ Ir a mis plantas</Link>}
    </section>
  )
}

function MisionNormal({ mision }) {
  const porcentaje = Math.round(Math.min(1, Math.max(0, mision.progress)) * 100)
  return (
    <section className="tarjeta mision aparece">
      <div className="mision-cabecera">
        <h2>🎯 {mision.title}</h2>
        {mision.isCompleted && <span className="mision-lista">¡Completada!</span>}
      </div>
      <div className="barra"><div className="barra-relleno" style={{ width: `${porcentaje}%` }} /></div>
      <div className="mision-pie">
        <small>{mision.currentCount} / {mision.targetCount}</small>
        {mision.reward && <small>Recompensa: {mision.reward}</small>}
      </div>
      {!mision.isCompleted && <Link to={destino(mision)} className="mision-ir">Ir →</Link>}
    </section>
  )
}

// Todas las misiones de hoy (GET /misiones); la del evento activo va primero y en rojo
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
  return (
    <div className="misiones">
      {evento.map((m) => <MisionEvento key={m.id} mision={m} />)}
      <h2 className="misiones-titulo">Misiones de hoy</h2>
      {diarias.length === 0 && <p className="misiones-vacio">Hoy no hay misiones diarias.</p>}
      {diarias.map((m) => <MisionNormal key={m.id} mision={m} />)}
    </div>
  )
}
