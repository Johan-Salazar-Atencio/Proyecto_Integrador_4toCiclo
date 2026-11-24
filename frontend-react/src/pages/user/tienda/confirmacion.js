// Textos de la confirmacion de compra (QA-64, como en el movil): antes "Comprar" cobraba al primer clic.
// El abono se compra y se aplica a la planta del jardin en el mismo paso.
export function textosCompra(item) {
  if (!item) return null
  const abono = item.tipo === 'ABONO'
  return {
    titulo: abono ? `¿Usar ${item.nombre} en tu planta?` : `¿Comprar ${item.nombre}?`,
    detalle: `Se descuentan ${item.precio} hojas de tu saldo.${abono ? ' Máximo un abono al día por planta.' : ''}`,
    boton: abono ? 'Usar abono' : 'Comprar',
  }
}
