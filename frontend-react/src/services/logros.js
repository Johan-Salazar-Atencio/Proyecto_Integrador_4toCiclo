import { llamar } from './api'

// Logros del usuario contra Spring (/api/v1/logros).
//   GET  /logros             -> { total, desbloqueados, logros: [{ idLogro, codigo, titulo, descripcion, icono, rareza, meta,
//                                progreso, desbloqueado, desbloqueadoEn, secreto, recompensaXp, recompensaMonedas }] }
//   GET  /logros/pendientes  -> [{ idLogro, codigo, titulo, descripcion, icono, rareza, recompensaXp, recompensaMonedas }]
//   POST /logros/vistos      { ids: [...] } -> 204
// Los secretos bloqueados llegan sin datos reales: la pagina solo muestra "???".

const MOCK = import.meta.env.VITE_MOCK_LOGROS === '1' // MOCK: se quita junto con services/mockLogros.js

export const obtenerLogros = () => (MOCK ? import('./mockLogros').then((m) => m.usuarioMock()) : llamar('/logros'))

export const obtenerPendientes = () => (MOCK ? import('./mockLogros').then((m) => m.pendientesMock()) : llamar('/logros/pendientes'))

// El servidor acepta como maximo 100 ids por llamada: si hay mas se envian en lotes, uno tras otro
const LOTE_VISTOS = 100
export async function marcarVistos(ids) {
  for (let i = 0; i < ids.length; i += LOTE_VISTOS) {
    const lote = ids.slice(i, i + LOTE_VISTOS)
    await (MOCK ? Promise.resolve(null) : llamar('/logros/vistos', { metodo: 'POST', cuerpo: { ids: lote } }))
  }
  return null
}

export const RAREZAS = [
  { valor: 'COMUN', texto: 'Común' },
  { valor: 'RARA', texto: 'Rara' },
  { valor: 'EPICA', texto: 'Épica' },
  { valor: 'LEGENDARIA', texto: 'Legendaria' },
]
export const textoRareza = (valor) => RAREZAS.find((r) => r.valor === valor)?.texto || valor
