// Mision del dia (GET /misiones/diaria; no aparece si no hay misiones activas)
export default function MisionDiaria({ mision }) {
  const porcentaje = Math.round(Math.min(1, Math.max(0, mision.progress)) * 100)

  return (
    <section className="tarjeta mision aparece" style={{ animationDelay: '0.2s' }}>
      <div className="mision-cabecera">
        <h2>🎯 Misión del día</h2>
        {mision.isCompleted && <span className="mision-lista">¡Completada!</span>}
      </div>
      <p>{mision.title}</p>
      <div className="barra"><div className="barra-relleno" style={{ width: `${porcentaje}%` }} /></div>
      <div className="mision-pie">
        <small>{mision.currentCount} / {mision.targetCount}</small>
        {mision.reward && <small>Recompensa: {mision.reward}</small>}
      </div>
    </section>
  )
}
