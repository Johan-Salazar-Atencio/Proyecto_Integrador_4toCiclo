// Motivo por el que un producto apagado no se puede volver a poner a la venta (null = si se puede).
// Las macetas se bloqueaban siempre ("aun no estan disponibles", de antes de que existieran) y una maceta
// desactivada ya no se podia reactivar. Django solo rechaza activar un fondo sin sus escenas.
export function motivoSinVenta(producto) {
  if (!producto || producto.activo) return null
  if (producto.tipo === 'FONDO' && !(producto.variantes?.length)) return 'A este fondo le faltan sus escenas'
  return null
}
