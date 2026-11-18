import { useCallback, useEffect, useRef, useState } from 'react'
import { obtenerEventoActivo, obtenerMisiones, obtenerPlantaActiva, obtenerUsuario } from '../../../services/home'
import { cuidarPlanta, listarPlantasDigitales, seleccionarPlanta } from '../../../services/jardin'
import { ACCIONES, mensajeCuidado } from '../jardin/acciones'

const MS_AVISO = 2500

// Carga los datos del inicio por separado: si uno falla, los demas se muestran igual
export default function useHome() {
  const [estado, setEstado] = useState({ cargando: true, usuario: null, alerta: null, planta: null, plantas: [], misiones: [], error: '' })
  const [aviso, setAviso] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const temporizador = useRef(null)

  const cargar = useCallback(async () => {
    setEstado((e) => ({ ...e, cargando: true, error: '' }))
    const [usuario, alerta, planta, misiones, plantas] = await Promise.allSettled([
      obtenerUsuario(), obtenerEventoActivo(), obtenerPlantaActiva(), obtenerMisiones(), listarPlantasDigitales(),
    ])
    const valor = (r) => (r.status === 'fulfilled' ? r.value : null)
    const fallo = [usuario, alerta, planta, misiones, plantas].some((r) => r.status === 'rejected')
    setEstado({
      cargando: false,
      usuario: valor(usuario),
      alerta: valor(alerta),
      planta: valor(planta),
      // Las plantas del baul estan congeladas: no se pasan con las flechas
      plantas: (valor(plantas) || []).filter((p) => !p.enBaul),
      // Todas las misiones de hoy (la del evento primero); el inicio muestra tres y el resto va en la ventana
      misiones: [...(valor(misiones) || [])].sort((a, b) => (b.kind === 'EVENT') - (a.kind === 'EVENT')),
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

  // Deja una planta como la del jardin
  const seleccionar = async (destino) => {
    if (ocupado || !destino || destino.id === estado.planta?.id) return
    setOcupado(true)
    try {
      const nueva = await seleccionarPlanta(destino.id)
      setEstado((e) => ({ ...e, planta: nueva }))
    } catch (err) {
      avisar(err.message || 'No se pudo cambiar de planta.')
    } finally {
      setOcupado(false)
    }
  }

  // Pasa a la planta anterior (-1) o siguiente (+1)
  const mover = (paso) => {
    const { plantas, planta } = estado
    if (plantas.length < 2) return
    const actual = Math.max(0, plantas.findIndex((p) => p.id === planta?.id))
    return seleccionar(plantas[(actual + paso + plantas.length) % plantas.length])
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

  return { ...estado, aviso, ocupado, mover, seleccionar, cuidar, recargar: cargar }
}
