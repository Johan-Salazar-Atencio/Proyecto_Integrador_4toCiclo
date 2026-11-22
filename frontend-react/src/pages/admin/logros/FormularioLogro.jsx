import { useMemo, useState } from 'react'
import IconoLogro, { ICONOS_LOGRO } from '../../../components/IconoLogro'
import { RAREZAS } from '../../../services/admin/logros'
import { Alternador, Campo, Icono } from '../misiones/kit'

const VACIO = {
  codigo: '', titulo: '', descripcion: '', icono: 'semilla', rareza: 'COMUN', condicion: '', meta: 1, parametro: '',
  recompensa_xp: 0, recompensa_monedas: 0, secreto: false, activo: true, orden: 0,
}
const CODIGO_VALIDO = /^[A-Z0-9_]+$/

// Validacion del cliente: devuelve { campo: mensaje }. El servidor vuelve a validar.
function validar(d, condicion, editando) {
  const e = {}
  if (!editando) {
    if (!d.codigo.trim()) e.codigo = 'Escribe un código.'
    else if (d.codigo.length > 50) e.codigo = 'Máximo 50 caracteres.'
    else if (!CODIGO_VALIDO.test(d.codigo)) e.codigo = 'Solo mayúsculas, números y guion bajo.'
  }
  if (!d.titulo.trim()) e.titulo = 'Escribe un título.'
  else if (d.titulo.length > 100) e.titulo = 'Máximo 100 caracteres.'
  if (d.descripcion.length > 255) e.descripcion = 'Máximo 255 caracteres.'
  if (!d.icono.trim()) e.icono = 'Elige o escribe un ícono.'
  else if (d.icono.length > 40) e.icono = 'Máximo 40 caracteres.'
  if (!d.condicion) e.condicion = 'Elige una condición.'
  if (!Number.isInteger(d.meta) || d.meta < 1) e.meta = 'La meta debe ser 1 o más.'
  if (!Number.isInteger(d.recompensa_xp) || d.recompensa_xp < 0) e.recompensa_xp = 'No puede ser negativo.'
  if (!Number.isInteger(d.recompensa_monedas) || d.recompensa_monedas < 0) e.recompensa_monedas = 'No puede ser negativo.'
  if (!Number.isInteger(d.orden) || d.orden < 0) e.orden = 'No puede ser negativo.'
  if (condicion?.requiereParametro && !String(d.parametro).trim()) e.parametro = 'Esta condición pide un parámetro.'
  return e
}

const MsgError = ({ texto }) => (texto ? <small className="lg-err" role="alert">{texto}</small> : null)

