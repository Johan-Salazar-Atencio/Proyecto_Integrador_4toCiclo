"""Fondos extra para vender en la tienda (96x96, 96 frames, mismo ritmo calmado que los predeterminados).

Cada uno sirve para un momento del dia (DIA, TARDE o NOCHE) y reemplaza al predeterminado de ese
momento cuando el usuario lo equipa. Reutilizan las herramientas de fondos.py. Todo lo que se mueve
da un numero entero de vueltas en 96 frames, asi el ciclo cierra sin saltos.

  cyberpunk (NOCHE)  ciudad con neones, lluvia y autos voladores
  nevado    (DIA)    campo con nieve cayendo y pinos
  desierto  (TARDE)  dunas, cactus y una planta rodadora
  sakura    (DIA)    cerezos con petalos al viento
"""
import math
import random

from PIL import Image

import fondos
from fondos import BAYER, FRAMES, N, Lienzo, cielo, hex_rgb as c, montanas, nubes


def piso(lz, borde, cesped, tierra, tierra2, brillo=None):
    """Suelo comun: borde en la fila 72, cesped 73-74 y tierra desde la 75 (donde se apoya la planta)."""
    for y in range(72, N):
        for x in range(N):
            if y == 72:
                lz.poner(x, y, borde)
            elif y < 75:
                lz.poner(x, y, cesped)
            else:
                lz.poner(x, y, tierra2 if (x * 7 + y * 13) % 11 == 0 else tierra)
                if brillo and (x * 5 + y * 3) % 23 == 0:
                    lz.poner(x, y, brillo)


def ciclo(f, vueltas, fase=0.0):
    """Angulo que da `vueltas` vueltas completas durante los 96 frames."""
    return 2 * math.pi * vueltas * f / FRAMES + fase


# --------------------------------------------------------------------------------- cyberpunk

