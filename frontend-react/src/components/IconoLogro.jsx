// Iconos de logros: el backend manda una clave de texto y aqui se resuelve a un emoji.
// El admin puede crear claves nuevas: si llega una desconocida se muestra el icono por defecto.
export const ICONOS_LOGRO = {
  semilla: '🌱',
  hoja: '🍃',
  huayco: '🌊',
  copo: '❄️',
  sol: '☀️',
  bicho: '🐛',
  escudo: '🛡️',
  refugio: '🏠',
  corazon: '❤️',
  gota: '💧',
  abono: '🌿',
  luz: '💡',
  racha: '🔥',
  nivel: '⭐',
  tienda: '🛍️',
  maceta: '🪴',
  invernadero: '🏡',
  fondo: '🖼️',
  mision: '🎯',
  amanecer: '🌅',
  luna: '🌙',
  secreto: '🔒',
}

export const ICONO_POR_DEFECTO = '🏆'

export const emojiLogro = (clave) => ICONOS_LOGRO[String(clave || '').toLowerCase()] || ICONO_POR_DEFECTO

export default function IconoLogro({ clave, className = '' }) {
  return <span className={`icono-logro ${className}`} aria-hidden="true">{emojiLogro(clave)}</span>
}
