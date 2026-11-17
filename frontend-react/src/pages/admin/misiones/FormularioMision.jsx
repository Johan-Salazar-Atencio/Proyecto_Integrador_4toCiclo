import { useState } from 'react'
import { TIPOS_MISION } from '../../../services/admin/misiones'

const VACIA = { titulo: '', descripcion: '', tipo: 'RIEGO', meta: 1, recompensa_xp: 10, recompensa_monedas: 10, orden: 0 }

// Alta y edicion de una mision. `mision` null = crear.
export default function FormularioMision({ mision, onGuardar, onCancelar }) {
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
        recompensa_monedas: datos.recompensa_monedas,
        orden: datos.orden,
      })
    } catch (err) {
      setError(err.message)
      setGuardando(false)
    }
  }

  return (
    <form className="tarjeta formulario mision-form aparece" onSubmit={enviar}>
      <h2>{mision ? 'Editar misión' : 'Nueva misión'}</h2>
      <label><span>Título</span>
        <input value={datos.titulo} maxLength={100} required onChange={(e) => cambiar('titulo', e.target.value)} />
      </label>
      <label><span>Descripción</span>
        <textarea rows={2} value={datos.descripcion} onChange={(e) => cambiar('descripcion', e.target.value)} />
      </label>
      <label><span>Qué debe hacer el usuario</span>
        <select value={datos.tipo} onChange={(e) => cambiar('tipo', e.target.value)}>
          {TIPOS_MISION.map((t) => <option key={t.valor} value={t.valor}>{t.icono} {t.texto}</option>)}
        </select>
      </label>
      <div className="mision-numeros">
        <label><span>Meta (veces)</span><input type="number" min={1} required value={datos.meta} onChange={numero('meta')} /></label>
        <label><span>XP</span><input type="number" min={0} required value={datos.recompensa_xp} onChange={numero('recompensa_xp')} /></label>
        <label><span>Monedas</span><input type="number" min={0} required value={datos.recompensa_monedas} onChange={numero('recompensa_monedas')} /></label>
        <label><span>Orden</span><input type="number" min={0} required value={datos.orden} onChange={numero('orden')} /></label>
      </div>
      {error && <p className="home-error" role="alert">{error}</p>}
      <div className="mision-acciones">
        <button type="button" className="mision-secundario" onClick={onCancelar} disabled={guardando}>Cancelar</button>
        <button type="submit" className="mision-primario" disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar'}</button>
      </div>
    </form>
  )
}
