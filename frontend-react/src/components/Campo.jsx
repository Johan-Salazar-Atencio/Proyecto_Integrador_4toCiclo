import { useState } from 'react'
import './campo.css'

// Campo de formulario con etiqueta, mensaje de error y "ojito" si es contrasena
export default function Campo({ etiqueta, tipo = 'text', error, ...resto }) {
  const [visible, setVisible] = useState(false)
  const esPassword = tipo === 'password'

  return (
    <label className="campo">
      <span className="campo-etiqueta">{etiqueta}</span>
      <span className={`campo-caja ${error ? 'campo-con-error' : ''}`}>
        <input type={esPassword && visible ? 'text' : tipo} {...resto} />
        {esPassword && (
          <button type="button" className="campo-ojo" onClick={() => setVisible(!visible)}
            aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              {visible
                ? <><path d="M3 3l18 18" /><path d="M10.6 6.1A9.8 9.8 0 0 1 12 6c5 0 8.5 4 9.5 6a12.6 12.6 0 0 1-2.4 3.1M6.6 7.6A12.5 12.5 0 0 0 2.5 12c1 2 4.5 6 9.5 6 1.5 0 2.8-.4 4-1" /><path d="M9.9 10a3 3 0 0 0 4.1 4.1" /></>
                : <><path d="M2.5 12C3.5 10 7 6 12 6s8.5 4 9.5 6c-1 2-4.5 6-9.5 6s-8.5-4-9.5-6z" /><circle cx="12" cy="12" r="3" /></>}
            </svg>
          </button>
        )}
      </span>
      {error && <span className="campo-error">{error}</span>}
    </label>
  )
}
