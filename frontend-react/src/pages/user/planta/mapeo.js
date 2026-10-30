// Del JSON del escaneo (ScanResultDto) a lo que muestra la ficha. Mismas reglas que PlantDetailScreen.kt.

const NOMBRE_DESCONOCIDA = 'planta desconocida'
const TIPOS_DIAGNOSTICO = ['PLAGAS', 'TOXICIDAD', 'OTRO']
const PRIORIDAD = ['RIEGO', 'LUZ', 'TEMPERATURA', 'SUSTRATO', 'PODA', 'ABONO']

const ESTILOS = {
  RIEGO: { icono: '💧', texto: 'Riego', fondo: 'var(--azul-claro)' },
  LUZ: { icono: '☀️', texto: 'Luz solar', fondo: 'var(--ambar-claro)' },
  TEMPERATURA: { icono: '🌡️', texto: 'Clima', fondo: 'var(--rojo-claro)' },
  SUSTRATO: { icono: '🌱', texto: 'Sustrato', fondo: 'var(--verde-claro)' },
  PODA: { icono: '✂️', texto: 'Poda', fondo: 'var(--verde-claro)' },
  ABONO: { icono: '🍃', texto: 'Abono', fondo: 'var(--verde-claro)' },
}
const ESTILO_GENERICO = { icono: 'ℹ️', texto: 'Cuidado', fondo: 'var(--fondo)' }

export const estiloDe = (tipo) => ESTILOS[tipo?.toUpperCase()] || ESTILO_GENERICO

// Spring responde "Planta desconocida" si la IA no responde; la IA puede contestar "No identificado..."
export function identificacionFallida(r) {
  const nombre = r.nombreComun?.trim().toLowerCase() || ''
  return !nombre || nombre === NOMBRE_DESCONOCIDA || nombre.startsWith('no identificado')
}

// Tarjetas de cuidado: todo lo que no es advertencia, con lo basico primero
export function tarjetasCuidado(r) {
  const rango = (tipo) => {
    const i = PRIORIDAD.indexOf(tipo?.toUpperCase())
    return i === -1 ? PRIORIDAD.length : i
  }
  return (r.cuidados || [])
    .filter((c) => !TIPOS_DIAGNOSTICO.includes(c.tipo?.toUpperCase()) && c.contenido?.trim())
    .sort((a, b) => rango(a.tipo) - rango(b.tipo) || a.orden - b.orden)
}

// Plagas, toxicidad y otros, concatenados; null si no hay
export function textoDiagnostico(r) {
  const textos = (r.cuidados || [])
    .filter((c) => TIPOS_DIAGNOSTICO.includes(c.tipo?.toUpperCase()) && c.contenido?.trim())
    .sort((a, b) => a.orden - b.orden)
    .map((c) => c.contenido.trim())
  return textos.length ? textos.join('\n\n') : null
}

// `confianza` llega en escala 0..100
export function etiquetaPrecision(r) {
  return r.confianza > 0 ? `IA Identificado ${Math.round(Math.min(100, r.confianza))}% Precisión` : null
}
