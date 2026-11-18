import { useEffect, useRef } from 'react'
import { ETAPA_MARCHITA, FOTOGRAMAS, LADO, SIN_MACETA, fotograma } from './plantaPixelSprites'
import './planta-pixel.css'

// Planta del jardin en pixel art (lienzo de 64x64 ampliado sin suavizar). El dibujo sale de la
// `apariencia` que da la IA al escanear (ver plantaPixelSprites.js). Todo el movimiento es a
// saltos de pixel entero, como en un juego: balanceo por fotogramas, parpadeo al cambiar de
// etapa y particulas al cuidarla (`efecto` = { tipo: 'REGAR' | 'SOL' | 'ABONAR', id }).
const MS_TIC = 80
const TICS_POR_FOTOGRAMA = 2
const BRINCO = [0, -1, -3, -4, -3, -1, 0, 1, 0]
const PARPADEOS = 7

const AGUA = ['#d8f1ff', '#58b4f0']
const SOL = ['#fff7d1', '#ffd866']
const ABONO = ['#d9f59a', '#ffe08a', '#9bd65c']
const BRILLO = ['#ffffff', '#fff3b0']

const elegir = (lista) => lista[Math.floor(Math.random() * lista.length)]

function destello(x, y, colores, vida = 7, extra = {}) {
  return { tipo: 'destello', x, y, vx: 0, vy: 0, vida, total: vida, color: elegir(colores), ...extra }
}

// Particulas que lanza cada efecto en el tic `t` desde que empezo
function emitir(tipo, t, lista) {
  const azar = (a, b) => a + Math.random() * (b - a)
  if (tipo === 'REGAR' && t < 12) {
    for (let i = 0; i < 2; i++) lista.push({ tipo: 'gota', x: azar(16, 48), y: -2, vx: 0, vy: 2.4, hasta: azar(22, 46), vida: 40 })
  }
  if (tipo === 'SOL' && t < 14 && t % 2 === 0) lista.push(destello(azar(8, 56), azar(4, 40), SOL, 8))
  if (tipo === 'ABONAR' && t < 10) {
    lista.push({ tipo: 'chispa', x: azar(20, 44), y: 46, vx: azar(-0.3, 0.3), vy: azar(-1.4, -0.7), vida: 16, color: elegir(ABONO) })
  }
  if (tipo === 'CRECER' && t === 0) {
    for (let i = 0; i < 14; i++) {
      const a = (i / 14) * 6.283
      lista.push(destello(32 + Math.cos(a) * 6, 26 + Math.sin(a) * 6, BRILLO, 10, { vx: Math.cos(a) * 1.5, vy: Math.sin(a) * 1.3 }))
    }
  }
}

function pintarParticula(ctx, p) {
  const x = Math.round(p.x)
  const y = Math.round(p.y)
  if (p.tipo === 'gota') {
    ctx.fillStyle = AGUA[0]
    ctx.fillRect(x, y - 1, 1, 1)
    ctx.fillStyle = AGUA[1]
    ctx.fillRect(x, y, 1, 2)
    return
  }
  ctx.fillStyle = p.color
  ctx.fillRect(x, y, 1, 1)
  // El destello crece a una cruz en la mitad de su vida
  if (p.tipo === 'destello' && Math.abs(p.vida - p.total / 2) < p.total / 4) {
    ctx.fillRect(x - 1, y, 3, 1)
    ctx.fillRect(x, y - 1, 1, 3)
  }
}

