import { useEffect, useState } from 'react'
import { efectosEvento, etiquetaTipo, iconoTipo } from '../../../services/admin/eventos'

const RESULTADO = {
  SALVADA: ['Salvada', 'est-final'],
  DANADA: ['Dañada', 'est-programado'],
  DESTRUIDA: ['Destruida', 'est-activo'],
}

// Efecto de un desastre ya terminado: cuantas plantas se salvaron y que le paso a cada una
export default function EfectoEvento({ evento, onCerrar }) {
  const [estado, setEstado] = useState({ cargando: true, datos: null, error: '' })

  useEffect(() => {
    let vivo = true
    efectosEvento(evento.id_desastre)
      .then((datos) => { if (vivo) setEstado({ cargando: false, datos, error: '' }) })
      .catch(() => { if (vivo) setEstado({ cargando: false, datos: null, error: 'No se pudo leer el efecto del desastre.' }) })
    return () => { vivo = false }
  }, [evento.id_desastre])

  const { cargando, datos, error } = estado

  return (
    <section className="tarjeta aparece efecto">
      <div className="dashboard-titulo">
        <div>
          <h2>{iconoTipo(evento.tipo_evento)} Efecto de {etiquetaTipo(evento.tipo_evento)} #EVT-{evento.id_desastre}</h2>
          <p>Qué pasó con las plantas de los usuarios cuando terminó el desastre</p>
        </div>
        <button className="efecto-cerrar" onClick={onCerrar}>Cerrar</button>
      </div>

      {cargando && <p className="historial-vacio">Cargando…</p>}
      {error && <p className="home-error" role="alert">{error}</p>}

      {datos && datos.resumen.total === 0 && (
        <p className="historial-vacio">Este desastre no afectó a ninguna planta (aún no terminó o no había plantas en la región).</p>
      )}

      {datos && datos.resumen.total > 0 && (
        <>
          <div className="efecto-resumen">
            <span className="cat-dato">Plantas <b>{datos.resumen.total}</b></span>
            <span className="cat-dato">Salvadas <b>{datos.resumen.salvadas}</b></span>
            <span className="cat-dato">Dañadas <b>{datos.resumen.danadas}</b></span>
            <span className="cat-dato">Destruidas <b>{datos.resumen.destruidas}</b></span>
            <span className="cat-dato">Se refugiaron <b>{datos.resumen.refugiadas}</b></span>
            <span className="cat-dato">XP dado <b>{datos.resumen.xp_total}</b></span>
          </div>
          <div className="tabla-escaneos">
            <table>
              <thead><tr><th>Usuario</th><th>Planta</th><th>Resultado</th><th>Se refugió</th><th>Daño</th><th>XP</th></tr></thead>
              <tbody>
                {datos.registros.map((r) => {
                  const [texto, clase] = RESULTADO[r.estado_final] || [r.estado_final, '']
                  return (
                    <tr key={r.id_registro}>
                      <td>{r.usuario}</td>
                      <td>{r.planta}</td>
                      <td><span className={`tabla-estado ${clase}`}>{texto}</span></td>
                      <td>{r.estaba_refugiada ? 'Sí' : 'No'}</td>
                      <td>{r.dano_salud}%</td>
                      <td>{r.xp_otorgado}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </>
      )}
    </section>
  )
}
