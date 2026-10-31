import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { RUTAS } from '../../../routes/rutas'
import { listarCatalogo } from '../../../services/catalogo'
import './catalogo.css'

const MS_ESPERA_BUSQUEDA = 350

// Enciclopedia: catalogo de especies con busqueda y filtro de toxicidad (GET /catalogo)
export default function Catalogo() {
  const [texto, setTexto] = useState('')
  const [soloToxicas, setSoloToxicas] = useState(false)
  const [estado, setEstado] = useState({ cargando: true, items: [], error: '' })

  const cargar = useCallback(async () => {
    setEstado((e) => ({ ...e, cargando: true, error: '' }))
    try {
      setEstado({ cargando: false, items: await listarCatalogo({ texto, soloToxicas }), error: '' })
    } catch (err) {
      setEstado({ cargando: false, items: [], error: err instanceof TypeError ? 'No se pudo conectar con el servidor.' : err.message })
    }
  }, [texto, soloToxicas])

  // Espera a que dejes de escribir antes de consultar
  useEffect(() => {
    const id = setTimeout(cargar, MS_ESPERA_BUSQUEDA)
    return () => clearTimeout(id)
  }, [cargar])

  const { cargando, items, error } = estado

  return (
    <div className="catalogo">
      <div className="catalogo-filtros aparece">
        <input type="search" placeholder="🔍 Buscar planta..." value={texto} onChange={(e) => setTexto(e.target.value)} />
        <button className={`catalogo-toxicas ${soloToxicas ? 'activa' : ''}`} aria-pressed={soloToxicas} onClick={() => setSoloToxicas(!soloToxicas)}>
          ⚠️ Solo tóxicas
        </button>
      </div>

      {error ? (
        <div className="home-error" role="alert">
          <span>{error}</span>
          <button onClick={cargar}>Reintentar</button>
        </div>
      ) : (
        <>
          <p className="catalogo-cuenta">{cargando ? 'Buscando…' : `${items.length} especies`}</p>

          {!cargando && items.length === 0 ? (
            <div className="coleccion-vacio aparece">
              <span>🔎</span>
              <h2>No se encontraron plantas</h2>
            </div>
          ) : (
            <div className="catalogo-lista">
              {items.map((p, i) => (
                <Link key={p.id} to={`${RUTAS.CATALOGO}/${p.id}`} state={{ planta: p }} className="tarjeta catalogo-item aparece" style={{ animationDelay: `${Math.min(i, 10) * 0.03}s` }}>
                  <span className="catalogo-icono">🌿</span>
                  <div>
                    <b>{p.nombreComun}</b>
                    <small>{p.nombreCientifico}</small>
                    <div className="catalogo-datos">
                      <span>💧 Cada {p.frecuenciaRiegoDias} d</span>
                      <span>☀️ {p.frecuenciaSolHoras} h</span>
                      {p.temperaturaMinC != null && p.temperaturaMaxC != null && <span>🌡️ {p.temperaturaMinC}–{p.temperaturaMaxC} °C</span>}
                      {p.esToxicaMascotas && <span className="chip-alerta">⚠️ Tóxica</span>}
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  )
}
