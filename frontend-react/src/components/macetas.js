// Macetas en pixel art: mismos colores y forma que la app Kotlin (MacetaPixel.kt).
export const COLUMNAS = 16
export const FILAS = 8
const ANCHO_POR_FILA = [16, 16, 14, 14, 13, 13, 12, 12]

const PALETAS = {
  AZUL: { cuerpo: '#4A7FC1', luz: '#7BA6DB', sombra: '#2F5A92' },
  CREMA: { cuerpo: '#EBD9B0', luz: '#F7EBCB', sombra: '#C9B27E' },
  ROSA: { cuerpo: '#E88FB0', luz: '#F4B6CE', sombra: '#C1668A' },
  DORADA: { cuerpo: '#D9A441', luz: '#F0C873', sombra: '#A97A22' },
  TERRACOTA: { cuerpo: '#C46A3C', luz: '#DE8A5C', sombra: '#9A4C26' },
}

export const paletaDeMaceta = (codigo) => PALETAS[codigo] || PALETAS.TERRACOTA

// Celdas { x, y, color } de la maceta en una cuadricula de 16 x 8
export function celdasDeMaceta(codigo) {
  const paleta = paletaDeMaceta(codigo)
  const celdas = []
  for (let fila = 0; fila < FILAS; fila++) {
    const ancho = ANCHO_POR_FILA[fila]
    const izquierda = (COLUMNAS - ancho) / 2
    for (let col = 0; col < ancho; col++) {
      let color = paleta.cuerpo
      if (fila === 0) color = paleta.luz
      else if (col === 0 && fila > 1) color = paleta.luz
      else if (col >= ancho - 2 && fila > 1) color = paleta.sombra
      else if (fila === FILAS - 1) color = paleta.sombra
      celdas.push({ x: izquierda + col, y: fila, color })
    }
  }
  return celdas
}
