import { useCallback, useEffect, useState } from 'react'
import { obtenerActividad, obtenerResumen, obtenerUltimosEscaneos } from '../../../services/admin/estadisticas'
import Grafico from './Grafico'
import TablaEscaneos from './TablaEscaneos'
import './dashboard.css'

const formato = (n) => n.toLocaleString('es-PE')

export default function Dashboard() {
  const [estado, setEstado] = useState({ cargando: true, resumen: null, actividad: [], escaneos: [], error: '' })

  const cargar = useCallback(async () => {
    setEstado((e) => ({ ...e, cargando: true, error: '' }))
    try {
      const [resumen, actividad, escaneos] = await Promise.all([obtenerResumen(), obtenerActividad(), obtenerUltimosEscaneos()])
      setEstado({ cargando: false, resumen, actividad, escaneos, error: '' })
    } catch {
      setEstado({ cargando: false, resumen: null, actividad: [], escaneos: [], error: 'No se pudo leer Django. ¿Está encendido en el puerto 8000?' })
    }
  }, [])

  useEffect(() => { cargar() }, [cargar])

  const { cargando, resumen, actividad, escaneos, error } = estado

  if (cargando) return <div className="home-cargando"><span className="home-giro" aria-label="Cargando" /></div>

  if (error) {
    return (
      <div className="home-error" role="alert">
        <span>{error}</span>
        <button onClick={cargar}>Reintentar</button>
      </div>
    )
  }

  const tarjetas = [
    { icono: '👥', titulo: 'Total usuarios', valor: formato(resumen.usuarios), nota: `+${resumen.crecimientoUsuarios}% este mes`, ejemplo: true },
    { icono: '📷', titulo: 'Escaneos IA hoy', valor: formato(resumen.escaneosHoy), nota: `Pico: ${resumen.picoPorMinuto} req/min`, ejemplo: true },
    { icono: '📖', titulo: 'Especies en catálogo', valor: formato(resumen.especies), nota: `${resumen.toxicas} tóxicas para mascotas${resumen.origenCatalogo === 'spring' ? ' · leído de Spring' : ''}` },
    { icono: '⚠️', titulo: 'Eventos activos', valor: formato(resumen.eventosActivos), nota: `${resumen.eventosTotales} ${resumen.eventosTotales === 1 ? 'evento' : 'eventos'} en total` },
  ]

  return (
    <div className="dashboard">
      <p className="dashboard-intro aparece">Resumen del motor IA, los eventos climáticos y la comunidad. Las tarjetas marcadas con <span className="dashboard-ejemplo">ejemplo</span> aún no vienen del servidor.</p>

      <section className="dashboard-tarjetas">
        {tarjetas.map((t, i) => (
          <article key={t.titulo} className="tarjeta kpi aparece" style={{ animationDelay: `${i * 0.06}s` }}>
            <div className="kpi-cabecera">
              <span>{t.titulo}</span>
              <i>{t.icono}</i>
            </div>
            <b>{t.valor}</b>
            <small>{t.nota}</small>
            {t.ejemplo && <span className="dashboard-ejemplo kpi-etiqueta">ejemplo</span>}
          </article>
        ))}
      </section>

      <section className="tarjeta aparece" style={{ animationDelay: '0.25s' }}>
        <div className="dashboard-titulo">
          <div>
            <h2>Actividad de usuarios y escaneos</h2>
            <p>Últimos 14 días</p>
          </div>
          <span className="dashboard-ejemplo">ejemplo</span>
        </div>
        <Grafico datos={actividad} />
      </section>

      <section className="tarjeta aparece" style={{ animationDelay: '0.32s' }}>
        <div className="dashboard-titulo">
          <div>
            <h2>Últimos escaneos registrados</h2>
            <p>Fotos subidas por los usuarios y procesadas por la IA</p>
          </div>
          <span className="dashboard-ejemplo">ejemplo</span>
        </div>
        <TablaEscaneos escaneos={escaneos} />
      </section>
    </div>
  )
}
