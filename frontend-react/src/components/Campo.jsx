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
            {visible ? '🙈' : '👁️'}
          </button>
        )}
      </span>
      {error && <span className="campo-error">{error}</span>}
    </label>
  )
}
