import { cabecerasAdmin } from './auth'

// Tienda del admin (tabla items_tienda). "Quitar" un producto = desactivarlo: no se borra, asi quienes
// ya lo compraron lo conservan y se puede reactivar.
//   GET   /django/api/admin/tienda/        { items: [...], fondos_disponibles: [{ id_fondo, nombre, momento }] }
//   POST  /django/api/admin/tienda/        agrega un fondo a la venta
//   PATCH /django/api/admin/tienda/{id}/   cambia nombre, descripcion, precio, nivel o activo

async function pedir(ruta, opciones = {}) {
  const respuesta = await fetch(`/django/api/admin/tienda/${ruta}`, {
    ...opciones,
    headers: { 'Content-Type': 'application/json', ...cabecerasAdmin() },
  })
  const texto = await respuesta.text()
  const datos = texto ? JSON.parse(texto) : null
  if (!respuesta.ok) throw new Error(datos?.error || `Error ${respuesta.status}`)
  return datos
}

export const listarProductos = () => pedir('')

export const agregarFondo = (datos) => pedir('', { method: 'POST', body: JSON.stringify(datos) })

export const editarProducto = (id, cambios) => pedir(`${id}/`, { method: 'PATCH', body: JSON.stringify(cambios) })

export const cambiarEstadoProducto = (id, activo) => editarProducto(id, { activo })
