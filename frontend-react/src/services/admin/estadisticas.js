import { llamar } from '../api'

// Datos del Dashboard. Dos tarjetas son REALES (Django, solo lectura) y el resto es de EJEMPLO
// porque Django aun no tiene endpoints de estadisticas.

const leerLista = async (ruta) => {
  const respuesta = await fetch(`/django${ruta}`)
  if (!respuesta.ok) throw new Error(`Django respondio ${respuesta.status}`)
  const datos = await respuesta.json()
  return Array.isArray(datos) ? datos : datos.results || []
}

// REAL: GET /api/catalog/ (especies) y GET /api/catalog/eventos/ (eventos), ambos de Django.
// Hoy /api/catalog/ responde 500 (AttributeError con `cuidadosseccion_set`, codigo de Django):
// si falla, las especies se leen del catalogo de Spring y se avisa en la tarjeta.
export async function obtenerResumen() {
  const [catalogo, eventos] = await Promise.allSettled([leerLista('/api/catalog/'), leerLista('/api/catalog/eventos/')])
  if (eventos.status === 'rejected') throw eventos.reason

  let especies = catalogo.status === 'fulfilled' ? catalogo.value : null
  let origenCatalogo = 'django'
  if (!especies) {
    especies = (await llamar('/catalogo')) || []
    origenCatalogo = 'spring'
  }
  const esToxica = (p) => p.es_toxica_mascotas ?? p.esToxicaMascotas

  return {
    especies: especies.length,
    toxicas: especies.filter(esToxica).length,
    origenCatalogo,
    eventosActivos: eventos.value.filter((e) => e.estado === 'ACTIVO').length,
    eventosTotales: eventos.value.length,
    // EJEMPLO
    usuarios: 12450,
    crecimientoUsuarios: 15,
    escaneosHoy: 3820,
    picoPorMinuto: 420,
  }
}

// EJEMPLO. TODO(django): GET /api/admin/actividad?dias=14 -> [{ fecha, usuarios, escaneos }]
export async function obtenerActividad() {
  const usuarios = [2100, 2350, 2280, 2600, 2900, 2750, 3050, 3300, 3180, 3420, 3380, 3250, 3600, 3720]
  const escaneos = [610, 720, 690, 840, 910, 880, 1020, 1150, 1090, 1290, 1210, 1170, 1340, 1420]
  const hoy = new Date()
  return usuarios.map((u, i) => {
    const fecha = new Date(hoy)
    fecha.setDate(hoy.getDate() - (usuarios.length - 1 - i))
    return { fecha, usuarios: u, escaneos: escaneos[i] }
  })
}

// EJEMPLO. TODO(django): GET /api/admin/escaneos/recientes?limit=5
//   -> [{ id, usuario, especie, familia, foto, confianza (0-100), hace, estado: VALIDADO | REVISION }]
export async function obtenerUltimosEscaneos() {
  return [
    { id: 1, usuario: 'Juan Pérez', especie: 'Monstera adansonii', familia: 'Araceae', confianza: 98.5, hace: 'Hace 3 min', estado: 'VALIDADO' },
    { id: 2, usuario: 'Elena Soto', especie: 'Monstera deliciosa', familia: 'Araceae', confianza: 96.2, hace: 'Hace 12 min', estado: 'VALIDADO' },
    { id: 3, usuario: 'Carlos Ruiz', especie: 'Echeveria elegans', familia: 'Crassulaceae', confianza: 94.0, hace: 'Hace 28 min', estado: 'VALIDADO' },
    { id: 4, usuario: 'Lucía Ramos', especie: 'Echinocactus grusonii', familia: 'Cactaceae', confianza: 88.4, hace: 'Hace 45 min', estado: 'REVISION' },
    { id: 5, usuario: 'Mateo Vega', especie: 'Passiflora edulis', familia: 'Passifloraceae', confianza: 71.3, hace: 'Hace 1 h', estado: 'REVISION' },
  ]
}
