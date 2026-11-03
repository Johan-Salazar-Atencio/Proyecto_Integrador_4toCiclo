import { useEffect, useState } from 'react'
import Boton from '../../../components/Boton'
import { cuidadosDe, descartarCambios, guardarEspecie } from '../../../services/admin/catalogo'
import { estiloDe } from '../../user/planta/mapeo'

const CAMPOS = ['nombreComun', 'nombreCientifico', 'riegoDias', 'solHoras', 'tempMin', 'tempMax', 'toxica', 'publicada']

// Panel lateral para revisar y editar una especie
export default function PanelEspecie({ especie, onCerrar, onCambio }) {
  const [form, setForm] = useState(() => Object.fromEntries(CAMPOS.map((c) => [c, especie[c]])))
  const [cuidados, setCuidados] = useState({ cargando: true, lista: [] })
  const [guardando, setGuardando] = useState(false)
  const [aviso, setAviso] = useState('')

  useEffect(() => {
    let vivo = true
    cuidadosDe(especie.id)
      .then((lista) => vivo && setCuidados({ cargando: false, lista }))
      .catch(() => vivo && setCuidados({ cargando: false, lista: [] }))
    return () => { vivo = false }
  }, [especie.id])

  const poner = (campo, valor) => { setForm((f) => ({ ...f, [campo]: valor })); setAviso('') }
  const numero = (campo) => (e) => poner(campo, e.target.value === '' ? '' : Number(e.target.value))

  const cambiado = CAMPOS.some((c) => form[c] !== especie[c])
  const valido = form.nombreComun.trim() && form.nombreCientifico.trim() && form.riegoDias > 0 && form.solHoras >= 0

  const guardar = async () => {
    setGuardando(true)
    await guardarEspecie(especie.id, { ...form, nombreComun: form.nombreComun.trim(), nombreCientifico: form.nombreCientifico.trim() })
    setGuardando(false)
    setAviso('Guardado (simulado: solo en este navegador)')
    onCambio()
  }

  const restaurar = () => {
    descartarCambios(especie.id)
    onCambio()
  }

  return (
    <aside className="cat-panel tarjeta aparece">
      <header>
        <div>
          <h2>{especie.nombreComun}</h2>
          <small>#ESP-{String(especie.id).padStart(4, '0')}</small>
        </div>
        <button className="cat-cerrar" onClick={onCerrar} aria-label="Cerrar panel">✕</button>
      </header>

      <label><span>Nombre común</span><input value={form.nombreComun} onChange={(e) => poner('nombreComun', e.target.value)} /></label>
      <label><span>Nombre científico</span><input value={form.nombreCientifico} onChange={(e) => poner('nombreCientifico', e.target.value)} /></label>

      <div className="cat-doble">
        <label><span>Riego (cada N días)</span><input type="number" min="1" value={form.riegoDias} onChange={numero('riegoDias')} /></label>
        <label><span>Sol (horas)</span><input type="number" min="0" value={form.solHoras} onChange={numero('solHoras')} /></label>
        <label><span>Temp. mín. (°C)</span><input type="number" value={form.tempMin} onChange={numero('tempMin')} /></label>
        <label><span>Temp. máx. (°C)</span><input type="number" value={form.tempMax} onChange={numero('tempMax')} /></label>
      </div>

      <label className="cat-interruptor">
        <input type="checkbox" checked={form.toxica} onChange={(e) => poner('toxica', e.target.checked)} />
        <span><b>⚠️ Tóxica para mascotas</b><small>Se avisa en la ficha de la planta</small></span>
      </label>
      <label className="cat-interruptor">
        <input type="checkbox" checked={form.publicada} onChange={(e) => poner('publicada', e.target.checked)} />
        <span><b>📖 Publicada en la enciclopedia</b><small>Disponible para los escaneos de los usuarios</small></span>
      </label>

      <div className="cat-cuidados">
        <h3>Guía de cuidados <small>(solo lectura)</small></h3>
        {cuidados.cargando ? <p className="cat-nota">Cargando…</p> : cuidados.lista.length === 0 ? <p className="cat-nota">Sin cuidados registrados.</p> : (
          <ul>
            {cuidados.lista.map((c) => (
              <li key={c.id}><span>{estiloDe(c.tipo).icono}</span><div><b>{c.titulo || estiloDe(c.tipo).texto}</b><p>{c.contenido}</p></div></li>
            ))}
          </ul>
        )}
      </div>

      {aviso && <p className="cat-ok" role="status">✅ {aviso}</p>}

      <div className="cat-acciones">
        <Boton onClick={guardar} cargando={guardando} disabled={!cambiado || !valido}>Guardar cambios</Boton>
        {especie.editada && <button className="cat-restaurar" onClick={restaurar}>Restaurar original</button>}
      </div>
    </aside>
  )
}
