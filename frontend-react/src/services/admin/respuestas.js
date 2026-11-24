// Logica pura del panel admin (sin fetch ni almacenamiento): se prueba con `npm test` (QA-33).

// Django responde los errores como {"error": "..."}: ese texto es el que ve el admin, nunca el JSON crudo
export async function errorDeRespuesta(respuesta) {
  let mensaje = ''
  try { mensaje = (await respuesta.json())?.error } catch { /* sin cuerpo JSON */ }
  return new Error(typeof mensaje === 'string' && mensaje ? mensaje : `Error ${respuesta.status}`)
}

// Solo los campos de la especie que cambiaron en el formulario (Django valida tipo y rango de cada uno)
export function cambiosDeEspecie(form, especie, campos) {
  return Object.fromEntries(campos.filter((c) => form[c] !== especie[c]).map((c) => [c, form[c]]))
}

// Cuerpo del POST de una especie nueva: nombres recortados y sin imagen si se deja vacia (Django pone la de defecto)
export function cuerpoEspecieNueva(form) {
  const cuerpo = {
    nombreComun: String(form.nombreComun ?? '').trim(),
    nombreCientifico: String(form.nombreCientifico ?? '').trim(),
    riegoDias: form.riegoDias,
    solHoras: form.solHoras,
    tempMin: form.tempMin,
    tempMax: form.tempMax,
    toxica: Boolean(form.toxica),
    publicada: Boolean(form.publicada),
  }
  const imagen = String(form.imagen ?? '').trim()
  if (imagen) cuerpo.imagen = imagen
  return cuerpo
}

// Texto de la confirmacion antes de cancelar una emergencia (QA-07)
export const CONFIRMAR_CANCELAR = '¿Cancelar esta emergencia?\n\nTermina ahora mismo y ninguna planta recibe daño ni premio. '
  + 'Las plantas que se habían prevenido quedan sin protección. No se puede deshacer.'
