// Celebracion de logros desbloqueados: "D + aviso al terminar" (decision de Johan, 2026-11-22).
// Todos los logros salen con la medalla que brota a pantalla completa (motorMedalla.js, mismos tiempos que el movil).
// Cuando su animacion TERMINA baja el aviso arriba; se va a los 4 s (6 s con animaciones reducidas) o con un toque
// y se queda sobre la pagina aunque se cierre la medalla. Si empieza otra celebracion, el aviso anterior se quita.
// Desde el 4.º logro se celebran los 2 mas raros y el resto sale en una tarjeta resumen.
//   logros: [{ idLogro, titulo, descripcion, icono, rareza, recompensaXp, recompensaMonedas }]
//   onTerminar(ids): se llama al CERRAR cada medalla (o el resumen, o "Saltar todos"); es lo unico que los marca
//   vistos en el servidor (si no se llama, siguen pendientes y el movil los celebra).
import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { textoRareza } from '../../../services/logros'
import { duracionAviso, ordenarLogros, partesRecompensa, pasoCelebracion, sumarATanda, sumarRecompensas } from './celebracionLogica'
import { iconoPixel, pintarIcono } from './iconoPixel'
import { dibujarMedalla, MotorMedalla, nivelRareza, SALTO_MS, TIC_MS } from './motorMedalla'

const reducirMovimiento = () => typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
// Haptico si, sonido no. El navegador puede ignorarlo (sin vibrador o sin gesto previo del usuario).
const VIBRACION = [[20], [30, 40, 30], [40, 40, 40, 40, 80], [60, 40, 60, 40, 120]]
const vibrar = (r) => { try { navigator.vibrate?.(VIBRACION[r]) } catch { /* opcional */ } }

function IconoCanvas({ clave, className, etiqueta }) {
  const ref = useRef(null)
  useEffect(() => { pintarIcono(ref.current, clave) }, [clave])
  return <canvas ref={ref} width="18" height="18" className={className} role={etiqueta ? 'img' : undefined} aria-label={etiqueta} aria-hidden={etiqueta ? undefined : 'true'} />
}

