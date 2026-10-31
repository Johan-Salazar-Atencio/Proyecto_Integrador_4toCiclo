import './boton.css'

export default function Boton({ children, cargando = false, disabled, variante = 'principal', ...resto }) {
  return (
    <button className={`boton boton-${variante}`} disabled={disabled || cargando} {...resto}>
      {cargando ? <span className="boton-giro" aria-label="Cargando" /> : children}
    </button>
  )
}
