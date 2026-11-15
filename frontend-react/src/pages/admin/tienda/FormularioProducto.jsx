import { useState } from 'react'
import { MOMENTOS } from '../../../services/tienda'

// Alta de un fondo a la venta o edicion de un producto. `producto` null = agregar un fondo nuevo.
export default function FormularioProducto({ producto, disponibles, onGuardar, onCancelar }) {
  const primero = disponibles[0]
  const [datos, setDatos] = useState(producto
    ? { nombre: producto.nombre, descripcion: producto.descripcion || '', precio_monedas: producto.precio_monedas, nivel_requerido: producto.nivel_requerido }
    : { id_fondo: primero?.id_fondo ?? '', nombre: primero?.nombre || '', descripcion: '', precio_monedas: 100, nivel_requerido: 1 })
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  const cambiar = (campo, valor) => setDatos((d) => ({ ...d, [campo]: valor }))
  const numero = (campo) => (e) => cambiar(campo, e.target.value === '' ? '' : Number(e.target.value))

  // Al elegir otro fondo, el nombre se completa con el suyo (se puede cambiar)
  const elegirFondo = (e) => {
    const id = Number(e.target.value)
    const fondo = disponibles.find((f) => f.id_fondo === id)
    setDatos((d) => ({ ...d, id_fondo: id, nombre: fondo?.nombre || d.nombre }))
  }

  const enviar = async (e) => {
    e.preventDefault()
    setGuardando(true)
    setError('')
    try {
      const comunes = {
        nombre: datos.nombre,
        descripcion: datos.descripcion,
        precio_monedas: datos.precio_monedas,
        nivel_requerido: datos.nivel_requerido,
      }
      await onGuardar(producto ? comunes : { ...comunes, tipo: 'FONDO', id_fondo: datos.id_fondo })
    } catch (err) {
      setError(err.message)
      setGuardando(false)
    }
  }

  return (
    <form className="tarjeta formulario mision-form aparece" onSubmit={enviar}>
      <h2>{producto ? 'Editar producto' : 'Agregar fondo a la tienda'}</h2>
      {!producto && (
        <label><span>Fondo</span>
          <select value={datos.id_fondo} required onChange={elegirFondo}>
            {disponibles.map((f) => <option key={f.id_fondo} value={f.id_fondo}>{MOMENTOS[f.momento]?.icono} {f.nombre} ({MOMENTOS[f.momento]?.texto})</option>)}
          </select>
        </label>
      )}
      <label><span>Nombre</span>
        <input value={datos.nombre} maxLength={100} required onChange={(e) => cambiar('nombre', e.target.value)} />
      </label>
      <label><span>Descripción</span>
        <textarea rows={2} maxLength={200} value={datos.descripcion} onChange={(e) => cambiar('descripcion', e.target.value)} />
      </label>
      <div className="mision-numeros">
        <label><span>Precio (monedas)</span><input type="number" min={0} required value={datos.precio_monedas} onChange={numero('precio_monedas')} /></label>
        <label><span>Nivel requerido</span><input type="number" min={1} required value={datos.nivel_requerido} onChange={numero('nivel_requerido')} /></label>
      </div>
      {error && <p className="home-error" role="alert">{error}</p>}
      <div className="mision-acciones">
        <button type="button" className="mision-secundario" onClick={onCancelar} disabled={guardando}>Cancelar</button>
        <button type="submit" className="mision-primario" disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar'}</button>
      </div>
    </form>
  )
}
