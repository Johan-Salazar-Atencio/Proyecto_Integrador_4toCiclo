import { useEffect, useState } from 'react'
import { obtenerEventoActivo, urlImagen } from '../services/home'
import { elegirEscena, obtenerArte } from '../services/arte'
import './escena.css'

const MS_SONDEO = 20000

// Fondo en pixel art con movimiento suave (tira de cuadros animada con CSS) y, si hay un desastre
// activo, su animacion encima. Va detras del contenido del marco: position absolute, sin eventos.
export default function EscenaPixel() {
  const [escena, setEscena] = useState(null)
  const [cuadro, setCuadro] = useState(0)

  // Arte una vez; la alerta y la hora se revisan cada 20 s mientras la pantalla esta abierta
  useEffect(() => {
    let vivo = true
    let arte = null
    const actualizar = async () => {
      arte = arte || (await obtenerArte())
      let alerta = null
      try { alerta = await obtenerEventoActivo() } catch { /* se conserva lo que haya */ }
      if (vivo) setEscena(elegirEscena(arte, new Date().getHours(), alerta?.disasterType, alerta?.phase))
    }
    actualizar()
    const id = setInterval(actualizar, MS_SONDEO)
    return () => { vivo = false; clearInterval(id) }
  }, [])

  const frames = escena?.animacion?.frames
  const msCapa = escena?.animacion?.msPerFrame
  useEffect(() => {
    setCuadro(0)
    if (!frames || frames.length < 2 || !msCapa) return undefined
    const id = setInterval(() => setCuadro((c) => (c + 1) % frames.length), msCapa)
    return () => clearInterval(id)
  }, [frames, msCapa])

  if (!escena) return null
  const { fondo } = escena
  return (
    <div className="escena" aria-hidden="true">
      <div
        className="escena-fondo"
        style={{
          backgroundImage: `url(${urlImagen(fondo.imageUrl)})`,
          '--cuadros': fondo.frames,
          '--ms': `${fondo.msPerFrame * fondo.frames}ms`,
        }}
      />
      {frames && frames.length > 0 && <img className="escena-capa" src={urlImagen(frames[cuadro % frames.length])} alt="" />}
    </div>
  )
}
