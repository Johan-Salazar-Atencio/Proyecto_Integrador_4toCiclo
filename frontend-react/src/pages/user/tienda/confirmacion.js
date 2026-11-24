// Textos de la confirmacion de compra (QA-64, como en el movil): antes "Comprar" cobraba al primer clic.
// El abono se compra y se suma al contador; se usa despues desde el jardin.
export function textosCompra(item) {
  if (!item) return null
  const abono = item.tipo === 'ABONO'
  return {
    titulo: `¿Comprar ${item.nombre}?`,
    detalle: `Se descuentan ${item.precio} hojas de tu saldo.${abono ? ' Se suma a tus abonos; lo usas desde tu jardín (máximo uno al día por planta).' : ''}`,
    boton: 'Comprar',
  }
}
