import { useCallback, useEffect, useState } from 'react'
import { cambiarParametro, cambiarRegla, listarParametros, listarReglas, RANGOS_PARAMETROS } from '../../../services/admin/juego'
import './juego.css'

const numero = (texto) => (texto === '' ? '' : Number(texto))

function FilaParametro({ parametro, onGuardado, onError }) {
  const rango = RANGOS_PARAMETROS[parametro.clave] || { min: 0, max: 100000, texto: parametro.clave }
  const [valor, setValor] = useState(parametro.valor)
  const [guardando, setGuardando] = useState(false)
  useEffect(() => { setValor(parametro.valor) }, [parametro.valor])

  const guardar = async () => {
    setGuardando(true)
    try {
      onGuardado(await cambiarParametro(parametro.clave, valor))
    } catch (err) {
      onError(err.message)
      setValor(parametro.valor)
    } finally {
      setGuardando(false)
    }
  }

  return (
    <div className="juego-fila">
      <div className="juego-fila-texto">
        <b>{rango.texto}</b>
        <small>{parametro.descripcion || parametro.clave} · de {rango.min} a {rango.max}</small>
      </div>
      <input type="number" min={rango.min} max={rango.max} value={valor} onChange={(e) => setValor(numero(e.target.value))} aria-label={rango.texto} />
      <button className="mision-primario" disabled={guardando || valor === '' || valor === parametro.valor} onClick={guardar}>Guardar</button>
    </div>
  )
}

function FilaRegla({ regla, onGuardado, onError }) {
  const [xp, setXp] = useState(regla.xp)
  const [monedas, setMonedas] = useState(regla.monedas)
  const [tope, setTope] = useState(regla.tope_diario ?? '')
  const [activa, setActiva] = useState(regla.activa)
  const [guardando, setGuardando] = useState(false)
  useEffect(() => {
    setXp(regla.xp); setMonedas(regla.monedas); setTope(regla.tope_diario ?? ''); setActiva(regla.activa)
  }, [regla])

  const cambio = xp !== regla.xp || monedas !== regla.monedas || (tope === '' ? null : tope) !== regla.tope_diario || activa !== regla.activa
  const guardar = async () => {
    setGuardando(true)
    try {
      onGuardado(await cambiarRegla(regla.id_regla, { xp, monedas, tope_diario: tope === '' ? null : tope, activa }))
    } catch (err) {
      onError(err.message)
    } finally {
      setGuardando(false)
    }
  }

  return (
    <tr className={activa ? '' : 'mision-apagada'}>
      <td><b>{regla.nombre}</b><small className="mision-detalle">{regla.motivo}</small></td>
      <td><input type="number" min="0" value={xp} onChange={(e) => setXp(numero(e.target.value))} aria-label={`XP de ${regla.nombre}`} /></td>
      <td><input type="number" min="0" value={monedas} onChange={(e) => setMonedas(numero(e.target.value))} aria-label={`Hojas de ${regla.nombre}`} /></td>
      <td><input type="number" min="1" value={tope} placeholder="Sin tope" onChange={(e) => setTope(numero(e.target.value))} aria-label={`Tope diario de ${regla.nombre}`} /></td>
      <td><input type="checkbox" checked={activa} onChange={(e) => setActiva(e.target.checked)} aria-label={`${regla.nombre} activa`} /></td>
      <td className="mision-botones"><button disabled={!cambio || guardando || xp === '' || monedas === ''} onClick={guardar}>Guardar</button></td>
    </tr>
  )
}

// Economia del juego: los valores que Spring lee al momento (costo de revivir, plazos...) y lo que paga cada accion
export default function JuegoAdmin() {
  const [estado, setEstado] = useState({ cargando: true, parametros: [], reglas: [], error: '' })
  const [aviso, setAviso] = useState('')

  const cargar = useCallback(async () => {
    try {
      const [parametros, reglas] = await Promise.all([listarParametros(), listarReglas()])
      setEstado({ cargando: false, parametros, reglas, error: '' })
    } catch (err) {
      setEstado({ cargando: false, parametros: [], reglas: [], error: err.message || 'No se pudo leer Django.' })
    }
  }, [])
  useEffect(() => { cargar() }, [cargar])

  const avisar = (texto) => { setAviso(texto); setTimeout(() => setAviso(''), 2800) }
  const reemplazar = (campo, clave, nueva, texto) => {
    setEstado((e) => ({ ...e, [campo]: e[campo].map((x) => (x[clave] === nueva[clave] ? nueva : x)) }))
    avisar(texto)
  }

  const { cargando, parametros, reglas, error } = estado
  if (cargando) return <div className="home-cargando"><span className="home-giro" aria-label="Cargando" /></div>

  return (
    <div className="juego">
      {error && <div className="home-error" role="alert"><span>{error}</span><button onClick={cargar}>Reintentar</button></div>}

      <section className="tarjeta aparece">
        <h2>Parámetros del juego</h2>
        <p className="juego-nota">Spring los lee al momento: no hace falta reiniciar nada.</p>
        {parametros.length === 0 && !error && <p className="historial-vacio">No hay parámetros.</p>}
        {parametros.map((p) => (
          <FilaParametro key={p.clave} parametro={p} onGuardado={(n) => reemplazar('parametros', 'clave', n, 'Parámetro guardado')} onError={avisar} />
        ))}
      </section>

      <section className="tarjeta aparece" style={{ animationDelay: '0.06s' }}>
        <h2>Recompensas por acción</h2>
        <p className="juego-nota">Hojas y XP que paga cada acción, con su tope diario. Vacío = sin tope.</p>
        <div className="tabla-escaneos">
          <table className="cat-tabla juego-tabla">
            <thead><tr><th>Acción</th><th>XP</th><th>Hojas</th><th>Tope diario</th><th>Activa</th><th /></tr></thead>
            <tbody>
              {reglas.map((r) => (
                <FilaRegla key={r.id_regla} regla={r} onGuardado={(n) => reemplazar('reglas', 'id_regla', n, 'Regla guardada')} onError={avisar} />
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {aviso && <div className="jardin-aviso" role="status">{aviso}</div>}
    </div>
  )
}