/** La medalla que brota de un logro, con su tarjeta. */
function Medalla({ paso, ocultarCuenta, onAnimacionTerminada, onCerrar, onSaltarTodos }) {
  const { logro, numero, total } = paso
  const r = nivelRareza(logro.rareza)
  const [quieto] = useState(reducirMovimiento)
  const [tarjeta, setTarjeta] = useState(quieto)
  const ref = useRef({})
  const fin = useRef(onAnimacionTerminada)
  fin.current = onAnimacionTerminada

  useLayoutEffect(() => {
    const { fx, ovIn, slot, ok } = ref.current
    const ctx = fx.getContext('2d')
    const icono = iconoPixel(logro.icono)
    const motor = new MotorMedalla(r, quieto, () => fin.current())
    ref.current.motor = motor
    ref.current.t0 = performance.now()
    let P = 3
    const medir = () => {
      P = innerWidth < 520 ? 3 : 4
      const gw = Math.ceil(innerWidth / P), gh = Math.ceil(innerHeight / P)
      fx.width = gw; fx.height = gh
      fx.style.width = gw * P + 'px'; fx.style.height = gh * P + 'px'
      ctx.imageSmoothingEnabled = false
      ref.current.ov.style.setProperty('--P', P + 'px')
      motor.colocar(motor.cx, motor.bot, gw, gh)
    }
    // El hueco tiembla con todo lo demas: se le resta el temblor para hallar su sitio quieto
    const colocar = () => {
      const b = slot.getBoundingClientRect()
      motor.colocar(Math.round((b.left + b.width / 2) / P) - motor.shx, Math.round(b.bottom / P) - motor.shy, motor.gw, motor.gh)
    }
    const dibujar = () => {
      dibujarMedalla(ctx, motor, icono)
      ovIn.style.transform = motor.shx || motor.shy ? `translate(${motor.shx * P}px, ${motor.shy * P}px)` : ''
      if (motor.tarjeta) setTarjeta(true)
    }
    ref.current.dibujar = dibujar
    ref.current.colocar = colocar
    medir()
    vibrar(r)

    let raf = 0, prev = performance.now(), acum = 0
    const bucle = (ahora) => {
      acum += Math.min(400, ahora - prev); prev = ahora
      let hubo = false
      while (acum >= TIC_MS) { acum -= TIC_MS; colocar(); motor.tic(); hubo = true }
      if (hubo) dibujar()
      raf = motor.debeParar() ? 0 : requestAnimationFrame(bucle)
    }
    colocar()
    if (quieto) { motor.completarEstatico(); dibujar() } else { motor.tic(); dibujar(); raf = requestAnimationFrame(bucle) }
    ok?.focus({ preventScroll: true })

    const alCambiar = () => { medir(); colocar(); dibujar() }
    addEventListener('resize', alCambiar)
    return () => { cancelAnimationFrame(raf); removeEventListener('resize', alCambiar) }
  }, [logro.idLogro, logro.icono, r, quieto])

  const antesDelSalto = () => {
    const { motor, t0 } = ref.current
    return !quieto && motor && !motor.done && performance.now() - t0 < SALTO_MS
  }
  const adelantar = () => { ref.current.motor.adelantar(); ref.current.dibujar() }
  // Toque en cualquier parte: el primer segundo salta al final; despues continua
  const alTocar = (e) => {
    if (e.target.closest('.cel-saltar')) return onSaltarTodos()
    if (e.target.closest('.cel-ok')) { if (antesDelSalto()) adelantar(); return onCerrar() }
    if (antesDelSalto()) return adelantar()
    onCerrar()
  }

  const quedan = total - numero
  const rec = partesRecompensa(logro.recompensaXp, logro.recompensaMonedas)
  const tituloId = `cel-tit-${logro.idLogro}`
  return (
    <div ref={(n) => { ref.current.ov = n }} className={`cel-ov rz-${r}${quieto ? ' quieto' : ''}`} role="dialog" aria-modal="true" aria-labelledby={tituloId} onClick={alTocar}>
      <div className="cel-ov-in" ref={(n) => { ref.current.ovIn = n }}>
        <canvas className="cel-fx" ref={(n) => { ref.current.fx = n }} aria-hidden="true" />
        <div className="cel-stage">
          <div className="cel-slot" ref={(n) => { ref.current.slot = n }} role="img" aria-label={`Medalla de ${logro.titulo}`} />
          <div className={`cel-card${tarjeta ? ' on' : ''}`}>
            <p className="cel-kick">¡Logro desbloqueado!</p>
            <span className="cel-chip">{textoRareza(logro.rareza)}</span>
            <h2 className="cel-tit" id={tituloId}>{logro.titulo}</h2>
            {logro.descripcion && <p className="cel-desc">{logro.descripcion}</p>}
            {rec.length > 0 && <div className="cel-rec">{rec.map((x) => <span key={x}>{x}</span>)}</div>}
            <button className="cel-ok" ref={(n) => { ref.current.ok = n }}>{quedan > 0 ? 'Siguiente' : '¡Genial!'}</button>
            {quedan > 0
              ? <button className="cel-saltar">Saltar todos ({quedan} más)</button>
              : <span className="cel-pista">Toca en cualquier parte para continuar</span>}
          </div>
        </div>
      </div>
      {total > 1 && <div className="cel-cuenta" style={ocultarCuenta ? { visibility: 'hidden' } : undefined}>{numero} de {total}</div>}
      <div className="cel-vh" aria-live="assertive">
        {`Logro desbloqueado: ${logro.titulo}. Rareza ${textoRareza(logro.rareza)}. ${logro.descripcion || ''} ${rec.join(', ')}`}
      </div>
    </div>
  )
}

/** Tarjeta resumen: "¡N logros más!" con sus iconos y la recompensa sumada. */
function Resumen({ logros, onCerrar }) {
  const okRef = useRef(null)
  const [quieto] = useState(reducirMovimiento)
  const { xp, hojas } = sumarRecompensas(logros)
  const rec = [xp > 0 ? `+${xp} XP` : '', hojas > 0 ? `+${hojas} hojas` : ''].filter(Boolean)
  useEffect(() => { okRef.current?.focus({ preventScroll: true }); vibrar(0) }, [])
  return (
    <div className={`cel-ov cel-resumen rz-0${quieto ? ' quieto' : ''}`} role="dialog" aria-modal="true" aria-labelledby="cel-tit-resumen" onClick={onCerrar}>
      <div className="cel-ov-in">
        <div className="cel-stage">
          <div className="cel-card on">
            <p className="cel-kick">Y además</p>
            <h2 className="cel-tit" id="cel-tit-resumen">¡{logros.length} logros más!</h2>
            <p className="cel-desc">Los desbloqueaste mientras no mirabas.</p>
            <div className="cel-resumen-grid">
              {logros.map((l) => <IconoCanvas key={l.idLogro} clave={l.icono} etiqueta={l.titulo} />)}
            </div>
            {rec.length > 0 && <div className="cel-rec">{rec.map((x) => <span key={x}>{x}</span>)}</div>}
            <button className="cel-ok" ref={okRef}>Ver mis logros</button>
            <span className="cel-pista">Toca en cualquier parte para cerrar</span>
          </div>
        </div>
      </div>
      <div className="cel-vh" aria-live="assertive">{`${logros.length} logros más desbloqueados. ${rec.join(', ')}`}</div>
    </div>
  )
}

