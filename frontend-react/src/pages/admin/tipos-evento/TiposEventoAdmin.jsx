import { useCallback, useEffect, useRef, useState } from 'react'
import { urlImagen } from '../../../services/home'
import { guardarTipoEvento, listarTiposEvento } from '../../../services/admin/tiposEvento'
import './tipos-evento.css'

const SALUD = [
  ['salud_final_bajo', 'Severidad baja'],
  ['salud_final_moderado', 'Severidad moderada'],
  ['salud_final_critico', 'Severidad crítica'],
]

function TarjetaTipo({ tipo, onGuardado, onError }) {
  const [datos, setDatos] = useState(valoresDe(tipo))
  const [imagen, setImagen] = useState(null)
  const [vista, setVista] = useState(null)
  const [guardando, setGuardando] = useState(false)
  const selector = useRef(null)

  useEffect(() => { setDatos(valoresDe(tipo)) }, [tipo])
  useEffect(() => {
    if (!imagen) { setVista(null); return undefined }
    const url = URL.createObjectURL(imagen)
    setVista(url)
    return () => URL.revokeObjectURL(url)
  }, [imagen])

  const cambiar = (campo, valor) => setDatos((d) => ({ ...d, [campo]: valor }))
  const hayCambios = imagen || JSON.stringify(datos) !== JSON.stringify(valoresDe(tipo))
  const foto = vista || urlImagen(tipo.imagen_url)

  const guardar = async () => {
    setGuardando(true)
    try {
      const nuevo = await guardarTipoEvento(tipo.id_tipo_desastre, {
        descripcion: datos.descripcion,
        consejo: datos.consejo,
        salud_final_bajo: datos.salud_final_bajo,
        salud_final_moderado: datos.salud_final_moderado,
        salud_final_critico: datos.salud_final_critico,
      }, imagen)
      setImagen(null)
      onGuardado(nuevo)
    } catch (err) {
      onError(err.message)
    } finally {
      setGuardando(false)
    }
  }

  return (
    <article className="tarjeta tipo-evento aparece" style={{ '--color': tipo.color_hex }}>
      <div className="tipo-evento-imagen">
        {foto ? <img src={foto} alt={`Imagen de ${tipo.nombre}`} /> : <span>Sin imagen</span>}
        <button type="button" onClick={() => selector.current?.click()}>{foto ? 'Cambiar imagen' : 'Subir imagen'}</button>
        <input ref={selector} type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={(e) => setImagen(e.target.files?.[0] || null)} />
      </div>
      <div className="tipo-evento-datos">
        <h3>{tipo.nombre} <small>{tipo.codigo}</small></h3>
        <label><span>Texto informativo (lo que se lee en la tarjeta del Inicio)</span>
          <textarea rows={4} maxLength={2000} value={datos.descripcion} onChange={(e) => cambiar('descripcion', e.target.value)} />
        </label>
        <label><span>Consejo de campo (al prevenir) · {datos.consejo.length}/250</span>
          <input maxLength={250} value={datos.consejo} onChange={(e) => cambiar('consejo', e.target.value)} />
        </label>
        <div className="tipo-evento-salud">
          {SALUD.map(([campo, texto]) => (
            <label key={campo}><span>{texto}</span>
              <input type="number" min="1" max="100" value={datos[campo]} onChange={(e) => cambiar(campo, e.target.value === '' ? '' : Number(e.target.value))} />
            </label>
          ))}
        </div>
        <small className="tipo-evento-nota">Salud con la que queda una planta que no se previno. Baja con la severidad: crítica ≤ moderada ≤ baja.</small>
        <button className="mision-primario" disabled={!hayCambios || guardando} onClick={guardar}>{guardando ? 'Guardando…' : 'Guardar cambios'}</button>
      </div>
    </article>
  )
}

const valoresDe = (t) => ({
  descripcion: t.descripcion || '',
  consejo: t.consejo || '',
  salud_final_bajo: t.salud_final_bajo,
  salud_final_moderado: t.salud_final_moderado,
  salud_final_critico: t.salud_final_critico,
})

// Tipos de evento: la imagen, el texto y el consejo que ve el usuario y la salud con la que quedan las plantas
export default function TiposEventoAdmin() {
  const [estado, setEstado] = useState({ cargando: true, tipos: [], error: '' })
  const [aviso, setAviso] = useState('')

  const cargar = useCallback(async () => {
    try {
      setEstado({ cargando: false, tipos: await listarTiposEvento(), error: '' })
    } catch (err) {
      setEstado({ cargando: false, tipos: [], error: err.message || 'No se pudo leer Django.' })
    }
  }, [])
  useEffect(() => { cargar() }, [cargar])

  const avisar = (texto) => { setAviso(texto); setTimeout(() => setAviso(''), 3200) }

  if (estado.cargando) return <div className="home-cargando"><span className="home-giro" aria-label="Cargando" /></div>
  return (
    <div className="tipos-evento">
      {estado.error && <div className="home-error" role="alert"><span>{estado.error}</span><button onClick={cargar}>Reintentar</button></div>}
      {estado.tipos.map((t) => (
        <TarjetaTipo
          key={t.id_tipo_desastre}
          tipo={t}
          onGuardado={(nuevo) => { setEstado((e) => ({ ...e, tipos: e.tipos.map((x) => (x.id_tipo_desastre === nuevo.id_tipo_desastre ? nuevo : x)) })); avisar(`${nuevo.nombre} guardado`) }}
          onError={avisar}
        />
      ))}
      {aviso && <div className="jardin-aviso" role="status">{aviso}</div>}
    </div>
  )
}
