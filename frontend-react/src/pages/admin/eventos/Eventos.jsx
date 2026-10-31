import { useCallback, useEffect, useRef, useState } from 'react'
import { cancelarEvento, crearEvento, listarEventos } from '../../../services/admin/eventos'
import EventoEnCurso from './EventoEnCurso'
import FormularioEvento from './FormularioEvento'
import HistorialEventos from './HistorialEventos'
import './eventos.css'

const MS_AVISO = 3500

export default function Eventos() {
  const [estado, setEstado] = useState({ cargando: true, eventos: [], error: '' })
  const [cancelando, setCancelando] = useState(false)
  const [aviso, setAviso] = useState('')
  const temporizador = useRef(null)

  const cargar = useCallback(async () => {
    try {
      setEstado({ cargando: false, eventos: await listarEventos(), error: '' })
    } catch {
      setEstado({ cargando: false, eventos: [], error: 'No se pudo leer Django. ¿Está encendido en el puerto 8000?' })
    }
  }, [])

  useEffect(() => { cargar() }, [cargar])
  useEffect(() => () => clearTimeout(temporizador.current), [])

  const avisar = (texto) => {
    setAviso(texto)
    clearTimeout(temporizador.current)
    temporizador.current = setTimeout(() => setAviso(''), MS_AVISO)
  }

  const lanzar = async (datos) => {
    await crearEvento(datos)
    await cargar()
    avisar('Evento lanzado (simulado: aún no se guarda en la base de datos)')
  }

  const cancelar = async (evento) => {
    setCancelando(true)
    await cancelarEvento(evento.id_desastre)
    await cargar()
    setCancelando(false)
    avisar('Emergencia cancelada (simulado)')
  }

  const { cargando, eventos, error } = estado
  const activo = eventos.find((e) => e.estado === 'ACTIVO')

  if (cargando) return <div className="home-cargando"><span className="home-giro" aria-label="Cargando" /></div>

  return (
    <div className="eventos">
      {error && (
        <div className="home-error" role="alert">
          <span>{error}</span>
          <button onClick={cargar}>Reintentar</button>
        </div>
      )}

      {activo && <EventoEnCurso evento={activo} cancelando={cancelando} onCancelar={() => cancelar(activo)} />}

      <FormularioEvento hayActivo={Boolean(activo)} onLanzar={lanzar} />

      <section className="tarjeta aparece" style={{ animationDelay: '0.16s' }}>
        <div className="dashboard-titulo">
          <div>
            <h2>🕘 Historial de desastres ejecutados</h2>
            <p>Registro de los eventos lanzados desde el panel</p>
          </div>
        </div>
        <HistorialEventos eventos={eventos} />
      </section>

      {aviso && <div className="jardin-aviso" role="status">{aviso}</div>}
    </div>
  )
}
