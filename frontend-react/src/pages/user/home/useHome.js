import { useCallback, useEffect, useState } from 'react'
import { obtenerEventoActivo, obtenerMisionDiaria, obtenerPlantaActiva, obtenerUsuario } from '../../../services/home'

// Carga los 4 datos del inicio por separado: si uno falla, los demas se muestran igual
export default function useHome() {
  const [estado, setEstado] = useState({ cargando: true, usuario: null, alerta: null, planta: null, mision: null, error: '' })

  const cargar = useCallback(async () => {
    setEstado((e) => ({ ...e, cargando: true, error: '' }))
    const [usuario, alerta, planta, mision] = await Promise.allSettled([
      obtenerUsuario(), obtenerEventoActivo(), obtenerPlantaActiva(), obtenerMisionDiaria(),
    ])
    const valor = (r) => (r.status === 'fulfilled' ? r.value : null)
    const fallo = [usuario, alerta, planta, mision].some((r) => r.status === 'rejected')
    setEstado({
      cargando: false,
      usuario: valor(usuario),
      alerta: valor(alerta),
      planta: valor(planta),
      mision: valor(mision),
      error: fallo ? 'No se pudo cargar todo desde el servidor.' : '',
    })
  }, [])

  useEffect(() => { cargar() }, [cargar])

  return { ...estado, recargar: cargar }
}
