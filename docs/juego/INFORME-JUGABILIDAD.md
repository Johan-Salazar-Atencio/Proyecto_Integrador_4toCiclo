# PlantIA — Informe de reglas, sistemas y jugabilidad

**Fecha:** 18 de noviembre de 2026
**Base analizada:** `main` local en `6e2f0a0`. Cuando se cita un archivo, la ruta es relativa a la raíz del repositorio.

**Método:**
1. Lectura del código que decide el juego: Spring (`PlantasService`, `CrecimientoPlanta`, `AbonoService`, `ProgresoService`, `MisionService`, `ResultadoDesastreService`, `EventoCicloService`, `EventoService`, `ParametrosJuego`, `UsuarioService`), la web (`etapas.js`, `animo.js`, `Jardin.jsx`) y el móvil (`PlantaPixelLogica.kt`).
2. Lectura de los valores reales de la base de datos: parámetros, niveles, reglas, misiones, tienda y tipos de desastre.
3. **Partida de prueba en el navegador** con la usuaria lucia, de semilla a madura, pasando por marchita, plaga prevenida, helada sin prevenir, recuperación, muerte y resurrección. Cada fórmula se comprobó contra lo que mostró la pantalla. Al terminar, se restauraron todos los datos.

> **Contenido de las capturas:** las 26 imágenes están en [`capturas/`](capturas/). Para simular el paso del tiempo se modificaron a mano `updated_at`, `ultimo_dia_cuidado` y `recuperacion_hasta` en la base de datos, y se crearon dos desastres de prueba de 1 a 3 minutos en una región sin otros jugadores. Todo lo demás son clics normales en la web.

---

## Índice

