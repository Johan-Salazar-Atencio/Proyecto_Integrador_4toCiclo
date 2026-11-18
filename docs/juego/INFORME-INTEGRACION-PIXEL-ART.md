# Informe — Integración del pixel art 64×64 y errores tras integrarlo

**Fecha:** 18 de noviembre de 2026
**Rama:** `main` local (sin subir a GitHub)
**Commits:**
- `34ac81f` — feat(jardin): plantas en pixel art 64x64 con animación por fotogramas (integración)
- `a9eb64c` — fix(jardin): marchita y muerta según la etapa, frutos verdes visibles y digitalizar sin foto (correcciones)

**Capturas:** [`capturas-pixel-art/`](capturas-pixel-art/), 26 imágenes de la prueba visual.

---

## 1. Resumen

| Pregunta | Respuesta |
|---|---|
| ¿De dónde venía el pixel art? | De la sesión del 16-nov, hecha en un worktree aparte (`.claude/worktrees/plant-digitalization-animation-f0f009`, rama `claude/plant-digitalization-animation-f0f009`). Nunca se commiteó ni llegó a `main`, por eso no aparecía en la web |
| ¿Se había perdido? | No. Estaba intacto, sin commitear |
| ¿Qué se hizo? | (1) Respaldo. (2) Aplicarlo sobre `main` con fusión a tres bandas. (3) Resolver a mano 4 archivos en conflicto con el trabajo de Johan. (4) Prueba visual completa. (5) Corregir los 5 errores encontrados |
| Estado | Integrado y corregido en la **web**. Compila (`npm run build`). Revisado en el navegador: jardín, inicio, invernadero y demo |
| Lo que **no** cubre | La app **móvil** sigue con sus sprites propios (`PlantaPixelLogica.kt`): la web y el móvil se ven distintos (sección 7) |

---

## 2. Origen y respaldo

| Archivo del worktree | Estado | Tamaño del cambio |
|---|---|---|
| `frontend-react/src/components/plantaPixelSprites.js` | modificado | +1 064 líneas aprox. (motor de sprites nuevo) |
| `frontend-react/src/components/PlantaPixel.jsx` | modificado | de SVG 32×32 a lienzo 64×64 animado |
| `frontend-react/src/components/planta-pixel.css` | modificado | sin animaciones CSS |
| `frontend-react/src/components/Digitalizacion.jsx` | **nuevo**, sin seguimiento en git | 150 líneas |
| `frontend-react/src/pages/user/DemoPlanta.jsx` | modificado | página de muestra |
| `frontend-react/src/pages/user/jardin/Jardin.jsx` | modificado | efectos y llegada desde Digitalizar |
| `frontend-react/src/pages/user/jardin/jardin.css` | modificado | tamaño del lienzo; se quitan los saltos CSS |
| `frontend-react/src/pages/user/planta/FichaPlanta.jsx` | modificado | envía `state: { digitalizada: true }` al jardín |

El worktree partía de `853f24b` (15-nov). Desde entonces `main` había recibido el rediseño de Johan (37 commits) y las correcciones de economía e IA.

**Respaldo hecho antes de tocar nada** (no se borra):
- `C:\cuarto ciclo\XPROYECTO INTEGRADOR\respaldos\pixel-art-worktree-2026-11-16.tgz`: los 8 archivos tal cual.
- `C:\cuarto ciclo\XPROYECTO INTEGRADOR\respaldos\pixel-art-worktree-2026-11-16.diff`: el parche completo.
- El **worktree original sigue intacto**: no se borró ni se modificó.

---

## 3. Cómo se integró

### 3.1 Aplicación del parche

```bash
git apply -3 ../respaldos/pixel-art-worktree-2026-11-16.diff
cp <worktree>/frontend-react/src/components/Digitalizacion.jsx frontend-react/src/components/
```

