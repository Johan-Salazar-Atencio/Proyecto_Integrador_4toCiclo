import { useEffect, useState } from 'react'
import { crearEspecie, cuidadosDe, guardarEspecie } from '../../../services/admin/catalogo'
import { cambiosDeEspecie } from '../../../services/admin/respuestas.js'
import { estiloDe } from '../../user/planta/mapeo'
import { Alternador, Campo, Icono } from '../misiones/kit'

const CAMPOS = ['nombreComun', 'nombreCientifico', 'imagen', 'riegoDias', 'solHoras', 'tempMin', 'tempMax', 'toxica', 'publicada']
const original = (especie) => Object.fromEntries(CAMPOS.map((c) => [c, especie[c]]))
// Valores con los que arranca una especie nueva (los mismos por defecto que usa Django)
const NUEVA = { nombreComun: '', nombreCientifico: '', imagen: '', riegoDias: 7, solHoras: 4, tempMin: 10, tempMax: 30, toxica: false, publicada: true }

// Panel "Editar especie" (o "Nueva especie" si `especie` es null): datos, guía de cuidados y moderación
export default function PanelEspecie({ especie, onCerrar, onCambio, onCreada }) {
  const nueva = !especie
  const base = nueva ? NUEVA : especie
  const [form, setForm] = useState(() => original(base))
  const [cuidados, setCuidados] = useState({ cargando: true, lista: [] })
  const [guardando, setGuardando] = useState(false)
  const [aviso, setAviso] = useState({ texto: '', error: false })

  useEffect(() => {
    if (nueva) return undefined
    let vivo = true
    cuidadosDe(especie.id)
      .then((lista) => vivo && setCuidados({ cargando: false, lista }))
      .catch(() => vivo && setCuidados({ cargando: false, lista: [] }))
    return () => { vivo = false }
  }, [nueva, especie?.id])

  const poner = (campo, valor) => { setForm((f) => ({ ...f, [campo]: valor })); setAviso({ texto: '', error: false }) }
  const numero = (campo) => (e) => poner(campo, e.target.value === '' ? '' : Number(e.target.value))

  const cambiado = nueva || CAMPOS.some((c) => form[c] !== especie[c])
  const valido = form.nombreComun.trim() && form.nombreCientifico.trim() && form.riegoDias > 0 && form.solHoras !== '' && form.solHoras >= 0

  const guardar = async () => {
    setGuardando(true)
    setAviso({ texto: '', error: false })
    try {
      if (nueva) {
        const creada = await crearEspecie(form)
        onCreada(creada.id_catalogo)
        return
      }
      // Solo lo que cambio: Django valida tipo y rango de cada campo (QA-10/QA-11)
      const limpio = { ...form, nombreComun: form.nombreComun.trim(), nombreCientifico: form.nombreCientifico.trim() }
      await guardarEspecie(especie.id, cambiosDeEspecie(limpio, especie, CAMPOS))
      setAviso({ texto: 'Guardado en la base de datos', error: false })
      onCambio()
    } catch (err) {
      setAviso({ texto: `Error al guardar: ${err.message || err}`, error: true })
    } finally {
      setGuardando(false)
    }
  }

  return (
    <aside className="ak-card ak-panel">
      <div className="ak-panel-cab">
        <div>
          <h3>{nueva ? 'Nueva especie' : 'Editar especie'}</h3>
          <small className="ak-cat-id">{nueva ? 'Se guarda en el catálogo al pulsar «Crear especie»' : `#ESP-${String(especie.id).padStart(4, '0')}`}</small>
        </div>
        <button type="button" className="ak-ib" onClick={onCerrar} aria-label="Cerrar panel"><Icono nombre="x" /></button>
      </div>

      <div className="ak-dos">
        <Campo etiqueta="Nombre común"><input value={form.nombreComun} onChange={(e) => poner('nombreComun', e.target.value)} /></Campo>
        <Campo etiqueta="Nombre científico"><input value={form.nombreCientifico} onChange={(e) => poner('nombreCientifico', e.target.value)} /></Campo>
      </div>
      <div className="ak-dos">
        <Campo etiqueta="Imagen (ruta)" ayuda={nueva ? 'Opcional: si la dejas vacía se usa la imagen por defecto.' : undefined}>
          <input value={form.imagen} onChange={(e) => poner('imagen', e.target.value)} placeholder="/sprites/default.png" />
        </Campo>
        {!nueva && (
          <Campo etiqueta="Escaneos de la IA" ayuda="Veces que se identificó esta especie.">
            <input value={especie.escaneos} readOnly disabled />
          </Campo>
        )}
      </div>

      <b>Guía de cuidados</b>
      <div className="ak-dos">
        <Campo etiqueta="Riego (cada N días)"><input type="number" min="1" value={form.riegoDias} onChange={numero('riegoDias')} /></Campo>
        <Campo etiqueta="Sol (horas)"><input type="number" min="0" value={form.solHoras} onChange={numero('solHoras')} /></Campo>
        <Campo etiqueta="Temp. mín. (°C)"><input type="number" value={form.tempMin} onChange={numero('tempMin')} /></Campo>
        <Campo etiqueta="Temp. máx. (°C)"><input type="number" value={form.tempMax} onChange={numero('tempMax')} /></Campo>
      </div>

      <Alternador titulo="Publicada" detalle="Disponible para los escaneos" activo={form.publicada} onChange={(v) => poner('publicada', v)} />
      <Alternador titulo="⚠ Tóxica para mascotas" detalle="Se avisa en la ficha" activo={form.toxica} onChange={(v) => poner('toxica', v)} />

      {nueva ? (
        <p className="ak-cat-nota">Las secciones de cuidado (riego, luz, abono…) las genera la IA la primera vez que un usuario escanee esta especie.</p>
      ) : (
      <div className="ak-cat-cuidados">
        <b>Secciones de cuidado <small>(solo lectura)</small></b>
        {cuidados.cargando ? <p>Cargando…</p> : cuidados.lista.length === 0 ? <p>Sin cuidados registrados.</p> : (
          <ul>
            {cuidados.lista.map((c) => (
              <li key={c.id}><span>{estiloDe(c.tipo).icono}</span><div><b>{c.titulo || estiloDe(c.tipo).texto}</b><p>{c.contenido}</p></div></li>
            ))}
          </ul>
        )}
      </div>
      )}

      {aviso.texto && <p className={aviso.error ? 'ak-error' : 'ak-ok'} role="status">{aviso.texto}</p>}

      <div className="ak-pie">
        <button type="button" className="ak-btn ak-btn-g ak-btn-sm" disabled={guardando || (!nueva && !cambiado)} onClick={() => { setForm(original(base)); setAviso({ texto: '', error: false }) }}>{nueva ? 'Limpiar' : 'Restaurar original'}</button>
        <button type="button" className="ak-btn" onClick={guardar} disabled={!cambiado || !valido || guardando}>{guardando ? 'Guardando…' : nueva ? 'Crear especie' : 'Guardar cambios'}</button>
      </div>
    </aside>
  )
}
