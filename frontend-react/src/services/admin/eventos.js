import { obtenerAdminId } from './auth'

// Eventos climaticos (tabla eventos_desastre).
// Lectura:  GET /django/api/catalog/eventos/  (real)
// Escritura: POST /django/api/admin/eventos/  (crea ACTIVO)
//            PATCH /django/api/admin/eventos/{id}/  { estado: "FINALIZADO" }

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

export const REGIONES = ['GLOBAL', 'Arequipa', 'Lima Metropolitana', 'Cusco y Altiplano', 'Costa norte', 'Selva central']

export const etiquetaTipo = (valor) => TIPOS.find((t) => t.valor === valor)?.texto || valor
export const iconoTipo = (valor) => TIPOS.find((t) => t.valor === valor)?.icono || '⚠️'

export async function listarEventos() {
  const respuesta = await fetch('/django/api/catalog/eventos/')
  if (!respuesta.ok) throw new Error(`Django respondio ${respuesta.status}`)
  const reales = await respuesta.json()
  return reales
    .map((e) => ({ ...e, simulado: false }))
    .sort((a, b) => new Date(b.fecha_inicio) - new Date(a.fecha_inicio))
}

// REAL: crea el evento en la BD (estado ACTIVO). Spring lo muestra al movil/web usuario
// automaticamente via /api/v1/eventos/activo.
export async function crearEvento({ tipo, region, severidad, mensaje, horas, minutos }) {
  const adminId = obtenerAdminId()
  const inicio = new Date()
  const fin = new Date(inicio.getTime() + (horas * 60 + minutos) * 60000)
  const respuesta = await fetch('/django/api/admin/eventos/', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Admin-Id': String(adminId ?? '') },
    body: JSON.stringify({
      tipo_evento: tipo,
      region,
      severidad,
      mensaje_notificacion: mensaje,
      fecha_inicio: inicio.toISOString(),
      fecha_fin: fin.toISOString(),
    }),
  })
  if (!respuesta.ok) {
    const texto = await respuesta.text()
    throw new Error(texto || `Error ${respuesta.status}`)
  }
  return await respuesta.json()
}

// REAL: marca el evento como FINALIZADO en la BD.
export async function cancelarEvento(id) {
  const adminId = obtenerAdminId()
  const respuesta = await fetch(`/django/api/admin/eventos/${id}/`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', 'X-Admin-Id': String(adminId ?? '') },
    body: JSON.stringify({ estado: 'FINALIZADO' }),
  })
  if (!respuesta.ok) {
    const texto = await respuesta.text()
    throw new Error(texto || `Error ${respuesta.status}`)
  }
  return await respuesta.json()
}