| Archivo | Resultado |
|---|---|
| `plantaPixelSprites.js` | limpio (Johan no lo había tocado) |
| `DemoPlanta.jsx` | limpio |
| `FichaPlanta.jsx` | limpio |
| `PlantaPixel.jsx` | **conflicto** (3 bloques) |
| `planta-pixel.css` | **conflicto** (1 bloque) |
| `Jardin.jsx` | **conflicto** (5 bloques) |
| `jardin.css` | **conflicto** (3 bloques) |

### 3.2 Qué chocaba y cómo se resolvió

#### `PlantaPixel.jsx` — dos enfoques distintos
- **Tu versión:** lienzo `<canvas>` de 64×64 con `requestAnimationFrame`:
  - ticks de 80 ms y 12 fotogramas de balanceo;
  - partículas al regar (gotas), dar sol (destellos) y abonar (chispas);
  - parpadeo blanco al cambiar de etapa y un pequeño brinco;
  - respeta `prefers-reduced-motion`.
- **La de Johan:** el SVG antiguo de 32×32 con dos props nuevas:
  - `maceta`, el código de la maceta comprada en la tienda (AZUL, ROSA, DORADA…);
  - `sinSuelo`, para no dibujar suelo en el estante del invernadero, donde la maceta se dibuja aparte.

**Resolución:** se queda tu lienzo y se le añaden las dos funciones de Johan:

```jsx
export default function PlantaPixel({ etapa = 0, estado = null, apariencia = null, titulo, animar = true,
                                      efecto = null, className = '', maceta = null, sinSuelo = false }) {
  const codigoMaceta = sinSuelo ? SIN_MACETA : maceta
  ...
  imagen.data.set(fotograma(apariencia, visible, k % FOTOGRAMAS, codigoMaceta, estado))
```

`plantaPixelSprites.js` aprende a pintar **su propia maceta con el color de la maceta comprada** (paletas de `macetas.js`, las mismas que el móvil):

```js
import { paletaDeMaceta } from './macetas'
export const SIN_MACETA = 'NINGUNA'

function estiloMaceta(forma, codigo) {
  if (!codigo) return forma.maceta                                    // maceta propia de cada forma
  return { ...forma.maceta, color: paletaDeMaceta(codigo).cuerpo }    // color de la tienda
}
// dibujar(): if (codigoMaceta !== SIN_MACETA) maceta(l, estiloMaceta(forma, codigoMaceta))
```

La clave de la caché de `fotograma` incluye el código de maceta, así cada combinación de planta y maceta se dibuja una sola vez. `macetaVacia(apariencia, codigoMaceta)`, que se usa al digitalizar, también la respeta.

#### `planta-pixel.css`
Se queda tu versión: lienzo al 100 %, `image-rendering: pixelated` / `crisp-edges` y **sin animaciones CSS**, porque todo el movimiento lo pinta el lienzo a saltos de píxel. Se elimina el balanceo con `rotate` del SVG antiguo.

#### `Jardin.jsx` — juntar las dos versiones

| Bloque | Lo de Johan (se conserva) | Lo tuyo (se añade) |
|---|---|---|
| imports | eventos, golpe, prevenir, revivir, abonos de tienda | `Digitalizacion`, `useLocation`, `useNavigate` |
| estado | `fondosAbierto`, `claveEscena`, `abonos`, `evento`, `golpe`, `escudo` | `efecto` (en lugar de `salto`) y `llegando` |
| `cuidar` | aviso de "se recuperó" con `antes.marchita` (incluye recuperación) | `setEfecto({ tipo: accion, id })` |
| dibujo | `<EscenaPixel>` de fondo y `maceta={planta.macetaCodigo}` | `<Digitalizacion>` al llegar y `efecto` en `PlantaPixel` |
| muerta | sección "💀 murió · Revivir" | — |

Además:
- **`setSalto` se sustituye por efectos del lienzo:** `ABONAR` al usar un abono de tienda y `CRECER` al revivir. El salto CSS ya no existe.
- **Los botones de cuidado quedan desactivados durante la llegada:** `disabled={cuidando || llegando}`.

