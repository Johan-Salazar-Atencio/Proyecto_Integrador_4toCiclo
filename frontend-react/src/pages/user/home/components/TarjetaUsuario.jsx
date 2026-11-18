import { urlImagen } from '../../../../services/home'
import { iniciales, porcentajeXp } from '../xp'

export { iniciales }

// Tarjeta de usuario: avatar con anillo de XP, saludo, insignia con nivel y barra de XP hacia el siguiente nivel
export default function TarjetaUsuario({ usuario }) {
  const porcentaje = porcentajeXp(usuario)
  const foto = urlImagen(usuario.profileImageUrl)

  return (
    <section className="tarjeta usr aparece">
      <span className="usr-anillo" style={{ '--p': `${porcentaje}%` }}>
        <b>
          {foto ? <img src={foto} alt="" onError={(e) => { e.currentTarget.style.display = 'none' }} /> : null}
          <span>{iniciales(usuario.name)}</span>
        </b>
      </span>
      <div className="usr-datos">
        <h2>¡Hola, {usuario.name}!</h2>
        <span className="etiqueta">{usuario.badge || 'Jardinero'} · Nivel {usuario.level}</span>
        <div className="barra" role="progressbar" aria-valuenow={porcentaje} aria-valuemin={0} aria-valuemax={100}>
          <div className="barra-relleno" style={{ width: `${porcentaje}%` }} />
        </div>
        <small>{usuario.experience} / {usuario.experienceToNextLevel} XP para el nivel {usuario.level + 1}</small>
      </div>
    </section>
  )
}
