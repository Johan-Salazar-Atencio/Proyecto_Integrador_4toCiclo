import { Link } from 'react-router-dom'
import { RUTAS } from '../../../../routes/rutas'
import { urlImagen } from '../../../../services/home'

// Iniciales para el avatar cuando no hay foto: "Johan Demo" -> "JD"
export const iniciales = (nombre) =>
  (nombre || '').trim().split(/\s+/).filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join('') || '?'

// Encabezado del inicio (propuesta E): avatar, saludo, rango y nivel, hojas y barra de XP hacia el siguiente nivel
export default function TarjetaUsuario({ usuario }) {
  const desde = usuario.experienceFromLevel || 0
  const tramo = Math.max(usuario.experienceToNextLevel - desde, 0)
  const porcentaje = tramo > 0 ? Math.min(100, Math.max(0, Math.round(((usuario.experience - desde) / tramo) * 100))) : 100
  const foto = urlImagen(usuario.profileImageUrl)

  return (
    <section className="usuario aparece">
      <div className="usuario-fila">
        <span className="usuario-avatar">
          {foto ? <img src={foto} alt="" onError={(e) => { e.currentTarget.style.display = 'none' }} /> : null}
          <b>{iniciales(usuario.name)}</b>
        </span>
        <div className="usuario-datos">
          <p className="usuario-saludo">¡Hola, {usuario.name}!</p>
          <span className="usuario-insignia">{usuario.badge || 'Jardinero'} · Nivel {usuario.level}</span>
        </div>
        <Link to={RUTAS.TIENDA} className="usuario-hojas" title="Ir a la tienda">🍃 {usuario.leaves}</Link>
      </div>
      <div className="barra" role="progressbar" aria-valuenow={porcentaje} aria-valuemin={0} aria-valuemax={100}>
        <div className="barra-relleno" style={{ width: `${porcentaje}%` }} />
      </div>
      <p className="usuario-xp">{usuario.experience} / {usuario.experienceToNextLevel} XP para el nivel {usuario.level + 1}</p>
    </section>
  )
}