#### `jardin.css` — colocar la planta en la escena
En la versión de Johan la planta va **encima de la escena pixel art** (96 píxeles lógicos de ancho). Con el sprite de 64×64, cada píxel de la planta mide lo mismo que uno de la escena:

```css
/* 64 de 96 de ancho; fila de tierra del sprite (47 de 64) sobre la tierra del fondo (fila 75):
   top = (75 - 47) / 96 */
.jardin-planta { position: absolute; left: 16.667%; top: 29.167%; width: 66.667%; aspect-ratio: 1 / 1; }
```

Se quitaron `.jardin-salta` y `@keyframes salta`.

#### `coleccion.css` — invernadero
En el estante, la maceta la dibuja `MacetaPixel` y la planta va con `sinSuelo`. Medido en el navegador, la tierra del sprite quedaba **11 px por debajo** del borde de la maceta y se tapaba la base:

```css
.estante-dibujo { ...; transform: translateY(-3.7%); }   /* unos 7 px en un estante de 189 px */
```

#### `Digitalizacion.jsx`
Recibe `maceta` y la pasa a `macetaVacia`, para que la secuencia caiga en la maceta comprada.

### 3.3 Comprobación de compatibilidad
- **Formato de la apariencia:** tus sprites leen `colorHoja`, `tieneFruto`… Django (`normalizar_identificacion`) ya los convierte a este formato, y la base de datos los guarda así (comprobado en `plantas_usuario.apariencia`). Compatible.
- **Quién importa de `plantaPixelSprites`:** `PlantaPixel.jsx`, `Digitalizacion.jsx` y `jardin/etapas.js` (`cicloDe`). Todos los exports que se usan existen.
- **`LoginAdmin.jsx`** tiene su propio `PlantaPixel` local en SVG 16×16, así que no le afecta.
- `npm run build`: correcto.

---

## 4. Prueba visual después de integrar

Partida con la usuaria de prueba lucia en `http://127.0.0.1:5173`.

> **Nota:** el navegador de pruebas tiene activado "reducir movimiento". Para ver las animaciones se sustituyó `window.matchMedia` **solo en esa pestaña**, sin tocar el código. Los jugadores con esa preferencia activada verán la planta quieta, como debe ser.

| # | Qué se probó | Resultado | Captura |
|---|---|---|---|
| 1 | `/demo-planta`: 9 formas × 7 etapas | ✅ arte correcto, maceta propia por forma | [01](capturas-pixel-art/01-demo-formas.jpg) |
| 2 | Llegar al jardín desde Digitalizar | ✅ pixelado → semilla que cae → brote. ⚠️ sin foto, un cuadrado verde (**E1**) | [02](capturas-pixel-art/02-digitalizar-foto.png), [03](capturas-pixel-art/03-digitalizar-semilla-cae.png), [04](capturas-pixel-art/04-digitalizar-brota.png), [05](capturas-pixel-art/05-jardin-semilla.jpg) |
| 3 | Regar (de semilla a brote) | ✅ parpadeo blanco de "evolución" | [06](capturas-pixel-art/06-regar-destello-evolucion.png) |
| 4 | Sol | ✅ destellos | [07](capturas-pixel-art/07-sol-chispas.png) |
| 5 | Abonar | ✅ chispas (duran menos de 0,5 s) | [08](capturas-pixel-art/08-abonar.png) |
| 6 | Días 2, 4, 5, 6 y 7 | ✅ cada etapa se distingue. ⚠️ día 6: frutos verdes que no se ven (**E5**) | [09](capturas-pixel-art/09-dia2.png)–[13](capturas-pixel-art/13-madura.jpg) |
| 7 | Maceta azul de la tienda | ✅ en jardín, inicio e invernadero | [14](capturas-pixel-art/14-maceta-azul.png), [15](capturas-pixel-art/15-inicio-maceta-azul.jpg), [16](capturas-pixel-art/16-invernadero.png) |
| 8 | Marchita con maceta dorada | ✅ hojas secas y caídas | [17](capturas-pixel-art/17-marchita-maceta-dorada.png) |
| 9 | Brote de 1 día marchito | ❌ aparece como adulta seca (**E2**) | [18](capturas-pixel-art/18-marchita-brote-dia1.png) |
| 10 | Helada activa | ✅ escarcha sobre la escena | [19](capturas-pixel-art/19-helada-activa.jpg) |
| 11 | Golpe de la helada | ✅ salud 80 → 20 y recuperación de 24 h. ❌ el aviso tapa la maceta (**E4**) | [20](capturas-pixel-art/20-golpe-helada.jpg) |
| 12 | Muerta | ❌ igual que la marchita (**E3**) | [21](capturas-pixel-art/21-muerta.jpg) |
| 13 | Revivir | ✅ vuelve madura. ⚠️ la cabecera de hojas no se actualiza (**P1**) | [22](capturas-pixel-art/22-revivida.jpg) |

