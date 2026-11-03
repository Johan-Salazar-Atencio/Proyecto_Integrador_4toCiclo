// Nivel, hojas y experiencia del usuario (GET /usuarios/me)
export default function TarjetaUsuario({ usuario }) {
  const falta = Math.max(usuario.experienceToNextLevel, 1)
  const porcentaje = Math.min(100, Math.round((usuario.experience / falta) * 100))

  return (
    <section className="tarjeta usuario aparece">
      <div className="usuario-fila">
        <div>
          <p className="usuario-saludo">¡Hola, {usuario.name}!</p>
          <span className="usuario-insignia">{usuario.badge || 'Jardinero'} · Nivel {usuario.level}</span>
        </div>
        <span className="usuario-hojas">🍃 {usuario.leaves}</span>
      </div>
      <div className="barra" role="progressbar" aria-valuenow={porcentaje} aria-valuemin={0} aria-valuemax={100}>
        <div className="barra-relleno" style={{ width: `${porcentaje}%` }} />
      </div>
      <p className="usuario-xp">{usuario.experience} / {usuario.experienceToNextLevel} XP para el siguiente nivel</p>
    </section>
  )
}
