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

// POST /plantas/{id}/prevenir -> pone a resguardo a esa planta del evento activo (409 si no hay evento o ya murio)
export const prevenirPlanta = (id) => llamar(`/plantas/${id}/prevenir`, { metodo: 'POST' })

// POST /plantas/{id}/revivir -> revive una planta muerta a cambio de hojas (402 si no alcanzan)
export const revivirPlanta = (id) => llamar(`/plantas/${id}/revivir`, { metodo: 'POST' })

// POST /plantas/{id}/golpe-visto -> marca como vista la animacion del evento que golpeo a la planta
export const golpeVisto = (id) => llamar(`/plantas/${id}/golpe-visto`, { metodo: 'POST' })

// POST /plantas/{id}/baul y /sacar-del-baul -> la planta queda congelada (409 si esta golpeada) o vuelve (409 si no hay lugar)
export const guardarEnBaul = (id) => llamar(`/plantas/${id}/baul`, { metodo: 'POST' })
export const sacarDelBaul = (id) => llamar(`/plantas/${id}/sacar-del-baul`, { metodo: 'POST' })

// PUT /plantas/{id}/apodo { apodo } -> vacio vuelve al nombre de la especie (maximo 50 letras)
export const renombrarPlanta = (id, apodo) => llamar(`/plantas/${id}/apodo`, { metodo: 'PUT', cuerpo: { apodo } })

// PUT /plantas/macetas { idItem, ids } -> pone la maceta comprada a las plantas elegidas
export const ponerMaceta = (idItem, idPlantas) => llamar('/plantas/macetas', { metodo: 'PUT', cuerpo: { idItem, ids: idPlantas } })
