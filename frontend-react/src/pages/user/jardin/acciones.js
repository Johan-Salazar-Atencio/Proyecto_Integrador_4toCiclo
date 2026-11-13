// Cuidados de "Mi jardin". Los porcentajes los aplica Spring (PlantasService.cuidar);
// aqui solo estan los textos de los botones y de los avisos.
export const ACCIONES = {
  REGAR: { texto: 'Regar', icono: '💧', pista: '+20%', campo: 'humidityPercent' },
  SOL: { texto: 'Sol', icono: '☀️', pista: '+15%', campo: 'lightPercent' },
  ABONAR: { texto: 'Abonar', icono: '🌿', pista: '+10%', campo: 'healthPercent' },
}

// Aviso tras un cuidado: si ya estaba al 100 % no sirvio de nada
export function mensajeCuidado(antes, despues, accion) {
  const nombre = despues.name
  const { campo } = ACCIONES[accion]
  if (antes[campo] >= 100) {
    return {
      REGAR: `${nombre} ya tiene toda el agua que necesita`,
      SOL: `${nombre} ya recibió toda la luz que necesita`,
      ABONAR: `${nombre} ya está en plena salud`,
    }[accion]
  }
  const subio = despues[campo] - antes[campo]
  return {
    REGAR: `Regaste a ${nombre}: +${subio}% de agua`,
    SOL: `${nombre} tomó sol: +${subio}% de luz`,
    ABONAR: `Abonaste a ${nombre}: +${subio}% de salud`,
  }[accion]
}
