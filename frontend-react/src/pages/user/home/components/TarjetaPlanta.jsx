import { useNavigate } from 'react-router-dom'
import { RUTAS } from '../../../../routes/rutas'
import EscenaPixel from '../../../../components/EscenaPixel'
import PlantaPixel from '../../../../components/PlantaPixel'
import { ACCIONES } from '../../jardin/acciones'
import { estadoAnimo } from '../../jardin/animo'
import { etapaDe } from '../../jardin/etapas'

const BOTONES = [
  { accion: 'REGAR', texto: 'Regar', icono: '💧', clase: 'cuidado-agua' },
  { accion: 'SOL', texto: 'Luz', icono: '☀️', clase: 'cuidado-luz' },
  { accion: 'ABONAR', texto: 'Abono', icono: '🌿', clase: 'cuidado-abono' },
]

// Planta del jardin (propuesta E): su escena en pixel art con la planta en su maceta, flechas para pasar a otra,
// contador, nombre con su especie y los tres cuidados. Tocar la escena o el nombre entra al jardin.
export default function TarjetaPlanta({ planta, plantas, ocupado, onMover, onCuidar }) {
  const irA = useNavigate()
  const etapa = etapaDe(planta)
  const animo = estadoAnimo(planta)
  const entrar = () => irA(RUTAS.JARDIN)

  return (
    <section className="tarjeta planta-inicio aparece" style={{ animationDelay: '0.1s' }}>
      <div key={planta.id} className={`jardin-marco inicio-marco ${etapa.marchita ? 'jardin-marco-marchita' : ''}`}>
        <EscenaPixel />
        <button className="inicio-toque" onClick={entrar} aria-label={`Abrir ${planta.name} en el jardín`} />
        <div className="jardin-planta">
          <PlantaPixel etapa={etapa.indice} apariencia={planta.apariencia} maceta={planta.macetaCodigo} titulo={`${planta.name}: ${etapa.nombre}`} />
        </div>
        <div className={`jardin-chip ${animo.clase}`}>
          <span className="jardin-chip-cara">{animo.cara}</span>
          {etapa.total > 0 && (
            <b>{etapa.marchita ? 'Marchita' : etapa.dias >= etapa.total ? 'Madura' : `Día ${etapa.dias}/${etapa.total}`}</b>
          )}
          {etapa.total > 0 && !etapa.marchita && (
            <span className="jardin-puntos" aria-hidden="true">
              {[1, 2, 3].map((n) => <i key={n} className={n <= etapa.fase ? 'punto-hecho' : ''} />)}
            </span>
          )}
        </div>
        {plantas.length > 1 && (
          <>
            <button className="inicio-flecha inicio-flecha-izq" onClick={() => onMover(-1)} disabled={ocupado} aria-label="Planta anterior">‹</button>
            <button className="inicio-flecha inicio-flecha-der" onClick={() => onMover(1)} disabled={ocupado} aria-label="Planta siguiente">›</button>
          </>
        )}
      </div>

      <div className="inicio-nombre">
        <button className="inicio-nombre-boton" onClick={entrar}>{planta.name}</button>
        <span className="planta-especie">{planta.species}</span>
      </div>
      {planta.fase === 'MUERTA' ? (
        // Murio por no abonarla a tiempo: no se cuida, se revive (en el jardin, a cambio de hojas)
        <button className="inicio-murio" onClick={entrar}>
          💀 Murió · Revivir en el jardín{planta.costoRevivir != null ? ` (${planta.costoRevivir} 🍃)` : ''}
        </button>
      ) : (
      <div className="inicio-cuidados">
        {BOTONES.map(({ accion, texto, icono, clase }) => (
          <button key={accion} className={`inicio-cuidado ${clase}`} onClick={() => onCuidar(accion)} disabled={ocupado} title={ACCIONES[accion].pista}>
            <span>{icono}</span>
            <b>{texto}</b>
          </button>
        ))}
      </div>
      )}
    </section>
  )
}
