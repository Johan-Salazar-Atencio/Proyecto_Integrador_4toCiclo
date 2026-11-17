import { cabecerasAdmin } from './auth'

// Tienda del admin (tabla items_tienda). "Quitar" un producto = desactivarlo: no se borra, asi quienes
// ya lo compraron lo conservan y se puede reactivar.
//   GET   /django/api/admin/tienda/        { items: [{ ..., variantes: [{ momento, vista, cuadros, ms }] }] }
//   POST  /django/api/admin/tienda/        agrega un fondo (multipart: nombre, descripcion, precio_monedas,
//                                          nivel_requerido y las imagenes dia, tarde y noche)
//   PATCH /django/api/admin/tienda/{id}/   cambia nombre, descripcion, precio, nivel o activo (JSON); con imagenes
//                                          nuevas va en multipart y reemplaza las escenas que se envien
//
// Cada imagen puede ser un GIF/WebP/APNG animado, o una tira PNG/JPG indicando cuantos cuadros tiene
// (cuadros_dia, cuadros_tarde, cuadros_noche). El servidor la convierte a la tira que dibuja el jardin.

export const MOMENTOS_FONDO = [
  { clave: 'dia', momento: 'DIA', icono: '☀️', texto: 'Día' },
  { clave: 'tarde', momento: 'TARDE', icono: '🌇', texto: 'Tarde' },
  { clave: 'noche', momento: 'NOCHE', icono: '🌙', texto: 'Noche' },
]

async function pedir(ruta, opciones = {}) {
  const { cuerpo, metodo = 'GET' } = opciones
  const esFormulario = cuerpo instanceof FormData
  const cabeceras = { ...cabecerasAdmin() }
  // Con FormData el navegador pone el Content-Type (con el limite de las partes)
  if (!esFormulario && cuerpo !== undefined) cabeceras['Content-Type'] = 'application/json'
  const respuesta = await fetch(`/django/api/admin/tienda/${ruta}`, {
    method: metodo,
    headers: cabeceras,
    body: cuerpo === undefined ? undefined : esFormulario ? cuerpo : JSON.stringify(cuerpo),
  })
  const texto = await respuesta.text()
  let datos = null
  try { datos = texto ? JSON.parse(texto) : null } catch { /* respuesta que no es JSON */ }
  if (!respuesta.ok) throw new Error(datos?.error || `Error ${respuesta.status}`)
  return datos
}

export const listarProductos = () => pedir('')

export const agregarFondo = (formulario) => pedir('', { metodo: 'POST', cuerpo: formulario })

export const editarProducto = (id, cambios) => pedir(`${id}/`, { metodo: 'PATCH', cuerpo: cambios })

export const cambiarEstadoProducto = (id, activo) => editarProducto(id, { activo })
