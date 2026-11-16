// Preferencias de este navegador: apariencia (Sistema, Claro u Oscuro) y recordatorio diario.
const CLAVE_TEMA = 'plantia.tema'
const CLAVE_RECORDATORIO = 'plantia.recordatorio'
const CLAVE_ULTIMO_AVISO = 'plantia.recordatorio.ultimo'

export const TEMAS = ['SISTEMA', 'CLARO', 'OSCURO']
export const HORA_RECORDATORIO = 9

function leer(clave) {
  try { return localStorage.getItem(clave) } catch { return null }
}
function escribir(clave, valor) {
  try {
    if (valor == null) localStorage.removeItem(clave)
    else localStorage.setItem(clave, valor)
  } catch { /* sin almacenamiento: la preferencia dura hasta recargar */ }
}

export const leerTema = () => (TEMAS.includes(leer(CLAVE_TEMA)) ? leer(CLAVE_TEMA) : 'SISTEMA')

// Aplica el tema al documento: SISTEMA sigue al navegador; CLARO u OSCURO se quedan fijos
export function aplicarTema(tema = leerTema()) {
  if (tema === 'SISTEMA') document.documentElement.removeAttribute('data-tema')
  else document.documentElement.setAttribute('data-tema', tema.toLowerCase())
}

export function guardarTema(tema) {
  escribir(CLAVE_TEMA, tema)
  aplicarTema(tema)
}

export const recordatorioActivo = () => leer(CLAVE_RECORDATORIO) === '1'
export const guardarRecordatorio = (activo) => escribir(CLAVE_RECORDATORIO, activo ? '1' : null)

const hoy = () => new Date().toISOString().slice(0, 10)

// Muestra el aviso diario una sola vez por dia, a partir de las 9:00, mientras la web esta abierta
export function avisarSiToca(ahora = new Date()) {
  if (!recordatorioActivo() || typeof Notification === 'undefined' || Notification.permission !== 'granted') return false
  if (ahora.getHours() < HORA_RECORDATORIO || leer(CLAVE_ULTIMO_AVISO) === hoy()) return false
  escribir(CLAVE_ULTIMO_AVISO, hoy())
  new Notification('Tus plantas te esperan', { body: 'Riégalas, dales luz y completa tu misión del día.' })
  return true
}
