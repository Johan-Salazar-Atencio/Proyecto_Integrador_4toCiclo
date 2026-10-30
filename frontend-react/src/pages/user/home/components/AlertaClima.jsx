// Evento climatico activo (GET /eventos/activo; no aparece si no hay evento)
export default function AlertaClima({ alerta }) {
  const grave = alerta.severity === 'HIGH' || alerta.severity === 'CRITICAL'

  return (
    <section className={`tarjeta alerta aparece ${grave ? 'alerta-grave' : ''}`} style={{ animationDelay: '0.05s' }}>
      <span className="alerta-icono">⚠️</span>
      <div className="alerta-texto">
        <h2>{alerta.title}</h2>
        <p>{alerta.description}</p>
        {alerta.timeRemaining && <small>Tiempo restante: {alerta.timeRemaining}</small>}
      </div>
    </section>
  )
}
