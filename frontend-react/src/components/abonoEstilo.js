// Color y dibujo de cada abono segun su codigo: los mismos del movil (AbonoEstilo.kt).
// El servidor manda la misma imagen para los tres, asi que el color se resuelve aqui.
const ESTILOS = {
  ABONO_MEDIO: { color: '#7CB518', hojas: 1 }, // suave: lima
  ABONO_1: { color: '#9A6B3F', hojas: 2 }, // organico: tierra
  ABONO_3: { color: '#9333EA', hojas: 3 }, // reforzado: violeta
}

// Un codigo desconocido se ve como el suave (lima), igual que en el movil
export const estiloAbono = (codigo) => ESTILOS[codigo] || ESTILOS.ABONO_MEDIO

// Suma de abonos para el aviso del servidor: "Tienes 3"
export const textoTienes = (cantidad) => `Tienes ${cantidad || 0}`

// Pasa de la respuesta de la tienda al inventario: solo los abonos, del mas barato al mas caro
export const ordenarAbonos = (lista) => [...(lista || [])].sort((a, b) => a.precio - b.precio)
