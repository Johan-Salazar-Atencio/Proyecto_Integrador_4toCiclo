import { useEffect, useRef, useState } from 'react'
import EncabezadoAdmin from '../../../components/admin/EncabezadoAdmin'
import IconoAdmin from '../../../components/admin/IconoAdmin'
import { guardarTipoEvento } from '../../../services/admin/tiposEvento'
import { PENDIENTE, imagenDe } from './util'

const SALUD = [
  { campo: 'salud_final_bajo', texto: 'Severidad baja', color: 'var(--ok)' },
  { campo: 'salud_final_moderado', texto: 'Moderada', color: 'var(--luz)' },
  { campo: 'salud_final_critico', texto: 'Crítica', color: 'var(--peligro)' },
]

const valoresDe = (t) => ({
  nombre: t?.nombre || '',
  descripcion: t?.descripcion || '',
  consejo: t?.consejo || '',
  salud_final_bajo: t?.salud_final_bajo ?? 70,
  salud_final_moderado: t?.salud_final_moderado ?? 50,
  salud_final_critico: t?.salud_final_critico ?? 30,
})

// Mueve un deslizador y arrastra a los otros para que siempre se cumpla critica <= moderada <= baja
function conRegla(d, campo, valor) {
  const v = Math.max(1, Math.min(100, Math.round(Number(valor) || 1)))
  const n = { ...d, [campo]: v }
  if (campo === 'salud_final_bajo') {
    n.salud_final_moderado = Math.min(n.salud_final_moderado, v)
    n.salud_final_critico = Math.min(n.salud_final_critico, n.salud_final_moderado)
  } else if (campo === 'salud_final_moderado') {
    n.salud_final_bajo = Math.max(n.salud_final_bajo, v)
    n.salud_final_critico = Math.min(n.salud_final_critico, v)
  } else {
    n.salud_final_moderado = Math.max(n.salud_final_moderado, v)
    n.salud_final_bajo = Math.max(n.salud_final_bajo, n.salud_final_moderado)
  }
  return n
}

