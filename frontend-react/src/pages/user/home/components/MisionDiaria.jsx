import { useState } from 'react'
import Icono from '../../../../components/Icono'
import MisionFila from '../../misiones/MisionFila'
import ModalMisiones from './ModalMisiones'

const EN_INICIO = 3

// Misiones de hoy (GET /misiones): tres filas y "Ver todas" abre la ventana con todas
export default function MisionDiaria({ misiones }) {
  const [abierto, setAbierto] = useState(false)
  if (!misiones.length) return null
  const hechas = misiones.filter((m) => m.isCompleted).length

  return (
    <section className="tarjeta ms aparece" style={{ animationDelay: '0.2s' }}>
      <div className="ms-cabecera">
        <div>
          <h2>Misión del día</h2>
          <small>{hechas} de {misiones.length} completadas</small>
        </div>
        <button className="boton-linea" onClick={() => setAbierto(true)} aria-haspopup="dialog">
          Ver todas <Icono nombre="r" tam={16} />
        </button>
      </div>
      <div className="ms-lista">
        {misiones.slice(0, EN_INICIO).map((m) => <MisionFila key={m.id} mision={m} />)}
      </div>
      {abierto && <ModalMisiones misiones={misiones} onCerrar={() => setAbierto(false)} />}
    </section>
  )
}