// Panel "Editar logro" (o "Nuevo logro" si `logro` es null). El código queda de solo lectura al editar.
export default function FormularioLogro({ logro, opciones, onGuardar, onCancelar }) {
  const editando = Boolean(logro)
  const [datos, setDatos] = useState(() => (logro ? { ...VACIO, ...logro, descripcion: logro.descripcion || '', parametro: logro.parametro || '' } : { ...VACIO, condicion: opciones.condiciones[0]?.codigo || '' }))
  const [errores, setErrores] = useState({})
  const [guardando, setGuardando] = useState(false)
  const [errorServidor, setErrorServidor] = useState('')

  const condicion = useMemo(() => opciones.condiciones.find((c) => c.codigo === datos.condicion), [opciones, datos.condicion])
  const iconos = useMemo(() => [...new Set([...opciones.iconos, ...Object.keys(ICONOS_LOGRO)])], [opciones])

  const cambiar = (campo, valor) => setDatos((d) => ({ ...d, [campo]: valor }))
  const numero = (campo) => (e) => cambiar(campo, e.target.value === '' ? '' : Number(e.target.value))

  const enviar = async (ev) => {
    ev.preventDefault()
    const problemas = validar(datos, condicion, editando)
    setErrores(problemas)
    if (Object.keys(problemas).length) return
    setGuardando(true)
    setErrorServidor('')
    const cuerpo = {
      titulo: datos.titulo.trim(),
      descripcion: datos.descripcion.trim(),
      icono: datos.icono.trim(),
      rareza: datos.rareza,
      condicion: datos.condicion,
      meta: datos.meta,
      parametro: condicion?.requiereParametro ? String(datos.parametro).trim() : null,
      recompensa_xp: datos.recompensa_xp,
      recompensa_monedas: datos.recompensa_monedas,
      secreto: datos.secreto,
      activo: datos.activo,
      orden: datos.orden,
    }
    if (!editando) cuerpo.codigo = datos.codigo.trim()
    try {
      await onGuardar(cuerpo)
    } catch (err) {
      setErrorServidor(err.message)
      setGuardando(false)
    }
  }

  return (
    <form className="ak-card ak-panel lg-form" onSubmit={enviar} noValidate>
      <div className="ak-panel-cab">
        <h3>{editando ? 'Editar logro' : 'Nuevo logro'}</h3>
        <button type="button" className="ak-ib lg-tactil" aria-label="Cerrar panel" onClick={onCancelar}><Icono nombre="x" /></button>
      </div>

      <Campo etiqueta="Código" ayuda={editando ? 'No se puede cambiar una vez creado.' : 'Mayúsculas, números y guion bajo. Ej.: PRIMERA_SEMILLA'}>
        <input value={datos.codigo} maxLength={50} readOnly={editando} disabled={editando}
          onChange={(e) => cambiar('codigo', e.target.value.toUpperCase())} />
        <MsgError texto={errores.codigo} />
      </Campo>
      <Campo etiqueta="Título">
        <input value={datos.titulo} maxLength={100} onChange={(e) => cambiar('titulo', e.target.value)} />
        <MsgError texto={errores.titulo} />
      </Campo>
      <Campo etiqueta="Descripción">
        <textarea rows={2} maxLength={255} value={datos.descripcion} onChange={(e) => cambiar('descripcion', e.target.value)} />
        <MsgError texto={errores.descripcion} />
      </Campo>

      <div className="ak-dos lg-dos">
        <Campo etiqueta="Rareza">
          <select value={datos.rareza} onChange={(e) => cambiar('rareza', e.target.value)}>
            {opciones.rarezas.map((r) => <option key={r} value={r}>{RAREZAS.find((x) => x.valor === r)?.texto || r}</option>)}
          </select>
        </Campo>
        <Campo etiqueta="Ícono">
          <input list="lg-iconos" value={datos.icono} maxLength={40} onChange={(e) => cambiar('icono', e.target.value.trim())} />
          <MsgError texto={errores.icono} />
        </Campo>
      </div>
      <datalist id="lg-iconos">{iconos.map((i) => <option key={i} value={i} />)}</datalist>
      <div className="lg-iconos" role="group" aria-label="Íconos sugeridos">
        {iconos.map((i) => (
          <button key={i} type="button" title={i} aria-label={`Ícono ${i}`} aria-pressed={datos.icono === i}
            className={`lg-ico-op ${datos.icono === i ? 'on' : ''}`} onClick={() => cambiar('icono', i)}>
            <IconoLogro clave={i} />
          </button>
        ))}
        <span className="lg-ico-vista" title="Así se verá"><IconoLogro clave={datos.icono} /></span>
      </div>

      <Campo etiqueta="Condición para desbloquear" ayuda={condicion?.explicacion}>
        <select value={datos.condicion} onChange={(e) => cambiar('condicion', e.target.value)}>
          {opciones.condiciones.map((c) => <option key={c.codigo} value={c.codigo}>{c.texto}</option>)}
        </select>
        <MsgError texto={errores.condicion} />
      </Campo>
      {condicion?.requiereParametro && (
        <Campo etiqueta="Parámetro" ayuda="Esta condición necesita un valor.">
          {condicion.valores.length > 0 ? (
            <select value={datos.parametro} onChange={(e) => cambiar('parametro', e.target.value)}>
              <option value="">Elige un valor…</option>
              {condicion.valores.map((v) => <option key={v} value={v}>{v}</option>)}
            </select>
          ) : (
            <input value={datos.parametro} maxLength={60} onChange={(e) => cambiar('parametro', e.target.value)} />
          )}
          <MsgError texto={errores.parametro} />
        </Campo>
      )}

      <div className="ak-dos lg-dos">
        <Campo etiqueta="Meta (veces)"><input type="number" min={1} inputMode="numeric" value={datos.meta} onChange={numero('meta')} /><MsgError texto={errores.meta} /></Campo>
        <Campo etiqueta="Orden"><input type="number" min={0} inputMode="numeric" value={datos.orden} onChange={numero('orden')} /><MsgError texto={errores.orden} /></Campo>
      </div>
      <div className="ak-dos lg-dos">
        <Campo etiqueta="Recompensa (hojas)"><input type="number" min={0} inputMode="numeric" value={datos.recompensa_monedas} onChange={numero('recompensa_monedas')} /><MsgError texto={errores.recompensa_monedas} /></Campo>
        <Campo etiqueta="Recompensa (XP)"><input type="number" min={0} inputMode="numeric" value={datos.recompensa_xp} onChange={numero('recompensa_xp')} /><MsgError texto={errores.recompensa_xp} /></Campo>
      </div>

      <Alternador titulo="Secreto" detalle="Bloqueado se ve como «???»" activo={datos.secreto} onChange={(v) => cambiar('secreto', v)} />
      <Alternador titulo="Activo" detalle="Los usuarios pueden desbloquearlo" activo={datos.activo} onChange={(v) => cambiar('activo', v)} />

      {errorServidor && <p className="ak-error" role="alert">{errorServidor}</p>}
      <div className="ak-pie lg-pie">
        <button type="button" className="ak-btn ak-btn-g" onClick={onCancelar} disabled={guardando}>Cancelar</button>
        <button type="submit" className="ak-btn" disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar'}</button>
      </div>
    </form>
  )
}
