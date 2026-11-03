import { Link } from 'react-router-dom'
import { RUTAS } from '../../../../routes/rutas'
import { urlImagen } from '../../../../services/home'

const ESTADISTICAS = [
  { clave: 'healthPercent', texto: '❤️ Salud' },
  { clave: 'humidityPercent', texto: '💧 Agua' },
  { clave: 'lightPercent', texto: '☀️ Luz' },
]

// Planta activa del jardin (GET /plantas/activa; no aparece si el usuario no tiene)
export default function TarjetaPlanta({ planta }) {
  const foto = urlImagen(planta.imageUrl)

  return (
    <section className="tarjeta planta aparece" style={{ animationDelay: '0.1s' }}>
      <div className="planta-foto">
        {foto ? <img src={foto} alt={planta.name} onError={(e) => { e.currentTarget.style.display = 'none' }} /> : null}
        <span className="planta-respaldo">🪴</span>
      </div>
      <div className="planta-datos">
        <h2>{planta.name}</h2>
        <p className="planta-especie">{planta.species}</p>
        {ESTADISTICAS.map(({ clave, texto }) => (
          <div key={clave} className="planta-estadistica">
            <span>{texto}</span>
            <div className="barra"><div className={`barra-relleno ${nivel(planta[clave])}`} style={{ width: `${planta[clave]}%` }} /></div>
            <b>{planta[clave]}%</b>
          </div>
        ))}
        {planta.lastWatered && <small>Último riego: {planta.lastWatered}</small>}
        <Link to={RUTAS.JARDIN} className="planta-boton">Entrar al Jardín Virtual →</Link>
      </div>
    </section>
  )
}

const nivel = (valor) => (valor >= 60 ? '' : valor >= 30 ? 'barra-ambar' : 'barra-roja')
