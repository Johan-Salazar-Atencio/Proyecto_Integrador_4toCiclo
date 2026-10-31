import { useState } from 'react'
import Boton from '../../../components/Boton'
import { REGIONES, SEVERIDADES, TIPOS } from '../../../services/admin/eventos'
import VistaNotificacion from './VistaNotificacion'

const MAX_MENSAJE = 200
const MAX_HORAS = 72

// Formulario para lanzar un evento. Las columnas coinciden con la tabla eventos_desastre.
export default function FormularioEvento({ hayActivo, onLanzar }) {
  const [datos, setDatos] = useState({ tipo: TIPOS[0].valor, region: REGIONES[0], severidad: 'MODERADO', horas: 6, minutos: 0, mensaje: TIPOS[0].mensaje.slice(0, MAX_MENSAJE) })
  const [mensajeEditado, setMensajeEditado] = useState(false)
  const [confirmando, setConfirmando] = useState(false)
  const [lanzando, setLanzando] = useState(false)
  const [error, setError] = useState('')

  const poner = (campo, valor) => setDatos((d) => ({ ...d, [campo]: valor }))

  // Al cambiar de tipo se sugiere un mensaje, salvo que ya lo hayas escrito tu
  const elegirTipo = (tipo) => {
    setDatos((d) => ({ ...d, tipo: tipo.valor, mensaje: mensajeEditado ? d.mensaje : tipo.mensaje.slice(0, MAX_MENSAJE) }))
  }

  const cambiarTiempo = (campo, delta, tope, paso = 1) => {
    setDatos((d) => ({ ...d, [campo]: Math.max(0, Math.min(tope, d[campo] + delta * paso)) }))
  }

  const duracionTotal = datos.horas * 60 + datos.minutos
  const valido = datos.mensaje.trim().length > 0 && duracionTotal >= 5

  const lanzar = async () => {
    setLanzando(true)
    setError('')
    try {
      await onLanzar({ ...datos, mensaje: datos.mensaje.trim() })
      setConfirmando(false)
    } catch (err) {
      setError(err.message || 'No se pudo lanzar el evento.')
      setConfirmando(false)
    } finally {
      setLanzando(false)
    }
  }

  const tipo = TIPOS.find((t) => t.valor === datos.tipo)

  return (
    <section className="tarjeta formulario aparece" style={{ animationDelay: '0.08s' }}>
      <div className="formulario-columnas">
        <div className="formulario-campos">
          <h2>Programar y activar nuevo evento</h2>

          <fieldset>
            <legend>Tipo de desastre</legend>
            <div className="formulario-tipos">
              {TIPOS.map((t) => (
                <button key={t.valor} type="button" className={datos.tipo === t.valor ? 'activa' : ''} aria-pressed={datos.tipo === t.valor} onClick={() => elegirTipo(t)}>
                  <span>{t.icono}</span>{t.texto}
                </button>
              ))}
            </div>
          </fieldset>

          <div className="formulario-fila">
            <label>
              <span>Región</span>
              <select value={datos.region} onChange={(e) => poner('region', e.target.value)}>
                {REGIONES.map((r) => <option key={r} value={r}>{r === 'GLOBAL' ? 'Global (todas las regiones)' : r}</option>)}
              </select>
            </label>

            <div className="formulario-tiempo">
              <span>Tiempo límite para el refugio</span>
              <div>
                <Contador valor={datos.horas} etiqueta="HORAS" onMenos={() => cambiarTiempo('horas', -1, MAX_HORAS)} onMas={() => cambiarTiempo('horas', 1, MAX_HORAS)} />
                <Contador valor={datos.minutos} etiqueta="MIN" onMenos={() => cambiarTiempo('minutos', -1, 55, 5)} onMas={() => cambiarTiempo('minutos', 1, 55, 5)} />
              </div>
            </div>
          </div>

          <fieldset>
            <legend>Severidad</legend>
            <div className="formulario-severidad">
              {SEVERIDADES.map((s) => (
                <button key={s.valor} type="button" className={`${datos.severidad === s.valor ? 'activa' : ''} sev-${s.valor.toLowerCase()}`} aria-pressed={datos.severidad === s.valor} onClick={() => poner('severidad', s.valor)}>
                  <b>{s.texto}</b>
                  <small>{s.detalle}</small>
                </button>
              ))}
            </div>
          </fieldset>

          <label>
            <span className="formulario-mensaje-titulo">Mensaje push a la comunidad <em>{datos.mensaje.length}/{MAX_MENSAJE}</em></span>
            <textarea rows={3} maxLength={MAX_MENSAJE} value={datos.mensaje} onChange={(e) => { setMensajeEditado(true); poner('mensaje', e.target.value) }} />
          </label>

          {hayActivo && <p className="formulario-aviso">⚠️ Ya hay un evento en curso. Si lanzas otro, la app mostrará el más reciente.</p>}
          {error && <div className="auth-error" role="alert">{error}</div>}

          <Boton onClick={() => setConfirmando(true)} disabled={!valido}>⚡ Lanzar evento y notificar usuarios</Boton>
        </div>

        <VistaNotificacion tipo={tipo} mensaje={datos.mensaje} region={datos.region} />
      </div>

      {confirmando && (
        <div className="layout-fondo" onClick={() => !lanzando && setConfirmando(false)}>
          <div className="layout-dialogo aparece" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
            <h2>¿Lanzar el evento?</h2>
            <p>{tipo.icono} {tipo.texto} · {datos.region} · {SEVERIDADES.find((s) => s.valor === datos.severidad).texto}. Se notificará a los usuarios y durará {datos.horas} h {datos.minutos} min.</p>
            <div className="layout-dialogo-botones">
              <Boton variante="suave" onClick={() => setConfirmando(false)} disabled={lanzando}>Volver</Boton>
              <Boton onClick={lanzar} cargando={lanzando}>Lanzar</Boton>
            </div>
          </div>
        </div>
      )}
    </section>
  )
}

function Contador({ valor, etiqueta, onMenos, onMas }) {
  return (
    <div className="contador">
      <button type="button" onClick={onMenos} aria-label={`Menos ${etiqueta}`}>−</button>
      <div><b>{String(valor).padStart(2, '0')}</b><small>{etiqueta}</small></div>
      <button type="button" onClick={onMas} aria-label={`Más ${etiqueta}`}>+</button>
    </div>
  )
}
