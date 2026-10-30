import { createContext, useContext, useState } from 'react'

// Sesion del administrador, separada de la del usuario (otra clave en el navegador)
const CLAVE = 'plantia.admin'
const SesionAdminContexto = createContext(null)

function leer() {
  try {
    return JSON.parse(localStorage.getItem(CLAVE)) || null
  } catch {
    return null
  }
}

export function SesionAdminProvider({ children }) {
  const [admin, setAdmin] = useState(leer)

  const iniciar = (datos) => {
    try { localStorage.setItem(CLAVE, JSON.stringify(datos)) } catch { /* sin almacenamiento */ }
    setAdmin(datos)
  }

  const cerrar = () => {
    try { localStorage.removeItem(CLAVE) } catch { /* nada que borrar */ }
    setAdmin(null)
  }

  return <SesionAdminContexto.Provider value={{ admin, iniciar, cerrar }}>{children}</SesionAdminContexto.Provider>
}

export const useSesionAdmin = () => useContext(SesionAdminContexto)
