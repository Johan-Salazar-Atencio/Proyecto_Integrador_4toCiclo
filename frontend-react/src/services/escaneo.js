import { llamar } from './api'

const LADO_MAXIMO = 1280
const CALIDAD_JPEG = 0.85

// Reduce la foto antes de subirla (igual que el movil): lado mayor 1280 px, JPEG 85 %,
// respetando la orientacion EXIF. Si algo falla se sube la foto original.
export async function reducirFoto(archivo) {
  try {
    const imagen = await createImageBitmap(archivo, { imageOrientation: 'from-image' })
    const escala = Math.min(1, LADO_MAXIMO / Math.max(imagen.width, imagen.height))
    const lienzo = document.createElement('canvas')
    lienzo.width = Math.round(imagen.width * escala)
    lienzo.height = Math.round(imagen.height * escala)
    lienzo.getContext('2d').drawImage(imagen, 0, 0, lienzo.width, lienzo.height)
    const blob = await new Promise((ok) => lienzo.toBlob(ok, 'image/jpeg', CALIDAD_JPEG))
    return blob || archivo
  } catch {
    return archivo
  }
}

// POST /escaneos -> ScanResultDto { idEscaneo, nombreComun, nombreCientifico, confianza (0-100), imagenUrl, cuidados[] }
export async function escanear(archivo, senal) {
  const foto = await reducirFoto(archivo)
  const formData = new FormData()
  formData.append('foto', foto, 'foto.jpg')
  return llamar('/escaneos', { metodo: 'POST', formData, senal })
}
