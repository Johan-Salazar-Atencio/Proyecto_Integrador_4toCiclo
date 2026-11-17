import { useCallback, useEffect, useRef, useState } from 'react'
import { obtenerEventoActivo, obtenerMisionDiaria, obtenerPlantaActiva, obtenerUsuario } from '../../../services/home'
import { cuidarPlanta, listarPlantasDigitales, seleccionarPlanta } from '../../../services/jardin'
import { ACCIONES, mensajeCuidado } from '../jardin/acciones'

const MS_AVISO = 2500

// Carga los datos del inicio por separado: si uno falla, los demas se muestran igual
export default function useHome() {
  const [estado, setEstado] = useState({ cargando: true, usuario: null, alerta: null, planta: null, plantas: [], mision: null, error: '' })
  const [aviso, setAviso] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const temporizador = useRef(null)

  const cargar = useCallback(async () => {
    setEstado((e) => ({ ...e, cargando: true, error: '' }))
    const [usuario, alerta, planta, mision, plantas] = await Promise.allSettled([
      obtenerUsuario(), obtenerEventoActivo(), obtenerPlantaActiva(), obtenerMisionDiaria(), listarPlantasDigitales(),
    ])
    const valor = (r) => (r.status === 'fulfilled' ? r.value : null)
    const fallo = [usuario, alerta, planta, mision, plantas].some((r) => r.status === 'rejected')
    setEstado({
      cargando: false,
      usuario: valor(usuario),
      alerta: valor(alerta),
      planta: valor(planta),
      // Las plantas del baul estan congeladas: no se pasan con las flechas
      plantas: (valor(plantas) || []).filter((p) => !p.enBaul),
      mision: valor(mision),
      error: fallo ? 'No se pudo cargar todo desde el servidor.' : '',
    })
  }, [])

  useEffect(() => { cargar() }, [cargar])
  useEffect(() => () => clearTimeout(temporizador.current), [])

  const avisar = (texto) => {
    setAviso(texto)
    clearTimeout(temporizador.current)
    temporizador.current = setTimeout(() => setAviso(''), MS_AVISO)
  }

  // Pasa a la planta anterior (-1) o siguiente (+1) y la deja como la del jardin
  const mover = async (paso) => {
    const { plantas, planta } = estado
    if (ocupado || plantas.length < 2) return
    const actual = Math.max(0, plantas.findIndex((p) => p.id === planta?.id))
    const siguiente = plantas[(actual + paso + plantas.length) % plantas.length]
    setOcupado(true)
    try {
      const nueva = await seleccionarPlanta(siguiente.id)
      setEstado((e) => ({ ...e, planta: nueva }))
    } catch (err) {
      avisar(err.message || 'No se pudo cambiar de planta.')
    } finally {
      setOcupado(false)
    }
  }

  // Riega, da luz o abona a la planta que se ve y avisa el resultado
  const cuidar = async (accion) => {
    const previa = estado.planta
    if (ocupado || !previa) return
    if (previa[ACCIONES[accion].campo] >= 100) return avisar(mensajeCuidado(previa, previa, accion))
    setOcupado(true)
    try {
      const planta = await cuidarPlanta(previa.id, accion)
      setEstado((e) => ({ ...e, planta, plantas: e.plantas.map((p) => (p.id === planta.id ? planta : p)) }))
      avisar(mensajeCuidado(previa, planta, accion))
    } catch (err) {
      avisar(err.message || 'No se pudo cuidar la planta.')
    } finally {
      setOcupado(false)
    }
  }

  return { ...estado, aviso, ocupado, mover, cuidar, recargar: cargar }
}
