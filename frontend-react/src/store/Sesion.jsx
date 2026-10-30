import { createContext, useContext, useState } from 'react'
import { borrarSesion, guardarSesion, leerSesion } from './sesionGuardada'

const SesionContexto = createContext(null)

export function SesionProvider({ children }) {
  const [usuario, setUsuario] = useState(leerSesion)

  const iniciar = (datos) => {
    guardarSesion(datos)
    setUsuario(datos)
  }

  const cerrar = () => {
    borrarSesion()
    setUsuario(null)
  }

  return (
    <SesionContexto.Provider value={{ usuario, iniciar, cerrar }}>
      {children}
    </SesionContexto.Provider>
  )
}

export const useSesion = () => useContext(SesionContexto)