---

## 5. Errores encontrados tras la integración y su corrección

### E1 — Digitalizar sin foto mostraba un cuadrado verde liso
- **Causa:** `Digitalizacion.jsx`, `pintarFoto`. Si no había imagen, pintaba `px(x, y, lado, lado, '#4f9a52')`. Ocurre cuando la planta no tiene foto (digitalizada desde el catálogo) o la foto no carga.
- **Corrección:** se pixela un **retrato de la propia planta** (tu sprite adulto, sin maceta, sobre el cielo del jardín):

```js
function retratoDe(apariencia) {
  // cielo #a9ddf2 → #e3f5e8, y encima fotograma(apariencia, 5, 0, SIN_MACETA) bajado 10 px
}
const fuente = imagen || retrato
const ancho = fuente.naturalWidth || fuente.width     // <img> o <canvas>
```

- **Verificado:** [captura 24](capturas-pixel-art/24-digitalizar-sin-foto-retrato.png): el tomate maduro se pixela y se convierte en semilla.

### E2 — La marchita era siempre la adulta
- **Causa:** `dibujar()` usaba `const e = marchita ? 3 : etapa`. La web pasaba `etapa = 6` (`ETAPA_MARCHITA`) y se perdía la etapa real.
- **Corrección:** el estado se separa de la etapa.
  - `fotograma(apariencia, etapa, k, maceta, estado)` y `PlantaPixel({ etapa, estado })`, con `estado` en `'marchita' | 'muerta' | null`.
  - `etapas.js`, `etapaDe()`, devuelve dos campos nuevos:
    - `sprite`: la etapa de crecimiento real, también cuando está marchita;
    - `estadoDibujo`.

    `indice` sigue siendo 6 cuando está marchita, porque Jardín lo usa para los avisos de "creció" y "se recuperó".
  - Los 4 sitios que dibujan plantas usan `etapa={x.sprite} estado={x.estadoDibujo}`: `Jardin.jsx`, `TarjetaPlanta.jsx` (dos veces) y `ColeccionVirtual.jsx`.
  - Compatibilidad: `etapa = 6` sin estado sigue significando "marchita adulta" (la usa la tira de la demo).
- **Verificado:** brote de 2 días golpeado → brote seco pequeño ([captura 25](capturas-pixel-art/25-golpe-aviso-arriba-brote-marchito.jpg)); demo con brote y madura marchitos ([23](capturas-pixel-art/23-demo-marchita-y-muerta-por-etapa.jpg)).

### E3 — No se distinguía muerta de marchita
- **Causa:** solo existía el estado "marchita". La etiqueta decía "Marchita" también en una planta muerta.
- **Corrección** en `rampas(ap, estado)`, para `MUERTA`:

| Paleta | Marchita | Muerta |
|---|---|---|
| hoja / brote | `mezclar(verde, '#b0803a', 0.74)` (pardo seco) | `mezclar(verde, '#6e6258', 0.95)` (gris pardo), sin el ajuste de saturación de hoja |
| amarilla / tallo | sin cambio | gris (`#5b5149`) |
| tronco | `#7d6650` | `#5e554d` |
| caída de las hojas (`P.caida`) | 3,5 | **5,5** |

