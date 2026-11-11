// Etapas de crecimiento de la planta del jardin. `desde` = cuidados acumulados para llegar a ella.
// La ultima (marchita) no depende del crecimiento sino de la salud.
export const ETAPAS = [
  { nombre: 'Semilla', desde: 0 },
  { nombre: 'Brote', desde: 2 },
  { nombre: 'Planta joven', desde: 5 },
  { nombre: 'Planta adulta', desde: 9 },
  { nombre: 'Con frutos', desde: 14 },
  { nombre: 'Cosecha', desde: 20 },
]
export const ETAPA_MARCHITA = ETAPAS.length
export const NOMBRE_MARCHITA = 'Marchita'

// Por debajo de esta salud la planta se marchita (y deja de crecer hasta recuperarla)
export const SALUD_MARCHITA = 30

export const estaMarchita = (planta) => planta.healthPercent < SALUD_MARCHITA

function etapaPorCrecimiento(crecimiento = 0) {
  let i = 0
  while (i + 1 < ETAPAS.length && crecimiento >= ETAPAS[i + 1].desde) i++
  return i
}

// { indice, nombre, siguiente, faltan } — `siguiente` es null en la ultima etapa o si esta marchita
export function etapaDe(planta) {
  if (estaMarchita(planta)) {
    return { indice: ETAPA_MARCHITA, nombre: NOMBRE_MARCHITA, siguiente: null, faltan: 0 }
  }
  const crecimiento = planta.crecimiento || 0
  const indice = etapaPorCrecimiento(crecimiento)
  const siguiente = ETAPAS[indice + 1] || null
  return {
    indice,
    nombre: ETAPAS[indice].nombre,
    siguiente: siguiente?.nombre || null,
    faltan: siguiente ? siguiente.desde - crecimiento : 0,
  }
}
