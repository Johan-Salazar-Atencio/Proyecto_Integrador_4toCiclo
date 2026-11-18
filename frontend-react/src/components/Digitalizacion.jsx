import { useEffect, useRef } from 'react'
import { LADO, SIN_MACETA, fotograma, macetaVacia } from './plantaPixelSprites'
import './planta-pixel.css'

// Secuencia de "digitalizar": la foto real se va pixelando, se condensa en una semilla y cae
// en la maceta. Al terminar llama a `alTerminar` y el jardin muestra la planta (etapa semilla).
// Se pinta en un lienzo de 256 px con pixeles logicos de 4 px, la misma rejilla que PlantaPixel.
const MS_TIC = 80
const ESCALA = 4
const LIENZO = LADO * ESCALA
const MS_ESPERA_FOTO = 1500

// Fase 1: lado de la foto en pixeles reales (160 = nitida ... 5 = casi una mancha de color)
const RESOLUCION = [160, 160, 160, 80, 40, 40, 20, 20, 10, 10, 5, 5]
// Fase 2: la foto se encoge hacia el centro (lado en pixeles logicos)
const ENCOGE = [36, 30, 24, 18, 12, 8, 5, 3]
// Fase 3: caida de la semilla (pixeles logicos bajo el punto de partida)
const CAIDA = [0, 1, 3, 6, 11, 17, 24, 27]
const TOTAL = RESOLUCION.length + ENCOGE.length + CAIDA.length

const FOTO = { x: 12, y: 3, lado: 40 }
const SEMILLA = ['#8a6238', '#c79a5e', '#f0d9a8']
const BRILLOS = ['#ffffff', '#fff3b0', '#d9f59a']

// Sin foto (o si no carga): un "retrato" de la propia planta adulta sobre un cielo, para que la
// secuencia pixele algo reconocible en vez de un bloque de color
function retratoDe(apariencia) {
  const lienzo = document.createElement('canvas')
  lienzo.width = lienzo.height = LADO
  const ctx = lienzo.getContext('2d')
  const cielo = ctx.createLinearGradient(0, 0, 0, LADO)
  cielo.addColorStop(0, '#a9ddf2')
  cielo.addColorStop(1, '#e3f5e8')
  ctx.fillStyle = cielo
  ctx.fillRect(0, 0, LADO, LADO)
  const planta = document.createElement('canvas')
  planta.width = planta.height = LADO
  const datos = planta.getContext('2d').createImageData(LADO, LADO)
  datos.data.set(fotograma(apariencia, 5, 0, SIN_MACETA))
  planta.getContext('2d').putImageData(datos, 0, 0)
  // La planta sin maceta llega hasta la fila 47: se baja para llenar el cuadro
  ctx.drawImage(planta, 0, 10)
  return lienzo
}

