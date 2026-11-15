import { useCallback, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { RUTAS } from '../../../routes/rutas'
import { obtenerPlantaJardin } from '../../../services/jardin'
import { comprarAbono, comprarItem, obtenerTienda } from '../../../services/tienda'
import VistasDelTema from '../../../components/VistasDelTema'
import './tienda.css'

const MS_AVISO = 3000

export default function Tienda() {
  const [estado, setEstado] = useState({ cargando: true, tienda: null, error: '' })
  const [pestana, setPestana] = useState('FONDO')
  const [aviso, setAviso] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const temporizador = useRef(null)

  const cargar = useCallback(async () => {
    try {
      setEstado({ cargando: false, tienda: await obtenerTienda(), error: '' })
    } catch (err) {
      setEstado({ cargando: false, tienda: null, error: err instanceof TypeError ? 'No se pudo conectar con el servidor.' : err.message })
    }
  }, [])

  useEffect(() => { cargar() }, [cargar])
  useEffect(() => () => clearTimeout(temporizador.current), [])

  const avisar = (texto) => {
    setAviso(texto)
    clearTimeout(temporizador.current)
    temporizador.current = setTimeout(() => setAviso(''), MS_AVISO)
  }

  // Ejecuta una accion de la tienda y muestra su resultado o el error del servidor
  const accion = async (hacer, exito) => {
    if (ocupado) return
    setOcupado(true)
    try {
      const resultado = await hacer()
      if (resultado?.items) setEstado((e) => ({ ...e, tienda: resultado }))
      else await cargar()
      avisar(exito)
    } catch (err) {
      avisar(err.message || 'No se pudo completar la acción.')
    } finally {
      setOcupado(false)
    }
  }

  const usarAbono = () => accion(async () => {
    const planta = await obtenerPlantaJardin()
    if (!planta) throw new Error('Primero digitaliza una planta para abonarla.')
    await comprarAbono(planta.id)
    return null
  }, '¡Abono aplicado! Tu planta ganó un día de crecimiento 🌱')

  if (estado.cargando) return <div className="home-cargando"><span className="home-giro" aria-label="Cargando" /></div>
  if (estado.error) {
    return (
      <div className="coleccion-vacio aparece">
        <span>😕</span>
        <h2>{estado.error}</h2>
        <button className="boton boton-suave" onClick={cargar}>Reintentar</button>
      </div>
    )
  }

  const { monedas, nivel, items } = estado.tienda
  const fondos = items.filter((i) => i.tipo === 'FONDO')
  const abonos = items.filter((i) => i.tipo === 'ABONO')

  return (
    <div className="tienda">
      <section className="tienda-cartera aparece">
        <span>🍃 <b>{monedas}</b> monedas</span>
        <span>⭐ Nivel <b>{nivel}</b></span>
      </section>

      <div className="tienda-pestanas" role="tablist">
        <button role="tab" aria-selected={pestana === 'FONDO'} className={pestana === 'FONDO' ? 'activa' : ''} onClick={() => setPestana('FONDO')}>🖼️ Fondos</button>
        <button role="tab" aria-selected={pestana === 'ABONO'} className={pestana === 'ABONO' ? 'activa' : ''} onClick={() => setPestana('ABONO')}>🌿 Abono</button>
      </div>

      {pestana === 'FONDO' && (
        fondos.length === 0 ? (
          <p className="historial-vacio">Aún no hay fondos a la venta.</p>
        ) : (
          <div className="tienda-grilla">
            {fondos.map((f) => (
              <article key={f.id} className="tarjeta tienda-item aparece">
                <VistasDelTema variantes={f.variantes} />
                <div className="tienda-datos">
                  <b>{f.nombre}</b>
                  <small>{f.descripcion}</small>
                </div>
                {f.comprado ? (
                  <Link className="boton boton-suave tienda-tengo" to={RUTAS.JARDIN}>✓ Lo tienes · elígelo en tu jardín 🖼️</Link>
                ) : f.bloqueado ? (
                  <button className="boton boton-suave" disabled>🔒 Nivel {f.nivelRequerido}</button>
                ) : (
                  <button className="boton boton-principal" disabled={ocupado || monedas < f.precio} onClick={() => accion(() => comprarItem(f.id), `Compraste «${f.nombre}». Ponlo desde el ícono 🖼️ de tu jardín`)}>
                    Comprar · {f.precio} 🍃
                  </button>
                )}
              </article>
            ))}
          </div>
        )
      )}

      {pestana === 'ABONO' && (
        abonos.length === 0 ? (
          <p className="historial-vacio">El abono no está disponible por ahora.</p>
        ) : (
          <div className="tienda-grilla">
            {abonos.map((a) => (
              <article key={a.id} className="tarjeta tienda-item aparece">
                <div className="tienda-icono-grande">🌿</div>
                <div className="tienda-datos">
                  <b>{a.nombre}</b>
                  <small>{a.descripcion}</small>
                </div>
                <button className="boton boton-principal" disabled={ocupado || monedas < a.precio || a.bloqueado} onClick={usarAbono}>
                  {a.bloqueado ? `🔒 Nivel ${a.nivelRequerido}` : `Usar en mi planta · ${a.precio} 🍃`}
                </button>
              </article>
            ))}
            <p className="tienda-nota">Se aplica a la planta de tu <Link to={RUTAS.JARDIN}>jardín</Link>. Máximo un abono al día por planta.</p>
          </div>
        )
      )}

      {aviso && <div className="jardin-aviso" role="status">{aviso}</div>}
    </div>
  )
}
