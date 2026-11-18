import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { RUTAS } from '../../../routes/rutas'
import { listarCatalogo } from '../../../services/catalogo'
import Icono from '../coleccion/Iconos'
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
        <label className="catalogo-buscar">
          <Icono nombre="search" tam={18} />
          <input type="search" placeholder="Buscar planta..." aria-label="Buscar planta" value={texto} onChange={(e) => setTexto(e.target.value)} />
        </label>
        <button className={`catalogo-toxicas ${soloToxicas ? 'activa' : ''}`} aria-pressed={soloToxicas} onClick={() => setSoloToxicas(!soloToxicas)}>
          <Icono nombre="warn" tam={16} /> Solo tóxicas
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
              <span className="coleccion-vacio-icono"><Icono nombre="search" tam={34} /></span>
              <h2>No se encontraron plantas</h2>
              <p>Prueba con otro nombre o quita el filtro.</p>
            </div>
          ) : (
            <div className="catalogo-lista">
              {items.map((p, i) => (
                <Link key={p.id} to={`${RUTAS.CATALOGO}/${p.id}`} state={{ planta: p }} className="catalogo-item aparece" style={{ animationDelay: `${Math.min(i, 10) * 0.03}s` }}>
                  <span className="catalogo-icono"><Icono nombre="leaf" tam={24} /></span>
                  <div>
                    <b>{p.nombreComun}</b>
                    <small>{p.nombreCientifico}</small>
                    <div className="catalogo-datos">
                      <span><Icono nombre="drop" tam={13} /> Cada {p.frecuenciaRiegoDias} d</span>
                      <span><Icono nombre="sun" tam={13} /> {p.frecuenciaSolHoras} h</span>
                      {p.temperaturaMinC != null && p.temperaturaMaxC != null && <span>{p.temperaturaMinC}–{p.temperaturaMaxC} °C</span>}
                      {p.esToxicaMascotas && <span className="chip-alerta"><Icono nombre="warn" tam={13} /> Tóxica</span>}
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
