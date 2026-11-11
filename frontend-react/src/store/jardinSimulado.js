// SIMULACION de las plantas digitales y sus cuidados (el backend aun no tiene endpoint para crearlas
// ni para regar / dar sol / abonar). Igual que MockGardenData.kt en el movil, pero se guarda en el
// navegador para sobrevivir a recargas.
// TODO(backend): reemplazar `digitalizar`, `activar` y `aplicarCuidado` por llamadas reales
// y borrar este archivo.
import { SALUD_MARCHITA } from '../pages/user/jardin/etapas'
import { obtenerUsuarioId } from './sesionGuardada'

export const ACCIONES = {
  REGAR: { texto: 'Regar', icono: '💧', pista: '+20%', delta: 20 },
  SOL: { texto: 'Sol', icono: '☀️', pista: '+15%', delta: 15 },
  ABONAR: { texto: 'Abonar', icono: '🌿', pista: '+10%', delta: 10 },
}

const VACIO = { plantas: [], activa: null, ajustes: { salud: 0, agua: 0, luz: 0, regada: false, crecimiento: 0 } }
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

// Desgaste por hora sin cuidados: el agua y la luz bajan solas, y si alguna queda en rojo
// la salud tambien baja. Con la salud bajo el 30% la planta se marchita (ver jardin/etapas.js).
const DESGASTE_HORA = { agua: 4, luz: 3, salud: 2 }
const NECESIDAD_CRITICA = 30
const MS_HORA = 3_600_000

function envejecer(planta, ahora = Date.now()) {
  const desde = planta.actualizado ?? ahora
  const horas = (ahora - desde) / MS_HORA
  if (horas <= 0) return { ...planta, actualizado: ahora }
  const humidityPercent = tope(Math.round(planta.humidityPercent - DESGASTE_HORA.agua * horas))
  const lightPercent = tope(Math.round(planta.lightPercent - DESGASTE_HORA.luz * horas))
  const descuidada = humidityPercent < NECESIDAD_CRITICA || lightPercent < NECESIDAD_CRITICA
  const healthPercent = descuidada
    ? tope(Math.round(planta.healthPercent - DESGASTE_HORA.salud * horas))
    : planta.healthPercent
  return { ...planta, humidityPercent, lightPercent, healthPercent, actualizado: ahora }
}

export const listarDigitales = () => leer().plantas

export function plantaActiva() {
  const datos = leer()
  const indice = datos.plantas.findIndex((p) => p.name === datos.activa)
  if (indice < 0) return null
  const planta = envejecer(datos.plantas[indice])
  datos.plantas[indice] = planta
  guardar(datos)
  return planta
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
    // Rasgos visuales que dio la IA (forma, colores, flor/fruto); null = dibujo por defecto
    apariencia: escaneo.apariencia || null,
    crecimiento: 0,
    actualizado: Date.now(),
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
    crecimiento: ajustes.crecimiento || 0,
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

  // Cada cuidado util hace crecer la planta, salvo si esta marchita: primero hay que recuperarla
  const crece = actual.healthPercent >= SALUD_MARCHITA
  const nueva = {
    ...actual,
    [campo]: tope(actual[campo] + delta),
    crecimiento: (actual.crecimiento || 0) + (crece ? 1 : 0),
    actualizado: Date.now(),
  }
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
    if (crece) a.crecimiento = (a.crecimiento || 0) + 1
  }
  guardar(datos)

  const mensaje = {
    REGAR: `Regaste a ${nombre}: +${delta}% de agua`,
    SOL: `${nombre} tomó sol: +${delta}% de luz`,
    ABONAR: `Abonaste a ${nombre}: +${delta}% de salud`,
  }[accion]
  return { planta: nueva, mensaje }
}
