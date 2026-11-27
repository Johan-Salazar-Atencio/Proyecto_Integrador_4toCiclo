import { useCallback, useEffect, useRef, useState } from 'react'
import { AVISO_LOGROS } from '../../../services/avisoLogros'
import { marcarVistos, obtenerPendientes } from '../../../services/logros'
import CelebracionLogro from './CelebracionLogro'

// Celebra los logros en cualquier pagina con sesion, como el movil: junta los `logrosNuevos` que trae cada accion
// (evento de api.js) y los pendientes del servidor (al entrar y al volver a la pestaña, para los que se ganaron
// fuera: fin de un evento, una respuesta sin cuerpo...). Un logro se marca visto solo al cerrar su celebracion.
export default function CelebradorLogros() {
  const [logros, setLogros] = useState([])
  const avisados = useRef(new Set())

  const sumar = useCallback((nuevos) => {
    if (!nuevos?.length) return
    setLogros((actuales) => {
      const ids = new Set(actuales.map((l) => l.idLogro))
      const faltan = nuevos.filter((l) => l?.idLogro > 0 && !ids.has(l.idLogro) && !avisados.current.has(l.idLogro))
      return faltan.length ? [...actuales, ...faltan] : actuales
    })
  }, [])

  useEffect(() => {
    let vivo = true
    const revisar = () => obtenerPendientes()
      .then((p) => { if (vivo && Array.isArray(p)) sumar(p) })
      .catch(() => {}) // los pendientes son un extra: si fallan se reintenta al volver a la pestaña
    const alVolver = () => { if (document.visibilityState === 'visible') revisar() }
    const alGanar = (e) => sumar(e.detail)
    revisar()
    document.addEventListener('visibilitychange', alVolver)
    window.addEventListener(AVISO_LOGROS, alGanar)
    return () => {
      vivo = false
      document.removeEventListener('visibilitychange', alVolver)
      window.removeEventListener(AVISO_LOGROS, alGanar)
    }
  }, [sumar])

  // CelebracionLogro llama a esto al cerrar cada medalla, el resumen o "Saltar todos"
  const terminar = useCallback((ids) => {
    const nuevos = (ids || []).filter((id) => id != null && !avisados.current.has(id))
    if (nuevos.length === 0) return
    nuevos.forEach((id) => avisados.current.add(id))
    // Si el POST falla, siguen pendientes en el servidor y se vuelven a celebrar en la siguiente revision
    marcarVistos(nuevos).catch(() => nuevos.forEach((id) => avisados.current.delete(id)))
  }, [])

  return <CelebracionLogro logros={logros} onTerminar={terminar} />
}
