// SIMULACION de las plantas digitales y sus cuidados (el backend aun no tiene endpoint para crearlas
// ni para regar / dar sol / abonar). Igual que MockGardenData.kt en el movil, pero se guarda en el
// navegador para sobrevivir a recargas.
// TODO(backend): reemplazar `digitalizar`, `activar` y `aplicarCuidado` por llamadas reales
// y borrar este archivo.
import { obtenerUsuarioId } from './sesionGuardada'

export const ACCIONES = {
  REGAR: { texto: 'Regar', icono: '💧', pista: '+20%', delta: 20 },
  SOL: { texto: 'Sol', icono: '☀️', pista: '+15%', delta: 15 },
  ABONAR: { texto: 'Abonar', icono: '🌿', pista: '+10%', delta: 10 },
}

const VACIO = { plantas: [], activa: null, ajustes: { salud: 0, agua: 0, luz: 0, regada: false } }
const clave = () => `plantia.jardin.${obtenerUsuarioId()}`

function leer() {
  try {
    return { ...VACIO, ...JSON.parse(localStorage.getItem(clave())) }
  } catch {
    return VACIO
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
  const datos = leer()
  const nueva = {
    name: escaneo.nombreComun?.trim() || 'Mi planta',
    species: escaneo.nombreCientifico || '',
    healthPercent: 90,
    humidityPercent: 60,
    lightPercent: 70,
    lastWatered: 'Recién digitalizada',
    imageUrl: escaneo.imagenUrl || null,
  }
  guardar({ ...datos, plantas: [nueva, ...datos.plantas.filter((p) => p.name !== nueva.name)], activa: nueva.name })
}

export function activar(nombre) {
  guardar({ ...leer(), activa: nombre })
}

const tope = (n) => Math.max(0, Math.min(100, n))

// Suma los cuidados simulados a la planta que devuelve el servidor (cuando no hay digitales)
export function aplicarAjustes(base) {
  const { ajustes } = leer()
  return {
    ...base,
    healthPercent: tope(base.healthPercent + ajustes.salud),
    humidityPercent: tope(base.humidityPercent + ajustes.agua),
    lightPercent: tope(base.lightPercent + ajustes.luz),
    lastWatered: ajustes.regada ? 'Hace unos minutos' : base.lastWatered,
  }
}

// Aplica un cuidado a la planta mostrada. Devuelve { planta, mensaje }
export function aplicarCuidado(actual, accion) {
  const { delta } = ACCIONES[accion]
  const nombre = actual.name
  const campo = { REGAR: 'humidityPercent', SOL: 'lightPercent', ABONAR: 'healthPercent' }[accion]
  const completo = {
    REGAR: `${nombre} ya tiene toda el agua que necesita`,
    SOL: `${nombre} ya recibió toda la luz que necesita`,
    ABONAR: `${nombre} ya está en plena salud`,
  }[accion]
  if (actual[campo] >= 100) return { planta: actual, mensaje: completo }

  const nueva = { ...actual, [campo]: tope(actual[campo] + delta) }
  if (accion === 'REGAR') nueva.lastWatered = 'Hace unos minutos'

  const datos = leer()
  const indice = datos.plantas.findIndex((p) => p.name === nombre)
  if (indice >= 0) {
    datos.plantas[indice] = nueva
  } else {
    // Planta del servidor: se recuerda el aporte para reaplicarlo al recargar
    const a = datos.ajustes
    if (accion === 'REGAR') { a.agua += delta; a.regada = true }
    if (accion === 'SOL') a.luz += delta
    if (accion === 'ABONAR') a.salud += delta
  }
  guardar(datos)

  const mensaje = {
    REGAR: `Regaste a ${nombre}: +${delta}% de agua`,
    SOL: `${nombre} tomó sol: +${delta}% de luz`,
    ABONAR: `Abonaste a ${nombre}: +${delta}% de salud`,
  }[accion]
  return { planta: nueva, mensaje }
}
