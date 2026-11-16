import { useCallback, useEffect, useState } from 'react'
import { obtenerEstadisticas } from '../../../services/admin/estadisticas'
import Grafico from './Grafico'
import TablaEscaneos from './TablaEscaneos'
import './dashboard.css'

const formato = (n) => (n == null ? '—' : Number(n).toLocaleString('es-PE'))

export default function Dashboard() {
  const [estado, setEstado] = useState({ cargando: true, resumen: null, actividad: [], escaneos: [], error: '' })

  // `silencioso`: el refresco automatico no vuelve a mostrar el giro de carga
  const cargar = useCallback(async (silencioso = false) => {
    if (!silencioso) setEstado((e) => ({ ...e, cargando: true, error: '' }))
    try {
      const { resumen, actividad, escaneos } = await obtenerEstadisticas()
      setEstado({ cargando: false, resumen, actividad, escaneos, error: '' })
    } catch {
      setEstado({ cargando: false, resumen: null, actividad: [], escaneos: [], error: 'No se pudo leer Django. ¿Está encendido en el puerto 8000?' })
    }
  }, [])

  useEffect(() => {
    cargar()
    const reloj = setInterval(() => cargar(true), 10000) // en vivo: se actualiza solo cada 10 s
    return () => clearInterval(reloj)
  }, [cargar])

  const { cargando, resumen, actividad, escaneos, error } = estado

  if (cargando) return <div className="home-cargando"><span className="home-giro" aria-label="Cargando" /></div>

  if (error) {
    return (
      <div className="home-error" role="alert">
        <span>{error}</span>
        <button onClick={() => cargar()}>Reintentar</button>
      </div>
    )
  }

  const tarjetas = [
    { icono: '👥', titulo: 'Total usuarios', valor: formato(resumen.usuarios), nota: `${resumen.usuariosNuevosMes} nuevos este mes${resumen.crecimientoUsuarios == null ? '' : ` (${resumen.crecimientoUsuarios >= 0 ? '+' : ''}${resumen.crecimientoUsuarios}% vs. mes pasado)`}` },
    { icono: '📷', titulo: 'Escaneos IA hoy', valor: formato(resumen.escaneosHoy), nota: `Pico: ${resumen.picoPorMinuto} escaneos/min` },
    { icono: '📖', titulo: 'Especies en catálogo', valor: formato(resumen.especies), nota: `${resumen.toxicas} tóxicas para mascotas` },
    { icono: '⚠️', titulo: 'Eventos activos', valor: formato(resumen.eventosActivos), nota: `${resumen.eventosTotales} ${resumen.eventosTotales === 1 ? 'evento' : 'eventos'} en total` },
  ]

  return (
    <div className="dashboard">
      <p className="dashboard-intro aparece">Resumen del motor IA, los eventos climáticos y la comunidad. Datos reales de la base de datos, se actualizan solos cada 10 segundos.</p>

      <section className="dashboard-tarjetas">
        {tarjetas.map((t, i) => (
          <article key={t.titulo} className="tarjeta kpi aparece" style={{ animationDelay: `${i * 0.06}s` }}>
            <div className="kpi-cabecera">
              <span>{t.titulo}</span>
              <i>{t.icono}</i>
            </div>
            <b>{t.valor}</b>
            <small>{t.nota}</small>
          </article>
        ))}
      </section>

      <section className="tarjeta aparece" style={{ animationDelay: '0.25s' }}>
        <div className="dashboard-titulo">
          <div>
            <h2>Usuarios nuevos y escaneos</h2>
            <p>Últimos 14 días: registros nuevos y escaneos por día</p>
          </div>
        </div>
        <Grafico datos={actividad} />
      </section>

      <section className="tarjeta aparece" style={{ animationDelay: '0.32s' }}>
        <div className="dashboard-titulo">
          <div>
            <h2>Últimos escaneos registrados</h2>
            <p>Fotos subidas por los usuarios y procesadas por la IA</p>
          </div>
        </div>
        <TablaEscaneos escaneos={escaneos} />
      </section>
    </div>
  )
}
