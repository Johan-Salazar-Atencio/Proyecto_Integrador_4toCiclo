import { Link } from 'react-router-dom'
import { RUTAS } from '../../../../routes/rutas'

// Mision del dia (GET /misiones/diaria; no aparece si no hay misiones activas). Tocarla abre todas las misiones.
export default function MisionDiaria({ mision, hayEvento = false }) {
  const porcentaje = Math.round(Math.min(1, Math.max(0, mision.progress)) * 100)

  return (
    <Link to={RUTAS.MISIONES} className="tarjeta mision mision-enlace aparece" style={{ animationDelay: '0.2s' }}>
      <div className="mision-cabecera">
        <h2>🎯 Misión del día</h2>
        {hayEvento && <span className="mision-evento-etiqueta">Evento</span>}
        {mision.isCompleted && <span className="mision-lista">¡Completada!</span>}
      </div>
      <p>{mision.title}</p>
      <div className="barra"><div className="barra-relleno" style={{ width: `${porcentaje}%` }} /></div>
      <div className="mision-pie">
        <small>{mision.currentCount} / {mision.targetCount}</small>
        {mision.reward && <small>Recompensa: {mision.reward}</small>}
      </div>
    </Link>
  )
}
