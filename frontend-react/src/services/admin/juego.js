import { cabecerasAdmin } from './auth'

// Economia del juego contra Django (endpoints reales; el admin los ajusta y Spring los lee al momento).
//   GET   /django/api/admin/parametros/          [{ clave, valor, descripcion }]
//   PATCH /django/api/admin/parametros/{clave}/  { valor }                        (entero dentro de su rango)
//   GET   /django/api/admin/reglas/              [{ id_regla, motivo, nombre, xp, monedas, tope_diario, activa }]
//   PATCH /django/api/admin/reglas/{id}/         { xp, monedas, tope_diario, activa }

// Rango que acepta Django para cada parametro (el servidor lo vuelve a validar)
export const RANGOS_PARAMETROS = {
  COSTO_REVIVIR: { min: 0, max: 100000, texto: 'Costo de revivir una planta (hojas)' },
  HORAS_RECUPERACION: { min: 1, max: 720, texto: 'Horas para abonar a una planta golpeada' },
  ABONO_RECUPERACION: { min: 1, max: 100, texto: 'Salud que recupera con el abono (%)' },
  SALUD_REVIVIDA: { min: 1, max: 100, texto: 'Salud con la que vuelve una planta revivida (%)' },
  MAX_PLANTAS_ACTIVAS: { min: 6, max: 60, texto: 'Plantas activas por usuario (fuera del baúl)' },
}

async function pedir(ruta, opciones = {}) {
  const respuesta = await fetch(`/django/api/admin/${ruta}`, {
    ...opciones,
    headers: { 'Content-Type': 'application/json', ...cabecerasAdmin() },
  })
  const texto = await respuesta.text()
  let datos = null
  try { datos = texto ? JSON.parse(texto) : null } catch { /* respuesta que no es JSON */ }
  if (!respuesta.ok) throw new Error(datos?.error || `Error ${respuesta.status}`)
  return datos
}

export const listarParametros = () => pedir('parametros/')
export const cambiarParametro = (clave, valor) => pedir(`parametros/${clave}/`, { method: 'PATCH', body: JSON.stringify({ valor }) })
export const listarReglas = () => pedir('reglas/')
export const cambiarRegla = (id, cambios) => pedir(`reglas/${id}/`, { method: 'PATCH', body: JSON.stringify(cambios) })
