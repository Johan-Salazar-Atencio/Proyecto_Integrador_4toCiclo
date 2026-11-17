import { urlImagen } from '../services/home'
import { MOMENTOS } from '../services/tienda'
import './vistas-tema.css'

// Las tres escenas de un tema de fondo (dia, tarde y noche) en miniatura, con su etiqueta.
export default function VistasDelTema({ variantes = [] }) {
  if (variantes.length === 0) return null
  return (
    <div className="vistas-tema">
      {variantes.map((v) => (
        <figure key={v.momento}>
          <img src={urlImagen(v.vistaUrl)} alt={`Escena de ${MOMENTOS[v.momento]?.texto || v.momento}`} onError={(e) => { e.currentTarget.style.visibility = 'hidden' }} />
          <figcaption>{MOMENTOS[v.momento]?.icono} {MOMENTOS[v.momento]?.texto}</figcaption>
        </figure>
      ))}
    </div>
  )
}
