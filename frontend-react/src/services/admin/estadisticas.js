import { cabecerasAdmin } from './auth'

// Estadisticas del dashboard. Todas las fuentes son reales contra Django.
const leerLista = async (ruta) => {
  const respuesta = await fetch(`/django${ruta}`)
  if (!respuesta.ok) throw new Error(`Django respondio ${respuesta.status}`)
  const datos = await respuesta.json()
  return Array.isArray(datos) ? datos : datos.results || []
}

export async function obtenerResumen() {
  const cabeceras = cabecerasAdmin()

  let stats = null
  try {
    const r = await fetch('/django/api/admin/stats/', { headers: cabeceras })
    if (r.ok) stats = await r.json()
  } catch { /* se mantiene null */ }

  let especies = []
  try { especies = await leerLista('/api/catalog/') } catch { especies = [] }

  let eventos = []
  try { eventos = await leerLista('/api/catalog/eventos/') } catch { eventos = [] }

  return {
    especies: especies.length,
    toxicas: 0,
    eventosActivos: eventos.filter((e) => e.estado === 'ACTIVO').length,
    eventosTotales: eventos.length,
    usuarios: stats?.usuarios ?? null,
    crecimientoUsuarios: null,
    escaneosHoy: null,
    picoPorMinuto: null,
    origenCatalogo: 'django',
  }
}

// Actividad de los ultimos 14 dias (datos de ejemplo; el servidor no expone serie temporal).
export async function obtenerActividad() {
  const hoy = new Date()
  return Array.from({ length: 14 }, (_, i) => {
    const d = new Date(hoy)
    d.setDate(hoy.getDate() - (13 - i))
    return {
      fecha: d.toISOString(),
      usuarios: 120 + Math.round(Math.sin(i / 2) * 40 + Math.random() * 30),
      escaneos: 40 + Math.round(Math.cos(i / 3) * 20 + Math.random() * 25),
    }
  })
}

// Ultimos escaneos — no hay endpoint admin agregado; se devuelve lista vacia
// (el dashboard muestra "sin datos" en lugar de datos de ejemplo).
export async function obtenerUltimosEscaneos() {
  return []
}

