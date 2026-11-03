// Lo que se muestra se deriva de los porcentajes (presentacion, igual que el movil)

export function estadoAnimo(planta) {
  if (planta.healthPercent >= 70) return { texto: 'Feliz', cara: '😊', clase: 'animo-feliz' }
  if (planta.healthPercent >= 40) return { texto: 'Regular', cara: '😐', clase: 'animo-regular' }
  return { texto: 'Triste', cara: '😢', clase: 'animo-triste' }
}

// Consejo segun la necesidad mas baja; si todo va bien, un mensaje positivo
export function consejo(planta) {
  const necesidades = [
    [planta.humidityPercent, `A ${planta.name} le falta agua: ¡riégala!`],
    [planta.lightPercent, `${planta.name} necesita más luz: sácala al sol.`],
    [planta.healthPercent, `${planta.name} está débil: un poco de abono le ayudará.`],
  ]
  const [minimo, mensaje] = necesidades.reduce((a, b) => (b[0] < a[0] ? b : a))
  return minimo >= 70 ? `${planta.name} está en excelente estado. ¡Sigue así!` : mensaje
}

export const nivelBarra = (valor) => (valor >= 60 ? '' : valor >= 30 ? 'barra-ambar' : 'barra-roja')
