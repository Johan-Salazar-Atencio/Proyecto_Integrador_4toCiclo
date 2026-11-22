import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { RUTAS } from '../../../routes/rutas'
import { obtenerUsuario, urlImagen } from '../../../services/home'
import { listarPlantasDigitales } from '../../../services/jardin'
import { cambiarRegion, listarRegiones, subirFoto } from '../../../services/usuario'
import { useSesion } from '../../../store/Sesion'
import { guardarRecordatorio, guardarTema, HORA_RECORDATORIO, leerTema, recordatorioActivo, TEMAS } from '../../../store/preferencias'
import { iniciales } from '../home/components/TarjetaUsuario'
import Icono from '../tienda/Icono'
import './perfil.css'

const TEXTO_TEMA = { SISTEMA: 'Sistema', CLARO: 'Claro', OSCURO: 'Oscuro' }

// Perfil (propuesta E): foto con boton de camara, nombre y rango, tres estadisticas, apariencia, region y la lista
// con editar datos, recordatorios, historial, terminos y cierre de sesion. En pantallas anchas va en dos columnas.
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
  const desde = datos?.experienceFromLevel || 0
  const tramo = Math.max((datos?.experienceToNextLevel ?? 0) - desde, 0)
  const porcentaje = datos && tramo > 0 ? Math.min(100, Math.max(0, Math.round(((datos.experience - desde) / tramo) * 100))) : datos ? 100 : 0

  return (
    <div className="perfil">
      <h2 className="perfil-titulo">Perfil</h2>
      <div className="perfil-columnas">
        <section className="perfil-cabecera aparece">
          <div className="perfil-foto">
            <span className="perfil-anillo" style={{ '--p': `${porcentaje}%` }}>
              <span className="perfil-avatar">
                {foto && <img src={foto} alt="" onError={(e) => { e.currentTarget.style.display = 'none' }} />}
                <b>{iniciales(nombre)}</b>
                {subiendo && <span className="home-giro perfil-subiendo" aria-label="Subiendo" />}
              </span>
            </span>
            <button className="perfil-camara" onClick={() => selector.current?.click()} aria-label="Cambiar foto de perfil" disabled={subiendo}><Icono n="cam" tam={16} /></button>
            <input ref={selector} type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={cambiarFoto} />
          </div>
          <h3>{nombre}</h3>
          {(datos?.email || sesion?.email) && <p>{datos?.email || sesion?.email}</p>}
          {datos && <span className="perfil-nivel">{datos.badge || 'Jardinero'} · Nivel {datos.level}</span>}

          {datos && (
            <>
              <div className="perfil-barra" role="progressbar" aria-valuenow={porcentaje} aria-valuemin={0} aria-valuemax={100}><i style={{ width: `${porcentaje}%` }} /></div>
              <small className="perfil-xp"><span>{datos.experience} / {datos.experienceToNextLevel} XP para el nivel {datos.level + 1}</span><span>{porcentaje} %</span></small>
              <span className="perfil-hojas"><Icono n="leaf" tam={18} />{datos.leaves} hojas</span>
              <div className="perfil-datos">
                <Dato valor={estado.plantas ?? '–'} texto="Plantas" />
                <Dato valor={datos.streakDays} texto="Días de racha" />
              </div>
            </>
          )}

          {estado.cargando && <span className="home-giro" aria-label="Cargando" />}
          {estado.error && !estado.cargando && (
            <div className="home-error" role="alert">
              <span>{estado.error}</span>
              <button onClick={cargar}>Reintentar</button>
            </div>
          )}
        </section>

        <div className="perfil-grupos aparece" style={{ animationDelay: '0.05s' }}>
          <section className="perfil-grupo">
            <h4>Apariencia</h4>
            <div className="perfil-segmentos" role="radiogroup" aria-label="Apariencia">
              {TEMAS.map((t) => (
                <button key={t} role="radio" aria-checked={tema === t} className={tema === t ? 'activa' : ''} onClick={() => elegirTema(t)}>{TEXTO_TEMA[t]}</button>
              ))}
            </div>
          </section>

          {datos && estado.regiones.length > 0 && (
            <section className="perfil-grupo perfil-region">
              <h4>Mi región</h4>
              <label className="perfil-select">
                <select value={datos.regionCode || 'GLOBAL'} onChange={(e) => elegirRegion(e.target.value)} aria-label="Mi región">
                  {estado.regiones.map((r) => <option key={r.code} value={r.code}>{r.name}</option>)}
                </select>
                <Icono n="down" />
              </label>
              <small>Recibes las alertas de desastre de tu región.</small>
            </section>
          )}

          <nav className="perfil-grupo perfil-lista">
            <button className="perfil-fila perfil-recordatorio" onClick={alternarRecordatorio} role="switch" aria-checked={recordar}>
              <span className="perfil-ico"><Icono n="bell" /></span>
              <div>
                <b>Recordatorios</b>
                <small>Un aviso diario a las {HORA_RECORDATORIO}:00 (con la web abierta)</small>
              </div>
              <i className={`perfil-interruptor ${recordar ? 'activo' : ''}`} aria-hidden="true" />
            </button>
            <Link to={RUTAS.LOGROS} className="perfil-fila"><span className="perfil-ico"><Icono n="trophy" /></span><b>Mis logros</b><Icono n="r" className="perfil-flecha" /></Link>
            <Link to={RUTAS.EDITAR_DATOS} className="perfil-fila"><span className="perfil-ico"><Icono n="edit" /></span><b>Editar datos</b><Icono n="r" className="perfil-flecha" /></Link>
            <Link to={RUTAS.HISTORIAL} className="perfil-fila"><span className="perfil-ico"><Icono n="clock" /></span><b>Historial de escaneos</b><Icono n="r" className="perfil-flecha" /></Link>
            <Link to={RUTAS.TERMINOS} className="perfil-fila"><span className="perfil-ico"><Icono n="doc" /></span><b>Términos y privacidad</b><Icono n="r" className="perfil-flecha" /></Link>
          </nav>

          <button className="perfil-salir" onClick={() => { cerrar(); irA(RUTAS.WELCOME, { replace: true }) }}>
            <Icono n="out" tam={20} />Cerrar sesión
          </button>
        </div>
      </div>

      {aviso && <div className="jardin-aviso" role="status">{aviso}</div>}
    </div>
  )
}

function Dato({ valor, texto }) {
  return (
    <div className="perfil-dato">
      <b>{valor}</b>
      <small>{texto}</small>
    </div>
  )
}
