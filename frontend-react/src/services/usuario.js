import { llamar } from './api'

// Datos del usuario contra Spring (/api/v1/usuarios/me). Cada cambio devuelve el resumen actualizado.

// PUT /usuarios/me/nombre { nombre } -> de 2 a 100 letras
export const cambiarNombre = (nombre) => llamar('/usuarios/me/nombre', { metodo: 'PUT', cuerpo: { nombre } })

// GET /regiones y PUT /usuarios/me { region } -> decide que desastres le llegan
export const listarRegiones = () => llamar('/regiones')
export const cambiarRegion = (region) => llamar('/usuarios/me', { metodo: 'PUT', cuerpo: { region } })

// PUT /usuarios/me/invernadero { idItem } -> invernadero de la coleccion virtual (409 si todavia no lo compro)
export const elegirInvernadero = (idItem) => llamar('/usuarios/me/invernadero', { metodo: 'PUT', cuerpo: { idItem } })

// POST /usuarios/me/foto (multipart, campo "foto") -> JPG, PNG o WEBP
export function subirFoto(archivo) {
  const datos = new FormData()
  datos.append('foto', archivo)
  return llamar('/usuarios/me/foto', { metodo: 'POST', formData: datos })
}
