import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { RUTAS } from '../../../routes/rutas'
import { detalleColeccion } from '../../../services/coleccion'
import FichaPlanta from '../planta/FichaPlanta'

// Ficha de una planta ya guardada (GET /coleccion/{id}): sin % de IA y solo con Digitalizar
export default function DetalleColeccion() {
  const { id } = useParams()
  const [estado, setEstado] = useState({ cargando: true, planta: null, error: '' })

  const cargar = useCallback(async () => {
    setEstado({ cargando: true, planta: null, error: '' })
    try {
      setEstado({ cargando: false, planta: await detalleColeccion(id), error: '' })
    } catch (err) {
      setEstado({ cargando: false, planta: null, error: err.message || 'No se pudo cargar la planta.' })
    }
  }, [id])

  useEffect(() => { cargar() }, [cargar])

  if (estado.cargando) return <div className="home-cargando"><span className="home-giro" aria-label="Cargando" /></div>

  if (estado.error) {
    return (
      <div className="coleccion-vacio aparece">
        <span>😕</span>
        <h2>{estado.error}</h2>
        <button className="boton boton-suave" onClick={cargar}>Reintentar</button>
        <Link to={RUTAS.COLECCION}>← Volver a la colección</Link>
      </div>
    )
  }

  return <FichaPlanta resultado={estado.planta} mostrarGuardar={false} />
}
