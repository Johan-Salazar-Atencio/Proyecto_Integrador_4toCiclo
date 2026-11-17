import { useState } from 'react'
import { urlImagen } from '../../../services/home'
import { MOMENTOS_FONDO } from '../../../services/admin/tienda'
import { Alternador, Campo, Icono, PENDIENTE } from '../misiones/kit'

export const TIPOS_PRODUCTO = [
  { valor: 'MACETA', texto: 'Maceta' },
  { valor: 'INVERNADERO', texto: 'Invernadero' },
  { valor: 'FONDO', texto: 'Fondo' },
  { valor: 'ABONO', texto: 'Abono' },
]

const VACIO_ESCENA = { archivo: null, cuadros: '' }

// Es una tira (PNG/JPG) y no una animacion (GIF/WebP): hay que decir cuantos cuadros tiene
const necesitaCuadros = (archivo) => archivo && /\.(png|jpe?g)$/i.test(archivo.name)

// Panel "Editar producto" (o "Nuevo producto" si `producto` es null).
// El backend solo crea fondos; un fondo trae tres escenas (día, tarde y noche) y al editar solo se reemplazan las que se suben de nuevo.
export default function FormularioProducto({ producto, onGuardar, onCancelar }) {
  const [tipo, setTipo] = useState(producto?.tipo || 'FONDO')
  const [datos, setDatos] = useState(producto
    ? { nombre: producto.nombre, descripcion: producto.descripcion || '', precio_monedas: producto.precio_monedas, nivel_requerido: producto.nivel_requerido, activo: producto.activo }
    : { nombre: '', descripcion: '', precio_monedas: 100, nivel_requerido: 1, activo: true })
  const [escenas, setEscenas] = useState({ dia: VACIO_ESCENA, tarde: VACIO_ESCENA, noche: VACIO_ESCENA })
  const [ms, setMs] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  const esFondo = tipo === 'FONDO'
  const sinSoporte = !producto && !esFondo // crear maceta, invernadero o abono: el backend aun no lo permite
  const cambiar = (campo, valor) => setDatos((d) => ({ ...d, [campo]: valor }))
  const numero = (campo) => (e) => cambiar(campo, e.target.value === '' ? '' : Number(e.target.value))
  const cambiarEscena = (clave, cambios) => setEscenas((e) => ({ ...e, [clave]: { ...e[clave], ...cambios } }))
  const actuales = Object.fromEntries((producto?.variantes || []).map((v) => [v.momento, v]))
  const vista = producto?.variantes?.[0]?.vista || producto?.vista

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
      const { activo, ...base } = datos
      const cuerpo = producto ? { ...base, ...(producto.tipo === 'MACETA' && !producto.activo ? {} : { activo }) } : { ...base, activo }
      if (subidas.length === 0 && producto) {
        await onGuardar(cuerpo) // solo datos: JSON
      } else {
        const formulario = new FormData()
        Object.entries(cuerpo).forEach(([campo, valor]) => formulario.append(campo, valor ?? ''))
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

  return (
    <form className="ak-card ak-panel" onSubmit={enviar}>
      <div className="ak-panel-cab">
        <h3>{producto ? 'Editar producto' : 'Nuevo producto'}</h3>
        <button type="button" className="ak-ib" aria-label="Cerrar panel" onClick={onCancelar}><Icono nombre="x" /></button>
      </div>

      <div className="ak-tienda-img">
        {vista ? (
          <img src={urlImagen(vista)} alt={`Vista de ${producto.nombre}`} onError={(e) => { e.currentTarget.style.display = 'none' }} />
        ) : (
          <span className="ak-tienda-sin">Sin imagen</span>
        )}
        <div className="ak-tienda-subir">
          <Icono nombre="up" />
          <div>
            <b>{esFondo ? 'Imagen = escenas del fondo' : 'Cambiar imagen'}</b>
            <small>{esFondo ? 'Súbelas más abajo' : PENDIENTE}</small>
          </div>
        </div>
      </div>

      <Campo etiqueta="Nombre">
        <input value={datos.nombre} maxLength={100} required onChange={(e) => cambiar('nombre', e.target.value)} />
      </Campo>
      <div className="ak-dos">
        <Campo etiqueta="Tipo">
          <select value={tipo} onChange={(e) => setTipo(e.target.value)} disabled={Boolean(producto)} title={producto ? PENDIENTE : undefined}>
            {TIPOS_PRODUCTO.map((t) => (
              <option key={t.valor} value={t.valor}>{t.texto}{!producto && t.valor !== 'FONDO' ? ' (pendiente de backend)' : ''}</option>
            ))}
          </select>
        </Campo>
        <Campo etiqueta="Nivel requerido">
          <input type="number" min={1} required value={datos.nivel_requerido} onChange={numero('nivel_requerido')} />
        </Campo>
      </div>
      <Campo etiqueta="Precio (hojas)">
        <input type="number" min={0} required value={datos.precio_monedas} onChange={numero('precio_monedas')} />
      </Campo>
      <Campo etiqueta="Descripción">
        <textarea rows={2} maxLength={200} value={datos.descripcion} onChange={(e) => cambiar('descripcion', e.target.value)} />
      </Campo>

      {esFondo && (
        <fieldset className="ak-escenas">
          <legend>Escenas del fondo {producto ? '(sube solo las que quieras reemplazar)' : '(sube al menos una)'}</legend>
          <p className="ak-sub">
            Cada escena es un <b>GIF, WebP o APNG animado</b>, o una <b>tira PNG/JPG</b> con los cuadros uno al lado del otro
            (indica cuántos). Si falta alguna, ese momento del día usa el fondo de siempre.
          </p>
          {MOMENTOS_FONDO.map((m) => (
            <div key={m.clave} className="ak-escena">
              <b>{m.icono} {m.texto}</b>
              {actuales[m.momento] && (
                <img src={urlImagen(actuales[m.momento].vista)} alt={`Escena actual de ${m.texto}`} onError={(e) => { e.currentTarget.style.display = 'none' }} />
              )}
              <input
                type="file"
                accept="image/gif,image/webp,image/png,image/jpeg"
                aria-label={`Imagen de ${m.texto}`}
                onChange={(e) => cambiarEscena(m.clave, { archivo: e.target.files?.[0] || null })}
              />
              {necesitaCuadros(escenas[m.clave].archivo) && (
                <Campo etiqueta="Cuadros de la tira">
                  <input type="number" min={1} max={200} value={escenas[m.clave].cuadros} onChange={(e) => cambiarEscena(m.clave, { cuadros: e.target.value })} />
                </Campo>
              )}
            </div>
          ))}
          <Campo etiqueta="Ms por cuadro (opcional)">
            <input type="number" min={50} max={1000} placeholder="120" value={ms} onChange={(e) => setMs(e.target.value)} />
          </Campo>
        </fieldset>
      )}

      <Alternador
        titulo="A la venta" detalle="Visible en la tienda" activo={datos.activo} onChange={(v) => cambiar('activo', v)}
        disabled={producto?.tipo === 'MACETA' && !producto.activo} tituloBoton={producto?.tipo === 'MACETA' && !producto.activo ? 'Las macetas aún no están disponibles' : undefined}
      />

      {sinSoporte && <p className="ak-error" role="status">Crear {TIPOS_PRODUCTO.find((t) => t.valor === tipo)?.texto.toLowerCase()}s está pendiente de backend: por ahora solo se pueden agregar fondos.</p>}
      {error && <p className="ak-error" role="alert">{error}</p>}
      <div className="ak-pie">
        <button type="button" className="ak-btn ak-btn-g" onClick={onCancelar} disabled={guardando}>Cancelar</button>
        <button type="submit" className="ak-btn" disabled={guardando || sinSoporte} title={sinSoporte ? PENDIENTE : undefined}>{guardando ? 'Guardando…' : 'Guardar'}</button>
      </div>
    </form>
  )
}
