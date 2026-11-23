import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Boton from '../../../components/Boton'
import { RUTAS } from '../../../routes/rutas'
import { agregarColeccion } from '../../../services/coleccion'
import { urlImagen } from '../../../services/home'
import { digitalizarPlanta, listarPlantasDigitales } from '../../../services/jardin'
import { avisoAntes, avisoDespues, plantaDeLaEspecie } from './yaEnJardin'
import { estiloDe, etiquetaPrecision, identificacionFallida, tarjetasCuidado, textoDiagnostico } from './mapeo'
import './ficha.css'

const LARGO_RESUMEN = 3
const LARGO_DIAGNOSTICO = 180

// Contenido de la ficha de una planta (ScanResultDto). Se usa para un escaneo nuevo y para
// una planta de la coleccion; en ese caso `mostrarGuardar` es false porque ya esta guardada.
export default function FichaPlanta({ resultado, mostrarGuardar = true, mostrarAcciones = true }) {
  const irA = useNavigate()
  const [guardando, setGuardando] = useState(false)
  const [errorGuardar, setErrorGuardar] = useState('')
  const [largo, setLargo] = useState(false)
  const [yaEnJardin, setYaEnJardin] = useState(null) // planta de la misma especie que ya esta en el jardin (QA-04)

  const fallida = identificacionFallida(resultado)
  const cuidados = tarjetasCuidado(resultado)
  const diagnostico = textoDiagnostico(resultado)
  const precision = etiquetaPrecision(resultado)
  const foto = urlImagen(resultado.imagenUrl)
  const diagnosticoLargo = diagnostico && diagnostico.length > LARGO_DIAGNOSTICO

  // Antes de digitalizar se mira si la especie ya esta en el jardin, para avisarlo con claridad
  const cientifico = resultado.nombreCientifico
  useEffect(() => {
    if (!mostrarAcciones || !cientifico) return undefined
    let vivo = true
    listarPlantasDigitales()
      .then((plantas) => { if (vivo) setYaEnJardin(plantaDeLaEspecie(plantas, { nombreCientifico: cientifico })) })
      .catch(() => {})
    return () => { vivo = false }
  }, [cientifico, mostrarAcciones])

  const alDigitalizar = async () => {
    if (guardando) return
    setGuardando(true)
    setErrorGuardar('')
    try {
      const planta = await digitalizarPlanta(resultado)
      // Si ya estaba, no se reinicio: el jardin lo avisa y no repite la secuencia de digitalizacion
      const aviso = avisoDespues(planta)
      irA(RUTAS.JARDIN, { state: aviso ? { aviso } : { digitalizada: true } })
    } catch (e) {
      setErrorGuardar(e?.message || 'No se pudo digitalizar la planta.')
      setGuardando(false)
    }
  }

  const alGuardar = async () => {
    if (guardando) return
    setGuardando(true)
    setErrorGuardar('')
    try {
      await agregarColeccion(resultado.idEscaneo)
      irA(RUTAS.COLECCION)
    } catch (e) {
      setErrorGuardar(e?.message || 'No se pudo guardar en la colección.')
    } finally {
      setGuardando(false)
    }
  }

  return (
    <div className="ficha">
      <section className="ficha-cabecera tarjeta aparece">
        <div className="ficha-foto">
          {foto ? <img src={foto} alt={resultado.nombreComun || 'Planta'} /> : <span>🪴</span>}
          {precision && !fallida && <span className="ficha-precision">✨ {precision}</span>}
        </div>
        <div className="ficha-nombre">
          <h2>{fallida ? 'Planta no identificada' : resultado.nombreComun}</h2>
          {!fallida && resultado.nombreCientifico && <p>{resultado.nombreCientifico}</p>}
        </div>
      </section>

      {fallida ? (
        <section className="ficha-fallo tarjeta aparece" style={{ animationDelay: '0.08s' }}>
          <span>⚠️</span>
          <h2>Fallo en la identificación</h2>
          <p>No pudimos reconocer la planta. Intenta de nuevo con una foto más clara y bien iluminada.</p>
          <Boton onClick={() => irA(RUTAS.ESCANEAR)}>Volver a escanear</Boton>
        </section>
      ) : (
        <>
          {cuidados.length > 0 && (
            <section className="aparece" style={{ animationDelay: '0.08s' }}>
              <h2 className="seccion-titulo">Resumen rápido</h2>
              <div className="ficha-resumen">
                {cuidados.slice(0, LARGO_RESUMEN).map((c) => {
                  const estilo = estiloDe(c.tipo)
                  return (
                    <div key={c.id || c.tipo} className="ficha-chip" style={{ background: estilo.fondo }}>
                      <span>{estilo.icono}</span>
                      <small>{estilo.texto}</small>
                      <b>{c.titulo || estilo.texto}</b>
                    </div>
                  )
                })}
              </div>
            </section>
          )}

          {cuidados.length > 0 && (
            <section className="aparece" style={{ animationDelay: '0.14s' }}>
              <h2 className="seccion-titulo">Guía de cuidados</h2>
              <div className="ficha-cuidados">
                {cuidados.map((c) => {
                  const estilo = estiloDe(c.tipo)
                  return (
                    <details key={c.id || c.tipo} className="tarjeta ficha-cuidado">
                      <summary>
                        <span className="ficha-cuidado-icono" style={{ background: estilo.fondo }}>{estilo.icono}</span>
                        <span>{estilo.texto}</span>
                      </summary>
                      <p>{c.contenido}</p>
                    </details>
                  )
                })}
              </div>
            </section>
          )}

          <section className="tarjeta ficha-diagnostico aparece" style={{ animationDelay: '0.2s' }}>
            <h2>🩺 Diagnóstico botánico</h2>
            {diagnostico ? (
              <>
                <p className={diagnosticoLargo && !largo ? 'ficha-recortado' : ''}>{diagnostico}</p>
                {diagnosticoLargo && (
                  <button className="ficha-leer" onClick={() => setLargo(!largo)}>{largo ? 'Leer menos' : 'Leer más…'}</button>
                )}
              </>
            ) : (
              <p>No se detectaron problemas.</p>
            )}
          </section>

          {mostrarAcciones && (
            <div className={`ficha-acciones ${mostrarGuardar ? '' : 'ficha-acciones-sola'}`}>
              <Boton onClick={alDigitalizar} disabled={guardando || yaEnJardin?.enBaul}>
                {yaEnJardin ? '🪴 Ver en mi jardín' : '🪴 Digitalizar'}
              </Boton>
              {mostrarGuardar && (
                <Boton onClick={alGuardar} disabled={guardando}>
                  {guardando ? 'Guardando…' : '💾 Guardar en colección'}
                </Boton>
              )}
              {mostrarGuardar && (
                <button className="boton boton-suave" onClick={() => irA(RUTAS.ESCANEAR)} disabled={guardando}>
                  Cancelar
                </button>
              )}
              {yaEnJardin && <p className="ficha-nota" role="status">{avisoAntes(yaEnJardin)}</p>}
              {errorGuardar && <div className="ficha-error" role="alert">{errorGuardar}</div>}
            </div>
          )}
        </>
      )}
    </div>
  )
}
