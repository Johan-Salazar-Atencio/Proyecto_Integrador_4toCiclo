import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import EncabezadoAdmin from '../../../components/admin/EncabezadoAdmin'
import IconoAdmin from '../../../components/admin/IconoAdmin'
import { obtenerEstadisticas } from '../../../services/admin/estadisticas'
import { RUTAS_ADMIN } from '../../../routes/rutas'
import { useSesionAdmin } from '../../../store/SesionAdmin'
import Grafico from './Grafico'
import TablaEscaneos from './TablaEscaneos'
import './dashboard.css'

const formato = (n) => (n == null ? '—' : Number(n).toLocaleString('es-PE'))

export default function Dashboard() {
  const [estado, setEstado] = useState({ cargando: true, resumen: null, actividad: [], escaneos: [], error: '' })
  const { cerrar } = useSesionAdmin()
  const irA = useNavigate()

  // `silencioso`: el refresco automatico no vuelve a mostrar el giro de carga
  const cargar = useCallback(async (silencioso = false) => {
    if (!silencioso) setEstado((e) => ({ ...e, cargando: true, error: '' }))
    try {
      const { resumen, actividad, escaneos } = await obtenerEstadisticas()
      setEstado({ cargando: false, resumen, actividad, escaneos, error: '' })
    } catch (err) {
      // 401 = sesion del admin caducada o invalida (por ejemplo, de antes de V31): se vuelve al acceso como en Logros.
      // Antes se mostraba "¿Django esta encendido?" aunque Django respondiera.
      if (err.estado === 401) {
        cerrar()
        irA(RUTAS_ADMIN.LOGIN, { replace: true })
        return
      }
      setEstado({ cargando: false, resumen: null, actividad: [], escaneos: [], error: 'No se pudo leer Django. ¿Está encendido en el puerto 8000?' })
    }
  }, [cerrar, irA])

  useEffect(() => {
    cargar()
    const reloj = setInterval(() => cargar(true), 10000) // en vivo: se actualiza solo cada 10 s
    return () => clearInterval(reloj)
  }, [cargar])

  const { cargando, resumen, actividad, escaneos, error } = estado

  const encabezado = (
    <EncabezadoAdmin titulo="Dashboard" subtitulo="Resumen del motor IA, los eventos climáticos y la comunidad. Datos reales de la base de datos.">
      <span className="dash-vivo"><i />Se actualiza cada 10 s</span>
    </EncabezadoAdmin>
  )

  if (cargando) return <>{encabezado}<div className="ad-cargando"><span className="home-giro" aria-label="Cargando" /></div></>

  if (error) {
    return (
      <>
        {encabezado}
        <div className="home-error" role="alert">
          <span>{error}</span>
          <button onClick={() => cargar()}>Reintentar</button>
        </div>
      </>
    )
  }

  const tarjetas = [
    { icono: 'users', color: 'var(--abono)', titulo: 'Total usuarios', valor: formato(resumen.usuarios), nota: `${resumen.usuariosNuevosMes} nuevos este mes${resumen.crecimientoUsuarios == null ? '' : ` (${resumen.crecimientoUsuarios >= 0 ? '+' : ''}${resumen.crecimientoUsuarios}% vs. mes pasado)`}` },
    { icono: 'cam', color: 'var(--agua)', titulo: 'Escaneos IA hoy', valor: formato(resumen.escaneosHoy), nota: `Pico: ${resumen.picoPorMinuto} escaneos/min` },
    { icono: 'book', color: 'var(--primario)', titulo: 'Especies en catálogo', valor: formato(resumen.especies), nota: `${resumen.toxicas} tóxicas para mascotas` },
    { icono: 'alert', color: 'var(--evento)', titulo: 'Eventos activos', valor: formato(resumen.eventosActivos), nota: `${resumen.eventosTotales} ${resumen.eventosTotales === 1 ? 'evento' : 'eventos'} en total` },
  ]

  return (
    <>
      {encabezado}

      <section className="dash-kpis">
        {tarjetas.map((t, i) => (
          <article key={t.titulo} className="ad-card dash-kpi aparece" style={{ '--k': t.color, animationDelay: `${i * 0.06}s` }}>
            <div className="dash-ico"><IconoAdmin n={t.icono} /></div>
            <div>
              <span>{t.titulo}</span>
              <b>{t.valor}</b>
              <small>{t.nota}</small>
            </div>
          </article>
        ))}
      </section>

      <div className="dash-cols">
        <section className="ad-card ad-c aparece" style={{ animationDelay: '0.25s' }}>
          <h3>Usuarios nuevos y escaneos</h3>
          <p className="ad-sub">Últimos 14 días: registros nuevos y escaneos por día</p>
          <Grafico datos={actividad} />
        </section>

        <section className="ad-card ad-c aparece" style={{ animationDelay: '0.32s' }}>
          <h3>Últimos escaneos registrados</h3>
          <p className="ad-sub">Fotos subidas por los usuarios y procesadas por la IA</p>
          <TablaEscaneos escaneos={escaneos} />
        </section>
      </div>
    </>
  )
}
