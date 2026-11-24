import { useCallback, useEffect, useState } from 'react'
import { urlImagen } from '../../../services/home'
import {
  agregarFondo, cambiarEstadoProducto, editarProducto, listarProductos,
} from '../../../services/admin/tienda'
import FormularioProducto, { TIPOS_PRODUCTO } from './FormularioProducto'
import { Aviso, BotonIcono, Chip, Icono, Interruptor, PENDIENTE } from '../misiones/kit'
import { motivoSinVenta } from './venta'
import MacetaPixel from '../../../components/MacetaPixel'
import Invernadero from '../../../components/Invernadero'
import './tienda-admin.css'

const COLOR_TIPO = { MACETA: 'var(--luz)', INVERNADERO: 'var(--primario)', FONDO: 'var(--agua)', ABONO: 'var(--abono)' }
const FILTROS = [['TODOS', 'Todos'], ['MACETA', 'Macetas'], ['INVERNADERO', 'Invernaderos'], ['FONDO', 'Fondos'], ['ABONO', 'Abono']]
const COLUMNAS = 'minmax(0, 1.4fr) 104px 62px 62px 44px 36px'
const textoTipo = (v) => TIPOS_PRODUCTO.find((t) => t.valor === v)?.texto.toUpperCase() || v

function Vista({ p }) {
  const imagen = p.variantes?.[0]?.vista || p.vista
  // Macetas e invernaderos no tienen imagen subida: se dibujan en pixel art por su codigo, como en la tienda del usuario
  if (p.tipo === 'MACETA' && p.codigo) return <span className="ak-tienda-th ak-tienda-th-dibujo"><MacetaPixel codigo={p.codigo} /></span>
  if (p.tipo === 'INVERNADERO' && p.codigo) return <span className="ak-tienda-th ak-tienda-th-dibujo"><Invernadero tema={p.codigo} momento="DIA" /></span>
  return (
    <span className="ak-tienda-th">
      {imagen ? <img src={urlImagen(imagen)} alt="" onError={(e) => { e.currentTarget.style.display = 'none' }} /> : <span>{p.nombre.charAt(0)}</span>}
    </span>
  )
}

export default function TiendaAdmin() {
  const [estado, setEstado] = useState({ cargando: true, items: [], error: '' })
  const [panel, setPanel] = useState(null) // null = cerrado, {} = nuevo, producto = editar
  const [filtro, setFiltro] = useState('TODOS')
  const [error, setError] = useState('')

  const cargar = useCallback(async () => {
    try {
      const datos = await listarProductos()
      setEstado({ cargando: false, items: datos.items, error: '' })
    } catch (err) {
      setEstado({ cargando: false, items: [], error: err.message || 'No se pudo leer Django.' })
    }
  }, [])

  useEffect(() => { cargar() }, [cargar])

  const guardar = async (datos) => {
    if (panel?.id_item) await editarProducto(panel.id_item, datos)
    else await agregarFondo(datos)
    setPanel(null)
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

  const { cargando, items, error: errorCarga } = estado
  if (cargando) return <div className="ak-cargando"><span className="home-giro" aria-label="Cargando" /></div>

  const visibles = items.filter((i) => filtro === 'TODOS' || i.tipo === filtro)
  const venta = visibles.filter((i) => i.activo)
  const apagados = visibles.filter((i) => !i.activo)

  const fila = (p) => (
    <div key={p.id_item} className={`ak-tr ${panel?.id_item === p.id_item ? 'ak-tr-sel' : ''} ${p.activo ? '' : 'ak-apagada'}`} style={{ '--cols': COLUMNAS, '--ancho': '620px' }}>
      <div className="ak-tienda-prod">
        <Vista p={p} />
        <span><b>{p.nombre}</b>{p.variantes?.length > 0 && <small>{p.variantes.length} de 3 escenas</small>}</span>
      </div>
      <Chip color={COLOR_TIPO[p.tipo]}>{textoTipo(p.tipo)}</Chip>
      <span className="ak-hojas"><Icono nombre="leaf" tam={15} />{p.precio_monedas}</span>
      <span>Nivel {p.nivel_requerido}</span>
      <Interruptor
        activo={p.activo} onChange={() => alternar(p)} etiqueta={`${p.nombre}: a la venta`}
        disabled={Boolean(motivoSinVenta(p))} titulo={motivoSinVenta(p) || undefined}
      />
      <span className="ak-ra">
        <BotonIcono icono="edit" etiqueta={`Editar ${p.nombre}`} onClick={() => setPanel(p)} />
      </span>
    </div>
  )

  return (
    <div className="ak-pagina">
      <div className="ak-cab">
        <div><h2 className="ak-h2">Tienda</h2><p>Crea y edita macetas, invernaderos, fondos y abonos que los usuarios compran con hojas.</p></div>
        <button className="ak-btn" onClick={() => setPanel({})}><Icono nombre="plus" tam={18} />Nuevo producto</button>
      </div>
      <Aviso error={errorCarga} onReintentar={cargar} />
      <Aviso error={error} />

      <div className="ak-chips" role="group" aria-label="Filtrar por tipo">
        {FILTROS.map(([valor, texto]) => (
          <button key={valor} className={filtro === valor ? 'on' : ''} aria-pressed={filtro === valor} onClick={() => setFiltro(valor)}>{texto}</button>
        ))}
      </div>

      <div className={`ak-cols ${panel ? 'ak-con-panel' : ''}`}>
        <section className="ak-card">
          <h3>A la venta <span>· {venta.length}</span></h3>
          <div className="ak-lista">
            <div className="ak-th" style={{ '--cols': COLUMNAS, '--ancho': '620px' }}>
              <span>Producto</span><span>Tipo</span><span>Precio</span><span>Nivel</span><span>Venta</span><span />
            </div>
            {venta.length === 0 ? <p className="ak-vacio">No hay productos a la venta.</p> : venta.map(fila)}
          </div>
          <h3>Desactivados <span>· {apagados.length}</span></h3>
          <div className="ak-lista">
            {apagados.length === 0 ? <p className="ak-vacio">No hay productos desactivados.</p> : apagados.map(fila)}
          </div>
        </section>

        {panel && (
          <FormularioProducto
            key={panel.id_item || 'nuevo'}
            producto={panel.id_item ? panel : null}
            onGuardar={guardar}
            onCancelar={() => setPanel(null)}
          />
        )}
      </div>
    </div>
  )
}
