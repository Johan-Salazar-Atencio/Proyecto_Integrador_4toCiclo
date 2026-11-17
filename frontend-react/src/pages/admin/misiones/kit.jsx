import { useEffect, useRef } from 'react'
import './kit.css'

// Piezas compartidas por Misiones, Tienda, Economía y Catálogo (prefijo ak-). Iconos de trazo copiados de la referencia.
export const PENDIENTE = 'Pendiente de backend'

const TRAZOS = {
  edit: 'M4 20h4L19 9l-4-4L4 16zM13 7l4 4',
  trash: 'M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13M10 11v6M14 11v6',
  plus: 'M12 5v14M5 12h14',
  x: 'M6 6l12 12M18 6L6 18',
  check: 'M5 12l5 5 9-10',
  leaf: 'M5 19c0-8 5-14 14-14 0 9-6 14-14 14zM5 19l7-7',
  search: 'M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14zM20 20l-4-4',
  up: 'M12 16V4M7 9l5-5 5 5M4 20h16',
  down: 'M6 9l6 6 6-6',
  alert: 'M12 3l10 18H2zM12 10v5M12 18v.5',
}

export function Icono({ nombre, tam = 17 }) {
  return (
    <svg width={tam} height={tam} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" className="ak-ico">
      <path d={TRAZOS[nombre]} />
    </svg>
  )
}

export function Interruptor({ activo, onChange, disabled = false, titulo, etiqueta }) {
  return (
    <button type="button" role="switch" aria-checked={activo} aria-label={etiqueta} title={titulo}
      className={`ak-sw ${activo ? '' : 'ak-sw-off'}`} disabled={disabled} onClick={() => onChange(!activo)} />
  )
}

export function BotonIcono({ icono, etiqueta, peligro = false, onClick, disabled = false, titulo }) {
  return (
    <button type="button" className={`ak-ib ${peligro ? 'ak-ib-d' : ''}`} aria-label={etiqueta} title={titulo || etiqueta} disabled={disabled} onClick={onClick}>
      <Icono nombre={icono} />
    </button>
  )
}

export function Campo({ etiqueta, children, ayuda }) {
  return (
    <label className="ak-fld">
      <span>{etiqueta}</span>
      {children}
      {ayuda && <small>{ayuda}</small>}
    </label>
  )
}

export function Chip({ color = 'var(--agua)', children }) {
  return <span className="ak-chip" style={{ '--c': color }}>{children}</span>
}

export function Alternador({ titulo, detalle, activo, onChange, disabled, tituloBoton }) {
  return (
    <div className="ak-tg">
      <span>{titulo}<small>{detalle}</small></span>
      <Interruptor activo={activo} onChange={onChange} disabled={disabled} titulo={tituloBoton} etiqueta={titulo} />
    </div>
  )
}

export function Aviso({ error, onReintentar }) {
  if (!error) return null
  return (
    <div className="ak-error" role="alert"><span>{error}</span>{onReintentar && <button onClick={onReintentar}>Reintentar</button>}</div>
  )
}