1. [El juego en una frase y el bucle principal](#1-el-juego-en-una-frase-y-el-bucle-principal)
2. [La planta: sus tres barras](#2-la-planta-sus-tres-barras)
3. [Desgaste con el tiempo (fórmula)](#3-desgaste-con-el-tiempo-fórmula)
4. [Cuidados: regar, sol y abonar](#4-cuidados-regar-sol-y-abonar)
5. [Crecimiento: días, fases y dibujos](#5-crecimiento-días-fases-y-dibujos)
6. [Abonos de tienda](#6-abonos-de-tienda)
7. [Marchitez, "enfermedad" y ánimo](#7-marchitez-enfermedad-y-ánimo)
8. [Desastres: qué pasa antes, durante y después](#8-desastres-qué-pasa-antes-durante-y-después)
9. [Muerte y resurrección](#9-muerte-y-resurrección)
10. [Progresión: XP, niveles, rangos y racha](#10-progresión-xp-niveles-rangos-y-racha)
11. [Misiones del día](#11-misiones-del-día)
12. [Economía de hojas](#12-economía-de-hojas)
13. [Jardín, baúl, macetas, fondos y tienda](#13-jardín-baúl-macetas-fondos-y-tienda)
14. [Prueba visual paso a paso](#14-prueba-visual-paso-a-paso)
15. [Análisis de jugabilidad](#15-análisis-de-jugabilidad)
16. [Fallos e incoherencias encontrados](#16-fallos-e-incoherencias-encontrados)
17. [Recomendaciones](#17-recomendaciones)
18. [Anexo: dónde está cada regla en el código](#18-anexo-dónde-está-cada-regla-en-el-código)

---

## 1. El juego en una frase y el bucle principal

**PlantIA es un "tamagotchi de plantas reales":**
1. escaneas una planta con la cámara;
2. la IA la identifica;
3. la "digitalizas" en un jardín de pixel art;
4. la mantienes viva y creciendo con cuidados diarios;
5. la proteges de desastres climáticos de Perú (huayco, helada, ola de calor, plaga), que lanza el administrador.

Todo lo que haces da XP (sube de nivel) y hojas (la moneda), que se gastan en abonos, macetas, fondos y en revivir plantas.

```
        ┌──────────── ESCANEAR (foto real) ────────────┐
        │  IA identifica → +XP/+hojas (especie nueva)  │
        ▼                                              │
   COLECCIÓN ──► DIGITALIZAR ──► JARDÍN (pixel art)    │
                    +30 XP          │                  │
                                    ▼                  │
              ┌──── CUIDAR cada día (regar/sol/abonar) ──── +XP/+hojas pequeños
              │         └─► 1 día de crecimiento por día real
              │                         │
              │           SEMILLA ──► BROTE ──► ADULTA (madura)
              │
              │  el tiempo desgasta agua y luz → baja la salud → MARCHITA
              │
              └──── DESASTRE (admin) ─► PREVENIR a tiempo → SALVADA (+XP, +hojas)
                                     └► no prevenir → GOLPEADA → 24 h para abonar
                                                     └► no abonar → MUERTA → revivir (250 hojas)
   MISIONES del día y RACHA → más XP y hojas → NIVELES → TIENDA (abonos, macetas, fondos)
```

---

## 2. La planta: sus tres barras

Cada planta del jardín (`plantas_usuario`) tiene tres porcentajes de 0 a 100:

| Barra | Columna | Qué la sube | Qué la baja |
|---|---|---|---|
| 💧 **Agua** | `hidratacion_porcentaje` | Regar: +20 | −4 por hora |
| ☀️ **Luz** | `sol_porcentaje` | Sol: +15 | −3 por hora |
| ❤️ **Salud** | `salud_porcentaje` | Abonar (cuidado): +10, o **+45** en recuperación | −2 por hora **si el agua o la luz están bajo 30**; los desastres la bajan de golpe |

Los valores siempre se limitan entre 0 y 100: `tope(n) = max(0, min(100, n))`.

**Valores al digitalizar** (`PlantasService.digitalizar`, líneas 135-159):

| Campo | Valor inicial |
|---|---|
| Salud | **90** |
| Agua | **60** |
| Luz | **70** |
| Etapa | `SEMILLA` |
| Días cuidados | 0 |
| Crecimiento | 0 |

Si ya tenías esa especie, la planta **se reinicia** con estos valores y **no da XP**, para que no se pueda repetir por el premio.

**Colores de las barras en la web** (`animo.js`, `nivelBarra`): verde si ≥ 60, ámbar si ≥ 30 y roja si < 30.

---

## 3. Desgaste con el tiempo (fórmula)

Se calcula en el servidor (`PlantasService.envejecer`, líneas 321-348) **cada vez que se consulta la planta**: al abrir el jardín, el inicio, la colección o al cuidarla. No hay ninguna tarea que la desgaste en segundo plano.

```
horas = horas COMPLETAS desde updated_at
si horas <= 0: no cambia nada

agua  = tope(agua − min(100, 4 × horas))
luz   = tope(luz  − min(100, 3 × horas))
si agua < 30  o  luz < 30:
    salud = tope(salud − min(100, 2 × horas))
updated_at = updated_at + horas          (se conservan los minutos sobrantes)
```

**Excepciones:** las plantas **muertas** y las del **baúl** no se desgastan. Una planta golpeada cuyo plazo venció **muere** en este mismo cálculo (sección 9).

### Cuánto aguanta una planta sin cuidados
Partiendo de 100 / 100 / 100 y consultando la planta cada hora:

| Horas sin cuidar | Agua | Luz | Salud | Estado |
|---|---|---|---|---|
| 12 | 52 | 64 | 100 | bien |
| 17 | 32 | 49 | 100 | último momento sin pérdida |
| 18 | **28** | 46 | 98 | el agua ya es crítica: la salud empieza a bajar |
| 24 | 4 | **28** | 86 | |
| 36 | 0 | 0 | 62 | |
| 53 | 0 | 0 | **28** | **marchita** |

Es decir, una planta bien cuidada aguanta **unos 2 días (53 h)** sin cuidados antes de marchitarse.

### Comprobado en la prueba
- Desde agua 80 y luz 85, **12 h** después: agua 80 − 48 = **32** y luz 85 − 36 = **49**; la salud no cambia porque ninguna llegó a < 30 ([captura 05](capturas/05-desgaste-12h.jpg)).
- **36 h** más: agua y luz a 0, y salud 100 − 2×36 = **28**, marchita ([captura 06](capturas/06-marchita-48h.jpg)).

### ⚠️ Cuánto daño sufre depende de cada cuánto se abre la app
Como el cálculo se hace **en bloque** y la condición "agua o luz < 30" se mira **al final** del bloque, la salud baja **2 × todas las horas del bloque**, aunque en las primeras el agua y la luz estuvieran bien.

| Misma planta (agua 80, luz 85, salud 100), 48 h sin cuidar | Salud final |
|---|---|
| Se consulta cada hora | 100 − 2 × 36 = **28** (el agua es crítica desde la hora 13) |
| Se consulta a las 12 h y a las 48 h (lo que pasó en la prueba) | 100 − 2 × 36 = **28** |
| Se consulta **una sola vez**, a las 48 h | 100 − 2 × 48 = **4** |

Resultado: **quien abre la app menos a menudo recibe más daño por el mismo descuido**. Ver sección 16.

---

## 4. Cuidados: regar, sol y abonar

`POST /api/v1/plantas/{id}/cuidados` (`PlantasService.cuidar`, líneas 197-238). Son gratuitos e ilimitados.

| Cuidado | Efecto | Es "útil" si… | Premio (regla, tope diario) | Misiones que avanza |
|---|---|---|---|---|
| Regar | agua +20 | el agua estaba < 100 | `REGAR` +2 XP / +1 hoja (5 al día) | `RIEGO` y `CUIDADO` |
| Sol | luz +15 | la luz estaba < 100 | `SOL` +2 XP / +1 hoja (5 al día) | `SOL` y `CUIDADO` |
| Abonar | salud +10 (+45 en recuperación) | la salud estaba < 100 | `ABONAR` +3 XP / +1 hoja (3 al día) | `ABONO` y `CUIDADO` |

**Reglas:**
- Un cuidado **no útil** (por ejemplo, regar con el agua al 100 %) no da premio ni hace crecer.
- Un cuidado **útil** con la **salud ≥ 30** después de aplicarlo:
  - suma `crecimiento + 1`;
  - **cuenta el día** para crecer: solo una vez por día real (sección 5).
- En una planta **muerta**, cualquier cuidado responde 409 "La planta esta muerta: hay que revivirla".
- Los topes diarios son **por usuario**, no por planta: con varias plantas se llega antes al tope.

**Comprobado:**
- Regar 60 → 80, sol 70 → 85, abonar 90 → 100 ([capturas 02-04](capturas/04-tres-cuidados.jpg)).
- Los tres cuidados dieron 2 + 2 + 3 XP, y al completar la misión "Cuidado Completo" (3 cuidados), +25 XP y +12 hojas.

---

## 5. Crecimiento: días, fases y dibujos

### 5.1 Duración según la forma de la planta
La IA devuelve la **forma** de la especie al escanear (`apariencia.forma`). `CrecimientoPlanta.diasTotales`:

| Forma | Días para madurar |
|---|---|
| `suculenta`, `cactus`, `roseta`, `helecho` | **5** (pequeña) |
| `palmera`, `arbol` | **12** (grande) |
| cualquier otra (`arbusto`, `monstera`…) o sin forma | **7** (mediana) |

### 5.2 Cómo se gana un día
```
cuidado útil  Y  salud ≥ 30  Y  hoy ≠ ultimo_dia_cuidado
   → dias_cuidados = min(diasTotales, dias_cuidados + 1)
   → ultimo_dia_cuidado = hoy
```

- **Como mucho 1 día de crecimiento por día real**, por mucho que cuides.
- El abono de tienda es la única forma de ir más rápido (sección 6).
- **Mínimo de días reales para madurar** solo con cuidados: 5, 7 o 12.

**Comprobado:**
- El primer riego llevó la planta al día **1/7**; sol y abono el mismo día no sumaron más ([captura 04](capturas/04-tres-cuidados.jpg)).
- Simulando "día siguiente", cada riego sumó un día: 4 → 5 → 6 → 7.

### 5.3 Fases del servidor (3)
`CrecimientoPlanta.faseSegun(dias, total)`:

```
dias ≥ total                    → ADULTA  (madura)
dias ≥ techo(total × 0,35)      → BROTE
si no                           → SEMILLA
```

| Días totales | BROTE desde el día | ADULTA en el día |
|---|---|---|
| 5 | 2 | 5 |
| 7 | 3 | 7 |
| 12 | 5 | 12 |

**La fase nunca retrocede** (`subirFase` compara con `rango`). En la web se ven como 1, 2 o 3 puntos verdes junto a "Día N/total".

### 5.4 Dibujos (6 + marchita)
La web (`etapas.js`, `indiceSprite`) y el móvil (`PlantaPixelLogica.kt`) usan **la misma** escala de 6 dibujos según `avance = dias / total`:

| Dibujo | Condición | Nombre (fruto / flor / follaje) |
|---|---|---|
| 0 | `dias = 0` | Semilla |
| 1 | `avance < 0,34` | Brote |
| 2 | `avance ≥ 0,34` | Planta joven |
| 3 | `avance ≥ 0,6` | Planta adulta |
| 4 | `avance ≥ 0,8` | Con frutos / Con botones / Frondosa |
| 5 | `avance ≥ 1` | Cosecha / En flor / Esplendor |
| 6 | marchita (sección 7) | Marchita |

El nombre de los dos últimos depende del **ciclo** (`cicloDe`):
- monstera → follaje;
- suculenta → flor si tiene flor o fruto, si no follaje;
- otras → fruto si tiene fruto, flor si tiene flor, si no follaje.

**Comprobado con un tomate (arbusto, 7 días, con fruto):**

| Día | Avance | Dibujo | Captura |
|---|---|---|---|
| 0 | 0 | semilla en maceta | [02](capturas/02-semilla-dia0.jpg) |
| 1 | 0,14 | brote | [03](capturas/03-tras-regar.jpg) |
| 4 | 0,57 | planta joven (fase 2) | [08](capturas/08-abono-reforzado-dia4.jpg) |
| 5 | 0,71 | planta adulta | [10](capturas/10-dia5-adulta.jpg) |
| 6 | 0,86 | con frutos verdes | [11](capturas/11-dia6-con-frutos.png) |
| 7 | 1 | cosecha, tomates rojos, "Madura" | [12](capturas/12-madura-cosecha.jpg) |

---

## 6. Abonos de tienda

`POST /api/v1/plantas/{id}/abono-tienda` con `{ "idItem": n }` (`AbonoService`). Cuestan hojas y **adelantan días** de crecimiento. El crecimiento se mide en **medios días**.

| Abono | Precio | Medios días | Efecto |
|---|---|---|---|
| Abono suave | 15 🍃 | 1 | +½ día (dos suaves = 1 día) |
| Abono orgánico | 30 🍃 | 2 | +1 día |
| Abono reforzado | 80 🍃 | 6 | +3 días |

```
total   = medios + (1 si había medio día pendiente)
dias    = min(diasTotales, dias + total ÷ 2)
pendiente = (total es impar) Y (dias < diasTotales)
```

**Se rechaza antes de cobrar si:**
1. la planta no es tuya → 404;
2. está **muerta o con salud < 30** → 409 "Primero hay que recuperar la planta";
3. **ya creció del todo** → 409 "La planta ya creció del todo";
4. **ya usaste un abono de tienda hoy con esa planta** → 409;
5. el abono no existe o no está activo → 404;
6. no te alcanzan las hojas → 402.

**Comprobado:**
- El reforzado llevó el tomate del día 1 al **4/7** y lo pasó a fase 2 ([captura 08](capturas/08-abono-reforzado-dia4.jpg)).
- Un segundo abono el mismo día respondió "Ya usaste un abono con esta planta hoy" ([captura 09](capturas/09-segundo-abono-rechazado.jpg)).

**Velocidad máxima de crecimiento:** cuidado diario (+1) más un reforzado (+3) = 4 días de crecimiento por día real. Así una planta de 7 días madura en **2 días reales** pagando 160 hojas, y una de 12 en 3 días pagando 240.

---

## 7. Marchitez, "enfermedad" y ánimo

### 7.1 Marchita
**Regla del servidor:** `SALUD_MARCHITA = 30` (`PlantasService.java:66`).

**En la web** (`etapas.js`, `estaMarchita`) la planta se ve marchita si se cumple cualquiera de estas condiciones:

```
salud < 30   O   fase = MUERTA   O   enRecuperacion (golpeada por un desastre)
```

Qué significa estar marchita (salud < 30):

| Efecto | Detalle |
|---|---|
| No crece | los cuidados útiles no cuentan días ni crecimiento |
| No admite abonos de tienda | 409 "Primero hay que recuperar la planta" |
| Se ve seca | sprite 6 (marrón) y etiqueta "Marchita" |
| **No muere** | por descuido la salud puede llegar a 0 %, pero la planta sigue viva (sección 9) |

**Cómo se recupera:** con el cuidado gratuito **Abonar** (+10) hasta llegar a ≥ 30. En la prueba, de 28 a 38 con un solo clic aparece "¡Tomate se recuperó!" ([captura 07](capturas/07-recuperada-con-abono.jpg)). Pero si el agua o la luz siguen bajo 30, la salud vuelve a caer 2 por hora.

### 7.2 ¿Existe una "enfermedad"?
**No hay un sistema de enfermedades.** No hay plagas individuales, hongos ni estados aleatorios por planta. Lo más parecido es:

| Lo que el jugador puede entender como "enfermar" | Qué es en realidad |
|---|---|
| La planta se pone marrón | **Marchita**: salud < 30 por falta de agua o luz |
| "En recuperación" con cuenta atrás | **Golpeada por un desastre** (sección 8) |
| "Plaga detectada" | Es **un tipo de desastre** global o regional que lanza el admin, no algo que le pase a una planta concreta |

La pestaña de cuidados de la ficha de especie tiene secciones `PLAGAS` y `TOXICIDAD` (`CatalogoService`), pero son **texto informativo** de la IA y no afectan al juego.

### 7.3 Ánimo (la carita)
Solo lo calcula la interfaz (`animo.js`, `estadoAnimo`):

| Condición | Carita |
|---|---|
| muerta o en recuperación | 😢 Triste |
| salud ≥ 70 | 😊 Feliz |
| salud ≥ 40 | 😐 Regular |
| salud < 40 | 😢 Triste |

El **consejo** muestra la necesidad más baja: agua → "le falta agua: ¡riégala!", luz → "necesita más luz", salud → "está débil: un poco de abono le ayudará". Si todo está ≥ 70: "está en excelente estado".

---

## 8. Desastres: qué pasa antes, durante y después

### 8.1 Tipos y textos
Hay 4 tipos en `tipos_desastre`, todos activos, con texto educativo sobre el campo peruano y un consejo:

| Código | Nombre | Consejo |
|---|---|---|
| `HUAYCO` | Huayco | "Las zanjas y los muros de contención desvían el agua y el lodo." |
| `HELADA` | Helada | "Cubrir los cultivos con mantas o plásticos por la noche los protege del frío." |
| `OLA_DE_CALOR` | Ola de calor | "Cubrir el suelo con paja o rastrojo ayuda a conservar la humedad." |
| `PLAGA` | Plaga | "Revisar las hojas todos los días permite detectar una plaga a tiempo." |

### 8.2 Ciclo de vida
El admin crea un evento con tipo, **severidad** (BAJO, MODERADO o CRÍTICO), **región**, fechas y mensaje. `EventoScheduler` llama a `EventoCicloService.avanzar` **cada 30 s** (`app.eventos.intervalo-ms`):

```
PROGRAMADO ──(llega fecha_inicio)──► ACTIVO ──(llega fecha_fin)──► FINALIZADO ──► se procesa el resultado
```

- Mientras está ACTIVO, la animación es **AVISO** durante el primer 40 % del tiempo (`FRACCION_AVISO = 0.4`) y **PICO** el resto.
- **Afecta a** las plantas de los usuarios de esa región, o a todas si la región es `GLOBAL`. La región la elige el jugador en su perfil ([captura 17](capturas/17-perfil-region.jpg)).

### 8.3 Durante el evento: prevenir
- En **Inicio** aparece la tarjeta del desastre con su explicación y una cuenta atrás, y una misión especial "Prevenir \<evento\>" (+80 XP · +40 hojas) ([captura 13](capturas/13-inicio-con-plaga.jpg)).
- En el **jardín**, el botón rojo "🛡️ Prevenir evento: \<tipo\>" ([captura 14](capturas/14-jardin-boton-prevenir.jpg)) marca la planta "A resguardo" ([captura 15](capturas/15-a-resguardo.jpg)).
- Hay que prevenir **planta por planta**. Es gratis y no da premio en el momento: el premio llega al terminar.
- La escena muestra el efecto del desastre (insectos en la plaga [18](capturas/18-plaga-en-escena.png), escarcha en la helada [19](capturas/19-helada-activa.jpg)).

### 8.4 Después: el resultado (`ResultadoDesastreService.aplicar`)
Para cada planta afectada (las muertas y las del baúl se saltan):

| Si estaba… | Resultado | Efecto |
|---|---|---|
| **A resguardo** | `SALVADA` | sin daño; +10 XP / +3 hojas (`REFUGIO`) por planta |
| **Sin proteger** | `DANADA` | `salud = min(salud, salud_final)`; empieza la **recuperación** de 24 h; se guarda el tipo para la animación del golpe |

Si el usuario **protegió todas** sus plantas afectadas: **+80 XP / +40 hojas** (`EVENTO_COMPLETO`).

**Salud final según la severidad** (columnas de `tipos_desastre`; hoy son iguales en los cuatro tipos):

| Severidad | Salud tras el golpe |
|---|---|
| BAJO | **40** |
| MODERADO | **20** |
| CRÍTICO | **10** |

Es un **tope**, no una resta: una planta con salud 15 golpeada por un desastre MODERADO se queda en 15, no baja más.

Las columnas `dano_bajo`, `dano_moderado` y `dano_critico` (10, 25 y 50) existen pero **no las usa** el código actual, que usa `salud_final_*`.

**Ventana de gracia:** si el servidor estuvo apagado y el evento terminó hace más de 6 h (`app.eventos.gracia-horas`), se marca como procesado **sin dañar** a nadie.

### 8.5 Recuperación tras el golpe
- `recuperacion_hasta = ahora + 24 h` (`HORAS_RECUPERACION`, ajustable por el admin).
- La web muestra "⚠️ En recuperación · 23:59:47" y "Solo el abono la recupera" ([captura 20](capturas/20-golpe-helada.jpg)).
- Cada **Abonar** (gratuito) suma **+45** (`ABONO_RECUPERACION`) en lugar de +10.
- Al llegar a **100** se sale de la recuperación.
- Desde el golpe la planta no puede ir al baúl (409), para que no se use el baúl para esquivar el plazo.

**Comprobado:**
1. Plaga MODERADA **prevenida**: `SALVADA`, salud intacta, +10 XP/+3 hojas (`REFUGIO`) y +80/+40 (`EVENTO_COMPLETO`).
2. Helada MODERADA **sin prevenir**: salud de 38 a **20**, aviso "¡La helada arrasó con tu planta!" durante 3,5 s, cuenta atrás de 24 h ([20](capturas/20-golpe-helada.jpg)).
3. Primer abono: de 20 a 65, sigue en recuperación ([21](capturas/21-recuperacion-primer-abono.jpg)). Segundo abono: 100, "¡Tomate se recuperó!", vuelve a verse madura ([22](capturas/22-recuperada-100.jpg)).

---

## 9. Muerte y resurrección

**Solo hay una forma de morir** (`PlantasService.envejecer`, líneas 326-329): estar golpeada por un desastre y que pasen las 24 h sin llegar a 100 de salud.

```
si recuperacion_hasta ≠ null  Y  ahora > recuperacion_hasta:
    etapa = MUERTA, salud = 0, sin recuperación, sin resguardo
    (se conservan los días cuidados)
```

**Por descuido no se muere:** la salud puede llegar a 0 %, pero la planta solo queda marchita y se recupera con abonos gratuitos.

**Muerta:**
- no se puede cuidar, abonar ni proteger;
- no se desgasta;
- **sigue ocupando un hueco** del jardín.

La web muestra "💀 Tomate murió · Revivir · 250" ([captura 23](capturas/23-muerta.jpg)).

**Revivir** (`PlantasService.revivir`):
- cuesta **250 hojas** (`COSTO_REVIVIR`); sin saldo, "No te alcanzan las hojas para revivirla" ([24](capturas/24-revivir-sin-hojas.jpg));
- vuelve con salud **50** (`SALUD_REVIVIDA`), agua 50 y luz 50;
- **conserva su fase** según los días cuidados: no vuelve a empezar ([25](capturas/25-revivida.jpg)).

---

## 10. Progresión: XP, niveles, rangos y racha

### 10.1 Niveles (tabla `niveles`)
`xp_requerido` es el XP **total** acumulado. Al subir se cobra el premio en hojas; se pueden subir varios niveles de una vez.

| Nivel | XP total | XP desde el anterior | Premio |
|---|---|---|---|
| 1 | 0 | — | — |
| 2 | 500 | 500 | 20 🍃 |
| 3 | 1 000 | 500 | 25 🍃 |
| 4 | 1 700 | 700 | 30 🍃 |
| 5 | 2 450 | 750 | 40 🍃 |
| 6 | 3 000 | 550 | 50 🍃 |
| 7 | 4 000 | 1 000 | 60 🍃 |
| 8 | 5 500 | 1 500 | 75 🍃 |
| 9 | 7 500 | 2 000 | 90 🍃 |
| 10 | 10 000 | 2 500 | 120 🍃 |

El **nivel 10 es el máximo**: no hay más niveles definidos. La curva **no es uniforme**: del 5 al 6 cuesta menos (550) que del 4 al 5 (750).

**Rangos** (`UsuarioService.badge`): niveles 1-4 **Aprendiz**, 5-9 **Cuidador**, 10 **Maestro Botánico**.

**Desbloqueos por nivel** (`items_tienda.nivel_requerido`): fondos "Desierto dorado" y "Cerezos en flor" (2), "Ciudad cyberpunk" (3); invernaderos "Cristal victoriano" (2) y "Taller de luces" (3); maceta "Dorada" (2). Todo lo demás está desde el nivel 1. **No hay nada que desbloquear a partir del nivel 4.**

### 10.2 Racha
`ProgresoService.actualizarRacha`:
- la primera acción con premio de cada día suma `RACHA` (+10 XP, +3 hojas);
- si ayer también hubo actividad, `racha_dias + 1`; si se saltó un día, vuelve a 1.

La racha **no multiplica** nada: solo es un contador y +10 XP al día.

### 10.3 Reglas de premio vigentes (`reglas_progreso`)

| Motivo | XP | Hojas | Tope diario | Cuándo |
|---|---|---|---|---|
| `ESCANEO_NUEVA` | 70 | 20 | 5 | escanear una especie que nunca habías escaneado |
| `ESCANEO_REPETIDO` | 10 | 3 | 3 | escanear otra vez una especie ya vista |
| `DIGITALIZAR` | 30 | 10 | — | llevar al jardín una especie que no tenías |
| `REGAR` / `SOL` | 2 | 1 | 5 cada una | cuidado útil |
| `ABONAR` | 3 | 1 | 3 | cuidado útil |
| `RACHA` | 10 | 3 | 1 | primera acción del día |
| `REFUGIO` | 10 | 3 | — | por cada planta salvada en un desastre |
| `EVENTO_COMPLETO` | 80 | 40 | — | salvar todas tus plantas de un desastre |
| `CUIDAR`, `ESPECIE_NUEVA`, `ESCANEO` | — | — | — | definidas pero **sin uso** en el código |

### 10.4 Ritmo estimado
XP de un día:

| Tipo de jugador | Qué hace al día | XP/día aprox. |
|---|---|---|
| Casual | racha (10) + 3 cuidados (7) + 2 misiones (40) | **~60** |
| Constante | racha (10) + 5 cuidados de cada tipo (29) + las 4 misiones (110) + 1 escaneo nuevo (70) | **~220** |
| Máximo | lo anterior + 5 escaneos nuevos (350) + 3 repetidos (30) + digitalizar especies nuevas (30 c/u) | **~550+** |

| Nivel | Casual | Constante | Máximo |
|---|---|---|---|
| 2 (500 XP) | ~8 días | ~2-3 días | 1 día |
| 5 (2 450 XP) | ~40 días | ~11 días | ~5 días |
| 10 (10 000 XP) | ~165 días | **~45 días** | ~18 días |

Los escaneos de especies nuevas son finitos: cuando ya escaneaste las plantas de tu entorno, el XP diario baja hacia el ritmo "constante" sin escaneos (~150/día).

---

## 11. Misiones del día

`MisionService`: cada día, cada usuario recibe **todas las misiones activas** con su propio progreso. Se reinician con la fecha y pagan una sola vez al día al completarse.

| Misión | Tipo | Meta | Premio | Activa |
|---|---|---|---|---|
| Riego Consciente | `RIEGO` | 2 riegos | 15 XP · 8 🍃 | sí |
| Cuidado Completo | `CUIDADO` (cualquier cuidado) | 3 | 25 XP · 12 🍃 | sí |
| Explorador Botánico | `ESCANEO` | 1 | 30 XP · 15 🍃 | sí |
| Jardinero Digital | `DIGITALIZAR` | 1 | 40 XP · 20 🍃 | sí |
| A Resguardo | `REFUGIO` | 1 | 40 XP · 50 🍃 | **no** |
| *Prevenir \<evento\>* (dinámica) | evento | todas tus plantas | 80 XP · 40 🍃 | solo con desastre activo |

**Total diario de las misiones fijas: 110 XP y 55 hojas.** "Jardinero Digital" exige digitalizar algo **cada día**, y solo paga si la especie es nueva. Cuando ya tienes todas tus especies en el jardín, esa misión no se puede completar.

Pantalla: [captura 16](capturas/16-misiones.jpg).

---

## 12. Economía de hojas

| Ingresos al día (jugador constante) | Hojas |
|---|---|
| Racha | 3 |
| Cuidados (5 + 5 + 3 en el tope) | 13 |
| Misiones fijas | 55 |
| 1 escaneo nuevo | 20 |
| Subida de nivel (de vez en cuando) | 20-120 |
| **Total** | **~90/día** |

Cada usuario nuevo empieza con **100 hojas** (`AuthService`).

| Gastos | Hojas | Equivale a… |
|---|---|---|
| Abono suave / orgánico / reforzado | 15 / 30 / 80 | 0,2 / 0,3 / 0,9 días de ingresos |
| Maceta (crema, rosa, dorada) | 80 / 100 / 150 | 1-1,7 días |
| Fondo (nevado, desierto, cerezos, cyberpunk) | 100 / 150 / 180 / 250 | 1-2,8 días |
| Invernadero (victoriano, luces) | 150 / 200 | 1,7-2,2 días |
| **Revivir una planta** | **250** | **~2,8 días** |

Todo el contenido cosmético de pago (3 macetas por 330, 4 fondos por 680 y 2 invernaderos por 350; terracota, azul y selva son gratis) cuesta **1 360 hojas**: unos **15 días** de juego constante. Después, las hojas solo sirven para abonos y para revivir.

---

## 13. Jardín, baúl, macetas, fondos y tienda

| Sistema | Regla | Dónde |
|---|---|---|
| Plantas en el jardín | máximo **12** (`MAX_PLANTAS_ACTIVAS`); las muertas también cuentan | `PlantasService.digitalizar` |
| Baúl | congela la planta: no crece, no se desgasta, no le afectan los desastres y no da premios; libera un hueco. No se puede guardar una planta en recuperación | `guardarEnBaul`, `sacarDelBaul` |
| Planta activa | la que se ve en "Mi jardín"; una por usuario | `seleccionar` |
| Apodo | hasta 50 caracteres; vacío vuelve al nombre de la especie | `renombrar` |
| Macetas | se aplican a varias plantas a la vez (hasta 50 por petición) | `MacetasService` |
| Fondos | temas con escena de día, tarde y noche (tiras de 96 fotogramas), por planta | `FondosPlantaService` |
| Invernaderos | estilo de la colección virtual | `UsuarioService` |

Tienda: [captura 26](capturas/26-tienda-macetas.jpg).

---

## 14. Prueba visual paso a paso

Partida real en `http://127.0.0.1:5173` con lucia@plantia.test (nivel 3, 150 hojas, sin plantas):

| # | Acción | Resultado observado | Captura |
|---|---|---|---|
| 1 | Entrar en la web sin sesión | Bienvenida "Tu jardín cobra vida" | [01](capturas/01-bienvenida.jpg) |
| 2 | Digitalizar un tomate (arbusto, 7 días) | Semilla en maceta, salud 90 / agua 60 / luz 70, día 0/7; +80 XP y +33 hojas (digitalizar + racha + misión) | [02](capturas/02-semilla-dia0.jpg) |
| 3 | Regar | Agua 80, brote, **día 1/7** | [03](capturas/03-tras-regar.jpg) |
| 4 | Sol y abonar | Luz 85, salud 100, sigue en el día 1/7 (un día por día); misión "Cuidado Completo" | [04](capturas/04-tres-cuidados.jpg) |
| 5 | Simular 12 h | Agua 32, luz 49, salud 100 | [05](capturas/05-desgaste-12h.jpg) |
| 6 | Simular 36 h más | Agua 0, luz 0, salud 28 → **Marchita** | [06](capturas/06-marchita-48h.jpg) |
| 7 | Regar y abonar | Salud 38, "¡Tomate se recuperó!" | [07](capturas/07-recuperada-con-abono.jpg) |
| 8 | Abono reforzado (80) | **Día 4/7**, fase 2, planta joven | [08](capturas/08-abono-reforzado-dia4.jpg) |
| 9 | Otro abono el mismo día | "Ya usaste un abono con esta planta hoy" | [09](capturas/09-segundo-abono-rechazado.jpg) |
| 10 | Día siguiente + regar | Día 5/7, planta adulta | [10](capturas/10-dia5-adulta.jpg) |
| 11 | Día siguiente + regar | Día 6/7, con frutos verdes | [11](capturas/11-dia6-con-frutos.png) |
| 12 | Día siguiente + sol | **Día 7/7 "Madura"**, tomates rojos | [12](capturas/12-madura-cosecha.jpg) |
| 13 | Lanzar una plaga MODERADA de 3 min | Tarjeta de desastre y cuenta atrás en Inicio; misión "Prevenir plaga" | [13](capturas/13-inicio-con-plaga.jpg) |
| 14 | Pulsar "Prevenir evento" | "A resguardo" / "Evento prevenido" | [14](capturas/14-jardin-boton-prevenir.jpg), [15](capturas/15-a-resguardo.jpg) |
| 15 | Fin de la plaga | `SALVADA`; +10 XP/+3 (REFUGIO) y +80/+40 (EVENTO_COMPLETO) | — |
| 16 | Lanzar una helada MODERADA de 1 min, sin prevenir | Escarcha en la escena | [19](capturas/19-helada-activa.jpg) |
| 17 | Fin de la helada | "¡La helada arrasó con tu planta!", salud 38 → 20, **en recuperación 24 h** | [20](capturas/20-golpe-helada.jpg) |
| 18 | Abonar | Salud 65 (+45), sigue en recuperación | [21](capturas/21-recuperacion-primer-abono.jpg) |
| 19 | Abonar | Salud 100, "¡Tomate se recuperó!" | [22](capturas/22-recuperada-100.jpg) |
| 20 | Simular el plazo vencido | "💀 Tomate murió", Revivir · 250 | [23](capturas/23-muerta.jpg) |
| 21 | Revivir con 177 hojas | "No te alcanzan las hojas para revivirla" | [24](capturas/24-revivir-sin-hojas.jpg) |
| 22 | Revivir con 300 hojas | "¡Tomate revivió!", 50 / 50 / 50, sigue madura | [25](capturas/25-revivida.jpg) |

**Limpieza:** se borraron la planta, los 2 eventos de prueba, sus registros y los 18 movimientos de premios. Lucia volvió a 150 hojas, 1000 XP, nivel 3, racha 1, región Lima y misiones de hoy a 0. No se tocó a ningún otro usuario: para el desastre, lucia se movió a Arequipa, donde no hay ningún otro jugador con plantas.

---

## 15. Análisis de jugabilidad

### 15.1 Lo que funciona bien
| Aspecto | Por qué funciona |
|---|---|
| **Gancho real** | Escanear plantas de verdad y verlas "nacer" en pixel art es original y une el mundo físico con el juego |
| **Arte y respuesta visual** | 6 dibujos por especie con colores de la IA, escenas animadas por desastre, tomates que maduran de verde a rojo; cada acción tiene aviso y animación |
| **Ritmo diario** | 1 día de crecimiento por día real, más racha y misiones, invita a volver cada día sin exigir horas |
| **Valor educativo** | Los desastres explican fenómenos reales del Perú con consejos agrícolas concretos |
| **Reglas seguras en el servidor** | Crecimiento, desgaste, premios y topes los calcula Spring: la web no puede hacer trampas, y tras corregir el doble gasto, la economía es coherente bajo concurrencia |
| **Ajustable sin código** | Costes, horas de recuperación, salud revivida, tope de plantas, premios, misiones y desastres se cambian desde el panel admin |
| **El abono de tienda tiene sentido** | Es la única forma de acelerar y está limitado a 1 al día por planta; los tres precios ofrecen una decisión real |

### 15.2 Problemas de diseño (afectan a la diversión)
| # | Problema | Efecto en el jugador | Gravedad |
|---|---|---|---|
| D1 | **No se muere por descuido**: sin desastres, lo peor es "marchita", y se arregla con 2-3 clics gratis | Cuidar no tiene consecuencias reales: la tensión de "mantenerla viva" desaparece | Alta |
| D2 | **Recuperarse de un desastre es trivial**: 2 abonos gratis dentro de 24 h | Prevenir apenas importa; solo muere quien no abre la app en todo un día | Alta |
| D3 | **No hay nada después de "Madura"**: no hay cosecha, semillas ni premio por completar | La meta principal termina en 2-12 días y la planta pasa a ser decorativa | Alta |
| D4 | El daño por descuido **depende de cada cuánto abres la app** (sección 3) | Injusto y difícil de entender: el mismo descuido cuesta 72 o 96 de salud | Media |
| D5 | **Cambiar de región esquiva el desastre**: la región se lee al terminar el evento (`findDeUsuariosDeRegion`) | Un jugador puede cambiar de región en su perfil durante el evento y no recibir daño | Media |
| D6 | "Jardinero Digital" pide digitalizar **cada día**, y solo paga con especies nuevas | Pronto deja de poder completarse y el 36 % del XP de misiones se pierde | Media |
| D7 | **Nada que desbloquear desde el nivel 4** y el nivel 10 no tiene premio especial | Subir de nivel pierde sentido a mitad de camino | Media |
| D8 | La "plaga" es un desastre global, **no una enfermedad de la planta** | Falta un reto individual: detectar y tratar problemas concretos de cada planta | Media |
| D9 | Los cosméticos se acaban en ~15 días y después las hojas solo sirven para abonos y revivir | La economía se queda sin objetivos a medio plazo | Baja |
| D10 | Cuidar es "pulsar los tres botones": no hay que decidir qué necesita la planta, porque todo está siempre disponible y sin coste | Poca profundidad; las especies se cuidan igual aunque la IA diga lo contrario (un cactus pide el mismo riego que un tomate) | Media |

---

## 16. Fallos e incoherencias encontrados

Vistos en la prueba o leyendo el código. Ninguno está corregido todavía.

| # | Fallo | Dónde | Visto en |
|---|---|---|---|
| F1 | **La cabecera no actualiza las hojas** tras cuidar, abonar o revivir (183 en pantalla con 198 reales; 198 tras pagar 80; 177 tras pagar 250) | barra superior de la web | capturas 04, 08, 25 |
| F2 | El botón **Abonar dice "+10 %"** aunque en recuperación suma +45 | `Jardin.jsx` | 20-21 |
| F3 | Una planta **muerta** muestra la etiqueta "Marchita" y el consejo "un poco de abono le ayudará", aunque no se puede abonar | `Jardin.jsx`, `animo.js` | 23 |
| F4 | El dibujo **marchito es siempre la planta adulta**: un brote de 1 día se marchita y aparece como planta adulta seca | `plantaPixelSprites.js` (`marchita` usa el dibujo completo) | 06 |
| F5 | El menú resalta **"Escanear"** estando en el jardín | barra superior | todas las del jardín |
| F6 | Inicio dice "3 de **5** completadas" y Misiones "3 de **4**" (uno cuenta la misión del evento y el otro no) | `MisionDiaria.jsx` / `MisionesUsuario.jsx` | 13, 16 |
| F7 | **Web y móvil no coinciden en "marchita"**: la web incluye `enRecuperacion`; el móvil (`VirtualGardenScreen.kt:357`) solo `salud < 30 \|\| muerta`. Una planta golpeada con salud 65 se ve marchita en la web y sana en el móvil | web / móvil | código |
| F8 | `dano_bajo`, `dano_moderado` y `dano_critico` de `tipos_desastre` **no se usan** (el daño sale de `salud_final_*`) | `ResultadoDesastreService` | código |
| F9 | `digitalizar` acepta **cualquier `idCatalogo`** por la API, sin exigir haberla escaneado o tenerla en la colección: se puede ganar `DIGITALIZAR` (+30 XP) con especies nunca vistas, hasta 12 | `PlantasService.digitalizar`, línea 109 | código |
| F10 | Las reglas `CUIDAR`, `ESPECIE_NUEVA` y `ESCANEO` están en la BD pero ningún código las usa; el admin puede editarlas sin efecto | `reglas_progreso` | código |
| F11 | El texto del mensaje y la descripción del abono orgánico no llevan tilde ("organico", "maximo") | `items_tienda` | 08 |

---

## 17. Recomendaciones

Ordenadas por impacto en la diversión respecto al esfuerzo:

1. **Cosecha al madurar (D3):** al llegar a "Madura", un botón "Cosechar" que dé hojas y XP según la especie y devuelva la planta a "Brote", o que dé una "semilla" para plantar otra. Así la meta se repite en un bucle.
2. **Consecuencias reales del descuido (D1, D4):**
   - que una planta con salud 0 durante más de 24 h muera;
   - calcular el daño hora a hora: la salud baja solo desde que el agua o la luz llegan a < 30, no por todo el bloque. Es una fórmula cerrada: horas críticas = horas − horas hasta llegar a 30.
3. **Necesidades según la especie (D10):** usar los cuidados de la IA (riego y luz) para que un cactus pierda agua más despacio y un helecho más rápido. Ya existe `apariencia.forma` y la ficha de cuidados.
4. **Recuperación con coste o riesgo (D2):** que en recuperación el abono gratuito sume menos (por ejemplo +20) o que haga falta un abono de tienda.
5. **Fijar la región del evento (D5):** guardar qué plantas afecta al **empezar** el evento, o bloquear el cambio de región durante un desastre activo.
6. **Arreglar la interfaz (F1-F6):**
   - refrescar el saldo con el `progreso` que devuelve cada respuesta;
   - mostrar "+45 %" en recuperación;
   - etiqueta "Muerta" y consejo de revivir;
   - marchita según la etapa;
   - menú activo correcto;
   - mismo contador de misiones en las dos pantallas.
7. **Alinear el móvil con la web en "marchita" (F7).**
8. **Validar `digitalizar` (F9):** exigir que la especie esté en la colección del usuario o en un escaneo suyo.
9. **Progresión (D6, D7):** cambiar "Jardinero Digital" por "Cuida 2 plantas distintas"; repartir desbloqueos hasta el nivel 10, con un fondo o maceta exclusivos de "Maestro Botánico".
10. **Enfermedades por planta (D8):** un estado aleatorio de baja probabilidad (hongo, pulgón) que aparezca con poca luz o exceso de agua y que se trate con una acción concreta. Encaja con las secciones `PLAGAS` que ya genera la IA.

---

## 18. Anexo: dónde está cada regla en el código

| Regla | Archivo | Líneas / símbolo |
|---|---|---|
| Valores iniciales al digitalizar | `backend-spring-boot/src/main/java/com/plantia/services/PlantasService.java` | 135-159 |
| Desgaste por hora | ídem | `envejecer`, 321-348; constantes 66-70 |
| Cuidados y crecimiento | ídem | `cuidar`, 197-238; `contarDia`, 481-488 |
| Medios días (abono de tienda) | ídem | `sumarMediosDias`, 500-506 |
| Muerte | ídem | `morir`, 432-438 (llamado en 326-329) |
| Revivir | ídem | `revivir`, 444-475 |
| Baúl | ídem | `guardarEnBaul` / `sacarDelBaul`, 381-412 |
| Días totales y fases | `…/services/CrecimientoPlanta.java` | `diasTotales`, `faseSegun`, `rango` |
| Abonos de tienda | `…/services/AbonoService.java` | `abonar` |
| Premios, niveles, racha, misiones | `…/services/ProgresoService.java` | `aplicar`, `sumar`, `actualizarRacha`, `avanzarMisiones`, `topeAlcanzado` |
| Asignación de misiones | `…/services/MisionService.java` | `asignadas` |
| Parámetros ajustables | `…/services/ParametrosJuego.java` + tabla `parametros_juego` | `COSTO_REVIVIR` 250, `HORAS_RECUPERACION` 24, `ABONO_RECUPERACION` 45, `SALUD_REVIVIDA` 50, `MAX_PLANTAS_ACTIVAS` 12 |
| Ciclo de los desastres | `…/services/EventoCicloService.java`, `…/scheduler/EventoScheduler.java` | cada 30 s |
| Resultado del desastre | `…/services/ResultadoDesastreService.java` | `aplicar`, `saludFinalSegunSeveridad`; gracia de 6 h |
| Alerta, fase AVISO/PICO | `…/services/EventoService.java` | `FRACCION_AVISO = 0.4` |
| Rangos | `…/services/UsuarioService.java` | `badge` |
| Dibujos, nombres y marchita (web) | `frontend-react/src/pages/user/jardin/etapas.js` | `indiceSprite`, `faseNumero`, `estaMarchita` |
| Ánimo y consejo (web) | `frontend-react/src/pages/user/jardin/animo.js` | `estadoAnimo`, `consejo`, `nivelBarra` |
| Ciclo de fruto / flor / follaje | `frontend-react/src/components/plantaPixelSprites.js` | `cicloDe` |
| Dibujos (móvil) | `mobile-kotlin/app/src/main/java/com/plantia/ui/escena/PlantaPixelLogica.kt` | `indiceSprite` |
