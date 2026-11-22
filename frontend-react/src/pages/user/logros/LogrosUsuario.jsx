import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import Icono from '../../../components/Icono'
import IconoLogro from '../../../components/IconoLogro'
import { marcarVistos, obtenerLogros, obtenerPendientes, RAREZAS, textoRareza } from '../../../services/logros'
import CelebracionLogro from './CelebracionLogro'
import '../home/home.css'
import '../coleccion/coleccion.css'
import './logros.css'

const FILTROS_ESTADO = [
  { valor: 'TODOS', texto: 'Todos' },
  { valor: 'DESBLOQUEADOS', texto: 'Desbloqueados' },
  { valor: 'BLOQUEADOS', texto: 'Bloqueados' },
]

const fecha = (iso) => {
  const d = iso ? new Date(iso) : null
  return d && !Number.isNaN(d.getTime()) ? d.toLocaleDateString('es-PE', { day: 'numeric', month: 'short', year: 'numeric' }) : ''
}

function TarjetaLogro({ logro }) {
  const { desbloqueado, secreto, rareza } = logro
  // Un secreto bloqueado no revela nada: ni titulo, ni descripcion, ni progreso, ni icono real
  const oculto = secreto && !desbloqueado
  const porcentaje = logro.meta > 0 ? Math.min(100, Math.round((logro.progreso / logro.meta) * 100)) : 0
  const clase = `logro logro-${rareza.toLowerCase()} ${desbloqueado ? 'logro-abierto' : 'logro-cerrado'}`
  return (
    <li className={clase}>
      <div className="logro-medalla">
        <IconoLogro clave={oculto ? 'secreto' : logro.icono} />
        {!desbloqueado && <span className="logro-candado" aria-hidden="true"><Icono nombre="lock" tam={14} /></span>}
      </div>
      <h3>{oculto ? '???' : logro.titulo}</h3>
      {oculto ? (
        <p>Logro secreto</p>
      ) : (
        <p>{logro.descripcion}</p>
      )}
      {!oculto && <span className="logro-rareza">{textoRareza(rareza)}</span>}
      {desbloqueado ? (
        <small className="logro-fecha">
          <Icono nombre="check" tam={14} /> {fecha(logro.desbloqueadoEn) || 'Desbloqueado'}
          {(logro.recompensaXp > 0 || logro.recompensaMonedas > 0) && <span> · +{logro.recompensaMonedas} hojas · +{logro.recompensaXp} XP</span>}
        </small>
      ) : !oculto && (
        <div className="logro-prog">
          <div className="barra" role="progressbar" aria-valuenow={logro.progreso} aria-valuemin={0} aria-valuemax={logro.meta} aria-label={`Progreso de ${logro.titulo}`}>
            <div className="barra-relleno" style={{ width: `${porcentaje}%` }} />
          </div>
          <small>{Math.min(logro.progreso, logro.meta)}/{logro.meta}</small>
        </div>
      )}
    </li>
  )
}

