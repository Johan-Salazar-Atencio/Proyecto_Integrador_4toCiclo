import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { RUTAS } from '../routes/rutas'
import { listarFondosPlanta, ponerFondoPlanta } from '../services/tienda'
import VistasDelTema from './VistasDelTema'
import './fondos-planta.css'

// Panel "Fondos de mi planta": el tema predeterminado y los que compro el usuario, cada uno con sus
// escenas de dia, tarde y noche. El que se elige se aplica solo a esta planta.
export default function FondosPlanta({ planta, onCerrar, onCambio }) {
  const [estado, setEstado] = useState({ cargando: true, temas: [], error: '' })
  const [ocupado, setOcupado] = useState(false)

  const cargar = useCallback(async () => {
    try {
      setEstado({ cargando: false, temas: await listarFondosPlanta(planta.id), error: '' })
    } catch (err) {
      setEstado({ cargando: false, temas: [], error: err.message || 'No se pudieron leer los fondos.' })
    }
  }, [planta.id])

  useEffect(() => { cargar() }, [cargar])

  const elegir = async (tema) => {
    if (ocupado || tema.enUso) return
    setOcupado(true)
    try {
      const temas = await ponerFondoPlanta(planta.id, tema.predeterminado ? null : tema.id)
      setEstado({ cargando: false, temas, error: '' })
      onCambio(tema)
    } catch (err) {
      setEstado((e) => ({ ...e, error: err.message || 'No se pudo cambiar el fondo.' }))
    } finally {
      setOcupado(false)
    }
  }

  return (
    <div className="fondos-planta-fondo" role="presentation" onClick={onCerrar}>
      <section className="fondos-planta tarjeta" role="dialog" aria-modal="true" aria-label={`Fondos de ${planta.name}`} onClick={(e) => e.stopPropagation()}>
        <header>
          <div>
            <h2>Fondos de {planta.name}</h2>
            <small>Se aplican solo a esta planta y cambian con la hora del día.</small>
          </div>
          <button className="fondos-planta-cerrar" onClick={onCerrar} aria-label="Cerrar">✕</button>
        </header>

        {estado.cargando && <div className="home-cargando"><span className="home-giro" aria-label="Cargando" /></div>}
        {estado.error && <p className="home-error" role="alert">{estado.error}</p>}

        <div className="fondos-planta-lista">
          {estado.temas.map((t) => (
            <article key={t.id} className={`fondos-planta-tema ${t.enUso ? 'en-uso' : ''}`}>
              <VistasDelTema variantes={t.variantes} />
              <div className="fondos-planta-datos">
                <b>{t.nombre}{t.predeterminado ? ' · el de siempre' : ''}</b>
                {t.descripcion && <small>{t.descripcion}</small>}
              </div>
              {t.enUso ? (
                <span className="fondos-planta-uso">✓ En uso</span>
              ) : (
                <button className="boton boton-principal" disabled={ocupado} onClick={() => elegir(t)}>
                  {t.predeterminado ? 'Volver a este' : 'Usar en esta planta'}
                </button>
              )}
            </article>
          ))}
        </div>

        <Link className="fondos-planta-tienda" to={RUTAS.TIENDA}>🛒 Conseguir más fondos en la tienda</Link>
      </section>
    </div>
  )
}
