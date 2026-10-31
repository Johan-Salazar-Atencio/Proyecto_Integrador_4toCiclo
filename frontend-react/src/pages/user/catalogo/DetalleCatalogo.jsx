import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useLocation, useParams } from 'react-router-dom'
import { RUTAS } from '../../../routes/rutas'
import { cuidadosCatalogo } from '../../../services/catalogo'
import FichaPlanta from '../planta/FichaPlanta'

// Ficha de una especie del catalogo: solo lectura (sin Digitalizar ni Guardar).
// La especie llega por el estado de la ruta; si se recarga la pagina se vuelve a la lista.
export default function DetalleCatalogo() {
  const { id } = useParams()
  const { state } = useLocation()
  const especie = state?.planta
  const [estado, setEstado] = useState({ cargando: true, cuidados: [], error: '' })

  const cargar = useCallback(async () => {
    setEstado({ cargando: true, cuidados: [], error: '' })
    try {
      setEstado({ cargando: false, cuidados: await cuidadosCatalogo(id), error: '' })
    } catch (err) {
      setEstado({ cargando: false, cuidados: [], error: err.message || 'No se pudo cargar la ficha.' })
    }
  }, [id])

  useEffect(() => { cargar() }, [cargar])

  if (!especie) return <Navigate to={RUTAS.CATALOGO} replace />
  if (estado.cargando) return <div className="home-cargando"><span className="home-giro" aria-label="Cargando" /></div>

  if (estado.error) {
    return (
      <div className="coleccion-vacio aparece">
        <span>😕</span>
        <h2>{estado.error}</h2>
        <button className="boton boton-suave" onClick={cargar}>Reintentar</button>
        <Link to={RUTAS.CATALOGO}>← Volver a la enciclopedia</Link>
      </div>
    )
  }

  const resultado = {
    nombreComun: especie.nombreComun,
    nombreCientifico: especie.nombreCientifico,
    confianza: null,
    imagenUrl: null,
    cuidados: estado.cuidados,
  }
  return <FichaPlanta resultado={resultado} mostrarGuardar={false} mostrarAcciones={false} />
}
