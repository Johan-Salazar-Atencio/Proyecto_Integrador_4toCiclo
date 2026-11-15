import { llamar } from './api'

// "Mi jardin" contra Spring (/api/v1/plantas). Antes se simulaba en localStorage
// (store/jardinSimulado.js) y lo que se cuidaba en la web no existia en el movil.

// GET /plantas/activa -> planta que se muestra en el jardin (null si no hay: 204)
export const obtenerPlantaJardin = () => llamar('/plantas/activa')

// GET /plantas -> todas las plantas digitales del usuario
export const listarPlantasDigitales = () => llamar('/plantas')

// POST /plantas -> digitaliza desde un escaneo propio o desde una especie de la coleccion
export function digitalizarPlanta(resultado) {
  const cuerpo = resultado.idEscaneo > 0
    ? { idEscaneo: resultado.idEscaneo }
    : { idCatalogo: resultado.idCatalogo }
  if (resultado.apariencia) cuerpo.apariencia = resultado.apariencia
  return llamar('/plantas', { metodo: 'POST', cuerpo })
}

// POST /plantas/{id}/cuidados { accion: REGAR | SOL | ABONAR }
export const cuidarPlanta = (id, accion) => llamar(`/plantas/${id}/cuidados`, { metodo: 'POST', cuerpo: { accion } })

// POST /plantas/{id}/seleccionar -> pasa a ser la planta del jardin
export const seleccionarPlanta = (id) => llamar(`/plantas/${id}/seleccionar`, { metodo: 'POST' })
