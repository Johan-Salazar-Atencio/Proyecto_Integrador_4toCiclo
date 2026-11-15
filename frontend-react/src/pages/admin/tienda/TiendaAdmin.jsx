import { useCallback, useEffect, useState } from 'react'
import { urlImagen } from '../../../services/home'
import { MOMENTOS } from '../../../services/tienda'
import {
  agregarFondo, cambiarEstadoProducto, editarProducto, listarProductos,
} from '../../../services/admin/tienda'
import FormularioProducto from './FormularioProducto'
import '../misiones/misiones.css'
import './tienda-admin.css'

const TEXTO_TIPO = { ABONO: '🌿 Abono', FONDO: '🖼️ Fondo', MACETA: '🪴 Maceta' }

export default function TiendaAdmin() {
  const [estado, setEstado] = useState({ cargando: true, items: [], disponibles: [], error: '' })
  const [formulario, setFormulario] = useState(null) // null = oculto, {} = agregar fondo, producto = editar
  const [error, setError] = useState('')

  const cargar = useCallback(async () => {
    try {
      const datos = await listarProductos()
      setEstado({ cargando: false, items: datos.items, disponibles: datos.fondos_disponibles, error: '' })
    } catch (err) {
      setEstado({ cargando: false, items: [], disponibles: [], error: err.message || 'No se pudo leer Django.' })
    }
  }, [])

  useEffect(() => { cargar() }, [cargar])

  const guardar = async (datos) => {
    if (formulario?.id_item) await editarProducto(formulario.id_item, datos)
    else await agregarFondo(datos)
    setFormulario(null)
    await cargar()
  }

  const alternar = async (producto) => {
    setError('')
    try {
      await cambiarEstadoProducto(producto.id_item, !producto.activo)
      await cargar()
    } catch (err) {
      setError(err.message)
    }
  }

  const { cargando, items, disponibles, error: errorCarga } = estado
  if (cargando) return <div className="home-cargando"><span className="home-giro" aria-label="Cargando" /></div>

  const activos = items.filter((i) => i.activo).length

  return (
    <div className="misiones">
      {errorCarga && (
        <div className="home-error" role="alert"><span>{errorCarga}</span><button onClick={cargar}>Reintentar</button></div>
      )}
      {error && <div className="home-error" role="alert"><span>{error}</span></div>}

      <section className="cat-resumen aparece">
        <span className="cat-dato">A la venta <b>{activos}</b></span>
        <span className="cat-dato">Desactivados <b>{items.length - activos}</b></span>
        <button
          className="mision-primario"
          disabled={disponibles.length === 0}
          title={disponibles.length === 0 ? 'No hay fondos nuevos para agregar' : ''}
          onClick={() => setFormulario({})}
        >+ Agregar fondo</button>
      </section>

      {formulario && (
        <FormularioProducto
          key={formulario.id_item || 'nuevo'}
          producto={formulario.id_item ? formulario : null}
          disponibles={disponibles}
          onGuardar={guardar}
          onCancelar={() => setFormulario(null)}
        />
      )}

      <section className="tarjeta aparece" style={{ animationDelay: '0.06s' }}>
        {items.length === 0 ? (
          <p className="historial-vacio">La tienda está vacía.</p>
        ) : (
          <div className="tabla-escaneos">
            <table className="cat-tabla">
              <thead><tr><th>Producto</th><th>Precio</th><th>Nivel</th><th>Estado</th><th /></tr></thead>
              <tbody>
                {items.map((p) => (
                  <tr key={p.id_item} className={p.activo ? '' : 'mision-apagada'}>
                    <td className="tienda-admin-producto">
                      {p.tipo === 'FONDO' && <img src={urlImagen(p.vista)} alt="" />}
                      <span>
                        <b>{p.nombre}</b>
                        <small className="mision-detalle">
                          {TEXTO_TIPO[p.tipo] || p.tipo}
                          {p.momento && ` · ${MOMENTOS[p.momento]?.icono || ''} ${MOMENTOS[p.momento]?.texto || p.momento}`}
                        </small>
                      </span>
                    </td>
                    <td>{p.precio_monedas} 🍃</td>
                    <td>{p.nivel_requerido}</td>
                    <td>{p.activo ? 'A la venta' : 'Desactivado'}</td>
                    <td className="mision-botones">
                      <button onClick={() => setFormulario(p)}>Editar</button>
                      {p.tipo !== 'MACETA' && <button onClick={() => alternar(p)}>{p.activo ? 'Desactivar' : 'Activar'}</button>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}