def edificio(lz, x0, ancho, y_top, cuerpo, borde, ventanas, f, rng, colores, base=72):
    for x in range(x0, x0 + ancho):
        for y in range(y_top, base):
            lz.poner(x, y, cuerpo)
        lz.poner(x, y_top, borde)
    for y in range(y_top, base):
        lz.poner(x0, y, borde)
    # ventanas: algunas se apagan y se encienden despacio
    for k, (vx, vy) in enumerate((vx, vy) for vy in range(y_top + 3, base - 2, 4) for vx in range(x0 + 2, x0 + ancho - 1, 3)):
        if rng.random() < ventanas and (f // 12 + k) % 4 != 0:
            lz.poner(vx, vy, colores[k % len(colores)])


def cyberpunk(f):
    lz = Lienzo()
    rng = random.Random(11)
    cielo(lz, [c(h) for h in ("#0a0420", "#180a3a", "#2e0f5c", "#5d1a78", "#a02a86", "#d9488f")])
    # luna neon con franjas, al estilo retrowave
    for y in range(20, 46):
        for x in range(54, 90):
            if (x - 72) ** 2 + (y - 33) ** 2 <= 13 * 13 and not (y > 33 and (y - 33) % 4 == 3):
                lz.poner(x, y, c("#ff5fa8") if y < 33 else c("#ff8a5c"))
    # edificios lejanos (siluetas) y cercanos (con neones)
    xs = 0
    while xs < N:
        ancho = rng.randint(8, 14)
        edificio(lz, xs, ancho, rng.randint(44, 58), c("#1b0f45"), c("#3a2580"), 0.35, f, rng, [c("#7b6cff"), c("#ff7be0")])
        xs += ancho
    xs = -4
    while xs < N:
        ancho = rng.randint(10, 17)
        edificio(lz, xs, ancho, rng.randint(26, 50), c("#0c0628"), c("#19e6ff") if rng.random() < 0.5 else c("#ff3fb4"),
                 0.6, f, rng, [c("#19e6ff"), c("#ffe45c"), c("#ff3fb4")])
        xs += ancho + rng.randint(0, 2)
    # letreros de neon que laten
    for k, (x0, y0, ancho, color) in enumerate(((10, 36, 7, "#19e6ff"), (38, 30, 8, "#ff3fb4"), (66, 40, 6, "#b6ff3f"), (84, 28, 7, "#ffe45c"))):
        t = 0.65 + 0.35 * math.sin(ciclo(f, 2, k))
        col = tuple(int(v * t) for v in c(color))
        for x in range(x0, x0 + ancho):
            lz.poner(x, y0, col)
            lz.poner(x, y0 + 1, col)
    # autos voladores
    for (y, velocidad, sentido, color) in ((18, 2, 1, "#ffe45c"), (26, 1, -1, "#ff3fb4")):
        x = (f * velocidad * sentido) % N
        for dx in range(5):
            lz.poner((x + dx * sentido) % N, y, c("#06031a"))
            lz.poner((x + dx * sentido) % N, y - 1, c("#2a1c66"))
        lz.poner((x + 5 * sentido) % N, y, c(color))
        for dx in range(1, 4):
            lz.poner((x - dx * sentido) % N, y, tuple(v // (dx + 1) for v in c(color)))
    # lluvia fina que cae en diagonal
    gota = random.Random(5)
    for _ in range(26):
        x0, y0 = gota.randint(0, N - 1), gota.randint(0, N - 1)
        y = (y0 + 4 * f) % N
        x = (x0 + f) % N
        if y < 80:
            lz.poner(x, y, c("#6f86ff"))
            lz.poner((x - 1) % N, y - 1, c("#4a5cc4"))
    piso(lz, c("#19e6ff"), c("#2a2350"), c("#1c1535"), c("#2e2356"), c("#ff3fb4"))
    return lz.imagen()


# ------------------------------------------------------------------------------------ nevado

def pino(lz, cx, base, alto, verde, nieve):
    lz.poner(cx, base, c("#5a3a28"))
    lz.poner(cx, base - 1, c("#5a3a28"))
    for i in range(alto):
        y = base - 2 - i
        ancho = max(0, (alto - i) * 3 // 4 + (1 if i % 3 == 0 else 0))
        for dx in range(-ancho, ancho + 1):
            lz.poner(cx + dx, y, nieve if (i % 3 == 2 and abs(dx) < ancho) else verde)


def nevado(f):
    lz = Lienzo()
    cielo(lz, [c(h) for h in ("#8fb8e0", "#a9cbe9", "#c6ddf1", "#e1eef8", "#f4f9fd")])
    for y in range(8, 21):  # sol palido con halo
        for x in range(66, 82):
            d = (x - 74) ** 2 + (y - 14) ** 2
            if d <= 25:
                lz.poner(x, y, c("#fff6d6"))
            elif d <= 40:
                lz.poner(x, y, c("#e9f0f6"))
    nubes(lz, f, [(8, 16, 1.0), (56, 30, 0.8)], c("#ffffff"), c("#d5e3ef"))
    montanas(lz, 58, 14, 0.9, c("#ffffff"), c("#b7cce2"), 80)
    montanas(lz, 65, 9, 2.4, c("#f4f8fc"), c("#9fb6cf"), 80)
    for x0, base, alto in ((6, 72, 9), (20, 72, 7), (34, 72, 10), (62, 72, 8), (78, 72, 11), (90, 72, 7)):
        pino(lz, x0, base, alto, c("#2f6b4f"), c("#f2f7fb"))
    # nieve que cae con un leve vaiven
    copo = random.Random(3)
    for k in range(46):
        x0, y0, rapido = copo.randint(0, N - 1), copo.randint(0, N - 1), k % 3 == 0
        y = (y0 + (2 if rapido else 1) * f) % N
        x = (x0 + round(2 * math.sin(ciclo(f, 2, k)))) % N
        if y < 86:
            lz.poner(x, y, c("#ffffff"))
            if rapido:
                lz.poner(x + 1, y, c("#e4eef7"))
    piso(lz, c("#ffffff"), c("#e6eff7"), c("#6b5d63"), c("#54484f"), c("#dfeaf3"))
    return lz.imagen()


# ----------------------------------------------------------------------------------- desierto

def cactus(lz, x, base, alto):
    verde, claro = c("#2f6a3c"), c("#4a8f52")
    for y in range(base - alto, base):
        lz.poner(x, y, verde)
        lz.poner(x + 1, y, claro)
    brazo = base - alto // 2
    for dx, dy in ((-3, 0), (-3, -1), (-3, -2), (-2, 0), (-1, 0)):
        lz.poner(x + dx, brazo + dy, verde)
    for dx, dy in ((4, -1), (4, -2), (4, -3), (3, -1), (2, -1)):
        lz.poner(x + dx, brazo + dy, verde)


def desierto(f):
    lz = Lienzo()
    cielo(lz, [c(h) for h in ("#ff8a3c", "#ffa545", "#ffbf5e", "#ffd980", "#ffeeb0")])
    # sol grande con rayos que respiran
    lz.circulo(48, 52, 15, c("#ffb23c"))
    lz.circulo(48, 52, 12, c("#fff1a8"))
    largo = 2 + (f // 12) % 2
    for ang in range(0, 360, 30):
        a = math.radians(ang)
        for d in range(17, 17 + largo):
            lz.poner(48 + round(math.cos(a) * d), 52 + round(math.sin(a) * d), c("#ffd45c"))
    nubes(lz, f, [(14, 18, 0.9), (60, 28, 0.7)], c("#ffe3b8"), c("#f0a868"))
    montanas(lz, 62, 7, 0.4, c("#e9b36c"), c("#d4954e"), 80)
    montanas(lz, 67, 5, 2.2, c("#f0c27c"), c("#dba55c"), 80)
    cactus(lz, 14, 72, 12)
    cactus(lz, 80, 72, 9)
    cactus(lz, 64, 72, 6)
    # planta rodadora que cruza el suelo (1 px por frame, rebotando)
    x = f % N
    y = 69 - round(2 * abs(math.sin(ciclo(f, 8))))
    for dx, dy in ((0, 0), (1, 0), (2, 0), (-1, 1), (0, 1), (1, 1), (2, 1), (3, 1), (0, 2), (1, 2), (2, 2)):
        lz.poner((x + dx) % N, y + dy, c("#8a6a3a") if (dx + dy) % 2 else c("#6a4e2a"))
    piso(lz, c("#f5d38a"), c("#e8bd6c"), c("#b9814a"), c("#a06a38"), c("#d49a5a"))
    return lz.imagen()


# ------------------------------------------------------------------------------------ sakura

def cerezo(lz, cx, base, alto, rng):
    for y in range(base - alto, base):
        for dx in (-1, 0, 1):
            lz.poner(cx + dx, y, c("#6b4630") if dx else c("#4e3322"))
    for dx, dy, r in ((0, -alto - 2, 9), (-8, -alto + 3, 7), (8, -alto + 3, 7), (-3, -alto - 7, 6), (4, -alto - 6, 6)):
        lz.circulo(cx + dx, base + dy, r, c("#ff9fc4"))
    for _ in range(70):
        px, py = cx + rng.randint(-14, 14), base - alto + rng.randint(-14, 6)
        lz.poner(px, py, c("#ffd3e4") if rng.random() < 0.5 else c("#ff7fb0"))


def sakura(f):
    lz = Lienzo()
    rng = random.Random(21)
    cielo(lz, [c(h) for h in ("#9fd3ff", "#bde0ff", "#d7ebff", "#eef5ff", "#ffeef4")])
    nubes(lz, f, [(6, 14, 1.0), (54, 26, 0.8)], c("#ffffff"), c("#ead7e6"))
    montanas(lz, 60, 12, 0.5, c("#d0dcf0"), c("#b2c1e0"), 80)
    montanas(lz, 66, 8, 2.0, c("#9ed0a8"), c("#76b58a"), 80)
    cerezo(lz, 16, 72, 20, rng)
    cerezo(lz, 80, 72, 17, rng)
    petalo = random.Random(9)
    for k in range(34):
        x0, y0, rapido = petalo.randint(0, N - 1), petalo.randint(0, N - 1), k % 2 == 0
        y = (y0 + (2 if rapido else 1) * f) % N
        x = (x0 + round(3 * math.sin(ciclo(f, 2, k)))) % N
        if y < 84:
            lz.poner(x, y, c("#ffb7d0"))
            if k % 4 == 0:
                lz.poner(x + 1, y, c("#ff8fb8"))
    piso(lz, c("#8fd16a"), c("#5fae4a"), c("#8b5e3c"), c("#74492c"), c("#ffc2d6"))
    return lz.imagen()


# Nombre del archivo, momento, generador
EXTRAS = {
    "cyberpunk": ("NOCHE", cyberpunk),
    "nevado": ("DIA", nevado),
    "desierto": ("TARDE", desierto),
    "sakura": ("DIA", sakura),
}
