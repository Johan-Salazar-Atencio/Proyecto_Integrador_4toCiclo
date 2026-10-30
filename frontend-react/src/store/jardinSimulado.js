// SIMULACION de las plantas digitales (el backend aun no tiene endpoint para crearlas ni cuidarlas).
// Igual que MockGardenData.kt en el movil, pero se guarda en el navegador para sobrevivir a recargas.
// TODO(backend): reemplazar `digitalizar` y `activar` por llamadas reales y borrar este archivo.
import { obtenerUsuarioId } from './sesionGuardada'

const clave = () => `plantia.jardin.${obtenerUsuarioId()}`

function leer() {
  try {
    return JSON.parse(localStorage.getItem(clave())) || { plantas: [], activa: null }
  } catch {
    return { plantas: [], activa: null }
  }
}

function guardar(datos) {
  try {
    localStorage.setItem(clave(), JSON.stringify(datos))
  } catch {
    /* sin almacenamiento: los cambios duran hasta recargar */
  }
}

export const listarDigitales = () => leer().plantas

export function plantaActiva() {
  const { plantas, activa } = leer()
  return plantas.find((p) => p.name === activa) || null
}

// La planta escaneada pasa a ser la activa; si ya existia una con ese nombre, se reemplaza
export function digitalizar(escaneo) {
  const { plantas } = leer()
  const nueva = {
    name: escaneo.nombreComun?.trim() || 'Mi planta',
    species: escaneo.nombreCientifico || '',
    healthPercent: 90,
    humidityPercent: 60,
    lightPercent: 70,
    lastWatered: 'Recién digitalizada',
    imageUrl: escaneo.imagenUrl || null,
  }
  guardar({ plantas: [nueva, ...plantas.filter((p) => p.name !== nueva.name)], activa: nueva.name })
}

export function activar(nombre) {
  guardar({ ...leer(), activa: nombre })
}