// Logros del usuario (GET /logros): total, filtros y grilla; los bloqueados van en gris y con candado
export default function LogrosUsuario() {
  const [estado, setEstado] = useState({ cargando: true, datos: null, pendientes: [], error: '' })
  const [estadoFiltro, setEstadoFiltro] = useState('TODOS')
  const [rarezaFiltro, setRarezaFiltro] = useState('TODAS')
  const [aviso, setAviso] = useState(true)
  const avisados = useRef(new Set())

  const cargar = useCallback(async () => {
    setEstado((e) => ({ ...e, cargando: true, error: '' }))
    try {
      const datos = await obtenerLogros()
      // Los pendientes son un extra: si fallan no se pierde la pagina
      const pendientes = await obtenerPendientes().catch(() => [])
      setEstado({ cargando: false, datos, pendientes: Array.isArray(pendientes) ? pendientes : [], error: '' })
    } catch (err) {
      if (err.estado === 401) return // api.js ya devuelve al acceso
      setEstado({ cargando: false, datos: null, pendientes: [], error: 'No se pudieron cargar los logros.' })
    }
  }, [])

  useEffect(() => { cargar() }, [cargar])

  // Un pendiente se marca visto SOLO cuando el usuario cierra su celebracion (regla del producto). Antes se marcaba
  // al cargar la pagina: el movil ya no lo celebraba y, con StrictMode, ni siquiera se veia el aviso.
  // CelebracionLogro llama a esto al cerrar cada medalla, el resumen o "Saltar todos".
  const terminarCelebracion = useCallback((ids) => {
    const nuevos = (ids || []).filter((id) => id != null && !avisados.current.has(id))
    if (nuevos.length === 0) return
    nuevos.forEach((id) => avisados.current.add(id))
    marcarVistos(nuevos).catch(() => nuevos.forEach((id) => avisados.current.delete(id)))
  }, [])

  const lista = estado.datos?.logros
  const visibles = useMemo(() => (lista || []).filter((l) => {
    if (estadoFiltro === 'DESBLOQUEADOS' && !l.desbloqueado) return false
    if (estadoFiltro === 'BLOQUEADOS' && l.desbloqueado) return false
    return rarezaFiltro === 'TODAS' || l.rareza === rarezaFiltro
  }), [lista, estadoFiltro, rarezaFiltro])

  if (estado.cargando && !estado.datos) return <div className="home-cargando"><span className="home-giro" aria-label="Cargando" /></div>
  if (estado.error) {
    return (
      <div className="coleccion-vacio aparece" role="alert">
        <span>😕</span>
        <h2>{estado.error}</h2>
        <p>Revisa tu conexión e inténtalo otra vez.</p>
        <button className="boton boton-suave logro-btn" onClick={cargar}>Reintentar</button>
      </div>
    )
  }

  const { total = 0, desbloqueados = 0 } = estado.datos || {}
  const porcentaje = total > 0 ? Math.round((desbloqueados / total) * 100) : 0
  const pendientes = estado.pendientes

  return (
    <div className="logros">
      {pendientes.length > 0 && aviso && (
        <div className="logros-aviso aparece" role="status">
          <span className="logros-aviso-ico"><IconoLogro clave={pendientes[0].icono} /></span>
          <span>
            {pendientes.length === 1
              ? <>¡Desbloqueaste <b>{pendientes[0].titulo}</b>!</>
              : <>¡Desbloqueaste <b>{pendientes.length} logros</b> nuevos!</>}
          </span>
          <button className="logros-aviso-x" onClick={() => setAviso(false)} aria-label="Cerrar aviso"><Icono nombre="x" tam={18} /></button>
        </div>
      )}
      <CelebracionLogro logros={pendientes} onTerminar={terminarCelebracion} />

      <section className="tarjeta logros-cab aparece">
        <div>
          <h2>Logros</h2>
          <p>{desbloqueados} de {total} desbloqueados</p>
        </div>
        <div className="logros-prog">
          <div className="barra" role="progressbar" aria-valuenow={porcentaje} aria-valuemin={0} aria-valuemax={100} aria-label="Logros desbloqueados">
            <div className="barra-relleno" style={{ width: `${porcentaje}%` }} />
          </div>
          <b>{porcentaje} %</b>
        </div>
      </section>

      <div className="logros-filtros" aria-label="Filtros">
        <div className="logros-chips" role="group" aria-label="Estado">
          {FILTROS_ESTADO.map((f) => (
            <button key={f.valor} aria-pressed={estadoFiltro === f.valor} className={estadoFiltro === f.valor ? 'activa' : ''} onClick={() => setEstadoFiltro(f.valor)}>{f.texto}</button>
          ))}
        </div>
        <div className="logros-chips" role="group" aria-label="Rareza">
          {[{ valor: 'TODAS', texto: 'Toda rareza' }, ...RAREZAS].map((r) => (
            <button key={r.valor} aria-pressed={rarezaFiltro === r.valor} className={`${rarezaFiltro === r.valor ? 'activa' : ''} ${r.valor !== 'TODAS' ? `chip-${r.valor.toLowerCase()}` : ''}`} onClick={() => setRarezaFiltro(r.valor)}>{r.texto}</button>
          ))}
        </div>
      </div>

      {(lista || []).length === 0 ? (
        <div className="coleccion-vacio aparece">
          <span>🏆</span>
          <h2>Aún no hay logros</h2>
          <p>Pronto habrá retos por cumplir.</p>
        </div>
      ) : visibles.length === 0 ? (
        <div className="coleccion-vacio aparece">
          <span>🔍</span>
          <h2>Ningún logro coincide</h2>
          <button className="boton boton-suave logro-btn" onClick={() => { setEstadoFiltro('TODOS'); setRarezaFiltro('TODAS') }}>Quitar filtros</button>
        </div>
      ) : (
        <ul className="logros-grilla">
          {visibles.map((l) => <TarjetaLogro key={l.idLogro} logro={l} />)}
        </ul>
      )}
    </div>
  )
}
