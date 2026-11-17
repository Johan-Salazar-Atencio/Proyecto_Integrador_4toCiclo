import { cabecerasAdmin } from './auth'

// Tipos de evento (huayco, helada, ola de calor, plaga...) contra Django.
//   GET   /django/api/admin/tipos-desastre/        [{ id_tipo_desastre, codigo, nombre, descripcion, consejo, imagen_url,
//                                                     color_hex, activo, salud_final_bajo, salud_final_moderado, salud_final_critico }]
//   PATCH /django/api/admin/tipos-desastre/{id}/   texto, consejo, salud final y, como archivo, `imagen` (JPG, PNG o WebP de 400 px o mas)

async function leer(respuesta) {
  const texto = await respuesta.text()
  let datos = null
  try { datos = texto ? JSON.parse(texto) : null } catch { /* respuesta que no es JSON */ }
  if (!respuesta.ok) throw new Error(datos?.error || `Error ${respuesta.status}`)
  return datos
}

export const listarTiposEvento = async () => leer(await fetch('/django/api/admin/tipos-desastre/', { headers: cabecerasAdmin() }))

// Con imagen viaja como formulario con archivo; sin ella, como JSON
export async function guardarTipoEvento(id, cambios, imagen) {
  if (imagen) {
    const datos = new FormData()
    Object.entries(cambios).forEach(([campo, valor]) => datos.append(campo, valor ?? ''))
    datos.append('imagen', imagen)
    return leer(await fetch(`/django/api/admin/tipos-desastre/${id}/`, { method: 'PATCH', headers: cabecerasAdmin(), body: datos }))
  }
  return leer(await fetch(`/django/api/admin/tipos-desastre/${id}/`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...cabecerasAdmin() },
    body: JSON.stringify(cambios),
  }))
}
