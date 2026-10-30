import { Link } from 'react-router-dom'
import { RUTAS } from '../../../../routes/rutas'

// TODO(backend): hoy la lista es fija; el movil la trae de GET /accesos
const ACCESOS = [
  { ruta: RUTAS.ESCANEAR, icono: '📷', titulo: 'Escanear', detalle: 'Identifica una planta', fondo: 'var(--verde-claro)' },
  { ruta: RUTAS.COLECCION, icono: '📚', titulo: 'Mi Colección', detalle: 'Tus plantas descubiertas', fondo: 'var(--azul-claro)' },
  { ruta: RUTAS.CATALOGO, icono: '📖', titulo: 'Enciclopedia', detalle: 'Guías botánicas', fondo: 'var(--ambar-claro)' },
]

export default function Accesos() {
  return (
    <section className="aparece" style={{ animationDelay: '0.15s' }}>
      <h2 className="seccion-titulo">Acceso rápido</h2>
      <div className="accesos">
        {ACCESOS.map((a) => (
          <Link key={a.titulo} to={a.ruta} className="acceso" style={{ background: a.fondo }}>
            <span className="acceso-icono">{a.icono}</span>
            <b>{a.titulo}</b>
            <small>{a.detalle}</small>
          </Link>
        ))}
      </div>
    </section>
  )
}
