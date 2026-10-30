import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Boton from '../../../components/Boton'
import { RUTAS } from '../../../routes/rutas'
import { escanear } from '../../../services/escaneo'
import EsperaEscaneo from './EsperaEscaneo'
import './escaneo.css'

const MS_CHECK = 900
const MAX_MB = 10

export default function Escaneo() {
  const [archivo, setArchivo] = useState(null)
  const [vista, setVista] = useState(null)
  const [fase, setFase] = useState('espera') // espera | enviando | exito
  const [error, setError] = useState('')
  const [arrastrando, setArrastrando] = useState(false)
  const control = useRef(null)
  const temporizador = useRef(null)
  const irA = useNavigate()

  // Libera la vista previa y cancela lo pendiente al salir
  useEffect(() => () => { control.current?.abort(); clearTimeout(temporizador.current) }, [])
  useEffect(() => () => { if (vista) URL.revokeObjectURL(vista) }, [vista])

  const elegir = (nuevo) => {
    if (!nuevo) return
    if (!nuevo.type.startsWith('image/')) return setError('Elige una imagen (JPG, PNG o WebP).')
    if (nuevo.size > MAX_MB * 1024 * 1024) return setError(`La foto pesa más de ${MAX_MB} MB.`)
    setError('')
    setArchivo(nuevo)
    setVista(URL.createObjectURL(nuevo))
  }

  const identificar = async () => {
    control.current = new AbortController()
    setError('')
    setFase('enviando')
    try {
      const resultado = await escanear(archivo, control.current.signal)
      setFase('exito')
      temporizador.current = setTimeout(() => irA(RUTAS.FICHA, { state: { resultado } }), MS_CHECK)
    } catch (err) {
      if (err.name === 'AbortError') return
      setFase('espera')
      setError(err instanceof TypeError || err.estado >= 500
        ? 'No se pudo conectar con el servidor. Inténtalo de nuevo.'
        : err.message || 'No se pudo identificar la planta.')
    }
  }

  const cancelar = () => {
    control.current?.abort()
    setFase('espera')
  }

  const soltar = (e) => {
    e.preventDefault()
    setArrastrando(false)
    elegir(e.dataTransfer.files?.[0])
  }

  return (
    <div className="escaneo aparece">
      <label
        className={`escaneo-zona ${arrastrando ? 'escaneo-arrastrando' : ''} ${vista ? 'escaneo-con-foto' : ''}`}
        onDragOver={(e) => { e.preventDefault(); setArrastrando(true) }}
        onDragLeave={() => setArrastrando(false)}
        onDrop={soltar}
      >
        <input type="file" accept="image/*" capture="environment" onChange={(e) => elegir(e.target.files?.[0])} />
        {vista ? (
          <img src={vista} alt="Vista previa de tu foto" />
        ) : (
          <div className="escaneo-vacio">
            <span className="escaneo-icono">📷</span>
            <b>Toma o elige una foto</b>
            <small>También puedes arrastrarla aquí</small>
          </div>
        )}
      </label>

      <ul className="escaneo-consejos">
        <li>☀️ Buena luz</li>
        <li>🍃 Hojas a la vista</li>
        <li>🎯 Una sola planta</li>
      </ul>

      {error && <div className="escaneo-error" role="alert">{error}</div>}

      <Boton onClick={identificar} disabled={!archivo}>Identificar planta</Boton>
      {vista && <button className="escaneo-otra" onClick={() => { setArchivo(null); setVista(null) }}>Quitar foto</button>}

      {fase !== 'espera' && <EsperaEscaneo foto={vista} listo={fase === 'exito'} onCancelar={cancelar} />}
    </div>
  )
}
