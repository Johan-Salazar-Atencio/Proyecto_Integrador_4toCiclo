import { useEffect, useState } from 'react'
import IconoAdmin from '../../../components/admin/IconoAdmin'
import { TIPOS } from '../../../services/admin/eventos'
import { SEV, imagenDe, textoMinutos } from './util'

const MAX_MENSAJE = 200
const PASO = 5
const MAX_MIN = 72 * 60

const mensajeSugerido = (tipo) => (TIPOS.find((t) => t.valor === tipo.codigo)?.mensaje || `Alerta de ${tipo.nombre.toLowerCase()} en tu zona: resguarda tus plantas.`).slice(0, MAX_MENSAJE)

// Ventana "¿Lanzar el evento?": region, severidad, duracion (que es tambien el tiempo limite del refugio) y mensaje push
export default function ModalLanzar({ tipo, regiones, onCerrar, onLanzar }) {
  const [datos, setDatos] = useState(() => ({
    region: regiones.some((r) => r.codigo === tipo.region_defecto) ? tipo.region_defecto : 'GLOBAL',
    severidad: 'MODERADO',
    minutos: Math.max(PASO, Math.min(MAX_MIN, tipo.duracion_defecto_min || 30)),
    mensaje: (tipo.mensaje_push || mensajeSugerido(tipo)).slice(0, MAX_MENSAJE),
  }))
  const [lanzando, setLanzando] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    const tecla = (e) => { if (e.key === 'Escape' && !lanzando) onCerrar() }
    window.addEventListener('keydown', tecla)
    return () => window.removeEventListener('keydown', tecla)
  }, [lanzando, onCerrar])

  const poner = (campo, valor) => setDatos((d) => ({ ...d, [campo]: valor }))
  const cambiarTiempo = (delta) => poner('minutos', Math.max(PASO, Math.min(MAX_MIN, datos.minutos + delta * PASO)))
  const valido = datos.mensaje.trim().length > 0 && datos.minutos >= PASO
  const foto = imagenDe(tipo)

  const lanzar = async () => {
    setLanzando(true)
    setError('')
    try {
      await onLanzar({
        tipo: tipo.codigo,
        region: datos.region,
        severidad: datos.severidad,
        horas: Math.floor(datos.minutos / 60),
        minutos: datos.minutos % 60,
        mensaje: datos.mensaje.trim(),
      })
    } catch (err) {
      setError(err.message || 'No se pudo lanzar el evento.')
      setLanzando(false)
    }
  }

  return (
    <div className="ad-modal" onClick={() => !lanzando && onCerrar()}>
      <div className="ad-hoja aparece" role="dialog" aria-modal="true" aria-label="Lanzar evento" onClick={(e) => e.stopPropagation()}>
        <div className="ev-modal-cab">
          <h3>¿Lanzar el evento?</h3>
          <button type="button" className="ad-ib" aria-label="Cerrar" onClick={onCerrar} disabled={lanzando}><IconoAdmin n="x" /></button>
        </div>

        <div className="ev-modal-tipo">
          {foto ? <img src={foto} alt="" /> : <div className="ev-sinimg"><IconoAdmin n="img" /></div>}
          <div><b>{tipo.nombre}</b><small className="ad-small">Tipo de evento seleccionado</small></div>
        </div>

        <div className="ad-dos">
          <div className="ad-fld">
            <label htmlFor="lz-region">Región</label>
            <select id="lz-region" className="ad-in" value={datos.region} onChange={(e) => poner('region', e.target.value)}>
              {regiones.map((r) => <option key={r.codigo} value={r.codigo}>{r.codigo === 'GLOBAL' ? 'Global (todas las regiones)' : r.nombre}</option>)}
            </select>
          </div>
          <div className="ad-fld">
            <span>Severidad</span>
            <div className="ad-seg3" role="group" aria-label="Severidad">
              {Object.entries(SEV).map(([valor, s]) => (
                <button key={valor} type="button" className={`${datos.severidad === valor ? 'on' : ''} ${s.clase}`} aria-pressed={datos.severidad === valor} onClick={() => poner('severidad', valor)}>{s.texto}</button>
              ))}
            </div>
          </div>
        </div>

        <div className="ad-fld">
          <span>Duración del evento y tiempo límite para el refugio</span>
          <div className="ad-paso">
            <button type="button" aria-label="Menos tiempo" onClick={() => cambiarTiempo(-1)}>−</button>
            <b>{textoMinutos(datos.minutos)}</b>
            <button type="button" aria-label="Más tiempo" onClick={() => cambiarTiempo(1)}>+</button>
          </div>
        </div>

        <div className="ad-fld">
          <label htmlFor="lz-msg">Mensaje push a la comunidad · {datos.mensaje.length}/{MAX_MENSAJE}</label>
          <textarea id="lz-msg" className="ad-in" rows={3} maxLength={MAX_MENSAJE} value={datos.mensaje} onChange={(e) => poner('mensaje', e.target.value)} />
        </div>

        <div className="ev-resumen"><IconoAdmin n="alert" />Se notificará a los usuarios de {datos.region === 'GLOBAL' ? 'todas las regiones' : regiones.find((r) => r.codigo === datos.region)?.nombre || datos.region} al lanzar.</div>
        {error && <div className="ad-error" role="alert">{error}</div>}

        <div className="ev-modal-pie">
          <button type="button" className="ad-btn ad-btn-g" onClick={onCerrar} disabled={lanzando}>Volver</button>
          <button type="button" className="ad-btn" onClick={lanzar} disabled={!valido || lanzando}><IconoAdmin n="bolt" />{lanzando ? 'Lanzando…' : 'Lanzar evento y notificar usuarios'}</button>
        </div>
      </div>
    </div>
  )
}