// `maceta` es el codigo de la maceta comprada (AZUL, ROSA...): se dibuja con su color. `sinSuelo` no dibuja
// maceta (cuando otro la dibuja aparte, como el estante del invernadero).
export default function PlantaPixel({ etapa = 0, apariencia = null, titulo, animar = true, efecto = null, className = '', maceta = null, sinSuelo = false }) {
  const codigoMaceta = sinSuelo ? SIN_MACETA : maceta
  const lienzo = useRef(null)
  const st = useRef({ etapa, tic: 0, cambio: null, efectos: [], particulas: [], brinco: -1 })
  const repintar = useRef(() => {})

  useEffect(() => {
    const ctx = lienzo.current.getContext('2d')
    const imagen = ctx.createImageData(LADO, LADO)
    const quieto = !animar || window.matchMedia('(prefers-reduced-motion: reduce)').matches
    const s = st.current

    const pintar = () => {
      let visible = s.etapa
      let blanco = false
      if (s.cambio) {
        const paso = Math.floor(s.cambio.tic / 2)
        visible = paso % 2 === 0 ? s.cambio.de : s.etapa
        blanco = paso % 2 === 1 && paso < PARPADEOS - 1
      }
      const k = quieto || visible === ETAPA_MARCHITA ? 0 : Math.floor(s.tic / TICS_POR_FOTOGRAMA)
      imagen.data.set(fotograma(apariencia, visible, k % FOTOGRAMAS, codigoMaceta))
      if (blanco) {
        // Silueta en blanco: el destello clasico de "evolucion"
        const d = imagen.data
        for (let i = 0; i < d.length; i += 4) if (d[i + 3] === 255) d[i] = d[i + 1] = d[i + 2] = 255
      }
      ctx.clearRect(0, 0, LADO, LADO)
      ctx.putImageData(imagen, 0, s.brinco >= 0 ? BRINCO[s.brinco] : 0)
      for (const p of s.particulas) pintarParticula(ctx, p)
    }

    const avanzar = () => {
      s.tic++
      if (s.cambio && ++s.cambio.tic >= PARPADEOS * 2) {
        s.cambio = null
        s.efectos.push({ tipo: 'CRECER', t: 0 })
        s.brinco = 0
      }
      if (s.brinco >= 0 && ++s.brinco >= BRINCO.length) s.brinco = -1
      for (const e of s.efectos) emitir(e.tipo, e.t++, s.particulas)
      s.efectos = s.efectos.filter((e) => e.t < 16)
      // De vez en cuando, un brillo sobre una hoja iluminada
      if (s.tic % 34 === 0 && s.etapa !== ETAPA_MARCHITA && !s.cambio) {
        const d = fotograma(apariencia, s.etapa, 0, codigoMaceta)
        for (let i = 0; i < 12; i++) {
          const x = 6 + Math.floor(Math.random() * 52)
          const y = 3 + Math.floor(Math.random() * 40)
          const j = (y * LADO + x) * 4
          if (d[j + 3] === 255 && d[j] + d[j + 1] + d[j + 2] > 430) {
            s.particulas.push(destello(x, y, BRILLO, 6))
            break
          }
        }
      }
      for (const p of s.particulas) {
        p.x += p.vx
        p.y += p.vy
        p.vida--
        if (p.tipo === 'gota' && p.y >= p.hasta) {
          p.vida = 0
          s.particulas.push({ tipo: 'chispa', x: p.x - 1, y: p.hasta, vx: -0.6, vy: -0.5, vida: 3, color: AGUA[0] })
          s.particulas.push({ tipo: 'chispa', x: p.x + 1, y: p.hasta, vx: 0.6, vy: -0.5, vida: 3, color: AGUA[0] })
        }
      }
      s.particulas = s.particulas.filter((p) => p.vida > 0)
    }

    repintar.current = pintar
    pintar()
    if (quieto) {
      s.quieto = true
      return undefined
    }
    s.quieto = false
    let cuadro
    let previo = performance.now()
    let acumulado = 0
    const bucle = (ahora) => {
      acumulado += Math.min(400, ahora - previo)
      previo = ahora
      let hubo = false
      while (acumulado >= MS_TIC) {
        acumulado -= MS_TIC
        avanzar()
        hubo = true
      }
      if (hubo) pintar()
      cuadro = requestAnimationFrame(bucle)
    }
    cuadro = requestAnimationFrame(bucle)
    return () => cancelAnimationFrame(cuadro)
  }, [apariencia, animar, codigoMaceta])

  // Cambio de etapa: parpadeo entre la anterior y la nueva (sin animacion, se cambia y ya)
  useEffect(() => {
    const s = st.current
    if (s.etapa === etapa) return
    s.cambio = s.quieto ? null : { de: s.etapa, tic: 0 }
    s.etapa = etapa
    repintar.current()
  }, [etapa])

  useEffect(() => {
    const s = st.current
    if (!efecto || s.quieto) return
    s.efectos.push({ tipo: efecto.tipo, t: 0 })
    if (s.brinco < 0 && !s.cambio) s.brinco = 0
  }, [efecto])

  return (
    <canvas
      ref={lienzo}
      className={`planta-pixel ${className}`}
      width={LADO}
      height={LADO}
      role="img"
      aria-label={titulo}
    />
  )
}