/** Aviso de arriba: baja al terminar la animacion y queda sobre la pagina hasta su tiempo o un toque. */
function Aviso({ aviso, onQuitar }) {
  const { logro } = aviso
  const r = nivelRareza(logro.rareza)
  return (
    <div className={`cel-aviso rz-${r}${aviso.quieto ? ' quieto' : ''}`} aria-hidden="true" onClick={onQuitar}>
      <IconoCanvas clave={logro.icono} />
      <div>
        <span className="cel-kick">¡Logro desbloqueado!</span>
        <b>{logro.titulo}</b>
        <small>{partesRecompensa(logro.recompensaXp, logro.recompensaMonedas).join(' · ')}</small>
      </div>
    </div>
  )
}

export default function CelebracionLogro({ logros, onTerminar }) {
  const [tanda, setTanda] = useState({ lista: [], hechos: 0 })
  const [aviso, setAviso] = useState(null)
  const cerrados = useRef(new Set())
  const mostrando = useRef(null)
  const temporizador = useRef(0)
  const foco = useRef(null)
  const terminar = useRef(onTerminar)
  terminar.current = onTerminar

  // Junta los pendientes nuevos: si no hay tanda empieza una; si la hay, se suman sin mover el que se muestra
  useEffect(() => {
    setTanda((t) => {
      const nuevos = ordenarLogros(logros).filter((l) => !cerrados.current.has(l.idLogro))
      if (t.hechos >= t.lista.length) return nuevos.length ? { lista: nuevos, hechos: 0 } : t
      return { ...t, lista: sumarATanda(t.lista, t.hechos, nuevos) }
    })
  }, [logros])

  // La medalla que ya se muestra no se cambia por el resumen aunque lleguen mas logros
  const paso = pasoCelebracion(tanda.lista, tanda.hechos, mostrando.current)
  mostrando.current = paso?.tipo === 'una' ? paso.logro.idLogro : null
  const clave = paso ? (paso.tipo === 'una' ? `logro-${paso.logro.idLogro}` : 'resumen') : null

  // Si empieza otra celebracion, el aviso anterior se quita (cerrar la ultima NO lo quita). Se decide al pintar,
  // antes de los efectos: con animaciones reducidas la medalla termina (y pide su aviso) en su primer efecto.
  const [claveVista, setClaveVista] = useState(null)
  if (clave !== claveVista) {
    setClaveVista(clave)
    if (clave && aviso) setAviso(null)
  }

  const quitarAviso = useCallback(() => { clearTimeout(temporizador.current); setAviso(null) }, [])
  const mostrarAviso = useCallback((logro) => {
    const quieto = reducirMovimiento()
    clearTimeout(temporizador.current)
    setAviso({ logro, quieto, serie: Date.now() })
    temporizador.current = setTimeout(() => setAviso(null), duracionAviso(quieto))
  }, [])
  useEffect(() => () => clearTimeout(temporizador.current), [])

  // Mientras hay celebracion: sin scroll detras y, al acabar, el foco vuelve a donde estaba
  const abierta = paso != null
  useEffect(() => {
    if (!abierta) return undefined
    foco.current = document.activeElement
    const antes = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = antes
      try { foco.current?.focus?.({ preventScroll: true }) } catch { /* nada */ }
    }
  }, [abierta])

  const cerrar = useCallback((ids) => {
    ids.forEach((id) => cerrados.current.add(id))
    setTanda((t) => ({ ...t, hechos: Math.min(t.lista.length, t.hechos + ids.length) }))
    terminar.current?.(ids)
  }, [])
  const saltarTodos = useCallback(() => {
    const resto = tanda.lista.slice(tanda.hechos).map((l) => l.idLogro)
    if (resto.length) cerrar(resto)
  }, [tanda, cerrar])

  // Escape = "Saltar todos" (en el resumen, cerrar)
  useEffect(() => {
    if (!abierta) return undefined
    const tecla = (e) => { if (e.key === 'Escape') { e.preventDefault(); saltarTodos() } }
    document.addEventListener('keydown', tecla)
    return () => document.removeEventListener('keydown', tecla)
  }, [abierta, saltarTodos])

  if (typeof document === 'undefined') return null
  return createPortal(
    <>
      {paso?.tipo === 'una' && (
        <Medalla
          key={paso.logro.idLogro}
          paso={paso}
          ocultarCuenta={aviso != null}
          onAnimacionTerminada={() => mostrarAviso(paso.logro)}
          onCerrar={() => cerrar([paso.logro.idLogro])}
          onSaltarTodos={saltarTodos}
        />
      )}
      {paso?.tipo === 'resumen' && <Resumen logros={paso.logros} onCerrar={() => cerrar(paso.logros.map((l) => l.idLogro))} />}
      {aviso && <Aviso key={aviso.serie} aviso={aviso} onQuitar={quitarAviso} />}
    </>,
    document.body,
  )
}
