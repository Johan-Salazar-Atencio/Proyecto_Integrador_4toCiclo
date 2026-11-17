import { useState } from 'react'
import { TIPOS_MISION } from '../../../services/admin/misiones'
import { Alternador, Campo, Icono, PENDIENTE } from './kit'

const VACIA = { titulo: '', descripcion: '', tipo: 'RIEGO', meta: 1, recompensa_xp: 10, recompensa_monedas: 10, orden: 0, activa: true }

// Panel "Editar misión" (o "Nueva misión" si `mision` es null)
export default function FormularioMision({ mision, onGuardar, onCancelar, onEliminar }) {
  const [datos, setDatos] = useState(mision ? { ...VACIA, ...mision, descripcion: mision.descripcion || '' } : VACIA)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  const cambiar = (campo, valor) => setDatos((d) => ({ ...d, [campo]: valor }))
  const numero = (campo) => (e) => cambiar(campo, e.target.value === '' ? '' : Number(e.target.value))

  const enviar = async (e) => {
    e.preventDefault()
    setGuardando(true)
    setError('')
    try {
      await onGuardar({
        titulo: datos.titulo,
        descripcion: datos.descripcion,
        tipo: datos.tipo,
        meta: datos.meta,
        recompensa_xp: datos.recompensa_xp,
        recompensa_monedas: datos.recompensa_monedas, // el backend lo llama "monedas"; en pantalla son "hojas"
        orden: datos.orden,
        activa: datos.activa,
      })
    } catch (err) {
      setError(err.message)
      setGuardando(false)
    }
  }

  return (
    <form className="ak-card ak-panel" onSubmit={enviar}>
      <div className="ak-panel-cab">
        <h3>{mision ? 'Editar misión' : 'Nueva misión'}</h3>
        <button type="button" className="ak-ib" aria-label="Cerrar panel" onClick={onCancelar}><Icono nombre="x" /></button>
      </div>
      <Campo etiqueta="Título">
        <input value={datos.titulo} maxLength={100} required onChange={(e) => cambiar('titulo', e.target.value)} />
      </Campo>
      <Campo etiqueta="Qué debe hacer el usuario">
        <select value={datos.tipo} onChange={(e) => cambiar('tipo', e.target.value)}>
          {TIPOS_MISION.map((t) => <option key={t.valor} value={t.valor}>{t.texto}</option>)}
        </select>
      </Campo>
      <Campo etiqueta="Descripción">
        <textarea rows={2} value={datos.descripcion} onChange={(e) => cambiar('descripcion', e.target.value)} />
      </Campo>
      <div className="ak-dos">
        <Campo etiqueta="Meta (veces)"><input type="number" min={1} required value={datos.meta} onChange={numero('meta')} /></Campo>
        <Campo etiqueta="Orden"><input type="number" min={0} required value={datos.orden} onChange={numero('orden')} /></Campo>
      </div>
      <div className="ak-dos">
        <Campo etiqueta="Recompensa (hojas)"><input type="number" min={0} required value={datos.recompensa_monedas} onChange={numero('recompensa_monedas')} /></Campo>
        <Campo etiqueta="Recompensa (XP)"><input type="number" min={0} required value={datos.recompensa_xp} onChange={numero('recompensa_xp')} /></Campo>
      </div>
      <Alternador titulo="Activa" detalle="Los usuarios la ven hoy" activo={datos.activa} onChange={(v) => cambiar('activa', v)} />
      <Alternador titulo="Solo durante un evento" detalle="Se muestra con la etiqueta EVENTO" activo={false} onChange={() => {}} disabled tituloBoton={PENDIENTE} />
      {error && <p className="ak-error" role="alert">{error}</p>}
      <div className="ak-pie">
        {mision && (
          <button type="button" className="ak-btn ak-btn-d ak-btn-sm" onClick={onEliminar} disabled={!onEliminar} title={onEliminar ? undefined : PENDIENTE}>
            <Icono nombre="trash" tam={15} />Eliminar
          </button>
        )}
        <button type="button" className="ak-btn ak-btn-g" onClick={onCancelar} disabled={guardando}>Cancelar</button>
        <button type="submit" className="ak-btn" disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar'}</button>
      </div>
    </form>
  )
}
