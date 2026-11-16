import React from 'react'
import PlantaVectorAnimada from '../../components/PlantaVectorAnimada'
import '../../components/planta-vector.css'

// Pagina de demo del prototipo de animacion vectorial (Monstera)
export default function DemoPlanta() {
  return (
    <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 24, alignItems: 'center' }}>
      <h1>Prototipo: Monstera vectorial animada</h1>
      <p>Salud 100%</p>
      <PlantaVectorAnimada tamaño={240} salud={1} />
      <p>Salud 50%</p>
      <PlantaVectorAnimada tamaño={200} salud={0.5} />
      <p>Salud 20% (mas movimiento)</p>
      <PlantaVectorAnimada tamaño={160} salud={0.2} />
    </div>
  )
}