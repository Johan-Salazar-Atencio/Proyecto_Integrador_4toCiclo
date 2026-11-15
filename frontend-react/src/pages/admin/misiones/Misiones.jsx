import { useCallback, useEffect, useState } from 'react'
import {
  cambiarEstadoMision, crearMision, editarMision, iconoTipoMision, listarMisiones, textoTipoMision,
} from '../../../services/admin/misiones'
import FormularioMision from './FormularioMision'
import './misiones.css'

export default function Misiones() {
  const [estado, setEstado] = useState({ cargando: true, misiones: [], error: '' })
  const [formulario, setFormulario] = useState(null) // null = oculto, {} = nueva, mision = editar
  const [error, setError] = useState('')

  const cargar = useCallback(async () => {
    try {
      setEstado({ cargando: false, misiones: await listarMisiones(), error: '' })
    } catch (err) {
      setEstado({ cargando: false, misiones: [], error: err.message || 'No se pudo leer Django.' })
    }
  }, [])

  useEffect(() => { cargar() }, [cargar])

  const guardar = async (datos) => {
    if (formulario?.id_mision) await editarMision(formulario.id_mision, datos)
    else await crearMision(datos)
    setFormulario(null)
    await cargar()
  }

  const alternar = async (mision) => {
    setError('')
    try {
      await cambiarEstadoMision(mision.id_mision, !mision.activa)
      await cargar()
    } catch (err) {
      setError(err.message)
    }
  }

  const { cargando, misiones, error: errorCarga } = estado
  if (cargando) return <div className="home-cargando"><span className="home-giro" aria-label="Cargando" /></div>

  const activas = misiones.filter((m) => m.activa).length

  return (
    <div className="misiones">
      {errorCarga && (
        <div className="home-error" role="alert"><span>{errorCarga}</span><button onClick={cargar}>Reintentar</button></div>
      )}
      {error && <div className="home-error" role="alert"><span>{error}</span></div>}

      <section className="cat-resumen aparece">
        <span className="cat-dato">Activas <b>{activas}</b></span>
        <span className="cat-dato">Desactivadas <b>{misiones.length - activas}</b></span>
        <button className="mision-primario" onClick={() => setFormulario({})}>+ Nueva misión</button>
      </section>

      {formulario && (
        <FormularioMision
          key={formulario.id_mision || 'nueva'}
          mision={formulario.id_mision ? formulario : null}
          onGuardar={guardar}
          onCancelar={() => setFormulario(null)}
        />
      )}

      <section className="tarjeta aparece" style={{ animationDelay: '0.06s' }}>
        {misiones.length === 0 ? (
          <p className="historial-vacio">Aún no hay misiones. Crea la primera.</p>
        ) : (
          <div className="tabla-escaneos">
            <table className="cat-tabla">
              <thead><tr><th>Misión</th><th>Meta</th><th>Recompensa</th><th>Estado</th><th /></tr></thead>
              <tbody>
                {misiones.map((m) => (
                  <tr key={m.id_mision} className={m.activa ? '' : 'mision-apagada'}>
                    <td>
                      <b>{iconoTipoMision(m.tipo)} {m.titulo}</b>
                      {m.predeterminada && <span className="mision-etiqueta">Predeterminada</span>}
                      <small className="mision-detalle">{textoTipoMision(m.tipo)}{m.descripcion ? ` · ${m.descripcion}` : ''}</small>
                    </td>
                    <td>{m.meta}</td>
                    <td>{m.recompensa_xp} XP · {m.recompensa_monedas} 🍃</td>
                    <td>{m.activa ? 'Activa' : 'Desactivada'}</td>
                    <td className="mision-botones">
                      <button onClick={() => setFormulario(m)}>Editar</button>
                      <button onClick={() => alternar(m)}>{m.activa ? 'Desactivar' : 'Activar'}</button>
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
