import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { RUTAS } from '../../../routes/rutas'
import { obtenerUsuario, urlImagen } from '../../../services/home'
import { listarPlantasDigitales } from '../../../services/jardin'
import { cambiarRegion, listarRegiones, subirFoto } from '../../../services/usuario'
import { useSesion } from '../../../store/Sesion'
import { guardarRecordatorio, guardarTema, HORA_RECORDATORIO, leerTema, recordatorioActivo, TEMAS } from '../../../store/preferencias'
import { iniciales } from '../home/components/TarjetaUsuario'
import './perfil.css'

const TEXTO_TEMA = { SISTEMA: 'Sistema', CLARO: 'Claro', OSCURO: 'Oscuro' }

// Perfil (propuesta E): foto con boton de camara, nombre y rango, tres estadisticas, apariencia, region y la lista
// con editar datos, recordatorios, historial, enciclopedia, terminos y cierre de sesion.
export default function Perfil() {
  const { usuario: sesion, cerrar } = useSesion()
  const irA = useNavigate()
  const [estado, setEstado] = useState({ cargando: true, datos: null, plantas: null, regiones: [], error: '' })
  const [tema, setTema] = useState(leerTema)
  const [recordar, setRecordar] = useState(recordatorioActivo)
  const [aviso, setAviso] = useState('')
  const [subiendo, setSubiendo] = useState(false)
  const selector = useRef(null)
  const temporizador = useRef(null)

  const cargar = useCallback(async () => {
    setEstado((e) => ({ ...e, cargando: true, error: '' }))
    const [datos, plantas, regiones] = await Promise.allSettled([obtenerUsuario(), listarPlantasDigitales(), listarRegiones()])
    setEstado({
      cargando: false,
      datos: datos.status === 'fulfilled' ? datos.value : null,
      plantas: plantas.status === 'fulfilled' ? (plantas.value || []).length : null,
      regiones: regiones.status === 'fulfilled' ? regiones.value || [] : [],
      error: datos.status === 'fulfilled' && datos.value ? '' : 'No se pudo cargar el perfil',
    })
  }, [])

  useEffect(() => { cargar() }, [cargar])
  useEffect(() => () => clearTimeout(temporizador.current), [])

  const avisar = (texto) => {
    setAviso(texto)
    clearTimeout(temporizador.current)
    temporizador.current = setTimeout(() => setAviso(''), 2800)
  }

  const elegirTema = (t) => { setTema(t); guardarTema(t) }

  const alternarRecordatorio = async () => {
    if (recordar) {
      guardarRecordatorio(false)
      setRecordar(false)
      return
    }
    if (typeof Notification === 'undefined') return avisar('Este navegador no permite notificaciones.')
    const permiso = Notification.permission === 'granted' ? 'granted' : await Notification.requestPermission()
    if (permiso !== 'granted') return avisar('Permite las notificaciones del navegador para activar el recordatorio.')
    guardarRecordatorio(true)
    setRecordar(true)
    avisar(`Recordatorio activado: te avisamos a las ${HORA_RECORDATORIO}:00 con la web abierta.`)
  }

  const cambiarFoto = async (e) => {
    const archivo = e.target.files?.[0]
    e.target.value = ''
    if (!archivo || subiendo) return
    setSubiendo(true)
    try {
      const datos = await subirFoto(archivo)
      setEstado((s) => ({ ...s, datos }))
    } catch (err) {
      avisar(err.message || 'No se pudo cambiar la foto.')
    } finally {
      setSubiendo(false)
    }
  }

  const elegirRegion = async (codigo) => {
    if (codigo === estado.datos?.regionCode) return
    try {
      const datos = await cambiarRegion(codigo)
      setEstado((s) => ({ ...s, datos }))
    } catch (err) {
      avisar(err.message || 'No se pudo cambiar la región.')
    }
  }

  const { datos } = estado
  const nombre = datos?.name || sesion?.nombre || 'Jardinero'
  const foto = urlImagen(datos?.profileImageUrl)

  return (
    <div className="perfil">
      <section className="perfil-cabecera tarjeta aparece">
        <div className="perfil-foto">
          <span className="perfil-avatar">
            {foto && <img src={foto} alt="" onError={(e) => { e.currentTarget.style.display = 'none' }} />}
            <b>{iniciales(nombre)}</b>
            {subiendo && <span className="home-giro perfil-subiendo" aria-label="Subiendo" />}
          </span>
          <button className="perfil-camara" onClick={() => selector.current?.click()} aria-label="Cambiar foto de perfil" disabled={subiendo}>📷</button>
          <input ref={selector} type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={cambiarFoto} />
        </div>
        <h2>{nombre}</h2>
        {(datos?.email || sesion?.email) && <p>{datos?.email || sesion?.email}</p>}
        {datos && <span className="perfil-nivel">{datos.badge || 'Jardinero'} · Nivel {datos.level}</span>}

        {estado.cargando && <span className="home-giro" aria-label="Cargando" />}
        {estado.error && !estado.cargando && (
          <div className="home-error" role="alert">
            <span>{estado.error}</span>
            <button onClick={cargar}>Reintentar</button>
          </div>
        )}
      </section>

      {datos && (
        <div className="perfil-datos aparece" style={{ animationDelay: '0.05s' }}>
          <Dato valor={estado.plantas ?? '–'} texto="Plantas" color="var(--verde)" />
          <Dato valor={datos.streakDays} texto="Días de racha" color="var(--azul)" />
          <Dato valor={datos.leaves} texto="Hojas" color="var(--ambar)" />
        </div>
      )}

      <section className="perfil-seccion aparece" style={{ animationDelay: '0.08s' }}>
        <small>Apariencia</small>
        <div className="perfil-segmentos" role="radiogroup" aria-label="Apariencia">
          {TEMAS.map((t) => (
            <button key={t} role="radio" aria-checked={tema === t} className={tema === t ? 'activa' : ''} onClick={() => elegirTema(t)}>{TEXTO_TEMA[t]}</button>
          ))}
        </div>
      </section>

      {datos && estado.regiones.length > 0 && (
        <section className="tarjeta perfil-region aparece" style={{ animationDelay: '0.1s' }}>
          <h3>Mi región</h3>
          <div className="perfil-chips">
            {estado.regiones.map((r) => (
              <button key={r.code} className={r.code === (datos.regionCode || 'GLOBAL') ? 'activa' : ''} onClick={() => elegirRegion(r.code)}>{r.name}</button>
            ))}
          </div>
          <small>Recibes las alertas de desastre de tu región.</small>
        </section>
      )}

      <nav className="perfil-enlaces aparece" style={{ animationDelay: '0.12s' }}>
        <Link to={RUTAS.EDITAR_DATOS} className="tarjeta"><span>✏️</span> Editar datos <i>›</i></Link>
        <button className="tarjeta perfil-recordatorio" onClick={alternarRecordatorio} role="switch" aria-checked={recordar}>
          <span>🔔</span>
          <div>
            <b>Recordatorios</b>
            <small>Un aviso diario a las {HORA_RECORDATORIO}:00 para cuidar tus plantas (con la web abierta)</small>
          </div>
          <i className={`perfil-interruptor ${recordar ? 'activo' : ''}`} aria-hidden="true" />
        </button>
        <Link to={RUTAS.HISTORIAL} className="tarjeta"><span>🕘</span> Historial de escaneos <i>›</i></Link>
        <Link to={RUTAS.CATALOGO} className="tarjeta"><span>📖</span> Enciclopedia <i>›</i></Link>
        <Link to={RUTAS.TERMINOS} className="tarjeta"><span>📄</span> Términos y privacidad <i>›</i></Link>
        <button className="tarjeta perfil-salir" onClick={() => { cerrar(); irA(RUTAS.WELCOME, { replace: true }) }}>
          <span>🚪</span> Cerrar sesión
        </button>
      </nav>

      {aviso && <div className="jardin-aviso" role="status">{aviso}</div>}
    </div>
  )
}

function Dato({ valor, texto, color }) {
  return (
    <div className="perfil-dato" style={{ '--color': color }}>
      <b>{valor}</b>
      <small>{texto}</small>
    </div>
  )
}
