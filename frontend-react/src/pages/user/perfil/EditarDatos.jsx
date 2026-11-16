import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { RUTAS } from '../../../routes/rutas'
import { obtenerUsuario } from '../../../services/home'
import { listarPlantasDigitales, renombrarPlanta } from '../../../services/jardin'
import { cambiarNombre } from '../../../services/usuario'
import { useSesion } from '../../../store/Sesion'
import './perfil.css'

// Un campo de texto con su boton Guardar, que solo se activa cuando el texto cambio
function FilaEditable({ titulo, inicial, etiqueta, maximo, onGuardar }) {
  const [texto, setTexto] = useState(inicial)
  useEffect(() => { setTexto(inicial) }, [inicial])
  return (
    <form className="tarjeta editar-fila" onSubmit={(e) => { e.preventDefault(); onGuardar(texto) }}>
      <small>{titulo}</small>
      <div>
        <input value={texto} maxLength={maximo} onChange={(e) => setTexto(e.target.value)} aria-label={etiqueta} />
        <button type="submit" className="boton boton-principal" disabled={texto.trim() === inicial.trim()}>Guardar</button>
      </div>
    </form>
  )
}

// Editar datos: el nombre del usuario y el nombre (apodo) de cada una de sus plantas
export default function EditarDatos() {
  const { usuario: sesion, iniciar } = useSesion()
  const [nombre, setNombre] = useState(null)
  const [plantas, setPlantas] = useState(null)
  const [aviso, setAviso] = useState('')
  const temporizador = useRef(null)

  useEffect(() => {
    obtenerUsuario().then((u) => setNombre(u?.name ?? '')).catch(() => setNombre(''))
    listarPlantasDigitales().then((l) => setPlantas(l || [])).catch(() => setPlantas([]))
    return () => clearTimeout(temporizador.current)
  }, [])

  const avisar = (texto) => {
    setAviso(texto)
    clearTimeout(temporizador.current)
    temporizador.current = setTimeout(() => setAviso(''), 2500)
  }

  const guardarNombre = async (texto) => {
    const limpio = texto.trim()
    if (limpio.length < 2) return avisar('El nombre debe tener al menos 2 letras')
    try {
      const u = await cambiarNombre(limpio)
      setNombre(u.name)
      if (sesion) iniciar({ ...sesion, nombre: u.name })
      avisar('Nombre guardado')
    } catch (err) {
      avisar(err.message || 'No se pudo guardar el nombre')
    }
  }

  const guardarPlanta = async (planta, texto) => {
    try {
      const nueva = await renombrarPlanta(planta.id, texto.trim())
      setPlantas((l) => l.map((p) => (p.id === nueva.id ? nueva : p)))
      avisar(`Nombre de ${nueva.name} guardado`)
    } catch (err) {
      avisar(err.message || 'No se pudo guardar')
    }
  }

  if (nombre === null || plantas === null) return <div className="home-cargando"><span className="home-giro" aria-label="Cargando" /></div>
  return (
    <div className="perfil editar">
      <Link to={RUTAS.PERFIL} className="editar-volver">‹ Perfil</Link>
      <FilaEditable titulo="Tu nombre" inicial={nombre} etiqueta="Nombre" maximo={100} onGuardar={guardarNombre} />
      <h3>Mis plantas</h3>
      {plantas.length === 0 && <p className="editar-vacio">Todavía no tienes plantas virtuales.</p>}
      {plantas.map((p) => (
        <FilaEditable key={p.id} titulo={p.species} inicial={p.name} etiqueta="Nombre de la planta" maximo={50} onGuardar={(t) => guardarPlanta(p, t)} />
      ))}
      {aviso && <div className="jardin-aviso" role="status">{aviso}</div>}
    </div>
  )
}
