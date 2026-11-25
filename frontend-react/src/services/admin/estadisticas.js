import { cabecerasAdmin } from './auth'

// Estadisticas del dashboard. Todas las fuentes son reales contra Django.
const leerLista = async (ruta) => {
  const respuesta = await fetch(`/django${ruta}`)
  if (!respuesta.ok) throw new Error(`Django respondio ${respuesta.status}`)
  const datos = await respuesta.json()
  return Array.isArray(datos) ? datos : datos.results || []
}

// Todo sale de una sola consulta real a Django (/api/admin/stats/): usuarios, escaneos, eventos y actividad.
export async function obtenerEstadisticas() {
  const r = await fetch('/django/api/admin/stats/', { headers: cabecerasAdmin() })
  if (!r.ok) {
    const error = new Error(`Django respondio ${r.status}`)
    error.estado = r.status // 401 = sesion del admin caducada o invalida
    throw error
  }
  const d = await r.json()
  return {
    resumen: {
      especies: d.especies,
      toxicas: d.toxicas,
      eventosActivos: d.eventos_activos,
      eventosTotales: d.eventos_total,
      usuarios: d.usuarios,
      usuariosNuevosMes: d.usuarios_nuevos_mes,
      crecimientoUsuarios: d.crecimiento_usuarios,
      escaneosHoy: d.escaneos_hoy,
      picoPorMinuto: d.pico_por_minuto,
    },
    actividad: d.actividad,
    escaneos: d.ultimos_escaneos,
  }
}
