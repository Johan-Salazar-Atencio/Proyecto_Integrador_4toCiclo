import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { RUTAS } from '../../../routes/rutas'
import { escanear } from '../../../services/escaneo'
import { escaneosRecientes } from '../../../services/historial'
import Icono from '../coleccion/Iconos'
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
  const [recientes, setRecientes] = useState([])
  const control = useRef(null)
  const temporizador = useRef(null)
  const irA = useNavigate()

  // Libera la vista previa y cancela lo pendiente al salir
  useEffect(() => () => { control.current?.abort(); clearTimeout(temporizador.current) }, [])
  useEffect(() => () => { if (vista) URL.revokeObjectURL(vista) }, [vista])

  // Los escaneos recientes solo acompañan: si fallan, la columna queda vacia
  useEffect(() => { escaneosRecientes().then((r) => setRecientes(r.slice(0, 5))).catch(() => {}) }, [])

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
      <header className="escaneo-cabecera">
        <h2>Escanear planta</h2>
        <span className="escaneo-etiqueta">Se identifica con IA</span>
      </header>

      <div className="escaneo-cuerpo">
        <div className="escaneo-principal">
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
                <span className="escaneo-icono"><Icono nombre="cam" tam={44} /></span>
                <h3>Toma o elige una foto</h3>
                <small>También puedes arrastrarla aquí</small>
              </div>
            )}
          </label>

          <ul className="escaneo-consejos">
            <li><Icono nombre="sun" tam={16} /> Buena luz</li>
            <li><Icono nombre="leaf" tam={16} /> Hojas a la vista</li>
            <li><Icono nombre="target" tam={16} /> Una sola planta</li>
          </ul>

          {error && <div className="escaneo-error" role="alert">{error}</div>}

          <div className="escaneo-botones">
            <button className="escaneo-identificar" onClick={identificar} disabled={!archivo}>Identificar planta</button>
            {vista && <button className="escaneo-otra" onClick={() => { setArchivo(null); setVista(null) }}>Quitar foto</button>}
          </div>
        </div>

        <aside className="escaneo-recientes">
          <div className="escaneo-recientes-cab">
            <h3>Escaneos recientes</h3>
            <Link to={RUTAS.HISTORIAL}>Ver historial</Link>
          </div>
          {recientes.length === 0 ? (
            <p className="escaneo-recientes-vacio">Aquí verás las últimas plantas que identifiques.</p>
          ) : (
            recientes.map((r) => (
              <div key={r.id} className="escaneo-reciente">
                <span className="escaneo-reciente-foto" style={r.statusColor ? { color: r.statusColor } : undefined}><Icono nombre="leaf" tam={24} /></span>
                <div><b>{r.plantName}</b><small>{r.confidence}</small></div>
              </div>
            ))
          )}
        </aside>
      </div>

      {fase !== 'espera' && <EsperaEscaneo foto={vista} listo={fase === 'exito'} onCancelar={cancelar} />}
    </div>
  )
}
