import { etiquetaTipo } from '../../../services/admin/eventos'
import { urlImagen } from '../../../services/home'

// Severidades del desastre con el color de la referencia (baja = verde, moderada = ambar, critica = rojo)
export const SEV = {
  BAJO: { texto: 'Baja', color: 'var(--ok)', clase: '' },
  MODERADO: { texto: 'Moderada', color: 'var(--luz)', clase: 'm' },
  CRITICO: { texto: 'Crítica', color: 'var(--peligro)', clase: 'k' },
}

export const tipoDe = (tipos, codigo) => tipos.find((t) => t.codigo === codigo)
export const nombreDe = (tipos, codigo) => tipoDe(tipos, codigo)?.nombre || etiquetaTipo(codigo)
export const imagenDe = (tipo) => urlImagen(tipo?.imagen_url)

// Minutos -> "45 min", "2 h", "1 h 30 min"
export function textoMinutos(min) {
  const h = Math.floor(min / 60)
  const m = min % 60
  if (h === 0) return `${m} min`
  return m === 0 ? `${h} h` : `${h} h ${m} min`
}
