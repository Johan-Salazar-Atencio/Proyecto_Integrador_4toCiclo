import { useState } from 'react'
import { urlImagen } from '../../../services/home'
import { MOMENTOS_FONDO } from '../../../services/admin/tienda'

const VACIO_ESCENA = { archivo: null, cuadros: '' }

// Es una tira (PNG/JPG) y no una animacion (GIF/WebP): hay que decir cuantos cuadros tiene
const necesitaCuadros = (archivo) => archivo && /\.(png|jpe?g)$/i.test(archivo.name)

// Alta de un tema de fondo o edicion de un producto. `producto` null = agregar un fondo nuevo.
// Un fondo trae tres escenas (dia, tarde y noche); al editar solo se reemplazan las que se suben de nuevo.
export default function FormularioProducto({ producto, onGuardar, onCancelar }) {
  const [datos, setDatos] = useState(producto
    ? { nombre: producto.nombre, descripcion: producto.descripcion || '', precio_monedas: producto.precio_monedas, nivel_requerido: producto.nivel_requerido }
    : { nombre: '', descripcion: '', precio_monedas: 100, nivel_requerido: 1 })
  const [escenas, setEscenas] = useState({ dia: VACIO_ESCENA, tarde: VACIO_ESCENA, noche: VACIO_ESCENA })
  const [ms, setMs] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  const esFondo = !producto || producto.tipo === 'FONDO'
  const cambiar = (campo, valor) => setDatos((d) => ({ ...d, [campo]: valor }))
  const numero = (campo) => (e) => cambiar(campo, e.target.value === '' ? '' : Number(e.target.value))
  const cambiarEscena = (clave, cambios) => setEscenas((e) => ({ ...e, [clave]: { ...e[clave], ...cambios } }))

  const enviar = async (e) => {
    e.preventDefault()
    setError('')
    const subidas = MOMENTOS_FONDO.filter((m) => escenas[m.clave].archivo)
    if (!producto && subidas.length === 0) {
      setError('Sube al menos una escena (día, tarde o noche).')
      return
    }
    for (const m of subidas) {
      const { archivo, cuadros } = escenas[m.clave]
      if (necesitaCuadros(archivo) && !(Number(cuadros) >= 1)) {
        setError(`La imagen de ${m.texto.toLowerCase()} es una tira: indica cuántos cuadros tiene.`)
        return
      }
    }
    setGuardando(true)
    try {
      if (subidas.length === 0 && producto) {
        await onGuardar(datos) // solo datos: JSON
      } else {
        const formulario = new FormData()
        Object.entries(datos).forEach(([campo, valor]) => formulario.append(campo, valor ?? ''))
        if (!producto) formulario.append('tipo', 'FONDO')
        if (ms !== '') formulario.append('ms', ms)
        subidas.forEach((m) => {
          formulario.append(m.clave, escenas[m.clave].archivo)
          if (necesitaCuadros(escenas[m.clave].archivo)) formulario.append(`cuadros_${m.clave}`, escenas[m.clave].cuadros)
        })
        await onGuardar(formulario)
      }
    } catch (err) {
      setError(err.message)
      setGuardando(false)
    }
  }

  const actuales = Object.fromEntries((producto?.variantes || []).map((v) => [v.momento, v]))

  return (
    <form className="tarjeta formulario mision-form aparece" onSubmit={enviar}>
      <h2>{producto ? 'Editar producto' : 'Agregar fondo a la tienda'}</h2>
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

      {esFondo && (
        <fieldset className="tienda-escenas">
          <legend>Escenas del fondo {producto ? '(sube solo las que quieras reemplazar)' : '(sube al menos una)'}</legend>
          <p className="tienda-ayuda">
            Cada escena es un <b>GIF, WebP o APNG animado</b>, o una <b>tira PNG/JPG</b> con los cuadros uno al lado del otro
            (indica cuántos). Se recorta al centro en cuadrado. Si falta alguna escena, ese momento del día usa el fondo de siempre.
          </p>
          {MOMENTOS_FONDO.map((m) => (
            <div key={m.clave} className="tienda-escena">
              <b>{m.icono} {m.texto}</b>
              {actuales[m.momento] && (
                <img className="tienda-escena-actual" src={urlImagen(actuales[m.momento].vista)} alt={`Escena actual de ${m.texto}`} onError={(e) => { e.currentTarget.style.display = 'none' }} />
              )}
              <input
                type="file"
                accept="image/gif,image/webp,image/png,image/jpeg"
                aria-label={`Imagen de ${m.texto}`}
                onChange={(e) => cambiarEscena(m.clave, { archivo: e.target.files?.[0] || null })}
              />
              {necesitaCuadros(escenas[m.clave].archivo) && (
                <label className="tienda-cuadros"><span>Cuadros de la tira</span>
                  <input type="number" min={1} max={200} value={escenas[m.clave].cuadros} onChange={(e) => cambiarEscena(m.clave, { cuadros: e.target.value })} />
                </label>
              )}
            </div>
          ))}
          <label className="tienda-cuadros"><span>Milisegundos por cuadro (opcional; un GIF usa el suyo)</span>
            <input type="number" min={50} max={1000} placeholder="120" value={ms} onChange={(e) => setMs(e.target.value)} />
          </label>
        </fieldset>
      )}

      {error && <p className="home-error" role="alert">{error}</p>}
      <div className="mision-acciones">
        <button type="button" className="mision-secundario" onClick={onCancelar} disabled={guardando}>Cancelar</button>
        <button type="submit" className="mision-primario" disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar'}</button>
      </div>
    </form>
  )
}
