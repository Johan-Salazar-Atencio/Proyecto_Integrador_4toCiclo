import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import Boton from '../../../components/Boton'
import './espera.css'

// Las etapas son temporizadas: no representan el progreso real del servidor
const ETAPAS = [
  ['Buscando información…', 'Preparando tu foto'],
  ['Analizando la imagen…', 'La IA observa hojas, forma y color'],
  ['Identificando la especie…', 'Comparando con miles de plantas'],
  ['Casi listo…', 'Armando tu guía de cuidados'],
]
const MS_POR_ETAPA = 2200

// Pantalla completa mientras se identifica la planta. Con `listo` muestra el check de exito.
export default function EsperaEscaneo({ foto, listo, onCancelar }) {
  const [etapa, setEtapa] = useState(0)

  useEffect(() => {
    if (listo) return undefined
    const id = setInterval(() => setEtapa((e) => Math.min(e + 1, ETAPAS.length - 1)), MS_POR_ETAPA)
    return () => clearInterval(id)
  }, [listo])

  useEffect(() => {
    const alTeclear = (e) => { if (e.key === 'Escape' && !listo) onCancelar() }
    window.addEventListener('keydown', alTeclear)
    return () => window.removeEventListener('keydown', alTeclear)
  }, [listo, onCancelar])

  // En el body para que cubra toda la pantalla aunque la pagina tenga animaciones
  return createPortal(
    <div className="espera" role="status" aria-live="polite">
      <div className="espera-foto aparece">
        <img src={foto} alt="Tu foto" />
        {listo ? (
          <svg className="espera-check" viewBox="0 0 52 52" aria-hidden="true">
            <circle cx="26" cy="26" r="24" />
            <path d="M14 27 l8 8 l16 -17" />
          </svg>
        ) : (
          <span className="espera-anillo" />
        )}
      </div>

      {listo ? (
        <h2 className="aparece">¡Planta identificada!</h2>
      ) : (
        <>
          <h2 key={etapa} className="aparece">{ETAPAS[etapa][0]}</h2>
          <p key={`p${etapa}`} className="aparece">{ETAPAS[etapa][1]}</p>
          <Boton variante="suave" onClick={onCancelar}>Cancelar</Boton>
        </>
      )}
    </div>,
    document.body,
  )
}
