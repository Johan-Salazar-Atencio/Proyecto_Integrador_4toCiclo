import { useEffect, useRef } from 'react'
import { createPortal } from 'react-dom'
import Icono from '../../../../components/Icono'
import MisionFila from '../../misiones/MisionFila'

const FOCALIZABLES = 'a[href], button:not([disabled]), [tabindex]:not([tabindex="-1"])'

// Ventana flotante con todas las misiones de hoy: Escape y el fondo cierran, el foco queda dentro y vuelve al boton que la abrio
export default function ModalMisiones({ misiones, onCerrar }) {
  const hoja = useRef(null)
  const cerrar = useRef(onCerrar)
  cerrar.current = onCerrar

  useEffect(() => {
    const previo = document.activeElement
    hoja.current?.querySelector('.modal-cerrar')?.focus()
    const alTeclear = (e) => {
      if (e.key === 'Escape') return cerrar.current()
      if (e.key !== 'Tab') return
      const items = [...hoja.current.querySelectorAll(FOCALIZABLES)]
      if (!items.length) return
      const primero = items[0]
      const ultimo = items[items.length - 1]
      if (e.shiftKey && document.activeElement === primero) { e.preventDefault(); ultimo.focus() }
      else if (!e.shiftKey && document.activeElement === ultimo) { e.preventDefault(); primero.focus() }
    }
    document.addEventListener('keydown', alTeclear)
    const overflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', alTeclear)
      document.body.style.overflow = overflow
      previo?.focus?.()
    }
  }, [])

  return createPortal(
    <div className="modal" onClick={onCerrar}>
      <div className="modal-hoja aparece" ref={hoja} role="dialog" aria-modal="true" aria-labelledby="modal-misiones-titulo" onClick={(e) => e.stopPropagation()}>
        <div className="modal-cabecera">
          <h3 id="modal-misiones-titulo">Misiones de hoy</h3>
          <button className="modal-cerrar" onClick={onCerrar} aria-label="Cerrar"><Icono nombre="x" /></button>
        </div>
        <div className="modal-lista">
          {misiones.map((m) => <MisionFila key={m.id} mision={m} onIr={onCerrar} />)}
        </div>
      </div>
    </div>,
    document.body,
  )
}
