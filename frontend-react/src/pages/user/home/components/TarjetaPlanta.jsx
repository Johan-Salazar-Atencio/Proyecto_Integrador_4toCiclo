import { useNavigate } from 'react-router-dom'
import { RUTAS } from '../../../../routes/rutas'
import EscenaPixel from '../../../../components/EscenaPixel'
import PlantaPixel from '../../../../components/PlantaPixel'
import Icono from '../../../../components/Icono'
import { ACCIONES } from '../../jardin/acciones'
import { estadoAnimo } from '../../jardin/animo'
import { etapaDe } from '../../jardin/etapas'

const BOTONES = [
  { accion: 'REGAR', texto: 'Regar', icono: 'drop', color: 'var(--agua)' },
  { accion: 'SOL', texto: 'Luz', icono: 'sun', color: 'var(--luz)' },
  { accion: 'ABONAR', texto: 'Abono', icono: 'leaf', color: 'var(--abono)' },
]

// Planta del jardin: su escena en pixel art con la planta en su maceta, flechas para pasar a otra, estado y dia,
// nombre con su especie, los tres cuidados y la fila de tus plantas. Tocar la escena o el nombre entra al jardin.
export default function TarjetaPlanta({ planta, plantas, ocupado, onMover, onElegir, onCuidar }) {
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
          <PlantaPixel etapa={etapa.sprite} estado={etapa.estadoDibujo} apariencia={planta.apariencia} maceta={planta.macetaCodigo} titulo={`${planta.name}: ${etapa.nombre}`} />
        </div>
        <div className={`jardin-chip ${animo.clase}`}>
          <span className="jardin-chip-cara">{animo.cara}</span>
          {etapa.total > 0 && (
            <b>{etapa.muerta ? 'Muerta' : etapa.marchita ? 'Marchita' : etapa.dias >= etapa.total ? 'Madura' : `Día ${etapa.dias}/${etapa.total}`}</b>
          )}
          {etapa.total > 0 && !etapa.marchita && (
            <span className="jardin-puntos" aria-hidden="true">
              {[1, 2, 3].map((n) => <i key={n} className={n <= etapa.fase ? 'punto-hecho' : ''} />)}
            </span>
          )}
        </div>
        {plantas.length > 1 && (
          <>
            <button className="inicio-flecha inicio-flecha-izq" onClick={() => onMover(-1)} disabled={ocupado} aria-label="Planta anterior"><Icono nombre="l" /></button>
            <button className="inicio-flecha inicio-flecha-der" onClick={() => onMover(1)} disabled={ocupado} aria-label="Planta siguiente"><Icono nombre="r" /></button>
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
          {BOTONES.map(({ accion, texto, icono, color }) => (
            <button key={accion} className="inicio-cuidado" style={{ '--c': color }} onClick={() => onCuidar(accion)} disabled={ocupado} title={ACCIONES[accion].pista}>
              <span><Icono nombre={icono} /></span>
              <b>{texto}</b>
            </button>
          ))}
        </div>
      )}

      {plantas.length > 1 && (
        <div className="inicio-mias" role="group" aria-label="Tus plantas">
          {plantas.slice(0, 4).map((p) => {
            const e = etapaDe(p)
            return (
              <button key={p.id} className={`inicio-mini ${p.id === planta.id ? 'activa' : ''}`} onClick={() => onElegir(p)} disabled={ocupado} aria-pressed={p.id === planta.id} title={p.name}>
                <span className="inicio-mini-vista">
                  <PlantaPixel etapa={e.sprite} estado={e.estadoDibujo} apariencia={p.apariencia} maceta={p.macetaCodigo} animar={false} />
                </span>
                <b>{p.name}</b>
              </button>
            )
          })}
        </div>
      )}
    </section>
  )
}
