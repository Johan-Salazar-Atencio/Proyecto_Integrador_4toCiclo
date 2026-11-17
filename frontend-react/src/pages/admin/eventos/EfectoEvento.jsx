import { useEffect, useState } from 'react'
import IconoAdmin from '../../../components/admin/IconoAdmin'
import { efectosEvento } from '../../../services/admin/eventos'
import { nombreDe } from './util'

const RESULTADO = {
  SALVADA: ['Salvada', 'var(--ok)'],
  DANADA: ['Dañada', 'var(--luz)'],
  DESTRUIDA: ['Destruida', 'var(--peligro)'],
}

// Efecto de un desastre ya terminado: cuantas plantas se salvaron y que le paso a cada una
export default function EfectoEvento({ evento, tipos, onCerrar }) {
  const [estado, setEstado] = useState({ cargando: true, datos: null, error: '' })

  useEffect(() => {
    let vivo = true
    efectosEvento(evento.id_desastre)
      .then((datos) => { if (vivo) setEstado({ cargando: false, datos, error: '' }) })
      .catch(() => { if (vivo) setEstado({ cargando: false, datos: null, error: 'No se pudo leer el efecto del desastre.' }) })
    return () => { vivo = false }
  }, [evento.id_desastre])

  useEffect(() => {
    const tecla = (e) => { if (e.key === 'Escape') onCerrar() }
    window.addEventListener('keydown', tecla)
    return () => window.removeEventListener('keydown', tecla)
  }, [onCerrar])

  const { cargando, datos, error } = estado
  const r = datos?.resumen

  return (
    <div className="ad-modal" onClick={onCerrar}>
      <div className="ad-hoja ev-efecto aparece" role="dialog" aria-modal="true" aria-label="Efecto del desastre" onClick={(e) => e.stopPropagation()}>
        <div className="ev-modal-cab">
          <div>
            <h3>Efecto de {nombreDe(tipos, evento.tipo_evento)} #EVT-{evento.id_desastre}</h3>
            <small className="ad-small">Qué pasó con las plantas de los usuarios cuando terminó el desastre</small>
          </div>
          <button type="button" className="ad-ib" aria-label="Cerrar" onClick={onCerrar}><IconoAdmin n="x" /></button>
        </div>

        {cargando && <p className="historial-vacio">Cargando…</p>}
        {error && <div className="ad-error" role="alert">{error}</div>}
        {r && r.total === 0 && <p className="historial-vacio">Este desastre no afectó a ninguna planta (aún no terminó o no había plantas en la región).</p>}

        {r && r.total > 0 && (
          <>
            <div className="ev-efecto-resumen">
              <span className="ad-chip" style={{ '--c': 'var(--primario)' }}>Plantas {r.total}</span>
              <span className="ad-chip" style={{ '--c': 'var(--ok)' }}>Salvadas {r.salvadas}</span>
              <span className="ad-chip" style={{ '--c': 'var(--luz)' }}>Dañadas {r.danadas}</span>
              <span className="ad-chip" style={{ '--c': 'var(--peligro)' }}>Destruidas {r.destruidas}</span>
              <span className="ad-chip" style={{ '--c': 'var(--agua)' }}>Se refugiaron {r.refugiadas}</span>
              <span className="ad-chip" style={{ '--c': 'var(--abono)' }}>XP dado {r.xp_total}</span>
            </div>
            <div className="ev-efecto-lista">
              <div className="ad-th" style={{ '--cols': 'minmax(0,1fr) minmax(0,1fr) 92px 64px 56px 56px' }}><span>Usuario</span><span>Planta</span><span>Resultado</span><span>Refugio</span><span>Daño</span><span>XP</span></div>
              {datos.registros.map((x) => {
                const [texto, color] = RESULTADO[x.estado_final] || [x.estado_final, 'var(--texto-suave)']
                return (
                  <div key={x.id_registro} className="ad-tr" style={{ '--cols': 'minmax(0,1fr) minmax(0,1fr) 92px 64px 56px 56px' }}>
                    <b>{x.usuario}</b>
                    <span>{x.planta}</span>
                    <span className="ad-chip" style={{ '--c': color }}>{texto}</span>
                    <span>{x.estaba_refugiada ? 'Sí' : 'No'}</span>
                    <span>{x.dano_salud}%</span>
                    <span>{x.xp_otorgado}</span>
                  </div>
                )
              })}
            </div>
          </>
        )}
      </div>
    </div>
  )
}
