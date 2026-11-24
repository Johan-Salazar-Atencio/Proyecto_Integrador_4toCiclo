import { cabecerasAdmin } from './auth'
import { errorDeRespuesta } from './respuestas.js'

// Eventos climaticos (tabla eventos_desastre).
// Lectura:  GET /django/api/catalog/eventos/  (real)
// Escritura: POST /django/api/admin/eventos/  (crea ACTIVO)
//            PATCH /django/api/admin/eventos/{id}/  { estado: "FINALIZADO" }

// Las paginas lo emiten en window cuando lanzan o cancelan un desastre, para refrescar la insignia del menu
export const AVISO_EVENTOS = 'plantia:eventos'

export const TIPOS = [
  { valor: 'HUAYCO', icono: '🌊', texto: 'Huayco / Lodazal', mensaje: '¡Alerta de huayco! Se pronostican lluvias torrenciales. Resguarda tus plantas en el interior inmediatamente.' },
  { valor: 'HELADA', icono: '❄️', texto: 'Helada nocturna', mensaje: 'Helada esta noche: acerca tus plantas a una ventana protegida y evita el riego al atardecer.' },
  { valor: 'OLA_DE_CALOR', icono: '☀️', texto: 'Ola de calor', mensaje: 'Ola de calor en tu zona: mueve tus plantas a la sombra y riégalas temprano.' },
  { valor: 'PLAGA', icono: '🐛', texto: 'Plaga de pulgones', mensaje: 'Se reportan pulgones en tu región: revisa el envés de las hojas y aísla las plantas afectadas.' },
]

export const SEVERIDADES = [
  { valor: 'BAJO', texto: 'Bajo', detalle: 'Daño leve en puntas foliares.' },
  { valor: 'MODERADO', texto: 'Moderado', detalle: 'Pérdida de flores y brotes.' },
  { valor: 'CRITICO', texto: 'Crítico', detalle: 'Peligro inminente de pérdida total si no se resguarda a tiempo.' },
]

// Las regiones salen de la tabla regiones (las mismas que el usuario elige en su perfil)
const REGION_GLOBAL = { codigo: 'GLOBAL', nombre: 'Global' }

export async function listarRegiones() {
  try {
    const respuesta = await fetch('/django/api/catalog/regiones/')
    if (!respuesta.ok) return [REGION_GLOBAL]
    const regiones = await respuesta.json()
    return regiones.length > 0 ? regiones : [REGION_GLOBAL]
  } catch {
    return [REGION_GLOBAL]
  }
}

export const etiquetaTipo = (valor) => TIPOS.find((t) => t.valor === valor)?.texto || valor
export const iconoTipo = (valor) => TIPOS.find((t) => t.valor === valor)?.icono || '⚠️'

export async function listarEventos() {
  const respuesta = await fetch('/django/api/catalog/eventos/')
  if (!respuesta.ok) throw new Error(`Django respondio ${respuesta.status}`)
  const eventos = await respuesta.json()
  return eventos
    .sort((a, b) => new Date(b.fecha_inicio) - new Date(a.fecha_inicio))
}

// REAL: crea el evento en la BD (estado ACTIVO). Spring lo muestra al movil/web usuario
// automaticamente via /api/v1/eventos/activo.
export async function crearEvento({ tipo, region, severidad, mensaje, horas, minutos }) {
  const inicio = new Date()
  const fin = new Date(inicio.getTime() + (horas * 60 + minutos) * 60000)
  const respuesta = await fetch('/django/api/admin/eventos/', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...cabecerasAdmin() },
    body: JSON.stringify({
      tipo_evento: tipo,
      region,
      severidad,
      mensaje_notificacion: mensaje,
      fecha_inicio: inicio.toISOString(),
      fecha_fin: fin.toISOString(),
    }),
  })
  if (!respuesta.ok) throw await errorDeRespuesta(respuesta) // {"error"} de Django, no el JSON crudo
  return await respuesta.json()
}

// REAL: cancela el evento: termina ya y SIN danos para nadie (Django lo deja FINALIZADO y procesado, QA-07).
export async function cancelarEvento(id) {
  const respuesta = await fetch(`/django/api/admin/eventos/${id}/`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...cabecerasAdmin() },
    body: JSON.stringify({ estado: 'FINALIZADO' }),
  })
  if (!respuesta.ok) throw await errorDeRespuesta(respuesta) // {"error"} de Django, no el JSON crudo
  return await respuesta.json()
}

// REAL: que le paso a cada planta cuando termino el desastre (registros_desastre_usuario).
// GET /django/api/admin/eventos/{id}/efectos/  ->  { resumen: {...}, registros: [...] }
export async function efectosEvento(id) {
  const respuesta = await fetch(`/django/api/admin/eventos/${id}/efectos/`, { headers: cabecerasAdmin() })
  if (!respuesta.ok) throw await errorDeRespuesta(respuesta) // {"error"} de Django, no el JSON crudo
  return await respuesta.json()
}