export default function Digitalizacion({ foto = null, apariencia = null, maceta = null, alTerminar, titulo = 'Digitalizando la planta' }) {
  const lienzo = useRef(null)
  const terminar = useRef(alTerminar)
  terminar.current = alTerminar

  useEffect(() => {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      terminar.current?.()
      return undefined
    }
    const ctx = lienzo.current.getContext('2d')
    const fondo = document.createElement('canvas')
    fondo.width = fondo.height = LADO
    const datos = fondo.getContext('2d').createImageData(LADO, LADO)
    datos.data.set(macetaVacia(apariencia, maceta))
    fondo.getContext('2d').putImageData(datos, 0, 0)
    const chico = document.createElement('canvas')

    let imagen = null
    const retrato = retratoDe(apariencia)
    let cuadro = 0
    let tic = 0
    let acabado = false
    let cancelado = false // el efecto se desmonto: no se pinta mas ni se avisa al jardin
    const px = (x, y, ancho, alto, color) => {
      ctx.fillStyle = color
      ctx.fillRect(Math.round(x) * ESCALA, Math.round(y) * ESCALA, ancho * ESCALA, alto * ESCALA)
    }

    // La foto (recorte cuadrado centrado) reducida a `res` x `res` y ampliada sin suavizar
    const pintarFoto = (x, y, lado, res) => {
      px(x - 1, y - 1, lado + 2, lado + 2, '#2c2028')
      const fuente = imagen || retrato
      const ancho = fuente.naturalWidth || fuente.width
      const alto = fuente.naturalHeight || fuente.height
      const n = Math.max(1, Math.round(res))
      chico.width = chico.height = n
      const c = chico.getContext('2d')
      const corte = Math.min(ancho, alto)
      c.drawImage(fuente, (ancho - corte) / 2, (alto - corte) / 2, corte, corte, 0, 0, n, n)
      ctx.drawImage(chico, x * ESCALA, y * ESCALA, lado * ESCALA, lado * ESCALA)
    }

    const pintar = () => {
      ctx.imageSmoothingEnabled = false
      ctx.clearRect(0, 0, LIENZO, LIENZO)
      ctx.drawImage(fondo, 0, 0, LIENZO, LIENZO)
      if (tic < RESOLUCION.length) {
        pintarFoto(FOTO.x, FOTO.y, FOTO.lado, RESOLUCION[tic])
        return
      }
      const centro = FOTO.x + FOTO.lado / 2
      const i = tic - RESOLUCION.length
      if (i < ENCOGE.length) {
        const lado = ENCOGE[i]
        const y = FOTO.y + FOTO.lado / 2 - 7 * (i / (ENCOGE.length - 1))
        pintarFoto(centro - lado / 2, y - lado / 2, lado, Math.min(5, lado))
        // Se va tiñendo del color de la semilla y suelta destellos
        ctx.globalAlpha = (i / ENCOGE.length) * 0.85
        px(centro - lado / 2, y - lado / 2, lado, lado, SEMILLA[1])
        ctx.globalAlpha = 1
        for (let k = 0; k < 5; k++) {
          const a = Math.random() * 6.283
          const d = lado / 2 + 2 + Math.random() * 7
          px(centro + Math.cos(a) * d, y + Math.sin(a) * d, 1, 1, BRILLOS[k % BRILLOS.length])
        }
        return
      }
      const j = Math.min(CAIDA.length - 1, i - ENCOGE.length)
      const y = FOTO.y + FOTO.lado / 2 - 8 + CAIDA[j]
      px(centro - 2, y - 1, 3, 3, '#2c2028')
      px(centro - 1, y, 1, 1, SEMILLA[2])
      px(centro - 2, y, 1, 1, SEMILLA[1])
      px(centro - 1, y - 1, 1, 1, SEMILLA[1])
      px(centro - 2, y + 1, 2, 1, SEMILLA[0])
      // Estela de la caida
      for (let k = 1; k <= Math.min(3, j); k++) px(centro - 1, y - 1 - k * 2, 1, 1, BRILLOS[k % BRILLOS.length])
    }

    const empezar = () => {
      let previo = performance.now()
      let acumulado = 0
      const bucle = (ahora) => {
        if (cancelado) return
        acumulado += Math.min(400, ahora - previo)
        previo = ahora
        let hubo = false
        while (acumulado >= MS_TIC && !acabado) {
          acumulado -= MS_TIC
          if (++tic >= TOTAL) acabado = true
          hubo = true
        }
        if (acabado) {
          terminar.current?.()
          return
        }
        if (hubo) pintar()
        cuadro = requestAnimationFrame(bucle)
      }
      pintar()
      cuadro = requestAnimationFrame(bucle)
    }

    // Se espera a la foto un momento; si no llega, la secuencia sigue con el retrato de la planta
    let arrancado = false
    const arrancar = () => {
      if (arrancado || cancelado) return
      arrancado = true
      empezar()
    }
    const espera = setTimeout(arrancar, foto ? MS_ESPERA_FOTO : 0)
    if (foto) {
      const img = new Image()
      img.onload = () => { imagen = img; arrancar() }
      img.onerror = arrancar
      img.src = foto
    }
    return () => {
      cancelado = true
      clearTimeout(espera)
      cancelAnimationFrame(cuadro)
    }
  }, [foto, apariencia, maceta])

  return <canvas ref={lienzo} className="planta-pixel" width={LIENZO} height={LIENZO} role="img" aria-label={titulo} />
}
