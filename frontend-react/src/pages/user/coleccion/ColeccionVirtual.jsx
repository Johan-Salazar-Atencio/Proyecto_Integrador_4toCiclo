import { useCallback, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { RUTAS } from '../../../routes/rutas'
import Invernadero, { ALTO, ANCHO, centroColumna, repisa } from '../../../components/Invernadero'
import MacetaPixel from '../../../components/MacetaPixel'
import PlantaPixel from '../../../components/PlantaPixel'
import { momentoDelDia } from '../../../services/arte'
import { guardarEnBaul, renombrarPlanta, sacarDelBaul } from '../../../services/jardin'
import { obtenerTienda } from '../../../services/tienda'
import { elegirInvernadero } from '../../../services/usuario'
import { obtenerUsuario } from '../../../services/home'
import { etapaDe } from '../jardin/etapas'
import Icono from './Iconos'

export const PLANTAS_POR_PAGINA = 6
const MS_PULSACION = 500
const NOMBRES = { SELVA: 'Selva tropical', VICTORIANO: 'Cristal victoriano', LUCES: 'Taller de luces' }

// Busca por apodo o por especie, sin importar mayusculas; una consulta vacia deja todo
export function filtrarPlantas(plantas, consulta) {
  const buscada = consulta.trim().toLowerCase()
  if (!buscada) return plantas
  return plantas.filter((p) => p.name.toLowerCase().includes(buscada) || p.species.toLowerCase().includes(buscada))
}

// Las plantas vivas repartidas de a 6 por pagina; siempre hay al menos una
export function paginasDelInvernadero(activas) {
  const paginas = []
  for (let i = 0; i < activas.length; i += PLANTAS_POR_PAGINA) paginas.push(activas.slice(i, i + PLANTAS_POR_PAGINA))
  return paginas.length ? paginas : [[]]
}

// Pantalla ancha (>= 1000px): el invernadero ocupa la columna grande y los temas y el baul van al lado
function useAncha() {
  const consulta = '(min-width: 1000px)'
  const [ancha, setAncha] = useState(() => typeof window !== 'undefined' && window.matchMedia(consulta).matches)
  useEffect(() => {
    const mq = window.matchMedia(consulta)
    const alCambiar = () => setAncha(mq.matches)
    mq.addEventListener('change', alCambiar)
    return () => mq.removeEventListener('change', alCambiar)
  }, [])
  return ancha
}

// Pulsacion larga: abre las acciones de la planta; un toque corto la abre en el jardin
function useLargaPulsacion(alMantener) {
  const temporizador = useRef(null)
  const larga = useRef(false)
  const parar = () => clearTimeout(temporizador.current)
  return {
    onPointerDown: () => {
      larga.current = false
      temporizador.current = setTimeout(() => { larga.current = true; alMantener() }, MS_PULSACION)
    },
    onPointerUp: parar,
    onPointerLeave: parar,
    onPointerCancel: parar,
    onContextMenu: (e) => { e.preventDefault(); parar(); larga.current = true; alMantener() },
    fueLarga: () => larga.current,
  }
}

function PlantaEnEstante({ planta, onAbrir, onMenu }) {
  const etapa = etapaDe(planta)
  const pulsacion = useLargaPulsacion(onMenu)
  const marca = planta.fase === 'MUERTA' ? '💀' : planta.enRecuperacion ? '⚠️' : planta.refugiada ? '🛡️' : null
  const { fueLarga, ...eventos } = pulsacion
  return (
    <div className="estante-planta">
      <button
        className="estante-boton"
        {...eventos}
        onClick={() => { if (!fueLarga()) onAbrir(planta) }}
        aria-label={`Abrir ${planta.name} en el jardín`}
        title={`${planta.name}: ${etapa.nombre}. Mantén pulsada para ver sus acciones.`}
      >
        <PlantaPixel etapa={etapa.sprite} estado={etapa.estadoDibujo} apariencia={planta.apariencia} titulo={planta.name} animar={false} sinSuelo className="estante-dibujo" />
        <MacetaPixel codigo={planta.macetaCodigo} className="estante-maceta" />
      </button>
      {marca && <span className="estante-marca">{marca}</span>}
      <button className="estante-menu" onClick={onMenu} aria-label={`Acciones de ${planta.name}`}>⋮</button>
    </div>
  )
}

function PaginaInvernadero({ tema, momento, plantas, vacioPorBusqueda, onAbrir, onMenu }) {
  const libre = plantas.length < PLANTAS_POR_PAGINA ? plantas.length : null
  return (
    <div className="virtual-pagina">
      <div className="virtual-escena" style={{ aspectRatio: `${ANCHO} / ${ALTO}` }}>
        <Invernadero tema={tema} momento={momento} huecoLibre={libre} />
        {plantas.map((planta, i) => {
          const fila = Math.floor(i / 3)
          const columna = i % 3
          return (
            <div
              key={planta.id}
              className="virtual-hueco"
              style={{
                left: `${((centroColumna(columna) - 11) / ANCHO) * 100}%`,
                top: `${((repisa(fila) - 22) / ALTO) * 100}%`,
                width: `${(22 / ANCHO) * 100}%`,
                height: `${(22 / ALTO) * 100}%`,
              }}
            >
              <PlantaEnEstante planta={planta} onAbrir={onAbrir} onMenu={() => onMenu(planta)} />
            </div>
          )
        })}
        {libre != null && (
          <Link
            to={RUTAS.ESCANEAR}
            className="virtual-mas"
            aria-label="Escanear una planta"
            style={{
              left: `${((centroColumna(libre % 3) - 11) / ANCHO) * 100}%`,
              top: `${((repisa(Math.floor(libre / 3)) - 22) / ALTO) * 100}%`,
              width: `${(22 / ANCHO) * 100}%`,
              height: `${(22 / ALTO) * 100}%`,
            }}
          />
        )}
        {vacioPorBusqueda && <p className="virtual-sin-resultados">Ninguna planta coincide con tu búsqueda</p>}
      </div>
    </div>
  )
}

function ListaBaul({ plantas, onSacar, ocupado }) {
  return (
    <>
      {plantas.length === 0 && <p className="virtual-baul-vacio">El baúl está vacío</p>}
      {plantas.map((p) => (
        <div key={p.id} className="virtual-baul-fila">
          <span className="virtual-baul-icono"><Icono nombre="chest" tam={20} /></span>
          <div>
            <b>{p.name}</b>
            <small>{p.species}</small>
          </div>
          <button className="virtual-sacar" onClick={() => onSacar(p)} disabled={ocupado}>Sacar</button>
        </div>
      ))}
    </>
  )
}

function PaginaBaul({ plantas, onSacar, ocupado }) {
  return (
    <div className="virtual-pagina">
      <div className="virtual-baul">
        <h2>Baúl · {plantas.length} guardada{plantas.length === 1 ? '' : 's'}</h2>
        <p>Las plantas guardadas aquí quedan congeladas: no crecen, no se desgastan y no les pasa nada. Solo liberan espacio.</p>
        <ListaBaul plantas={plantas} onSacar={onSacar} ocupado={ocupado} />
      </div>
    </div>
  )
}

/**
 * Coleccion virtual (propuesta E): un invernadero en pixel art con las plantas en macetas, de a 6 por pagina (desliza o
 * usa las flechas) y, al final, el baul. Tocar una planta la abre en el jardin; mantenerla pulsada (o el "⋮") abre
 * sus acciones: apodo y baul.
 */
export default function ColeccionVirtual({ digitales, consulta = '', onCambio, onAbrir, onAviso }) {
  const ancha = useAncha()
  const [tema, setTema] = useState('SELVA')
  const [propios, setPropios] = useState([])
  const [menuTema, setMenuTema] = useState(false)
  const [conAcciones, setConAcciones] = useState(null)
  const [renombrando, setRenombrando] = useState(null)
  const [apodo, setApodo] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const [pagina, setPagina] = useState(0)
  const pager = useRef(null)
  const momento = momentoDelDia(new Date().getHours())

  // El invernadero puesto (del perfil) y los que el usuario tiene (el predeterminado y los comprados)
  useEffect(() => {
    obtenerUsuario().then((u) => { if (u?.greenhouseCode) setTema(u.greenhouseCode) }).catch(() => {})
    obtenerTienda().then((t) => setPropios((t?.items || []).filter((i) => i.tipo === 'INVERNADERO' && i.comprado))).catch(() => {})
  }, [])

  const vistas = filtrarPlantas(digitales, consulta)
  const activas = vistas.filter((p) => !p.enBaul)
  const enBaul = vistas.filter((p) => p.enBaul)
  const paginas = paginasDelInvernadero(activas)
  const total = ancha ? paginas.length : paginas.length + 1

  const alDesplazar = useCallback(() => {
    const el = pager.current
    if (el && el.clientWidth) setPagina(Math.round(el.scrollLeft / el.clientWidth))
  }, [])
  const irA = (n) => {
    const el = pager.current
    if (el) el.scrollTo({ left: ((n + total) % total) * el.clientWidth, behavior: 'smooth' })
  }

  const elegirTema = async (item) => {
    setMenuTema(false)
    if (item.codigo === tema) return
    try {
      const u = await elegirInvernadero(item.id)
      setTema(u?.greenhouseCode || item.codigo)
    } catch (err) {
      onAviso(err.message || 'No se pudo cambiar el invernadero.')
    }
  }

  const cambiarPlanta = async (accion, planta, mensaje) => {
    if (ocupado) return
    setOcupado(true)
    try {
      const nueva = await accion()
      onCambio(nueva)
      if (mensaje) onAviso(mensaje(nueva))
    } catch (err) {
      onAviso(err.estado === 409 ? err.message : err.message || 'No se pudo hacer el cambio.')
    } finally {
      setOcupado(false)
    }
  }

  const sacar = (p) => cambiarPlanta(() => sacarDelBaul(p.id), p, (n) => `${n.name} salió del baúl`)
  const temas = (
    <>
      <small className="virtual-sub">Tus temas de invernadero</small>
      {propios.map((i) => (
        <button key={i.id} role="menuitem" className={`virtual-tema-fila ${i.codigo === tema ? 'activo' : ''}`} onClick={() => elegirTema(i)}>
          <span className="virtual-tema-marca">{i.codigo === tema && <Icono nombre="check" tam={16} />}</span>
          <span className="virtual-tema-nombre">{i.nombre}<small>{i.codigo === tema ? 'Tema actual' : 'Disponible'}</small></span>
        </button>
      ))}
      <Link to={RUTAS.TIENDA} className="virtual-menu-mas">Conseguir más temas en la tienda</Link>
    </>
  )

  const nombreDelTema = NOMBRES[tema] || 'Invernadero'
  const enPaginas = pagina < paginas.length
  const marco = (
    <div className="virtual-marco">
      <div className="virtual-pager" ref={pager} onScroll={alDesplazar}>
        {paginas.map((plantas, i) => (
          <PaginaInvernadero
            key={i}
            tema={tema}
            momento={momento}
            plantas={plantas}
            vacioPorBusqueda={consulta.trim() !== '' && activas.length === 0}
            onAbrir={onAbrir}
            onMenu={setConAcciones}
          />
        ))}
        {!ancha && <PaginaBaul plantas={enBaul} ocupado={ocupado} onSacar={sacar} />}
      </div>
      {total > 1 && (
        <>
          <button className="virtual-flecha virtual-flecha-izq" onClick={() => irA(pagina - 1)} aria-label="Página anterior">‹</button>
          <button className="virtual-flecha virtual-flecha-der" onClick={() => irA(pagina + 1)} aria-label="Página siguiente">›</button>
          <div className="virtual-puntos">
            {Array.from({ length: total }, (_, i) => (
              <button key={i} className={i === pagina ? 'activo' : ''} onClick={() => irA(i)} aria-label={`Ir a la página ${i + 1}`} />
            ))}
          </div>
        </>
      )}
    </div>
  )

  return (
    <div className={`virtual ${ancha ? 'virtual-ancho' : ''}`}>
      {!ancha && (
        <div className="virtual-barra">
          <div className="virtual-tema">
            <button className="virtual-chip" onClick={() => setMenuTema((v) => !v)} aria-expanded={menuTema}>
              <Icono nombre="leaf" tam={16} /> {nombreDelTema} ▾
            </button>
            {menuTema && <div className="virtual-menu" role="menu">{temas}</div>}
          </div>
        </div>
      )}

      {marco}
      {!ancha && (
        <p className="virtual-pie">{enPaginas ? `Invernadero ${pagina + 1} de ${paginas.length}` : `Baúl · ${enBaul.length} guardada${enBaul.length === 1 ? '' : 's'}`}</p>
      )}

      {ancha && (
        <aside className="virtual-lateral">
          <section className="virtual-tarjeta">
            <div className="virtual-tarjeta-cab">
              <h3>Invernadero {Math.min(pagina, paginas.length - 1) + 1} de {paginas.length}</h3>
              <div className="virtual-dots">
                {paginas.map((_, i) => (
                  <button key={i} className={i === pagina ? 'on' : ''} onClick={() => irA(i)} aria-label={`Ir al invernadero ${i + 1}`} />
                ))}
              </div>
            </div>
            {temas}
          </section>
          <section className="virtual-tarjeta">
            <h3>Baúl · {enBaul.length} guardada{enBaul.length === 1 ? '' : 's'}</h3>
            <small className="virtual-sub">Las plantas guardadas quedan congeladas: no crecen, no se desgastan y no les pasa nada.</small>
            <ListaBaul plantas={enBaul} ocupado={ocupado} onSacar={sacar} />
          </section>
        </aside>
      )}

      {conAcciones && (
        <div className="layout-fondo" onClick={() => setConAcciones(null)}>
          <div className="layout-dialogo aparece virtual-acciones" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
            <h2>{conAcciones.name}</h2>
            <p>{conAcciones.species}</p>
            <button onClick={() => { const p = conAcciones; setConAcciones(null); onAbrir(p) }}>Abrir en el jardín</button>
            <button onClick={() => { setApodo(conAcciones.name); setRenombrando(conAcciones); setConAcciones(null) }}>Cambiar apodo</button>
            {conAcciones.enRecuperacion ? (
              <small>Está en recuperación: abónala antes de guardarla en el baúl</small>
            ) : (
              <button onClick={() => { const p = conAcciones; setConAcciones(null); cambiarPlanta(() => guardarEnBaul(p.id), p, (n) => `${n.name} está en el baúl`) }}>Guardar en el baúl</button>
            )}
          </div>
        </div>
      )}

      {renombrando && (
        <div className="layout-fondo" onClick={() => setRenombrando(null)}>
          <form
            className="layout-dialogo aparece"
            role="dialog"
            aria-modal="true"
            onClick={(e) => e.stopPropagation()}
            onSubmit={(e) => {
              e.preventDefault()
              const p = renombrando
              setRenombrando(null)
              cambiarPlanta(() => renombrarPlanta(p.id, apodo.trim()), p, (n) => `Ahora se llama ${n.name}`)
            }}
          >
            <h2>Apodo de tu planta</h2>
            <input className="virtual-apodo" value={apodo} maxLength={50} onChange={(e) => setApodo(e.target.value)} aria-label="Apodo" autoFocus />
            <small>Déjalo vacío para volver al nombre de la especie · {apodo.length}/50</small>
            <div className="layout-dialogo-botones">
              <button type="button" className="boton boton-suave" onClick={() => setRenombrando(null)}>Cancelar</button>
              <button type="submit" className="boton boton-principal">Guardar</button>
            </div>
          </form>
        </div>
      )}
    </div>
  )
}
