// Eventos climaticos (tabla eventos_desastre). LEER es real (Django, solo lectura).
// LANZAR y CANCELAR estan SIMULADOS: se guardan en el navegador porque Django aun no tiene
// endpoints de escritura.
//
// TODO(django): reemplazar `crearEvento` y `cancelarEvento` por
//   POST  /django/api/admin/eventos
//         { tipo_evento, region, severidad, mensaje_notificacion, fecha_inicio, fecha_fin }  ->  201 + evento (estado ACTIVO)
//   PATCH /django/api/admin/eventos/{id_desastre}  { estado: "FINALIZADO" }  ->  200
// Spring ya lee el evento ACTIVO mas reciente en GET /api/v1/eventos/activo, asi que el movil y la
// web de usuario lo mostraran sin cambios cuando se guarde en la base.

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

const CLAVE = 'plantia.admin.eventos'
const leerLocal = () => {
  try {
    return JSON.parse(localStorage.getItem(CLAVE)) || { creados: [], cancelados: [] }
  } catch {
    return { creados: [], cancelados: [] }
  }
}
const guardarLocal = (datos) => {
  try { localStorage.setItem(CLAVE, JSON.stringify(datos)) } catch { /* sin almacenamiento */ }
}

// REAL: GET /api/catalog/eventos/ -> [{ id_desastre, tipo_evento, region, mensaje_notificacion, severidad, estado, fecha_inicio, fecha_fin }]
// Se le suman los eventos simulados y se aplican las cancelaciones simuladas.
export async function listarEventos() {
  const respuesta = await fetch('/django/api/catalog/eventos/')
  if (!respuesta.ok) throw new Error(`Django respondio ${respuesta.status}`)
  const reales = await respuesta.json()
  const { creados, cancelados } = leerLocal()
  return [...creados, ...reales]
    .map((e) => ({ ...e, simulado: e.id_desastre >= 1000, estado: cancelados.includes(e.id_desastre) ? 'FINALIZADO' : e.estado }))
    .sort((a, b) => new Date(b.fecha_inicio) - new Date(a.fecha_inicio))
}

// SIMULADO. `horas` y `minutos` forman la duracion; el evento empieza ahora.
export async function crearEvento({ tipo, region, severidad, mensaje, horas, minutos }) {
  await new Promise((ok) => setTimeout(ok, 700))
  const datos = leerLocal()
  const inicio = new Date()
  const fin = new Date(inicio.getTime() + (horas * 60 + minutos) * 60000)
  const evento = {
    id_desastre: 1000 + datos.creados.length + 1,
    tipo_evento: tipo,
    region,
    mensaje_notificacion: mensaje,
    severidad,
    estado: 'ACTIVO',
    fecha_inicio: inicio.toISOString(),
    fecha_fin: fin.toISOString(),
  }
  guardarLocal({ ...datos, creados: [evento, ...datos.creados] })
  return evento
}

// SIMULADO: marca el evento como FINALIZADO
export async function cancelarEvento(id) {
  await new Promise((ok) => setTimeout(ok, 400))
  const datos = leerLocal()
  guardarLocal({ ...datos, cancelados: [...new Set([...datos.cancelados, id])] })
}
