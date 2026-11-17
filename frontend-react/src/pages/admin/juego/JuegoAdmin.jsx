import { useCallback, useEffect, useState } from 'react'
import { cambiarParametro, cambiarRegla, listarParametros, listarReglas, RANGOS_PARAMETROS } from '../../../services/admin/juego'
import { Aviso, BotonIcono, Icono, Interruptor, PENDIENTE } from '../misiones/kit'
import './juego.css'

const numero = (texto) => (texto === '' ? '' : Number(texto))
const COL_REGLAS = '1.25fr 74px 74px 90px 44px auto'
const COL_PARAM = '1fr 90px auto'

// Economia del juego: lo que paga cada accion, los parametros que Spring lee al momento y los niveles
export default function JuegoAdmin() {
  const [estado, setEstado] = useState({ cargando: true, parametros: [], reglas: [], error: '' })
  const [borrador, setBorrador] = useState({ reglas: {}, parametros: {} }) // solo lo que el admin cambió
  const [guardando, setGuardando] = useState(false)
  const [aviso, setAviso] = useState({ texto: '', error: false })

  const cargar = useCallback(async () => {
    try {
      const [parametros, reglas] = await Promise.all([listarParametros(), listarReglas()])
      setEstado({ cargando: false, parametros, reglas, error: '' })
      setBorrador({ reglas: {}, parametros: {} })
    } catch (err) {
      setEstado({ cargando: false, parametros: [], reglas: [], error: err.message || 'No se pudo leer Django.' })
    }
  }, [])
  useEffect(() => { cargar() }, [cargar])

  const reglaActual = (r) => ({ xp: r.xp, monedas: r.monedas, tope_diario: r.tope_diario ?? '', activa: r.activa, ...borrador.reglas[r.id_regla] })
  const valorActual = (p) => borrador.parametros[p.clave] ?? p.valor

  const tocarRegla = (r, cambios) => setBorrador((b) => ({ ...b, reglas: { ...b.reglas, [r.id_regla]: { ...b.reglas[r.id_regla], ...cambios } } }))
  const tocarParametro = (p, valor) => setBorrador((b) => ({ ...b, parametros: { ...b.parametros, [p.clave]: valor } }))

  const { cargando, parametros, reglas, error } = estado

  // Solo cuenta como cambio lo que difiere de lo guardado
  const reglasCambiadas = reglas.filter((r) => {
    const a = reglaActual(r)
    return a.xp !== r.xp || a.monedas !== r.monedas || (a.tope_diario === '' ? null : a.tope_diario) !== r.tope_diario || a.activa !== r.activa
  })
  const parametrosCambiados = parametros.filter((p) => valorActual(p) !== p.valor)
  const hayCambios = reglasCambiadas.length + parametrosCambiados.length > 0
  const hayVacios = reglasCambiadas.some((r) => reglaActual(r).xp === '' || reglaActual(r).monedas === '') || parametrosCambiados.some((p) => valorActual(p) === '')

  const guardarTodo = async () => {
    setGuardando(true)
    setAviso({ texto: '', error: false })
    try {
      for (const r of reglasCambiadas) {
        const a = reglaActual(r)
        await cambiarRegla(r.id_regla, { xp: a.xp, monedas: a.monedas, tope_diario: a.tope_diario === '' ? null : a.tope_diario, activa: a.activa })
      }
      for (const p of parametrosCambiados) await cambiarParametro(p.clave, valorActual(p))
      await cargar()
      setAviso({ texto: 'Cambios guardados. Spring ya los usa.', error: false })
    } catch (err) {
      setAviso({ texto: err.message, error: true })
    } finally {
      setGuardando(false)
    }
  }

  if (cargando) return <div className="ak-cargando"><span className="home-giro" aria-label="Cargando" /></div>

  return (
    <div className="ak-pagina">
      <div className="ak-cab">
        <p>Edita cuánto paga cada acción, los parámetros y los niveles. Vacío = sin tope. Spring los lee al momento: no hace falta reiniciar nada.</p>
        <button className="ak-btn" onClick={guardarTodo} disabled={!hayCambios || hayVacios || guardando}>
          <Icono nombre="check" tam={18} />{guardando ? 'Guardando…' : 'Guardar todo'}
        </button>
      </div>
      <Aviso error={error} onReintentar={cargar} />
      {aviso.texto && (aviso.error ? <Aviso error={aviso.texto} /> : <p className="ak-ok" role="status">{aviso.texto}</p>)}

      <div className="ak-juego-cols">
        <section className="ak-card">
          <div className="ak-titulo">
            <h3>Recompensas por acción</h3>
            <button className="ak-btn ak-btn-o ak-btn-sm" disabled title={PENDIENTE}><Icono nombre="plus" tam={15} />Nueva regla</button>
          </div>
          <div className="ak-lista">
            <div className="ak-th" style={{ '--cols': COL_REGLAS, '--ancho': '520px' }}>
              <span>Acción</span><span>Hojas</span><span>XP</span><span>Tope diario</span><span>Activa</span><span style={{ width: 34 }} />
            </div>
            {reglas.length === 0 && <p className="ak-vacio">No hay reglas.</p>}
            {reglas.map((r) => {
              const a = reglaActual(r)
              return (
                <div key={r.id_regla} className={`ak-tr ${a.activa ? '' : 'ak-apagada'}`} style={{ '--cols': COL_REGLAS, '--ancho': '520px' }}>
                  <div><b>{r.nombre}</b><small>{r.motivo}</small></div>
                  <input className="ak-in ak-in-c" type="number" min="0" value={a.monedas} onChange={(e) => tocarRegla(r, { monedas: numero(e.target.value) })} aria-label={`Hojas de ${r.nombre}`} />
                  <input className="ak-in ak-in-c" type="number" min="0" value={a.xp} onChange={(e) => tocarRegla(r, { xp: numero(e.target.value) })} aria-label={`XP de ${r.nombre}`} />
                  <input className="ak-in ak-in-c" type="number" min="1" value={a.tope_diario} placeholder="Sin tope" onChange={(e) => tocarRegla(r, { tope_diario: numero(e.target.value) })} aria-label={`Tope diario de ${r.nombre}`} />
                  <Interruptor activo={a.activa} onChange={(v) => tocarRegla(r, { activa: v })} etiqueta={`${r.nombre}: activa`} />
                  <BotonIcono icono="trash" peligro etiqueta={`Eliminar ${r.nombre}`} disabled titulo={PENDIENTE} />
                </div>
              )
            })}
          </div>
        </section>

        <div className="ak-juego-lado">
          <section className="ak-card">
            <div className="ak-titulo">
              <h3>Parámetros del juego</h3>
              <button className="ak-btn ak-btn-o ak-btn-sm" disabled title={PENDIENTE}><Icono nombre="plus" tam={15} />Nuevo</button>
            </div>
            <div className="ak-lista">
              {parametros.length === 0 && <p className="ak-vacio">No hay parámetros.</p>}
              {parametros.map((p) => {
                const rango = RANGOS_PARAMETROS[p.clave] || { min: 0, max: 100000, texto: p.clave }
                return (
                  <div key={p.clave} className="ak-tr" style={{ '--cols': COL_PARAM }}>
                    <div><b>{rango.texto}</b><small>{p.descripcion || p.clave} · de {rango.min} a {rango.max}</small></div>
                    <input className="ak-in ak-in-c" type="number" min={rango.min} max={rango.max} value={valorActual(p)} onChange={(e) => tocarParametro(p, numero(e.target.value))} aria-label={rango.texto} />
                    <BotonIcono icono="trash" peligro etiqueta={`Eliminar ${rango.texto}`} disabled titulo={PENDIENTE} />
                  </div>
                )
              })}
            </div>
          </section>

          <section className="ak-card">
            <div className="ak-titulo">
              <h3>Niveles e insignias</h3>
              <button className="ak-btn ak-btn-o ak-btn-sm" disabled title={PENDIENTE}><Icono nombre="plus" tam={15} />Nuevo nivel</button>
            </div>
            <p className="ak-vacio">Los niveles y sus insignias aún no se pueden leer ni editar desde aquí. <b>{PENDIENTE}</b>.</p>
          </section>
        </div>
      </div>
    </div>
  )
}
