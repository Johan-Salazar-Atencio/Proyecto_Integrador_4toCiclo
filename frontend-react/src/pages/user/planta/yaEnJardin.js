// QA-04: digitalizar una especie que ya esta en el jardin NO la reinicia (Spring conserva apodo, dias, crecimiento,
// estado y abono; solo actualiza la foto). La ficha lo avisa antes y el jardin despues.

const normalizar = (texto) => (texto || '').trim().toLowerCase()

// La planta del jardin (o del baul) de la misma especie que el escaneo; null si no hay. `species` es el nombre
// cientifico que manda Spring en cada planta.
export function plantaDeLaEspecie(plantas, resultado) {
  const especie = normalizar(resultado?.nombreCientifico)
  if (!especie || !Array.isArray(plantas)) return null
  return plantas.find((p) => normalizar(p?.species) === especie) || null
}

// Texto bajo el boton de la ficha cuando la especie ya esta en el jardin
export function avisoAntes(planta) {
  if (!planta) return ''
  if (planta.enBaul) return `${planta.name} está guardada en tu baúl: sácala primero para verla en el jardín.`
  if (planta.fase === 'MUERTA') return `${planta.name} ya está en tu jardín y está muerta: digitalizarla otra vez no la revive.`
  return `${planta.name} ya está en tu jardín. Digitalizarla otra vez no la reinicia: conserva su progreso y solo se actualiza la foto.`
}

// Aviso del jardin al volver de digitalizar una especie que ya estaba (la respuesta trae yaEnJardin: true)
export function avisoDespues(respuesta) {
  if (!respuesta?.yaEnJardin) return ''
  return `${respuesta.name || 'Tu planta'} ya estaba en tu jardín: conserva su progreso.`
}
