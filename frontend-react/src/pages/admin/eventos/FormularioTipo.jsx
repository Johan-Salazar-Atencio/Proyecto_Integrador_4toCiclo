import { useEffect, useRef, useState } from 'react'
import EncabezadoAdmin from '../../../components/admin/EncabezadoAdmin'
import IconoAdmin from '../../../components/admin/IconoAdmin'
import { crearTipoEvento, guardarTipoEvento } from '../../../services/admin/tiposEvento'
import { imagenDe } from './util'

const SALUD = [
  { campo: 'salud_final_bajo', texto: 'Severidad baja', color: 'var(--ok)' },
  { campo: 'salud_final_moderado', texto: 'Moderada', color: 'var(--luz)' },
  { campo: 'salud_final_critico', texto: 'Crítica', color: 'var(--peligro)' },
]

const valoresDe = (t) => ({
  nombre: t?.nombre || '',
  descripcion: t?.descripcion || '',
  consejo: t?.consejo || '',
  region_defecto: t?.region_defecto || 'GLOBAL',
  duracion_defecto_min: t?.duracion_defecto_min ?? 30,
  mensaje_push: t?.mensaje_push || '',
  activo: t ? Boolean(t.activo) : true,
  salud_final_bajo: t?.salud_final_bajo ?? 70,
  salud_final_moderado: t?.salud_final_moderado ?? 50,
  salud_final_critico: t?.salud_final_critico ?? 30,
})

// Minutos -> "00:30:00" para la vista previa
const reloj = (min) => {
  const m = Number(min) || 0
  return `${String(Math.floor(m / 60)).padStart(2, '0')}:${String(m % 60).padStart(2, '0')}:00`
}

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
// Crea con POST y edita con PATCH contra Django; el interruptor «activo» también se guarda aquí.
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
  const nombreValido = datos.nombre.trim().length >= 2
  const hayCambios = Boolean(imagen) || JSON.stringify(datos) !== JSON.stringify(valoresDe(tipo))

  const guardar = async () => {
    setGuardando(true)
    try {
      const cuerpo = {
        nombre: datos.nombre.trim(),
        descripcion: datos.descripcion,
        consejo: datos.consejo,
        region_defecto: datos.region_defecto,
        duracion_defecto_min: datos.duracion_defecto_min,
        mensaje_push: datos.mensaje_push,
        activo: datos.activo,
        salud_final_bajo: datos.salud_final_bajo,
        salud_final_moderado: datos.salud_final_moderado,
        salud_final_critico: datos.salud_final_critico,
      }
      const guardado = nuevo ? await crearTipoEvento(cuerpo, imagen) : await guardarTipoEvento(tipo.id_tipo_desastre, cuerpo, imagen)
      onGuardado(guardado, nuevo)
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
        subtitulo={nuevo ? 'Define cómo se ve y cómo afecta un desastre. Después podrás lanzarlo cuando quieras.' : 'Cambia el nombre, el texto, la imagen y la salud con la que quedan las plantas.'}
      >
        <button type="button" className="ad-btn ad-btn-g" onClick={onVolver}><IconoAdmin n="back" />{nuevo ? 'Cancelar' : 'Volver'}</button>
      </EncabezadoAdmin>

      <div className="ev-form">
        <section className="ad-card ad-c">
          <div className="ad-fld">
            <label htmlFor="tp-nombre">Nombre del evento</label>
            <input id="tp-nombre" className="ad-in" value={datos.nombre} maxLength={80} placeholder="Sequía prolongada" onChange={(e) => poner('nombre', e.target.value)} />
            {!nuevo && <small className="ad-small">El código interno ({tipo.codigo}) no cambia.</small>}
          </div>

          <div className="ad-dos">
            <div className="ad-fld">
              <label htmlFor="tp-region">Región por defecto</label>
              <select id="tp-region" className="ad-in" value={datos.region_defecto} onChange={(e) => poner('region_defecto', e.target.value)}>
                {regiones.map((r) => <option key={r.codigo} value={r.codigo}>{r.codigo === 'GLOBAL' ? 'Global (todas las regiones)' : r.nombre}</option>)}
              </select>
            </div>
            <div className="ad-fld">
              <label htmlFor="tp-dur">Duración por defecto (min)</label>
              <input id="tp-dur" className="ad-in" type="number" min="5" max="4320" value={datos.duracion_defecto_min} onChange={(e) => poner('duracion_defecto_min', e.target.value === '' ? '' : Math.round(Number(e.target.value)))} />
            </div>
          </div>
          <small className="ad-small">Son los valores que aparecen al lanzar el evento; se pueden cambiar en ese momento.</small>

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
            <textarea id="tp-push" className="ad-in" rows={2} maxLength={200} value={datos.mensaje_push} onChange={(e) => poner('mensaje_push', e.target.value)} placeholder="Sequía en tu región: protege tus plantas." />
            <small className="ad-small">Se propone al lanzar el evento; si lo dejas vacío se usa un mensaje genérico.</small>
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
            <div className="ev-activo">
              <span>Evento activo<small className="ad-small">Disponible para lanzar</small></span>
              <button type="button" role="switch" aria-checked={datos.activo} aria-label="Evento activo" className="ad-sw" onClick={() => poner('activo', !datos.activo)} />
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
                <div className="ev-termina"><small>Termina en</small><span>{reloj(datos.duracion_defecto_min)}</span></div>
              </div>
            </div>
            <div className="ev-form-pie">
              <button type="button" className="ad-btn" disabled={!nombreValido || !hayCambios || guardando || datos.duracion_defecto_min === ''} onClick={guardar}>
                {guardando ? 'Guardando…' : nuevo ? 'Crear tipo de evento' : 'Guardar tipo de evento'}
              </button>
            </div>
          </section>
        </div>
      </div>
    </>
  )
}
