import IconoAdmin from '../../../components/admin/IconoAdmin'
import { imagenDe } from './util'

// Tarjeta de un tipo de evento: imagen, nombre, interruptor, salud por severidad y acciones.
// Un tipo desactivado se ve atenuado pero la tarjeta y el interruptor siguen activos: solo «Lanzar» se desactiva.
export default function TarjetaTipo({ tipo, hayActivo, cambiando, onLanzar, onEditar, onAlternar }) {
  const foto = imagenDe(tipo)
  const apagado = !tipo.activo
  const sinLanzar = hayActivo || apagado
  const motivo = hayActivo ? 'Ya hay un evento en curso' : apagado ? 'El tipo está desactivado' : ''

  return (
    <article className={`ev-tipo ${apagado ? 'ev-tipo-off' : ''}`}>
      <div className="ev-tipo-foto">
        {foto ? <img src={foto} alt={tipo.nombre} /> : <div className="ev-sinimg"><IconoAdmin n="img" />Sin imagen</div>}
        {apagado && <span className="ev-tipo-chip">Desactivado</span>}
      </div>
      <div className="ev-tipo-cuerpo">
        <div className="ev-tipo-fila">
          <b>{tipo.nombre}</b>
          <button
            type="button"
            role="switch"
            aria-checked={Boolean(tipo.activo)}
            aria-label={`${tipo.nombre} activo`}
            className="ad-sw"
            disabled={cambiando}
            title={tipo.activo ? 'Desactivar este tipo' : 'Activar este tipo'}
            onClick={() => onAlternar(tipo)}
          />
        </div>
        <div className="ev-salud" title="Salud con la que queda una planta no prevenida (baja · moderada · crítica)">
          <span>{tipo.salud_final_bajo}%</span><span>{tipo.salud_final_moderado}%</span><span>{tipo.salud_final_critico}%</span>
        </div>
        <div className="ev-tipo-fila ev-tipo-botones">
          <button type="button" className={`ad-btn ${sinLanzar ? 'ad-btn-g' : ''}`} disabled={sinLanzar} title={motivo} onClick={() => onLanzar(tipo)}>
            <IconoAdmin n="bolt" />Lanzar
          </button>
          <button type="button" className="ad-ib" aria-label={`Editar ${tipo.nombre}`} title="Editar" onClick={() => onEditar(tipo)}><IconoAdmin n="edit" /></button>
        </div>
      </div>
    </article>
  )
}
