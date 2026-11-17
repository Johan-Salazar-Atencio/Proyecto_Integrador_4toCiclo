import { useCallback, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { RUTAS } from '../../../routes/rutas'
import { listarPlantasDigitales, obtenerPlantaJardin, ponerMaceta } from '../../../services/jardin'
import { comprarAbono, comprarItem, obtenerTienda } from '../../../services/tienda'
import Invernadero from '../../../components/Invernadero'
import MacetaPixel from '../../../components/MacetaPixel'
import { paletaDeMaceta } from '../../../components/macetas'
import VistasDelTema from '../../../components/VistasDelTema'
import './tienda.css'

const MS_AVISO = 3000

// Tienda (propuesta E): titulo con las hojas, Abono / Fondos / Macetas en un solo control, macetas en dos
// columnas (aplicarlas a varias plantas) e invernaderos con su vista previa vertical.
export default function Tienda() {
  const [estado, setEstado] = useState({ cargando: true, tienda: null, error: '' })
  const [pestana, setPestana] = useState('MACETA')
  const [aviso, setAviso] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const [aplicando, setAplicando] = useState(null) // maceta a la que se le eligen plantas
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
      avisar(err.estado === 402 ? 'No te alcanzan las hojas.' : err.message || 'No se pudo completar la acción.')
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

  const { monedas, items } = estado.tienda
  const fondos = items.filter((i) => i.tipo === 'FONDO')
  const abonos = items.filter((i) => i.tipo === 'ABONO')
  const macetas = items.filter((i) => i.tipo === 'MACETA')
  const invernaderos = items.filter((i) => i.tipo === 'INVERNADERO')

  return (
    <div className="tienda">
      <header className="tienda-titulo aparece">
        <h2>Tienda</h2>
        <span className="tienda-hojas">🍃 <b>{monedas}</b></span>
      </header>

      <div className="tienda-segmentos" role="tablist">
        {[['ABONO', 'Abono'], ['FONDO', 'Fondos'], ['MACETA', 'Macetas']].map(([clave, texto]) => (
          <button key={clave} role="tab" aria-selected={pestana === clave} className={pestana === clave ? 'activa' : ''} onClick={() => setPestana(clave)}>{texto}</button>
        ))}
      </div>

      {pestana === 'MACETA' && (
        macetas.length === 0 ? (
          <p className="historial-vacio">Aún no hay macetas a la venta.</p>
        ) : (
          <>
            <div className="tienda-macetas">
              {macetas.map((m) => {
                const color = paletaDeMaceta(m.codigo).cuerpo
                return (
                  <article key={m.id} className="tienda-maceta aparece" style={{ '--color': color }}>
                    <div className="tienda-maceta-vista"><MacetaPixel codigo={m.codigo} titulo={m.nombre} /></div>
                    <div className="tienda-maceta-datos">
                      <b>{m.nombre}</b>
                      {m.comprado ? <span className="chip-tuya">Tuya</span> : !m.bloqueado && <span className="chip-precio">🍃 {m.precio}</span>}
                    </div>
                    {m.comprado ? (
                      <button className="boton boton-suave" disabled={ocupado} onClick={() => setAplicando(m)}>Aplicar a plantas</button>
                    ) : m.bloqueado ? (
                      <button className="boton boton-suave" disabled>🔒 Nivel {m.nivelRequerido}</button>
                    ) : (
                      <button className="boton boton-principal" disabled={ocupado || monedas < m.precio} onClick={() => accion(() => comprarItem(m.id), `Maceta ${m.nombre} comprada`)}>Comprar</button>
                    )}
                  </article>
                )
              })}
            </div>
            <p className="tienda-nota">Las macetas se ven en tu colección de plantas virtuales y bajo tu planta.</p>
          </>
        )
      )}

      {pestana === 'FONDO' && (
        <>
          {invernaderos.length > 0 && (
            <>
              <h3 className="tienda-subtitulo">Invernaderos de tu colección</h3>
              <div className="tienda-invernaderos">
                {invernaderos.map((i) => (
                  <article key={i.id} className={`tienda-invernadero aparece ${i.comprado ? 'tienda-invernadero-mio' : ''}`}>
                    <Invernadero tema={i.codigo} momento="DIA" className="tienda-invernadero-vista" />
                    <div className="tienda-invernadero-datos">
                      <b>{i.nombre}</b>
                      <small>{i.descripcion}</small>
                      {i.comprado ? (
                        <span className="chip-tuya">✓ Lo tienes · elígelo desde el chip de tu colección</span>
                      ) : i.bloqueado ? (
                        <span className="chip-bloqueado">🔒 Nivel {i.nivelRequerido}</span>
                      ) : (
                        <div className="tienda-invernadero-compra">
                          <span className="chip-precio">🍃 {i.precio}</span>
                          <button className="boton boton-principal" disabled={ocupado || monedas < i.precio} onClick={() => accion(() => comprarItem(i.id), `Compraste «${i.nombre}». Elígelo desde el chip de tu colección`)}>Comprar</button>
                        </div>
                      )}
                    </div>
                  </article>
                ))}
              </div>
            </>
          )}
          <h3 className="tienda-subtitulo">Fondos de la escena de cada planta</h3>
          {fondos.length === 0 ? (
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
          )}
        </>
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

      {aplicando && (
        <AplicarMaceta
          maceta={aplicando}
          onCerrar={() => setAplicando(null)}
          onListo={(n) => { setAplicando(null); avisar(`Maceta ${aplicando.nombre} puesta a ${n} planta${n === 1 ? '' : 's'}`) }}
          onError={(texto) => { setAplicando(null); avisar(texto) }}
        />
      )}

      {aviso && <div className="jardin-aviso" role="status">{aviso}</div>}
    </div>
  )
}

// Hoja para elegir a cuales plantas virtuales se les pone la maceta (una, varias o todas, con buscador)
function AplicarMaceta({ maceta, onCerrar, onListo, onError }) {
  const [plantas, setPlantas] = useState(null)
  const [elegidas, setElegidas] = useState(new Set())
  const [consulta, setConsulta] = useState('')
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    listarPlantasDigitales().then((l) => setPlantas((l || []).filter((p) => !p.enBaul))).catch(() => onError('No se pudieron leer tus plantas.'))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const vistas = (plantas || []).filter((p) => {
    const q = consulta.trim().toLowerCase()
    return !q || p.name.toLowerCase().includes(q) || p.species.toLowerCase().includes(q)
  })
  const alternar = (id) => setElegidas((s) => { const n = new Set(s); if (n.has(id)) n.delete(id); else n.add(id); return n })
  const todas = vistas.length > 0 && vistas.every((p) => elegidas.has(p.id))
  const alternarTodas = () => setElegidas(todas ? new Set() : new Set(vistas.map((p) => p.id)))

  const aplicar = async () => {
    if (!elegidas.size || enviando) return
    setEnviando(true)
    try {
      await ponerMaceta(maceta.id, [...elegidas])
      onListo(elegidas.size)
    } catch (err) {
      onError(err.message || 'No se pudo poner la maceta.')
    }
  }

  return (
    <div className="layout-fondo" onClick={onCerrar}>
      <div className="layout-dialogo aparece maceta-hoja" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
        <div className="maceta-hoja-titulo">
          <div className="tienda-maceta-vista maceta-hoja-vista"><MacetaPixel codigo={maceta.codigo} /></div>
          <div>
            <h2>Aplicar maceta {maceta.nombre}</h2>
            <p>Elige las plantas virtuales</p>
          </div>
        </div>
        <div className="maceta-hoja-buscar">
          <input value={consulta} onChange={(e) => setConsulta(e.target.value)} placeholder="Buscar planta" aria-label="Buscar planta" />
          <button type="button" onClick={alternarTodas}>{todas ? 'Quitar todas' : 'Todas'}</button>
        </div>
        <div className="maceta-hoja-lista">
          {plantas === null && <p>Cargando…</p>}
          {plantas && vistas.length === 0 && <p>No hay plantas para mostrar.</p>}
          {vistas.map((p) => (
            <label key={p.id} className={`maceta-hoja-fila ${elegidas.has(p.id) ? 'elegida' : ''}`}>
              <input type="checkbox" checked={elegidas.has(p.id)} onChange={() => alternar(p.id)} />
              <span>{p.name}</span>
              <small>{p.macetaCodigo}</small>
              <i style={{ background: paletaDeMaceta(p.macetaCodigo).cuerpo }} />
            </label>
          ))}
        </div>
        <div className="layout-dialogo-botones">
          <button type="button" className="boton boton-suave" onClick={onCerrar}>Cancelar</button>
          <button type="button" className="boton boton-principal" disabled={!elegidas.size || enviando} onClick={aplicar}>
            {elegidas.size ? `Aplicar a ${elegidas.size} planta${elegidas.size === 1 ? '' : 's'}` : 'Selecciona plantas'}
          </button>
        </div>
      </div>
    </div>
  )
}
