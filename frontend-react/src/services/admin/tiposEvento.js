import { cabecerasAdmin } from './auth.js'

// Tipos de evento (huayco, helada, ola de calor, plaga...) contra Django.
//   GET   /django/api/admin/tipos-desastre/        [{ id_tipo_desastre, codigo, nombre, descripcion, consejo, imagen_url,
//                                                     color_hex, activo, region_defecto, duracion_defecto_min, mensaje_push,
//                                                     salud_final_bajo, salud_final_moderado, salud_final_critico }]
//   POST  /django/api/admin/tipos-desastre/        crea un tipo (nombre obligatorio; el código sale del nombre) -> 201 con el tipo
//   PATCH /django/api/admin/tipos-desastre/{id}/   cualquiera de esos campos (incluido `activo`) y, como archivo, `imagen`
//                                                  (JPG, PNG o WebP de 400 px o más)

async function leer(respuesta) {
  const texto = await respuesta.text()
  let datos = null
  try { datos = texto ? JSON.parse(texto) : null } catch { /* respuesta que no es JSON */ }
  if (!respuesta.ok) throw new Error(datos?.error || `Error ${respuesta.status}`)
  return datos
}

const RUTA = '/django/api/admin/tipos-desastre/'

// Con imagen viaja como formulario con archivo; sin ella, como JSON
export function peticionTipo(metodo, ruta, cambios, imagen) {
  if (imagen) {
    const datos = new FormData()
    Object.entries(cambios).forEach(([campo, valor]) => datos.append(campo, valor ?? ''))
    datos.append('imagen', imagen)
    return [ruta, { method: metodo, headers: cabecerasAdmin(), body: datos }]
  }
  return [ruta, { method: metodo, headers: { 'Content-Type': 'application/json', ...cabecerasAdmin() }, body: JSON.stringify(cambios) }]
}

export const listarTiposEvento = async () => leer(await fetch(RUTA, { headers: cabecerasAdmin() }))

export const guardarTipoEvento = async (id, cambios, imagen) => leer(await fetch(...peticionTipo('PATCH', `${RUTA}${id}/`, cambios, imagen)))

export const crearTipoEvento = async (datos, imagen) => leer(await fetch(...peticionTipo('POST', RUTA, datos, imagen)))

// Interruptor de la tarjeta: solo cambia `activo`
export const cambiarActivoTipo = (id, activo) => guardarTipoEvento(id, { activo }, null)