La etiqueta muestra **"Muerta"** en el jardín y en el inicio (`etapa.muerta`).
- **Verificado:** [captura 26](capturas-pixel-art/26-muerta-gris.jpg) y la fila de la demo.
- **Primer intento incompleto:** a la muerta le quedaban hojas verdes, porque las paletas `amarilla` y `tallo` no se habían cambiado. Se corrigió en el mismo commit antes de cerrarlo.

### E4 — El aviso del golpe tapaba la maceta
- **Causa:** `.jardin-golpe { inset: auto 6% 12% 6% }` (abajo). Con el sprite de 64×64, más alto, la maceta queda justo ahí.
- **Corrección:** `inset: 15% 6% auto 6%`: arriba, bajo el chip de etapa.
- **Verificado:** [captura 25](capturas-pixel-art/25-golpe-aviso-arriba-brote-marchito.jpg).

### E5 — Frutos verdes invisibles en la etapa "con frutos"
- **Causa:** la paleta `verde` del fruto sin madurar era `mezclar(colorHoja, '#c4d64a', 0.5)`, casi igual que las hojas, y de radio 1,9.
- **Corrección:** `mezclar('#dbe96a', colorFruto, 0.12)` (lima claro con un toque del color maduro) y radio **2,3**.
- **Verificado:** en la demo, el arbusto "con frutos" muestra bolitas lima claras entre las hojas ([23](capturas-pixel-art/23-demo-marchita-y-muerta-por-etapa.jpg)).

---

## 6. Errores que siguen abiertos

| ID | Error | Dónde | Notas |
|---|---|---|---|
| P1 | La cabecera de hojas no se actualiza tras cuidar, abonar o revivir (mostraba 300 tras pagar 250) | barra superior de la web | Ya estaba antes de la integración (F1 del informe de jugabilidad) |
| P2 | El botón Abonar dice "+10 %" aunque en recuperación suma +45 | `Jardin.jsx`, `ACCIONES` | Anterior (F2) |
| P3 | En una planta muerta, el consejo dice "un poco de abono le ayudará" | `animo.js`, `consejo` | La etiqueta ya está corregida (E3); el consejo no |
| P4 | El menú resalta "Escanear" estando en el jardín | barra superior | Anterior (F5) |
| P5 | Inicio "3 de 5" frente a Misiones "3 de 4" | contadores de misiones | Anterior (F6) |
| P6 | **La web y el móvil ya no se parecen**: el móvil sigue con sus sprites (`PlantaPixelLogica.kt`, 32×32) y tampoco cuenta la recuperación como marchita | `mobile-kotlin` | Nuevo tras la integración: el pixel art solo se integró en la web |
| P7 | Con "reducir movimiento" activo no hay partículas ni secuencia de digitalizar | por diseño | No es un error: es accesibilidad |

---

## 7. Notas para el equipo

1. **Para subir esto a GitHub**, los commits `34ac81f` y `a9eb64c` dependen del rediseño de Johan (`macetas.js`, `EscenaPixel`, `MacetaPixel`). Van en el mismo `main` local, 45 commits por delante de `origin/main`.
2. **El worktree del 16-nov** se puede borrar cuando el equipo confirme que todo está bien. No se ha borrado: el respaldo y el worktree siguen ahí.
3. **El móvil (P6)** necesitaría portar `plantaPixelSprites.js` a Kotlin, o generar los fotogramas en el servidor, para que las dos apps se vean igual.
4. **Las capturas del informe de jugabilidad** (`capturas/`) se hicieron con los sprites antiguos. Las de este informe (`capturas-pixel-art/`) muestran el arte actual.
5. **Datos de prueba:** cada prueba creó y borró una planta de lucia y, en un caso, un desastre de prueba en Arequipa (región sin jugadores). Lucia quedó con 150 hojas, 1000 XP, nivel 3, racha 1, en Lima y sin plantas.