// Nuevo tipo de evento (tipo === null) o edicion de uno existente.
// Hoy el backend solo deja cambiar texto, consejo, salud e imagen de un tipo que ya existe.
export default function FormularioTipo({ tipo, regiones, onVolver, onGuardado, onError }) {
  const nuevo = tipo === null
  const [datos, setDatos] = useState(() => valoresDe(tipo))
  const [imagen, setImagen] = useState(null)
  const [vista, setVista] = useState(null)
  const [guardando, setGuardando] = useState(false)
  const selector = useRef(null)

  useEffect(() => {
    if (!imagen) { setVista(null); return undefined }
    const url = URL.createObjectURL(imagen)
    setVista(url)
    return () => URL.revokeObjectURL(url)
  }, [imagen])

  const poner = (campo, valor) => setDatos((d) => ({ ...d, [campo]: valor }))
  const foto = vista || imagenDe(tipo)
  const hayCambios = Boolean(imagen) || JSON.stringify(datos) !== JSON.stringify(valoresDe(tipo))

  const guardar = async () => {
    setGuardando(true)
    try {
      const nuevoTipo = await guardarTipoEvento(tipo.id_tipo_desastre, {
        descripcion: datos.descripcion,
        consejo: datos.consejo,
        salud_final_bajo: datos.salud_final_bajo,
        salud_final_moderado: datos.salud_final_moderado,
        salud_final_critico: datos.salud_final_critico,
      }, imagen)
      onGuardado(nuevoTipo)
    } catch (err) {
      onError(err.message || 'No se pudo guardar el tipo de evento.')
    } finally {
      setGuardando(false)
    }
  }

  return (
    <>
      <EncabezadoAdmin
        titulo={nuevo ? 'Nuevo tipo de evento' : `Editar ${tipo.nombre}`}
        subtitulo={nuevo ? 'Define cómo se ve y cómo afecta un desastre. Después podrás lanzarlo cuando quieras.' : 'Cambia el texto, la imagen y la salud con la que quedan las plantas.'}
      >
        <button type="button" className="ad-btn ad-btn-g" onClick={onVolver}><IconoAdmin n="back" />{nuevo ? 'Cancelar' : 'Volver'}</button>
      </EncabezadoAdmin>

      {nuevo && (
        <div className="ad-aviso" role="status"><IconoAdmin n="alert" /><span>{PENDIENTE}: por ahora el servidor no puede crear tipos de evento nuevos. Puedes armar el formulario y ver la vista previa, pero no se guarda.</span></div>
      )}

      <div className="ev-form">
        <section className="ad-card ad-c">
          <div className="ad-fld">
            <label htmlFor="tp-nombre">Nombre del evento</label>
            <input id="tp-nombre" className="ad-in" value={datos.nombre} maxLength={80} placeholder="Sequía prolongada" disabled={!nuevo} title={nuevo ? '' : PENDIENTE} onChange={(e) => poner('nombre', e.target.value)} />
            {!nuevo && <small className="ad-small">{PENDIENTE}: cambiar el nombre.</small>}
          </div>

          <div className="ad-dos">
            <div className="ad-fld">
              <label htmlFor="tp-region">Región por defecto</label>
              <select id="tp-region" className="ad-in" disabled={!nuevo} title={nuevo ? '' : PENDIENTE} defaultValue="GLOBAL">
                {regiones.map((r) => <option key={r.codigo} value={r.codigo}>{r.codigo === 'GLOBAL' ? 'Global (todas las regiones)' : r.nombre}</option>)}
              </select>
            </div>
            <div className="ad-fld">
              <label htmlFor="tp-dur">Duración por defecto (min)</label>
              <input id="tp-dur" className="ad-in" type="number" min="5" defaultValue={30} disabled={!nuevo} title={nuevo ? '' : PENDIENTE} />
            </div>
          </div>
          {!nuevo && <small className="ad-small">{PENDIENTE}: región y duración por defecto del tipo. Se eligen al lanzar.</small>}

          <div className="ad-fld">
            <label htmlFor="tp-texto">Texto informativo (lo que se lee en la tarjeta del Inicio)</label>
            <textarea id="tp-texto" className="ad-in" rows={4} maxLength={2000} value={datos.descripcion} placeholder="La falta de lluvia reseca el suelo. Riega con poca agua pero más seguido…" onChange={(e) => poner('descripcion', e.target.value)} />
          </div>

          <div className="ad-fld">
            <label htmlFor="tp-consejo">Consejo de campo (al prevenir) · {datos.consejo.length}/250</label>
            <input id="tp-consejo" className="ad-in" maxLength={250} value={datos.consejo} onChange={(e) => poner('consejo', e.target.value)} />
          </div>

          <div className="ad-fld">
            <label htmlFor="tp-push">Mensaje push predeterminado</label>
            <textarea id="tp-push" className="ad-in" rows={2} disabled={!nuevo} title={nuevo ? '' : PENDIENTE} placeholder="Sequía en tu región: protege tus plantas." />
            {!nuevo && <small className="ad-small">{PENDIENTE}: guardar un mensaje predeterminado. El mensaje se escribe al lanzar.</small>}
          </div>

          <div className="ad-fld">
            <span>Salud con la que queda una planta que no se previno</span>
            {SALUD.map((s) => (
              <div key={s.campo} className="ev-deslizador" style={{ '--c': s.color }}>
                <label htmlFor={`tp-${s.campo}`}>{s.texto}</label>
                <input id={`tp-${s.campo}`} type="range" min="1" max="100" value={datos[s.campo]} onChange={(e) => setDatos((d) => conRegla(d, s.campo, e.target.value))} />
                <div className="ev-deslizador-num"><input aria-label={`${s.texto} (%)`} type="number" min="1" max="100" value={datos[s.campo]} onChange={(e) => setDatos((d) => conRegla(d, s.campo, e.target.value))} />%</div>
              </div>
            ))}
            <small className="ad-small">Baja con la severidad: crítica ≤ moderada ≤ baja.</small>
          </div>
        </section>

        <div className="ev-form-lado">
          <section className="ad-card ad-c">
            <h3>Imagen del evento</h3>
            <button type="button" className="ev-subir" onClick={() => selector.current?.click()}>
              <span className="ad-ib"><IconoAdmin n="up" /></span>
              <span><b>{foto ? 'Cambiar imagen' : 'Subir imagen'}</b><small className="ad-small">JPG, PNG o WebP · horizontal, 400 px o más</small></span>
            </button>
            <input ref={selector} type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={(e) => setImagen(e.target.files?.[0] || null)} />
            {nuevo && imagen && <small className="ad-small">La imagen se sube cuando el tipo exista ({PENDIENTE}).</small>}
            <div className="ev-activo">
              <span>Evento activo<small className="ad-small">Disponible para lanzar</small></span>
              <button type="button" role="switch" aria-checked={nuevo ? true : Boolean(tipo.activo)} aria-label="Evento activo" className="ad-sw" disabled title={PENDIENTE} />
            </div>
          </section>

          <section className="ad-card ad-c">
            <h3>Así lo verán en el Inicio</h3>
            <div className="ev-previa">
              {foto ? <img src={foto} alt="" /> : <div className="ev-sinimg ev-sinimg-previa"><IconoAdmin n="img" /></div>}
              <div className="ev-previa-cuerpo">
                <span className="ev-etiqueta">Desastre · Evento activo</span>
                <b>{datos.nombre || 'Nombre del evento'}</b>
                <small className="ad-small">{datos.descripcion ? (datos.descripcion.length > 110 ? `${datos.descripcion.slice(0, 110)}…` : datos.descripcion) : 'Aquí se verá el texto informativo.'}</small>
                <div className="ev-termina"><small>Termina en</small><span>00:30:00</span></div>
              </div>
            </div>
            <div className="ev-form-pie">
              <button type="button" className="ad-btn" disabled={nuevo || !hayCambios || guardando} title={nuevo ? PENDIENTE : ''} onClick={guardar}>
                {nuevo ? 'Crear tipo de evento' : guardando ? 'Guardando…' : 'Guardar tipo de evento'}
              </button>
            </div>
            {nuevo && <small className="ad-small">{PENDIENTE}: crear el tipo.</small>}
          </section>
        </div>
      </div>
    </>
  )
}
