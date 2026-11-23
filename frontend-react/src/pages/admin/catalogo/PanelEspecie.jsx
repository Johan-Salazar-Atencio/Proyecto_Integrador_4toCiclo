import { useEffect, useState } from 'react'
import { cuidadosDe, guardarEspecie } from '../../../services/admin/catalogo'
import { cambiosDeEspecie } from '../../../services/admin/respuestas.js'
import { estiloDe } from '../../user/planta/mapeo'
import { Alternador, Campo, Icono, PENDIENTE } from '../misiones/kit'

const CAMPOS = ['nombreComun', 'nombreCientifico', 'imagen', 'riegoDias', 'solHoras', 'tempMin', 'tempMax', 'toxica', 'publicada']
const original = (especie) => Object.fromEntries(CAMPOS.map((c) => [c, especie[c]]))

// Panel "Editar especie": datos, guía de cuidados y moderación
export default function PanelEspecie({ especie, onCerrar, onCambio }) {
  const [form, setForm] = useState(() => original(especie))
  const [cuidados, setCuidados] = useState({ cargando: true, lista: [] })
  const [guardando, setGuardando] = useState(false)
  const [aviso, setAviso] = useState({ texto: '', error: false })

  useEffect(() => {
    let vivo = true
    cuidadosDe(especie.id)
      .then((lista) => vivo && setCuidados({ cargando: false, lista }))
      .catch(() => vivo && setCuidados({ cargando: false, lista: [] }))
    return () => { vivo = false }
  }, [especie.id])

  const poner = (campo, valor) => { setForm((f) => ({ ...f, [campo]: valor })); setAviso({ texto: '', error: false }) }
  const numero = (campo) => (e) => poner(campo, e.target.value === '' ? '' : Number(e.target.value))

  const cambiado = CAMPOS.some((c) => form[c] !== especie[c])
  const valido = form.nombreComun.trim() && form.nombreCientifico.trim() && form.riegoDias > 0 && form.solHoras >= 0

  const guardar = async () => {
    setGuardando(true)
    setAviso({ texto: '', error: false })
    try {
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
          <h3>Editar especie</h3>
          <small className="ak-cat-id">#ESP-{String(especie.id).padStart(4, '0')}</small>
        </div>
        <button type="button" className="ak-ib" onClick={onCerrar} aria-label="Cerrar panel"><Icono nombre="x" /></button>
      </div>

      <div className="ak-dos">
        <Campo etiqueta="Nombre común"><input value={form.nombreComun} onChange={(e) => poner('nombreComun', e.target.value)} /></Campo>
        <Campo etiqueta="Nombre científico"><input value={form.nombreCientifico} onChange={(e) => poner('nombreCientifico', e.target.value)} /></Campo>
      </div>
      <div className="ak-dos">
        <Campo etiqueta="Familia (taxonomía)"><input value="" placeholder={PENDIENTE} disabled title={PENDIENTE} readOnly /></Campo>
        <Campo etiqueta="Imagen (ruta)"><input value={form.imagen} onChange={(e) => poner('imagen', e.target.value)} /></Campo>
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

      {aviso.texto && <p className={aviso.error ? 'ak-error' : 'ak-ok'} role="status">{aviso.texto}</p>}

      <div className="ak-pie">
        <button type="button" className="ak-btn ak-btn-g ak-btn-sm" disabled={!cambiado || guardando} onClick={() => { setForm(original(especie)); setAviso({ texto: '', error: false }) }}>Restaurar original</button>
        <button type="button" className="ak-btn" onClick={guardar} disabled={!cambiado || !valido || guardando}>{guardando ? 'Guardando…' : 'Guardar cambios'}</button>
      </div>
    </aside>
  )
}
