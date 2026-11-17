"""Temas de fondo para la tienda: cada tema tiene tres escenas (DIA, TARDE y NOCHE) de 96x96 y 96 frames.

El jardin cambia de escena con la hora (6-12 dia, 12-18 tarde, 18-6 noche), igual que el fondo
predeterminado. Todo lo que se mueve da un numero entero de vueltas en 96 frames, asi el ciclo cierra
sin saltos, y nada va a mas de 2 px por frame. Reutilizan las herramientas de fondos.py.

  cyberpunk  ciudad con neones, autos voladores y lluvia por la noche
  nevado     campo con nieve cayendo y pinos
  desierto   dunas, cactus y una planta rodadora
  sakura     cerezos con petalos al viento
"""
import math
import random

import fondos
from fondos import FRAMES, N, Lienzo, cielo, hex_rgb as c, montanas, nubes, sol

MOMENTOS = ("DIA", "TARDE", "NOCHE")


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


def cols(*hexes):
    return [c(h) for h in hexes]


def estrellas(lz, f, cantidad=30, semilla=7, alto=46):
    est = random.Random(semilla)
    for k in range(cantidad):
        x, y, fase = est.randint(1, N - 2), est.randint(1, alto), est.randint(0, 23)
        brillo = (f + fase) % 24 < 4  # cada estrella brilla 4 de cada 24 frames
        lz.poner(x, y, c("#ffffff") if brillo else c("#7f8fd6"))
        if brillo and k % 4 == 0:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                lz.poner(x + dx, y + dy, c("#aab6f0"))


def luna(lz, cx=72, cy=17):
    lz.circulo(cx, cy, 9, c("#2b3b86"))
    lz.circulo(cx, cy, 7, c("#f7f2c6"))
    for dx, dy in ((-2, -2), (2, 1), (-1, 3), (3, -3)):
        lz.poner(cx + dx, cy + dy, c("#d8d09a"))


# --------------------------------------------------------------------------------- cyberpunk

def edificio(lz, x0, ancho, y_top, cuerpo, borde, prob, f, rng, colores, base=72):
    for x in range(x0, x0 + ancho):
        for y in range(y_top, base):
            lz.poner(x, y, cuerpo)
        lz.poner(x, y_top, borde)
    for y in range(y_top, base):
        lz.poner(x0, y, borde)
    # ventanas: algunas se apagan y se encienden despacio
    celdas = [(vx, vy) for vy in range(y_top + 3, base - 2, 4) for vx in range(x0 + 2, x0 + ancho - 1, 3)]
    for k, (vx, vy) in enumerate(celdas):
        if rng.random() < prob and (f // 12 + k) % 4 != 0:
            lz.poner(vx, vy, colores[k % len(colores)])


CIBER = {
    "NOCHE": dict(
        cielo=("#0a0420", "#180a3a", "#2e0f5c", "#5d1a78", "#a02a86", "#d9488f"),
        lejos=("#1b0f45", "#3a2580"), cerca=("#0c0628",), neon=("#19e6ff", "#ff3fb4"), prob=(0.35, 0.6),
        ventanas=("#19e6ff", "#ffe45c", "#ff3fb4"), brillo=1.0, lluvia=True, astro="luna"),
    "TARDE": dict(
        cielo=("#2a0f4a", "#5a1a6e", "#a02a7a", "#e0507a", "#ff8a5c", "#ffc27a"),
        lejos=("#3a1a5a", "#7a3a9a"), cerca=("#1a0c38",), neon=("#19e6ff", "#ff3fb4"), prob=(0.3, 0.5),
        ventanas=("#ffd27a", "#ff7be0", "#19e6ff"), brillo=0.85, lluvia=False, astro="sol_bajo"),
    "DIA": dict(
        cielo=("#6b7fa8", "#8a9ac0", "#a9b4d0", "#c4c9dc", "#dcd6e0", "#e6dde0"),
        lejos=("#4a4e82", "#6a70a8"), cerca=("#2a2d5c",), neon=("#2ab0c8", "#c8348e"), prob=(0.2, 0.15),
        ventanas=("#b8c0ff", "#ffe9a8"), brillo=0.5, lluvia=False, astro="sol_palido"),
}


def cyberpunk(f, momento):
    p = CIBER[momento]
    lz = Lienzo()
    rng = random.Random(11)  # misma ciudad en las tres escenas
    cielo(lz, cols(*p["cielo"]))
    if p["astro"] == "luna":
        # luna neon con franjas, al estilo retrowave
        for y in range(20, 46):
            for x in range(54, 90):
                if (x - 72) ** 2 + (y - 33) ** 2 <= 13 * 13 and not (y > 33 and (y - 33) % 4 == 3):
                    lz.poner(x, y, c("#ff5fa8") if y < 33 else c("#ff8a5c"))
    elif p["astro"] == "sol_bajo":
        for y in range(36, 70):
            for x in range(34, 70):
                if (x - 52) ** 2 + (y - 56) ** 2 <= 15 * 15 and not (y > 56 and (y - 56) % 4 == 3):
                    lz.poner(x, y, c("#ffe29a") if y < 56 else c("#ffb25c"))
    else:
        lz.circulo(72, 24, 9, c("#d8d0d4"))
        lz.circulo(72, 24, 7, c("#f2ece0"))
    xs = 0
    while xs < N:
        ancho = rng.randint(8, 14)
        edificio(lz, xs, ancho, rng.randint(44, 58), c(p["lejos"][0]), c(p["lejos"][1]), p["prob"][0] * 0.6, f, rng,
                 cols("#7b6cff", "#ff7be0"))
        xs += ancho
    xs = -4
    while xs < N:
        ancho = rng.randint(10, 17)
        borde = c(p["neon"][0]) if rng.random() < 0.5 else c(p["neon"][1])
        edificio(lz, xs, ancho, rng.randint(26, 50), c(p["cerca"][0]), borde, p["prob"][1], f, rng, cols(*p["ventanas"]))
        xs += ancho + rng.randint(0, 2)
    # letreros de neon que laten
    for k, (x0, y0, ancho, color) in enumerate(((10, 36, 7, "#19e6ff"), (38, 30, 8, "#ff3fb4"), (66, 40, 6, "#b6ff3f"), (84, 28, 7, "#ffe45c"))):
        t = (0.65 + 0.35 * math.sin(ciclo(f, 2, k))) * p["brillo"]
        col = tuple(int(v * t) for v in c(color))
        for x in range(x0, x0 + ancho):
            lz.poner(x, y0, col)
            lz.poner(x, y0 + 1, col)
    # autos voladores (1 px por frame)
    for (y, sentido, color) in ((18, 1, "#ffe45c"), (26, -1, "#ff3fb4")):
        x = (f * sentido) % N
        for dx in range(5):
            lz.poner((x + dx * sentido) % N, y, c("#06031a"))
            lz.poner((x + dx * sentido) % N, y - 1, c("#2a1c66"))
        lz.poner((x + 5 * sentido) % N, y, c(color))
        for dx in range(1, 4):
            lz.poner((x - dx * sentido) % N, y, tuple(v // (dx + 1) for v in c(color)))
    if p["lluvia"]:  # llovizna fina que cae en diagonal (2 px por frame)
        gota = random.Random(5)
        for _ in range(26):
            x0, y0 = gota.randint(0, N - 1), gota.randint(0, N - 1)
            y, x = (y0 + 2 * f) % N, (x0 + f) % N
            if y < 80:
                lz.poner(x, y, c("#6f86ff"))
                lz.poner((x - 1) % N, y - 1, c("#4a5cc4"))
    claro = momento == "DIA"
    piso(lz, c("#19e6ff" if not claro else "#2ab0c8"), c("#2a2350" if not claro else "#3a3a6a"),
         c("#1c1535" if not claro else "#2c2850"), c("#2e2356" if not claro else "#3c386a"), c("#ff3fb4" if not claro else "#c8348e"))
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


NEVADO = {
    "DIA": dict(cielo=("#8fb8e0", "#a9cbe9", "#c6ddf1", "#e1eef8", "#f4f9fd"), nube=("#ffffff", "#d5e3ef"),
                m1=("#ffffff", "#b7cce2"), m2=("#f4f8fc", "#9fb6cf"), pino=("#2f6b4f", "#f2f7fb"),
                copo="#ffffff", suelo=("#ffffff", "#e6eff7", "#6b5d63", "#54484f", "#dfeaf3")),
    "TARDE": dict(cielo=("#6e5a9a", "#c0709a", "#f0907a", "#ffb27a", "#ffd8a0"), nube=("#ffc4b0", "#c87090"),
                  m1=("#ffe2e0", "#c49ab4"), m2=("#f4d4dc", "#a888a8"), pino=("#2a5a48", "#f8dcd8"),
                  copo="#fff0e8", suelo=("#ffe9e4", "#f0d0d8", "#6a5460", "#53424e", "#f0d4d8")),
    "NOCHE": dict(cielo=("#070a24", "#0d1440", "#16215c", "#22337a", "#33489a"), nube=("#34478f", "#1c2a66"),
                  m1=("#c8d8f4", "#566ca8"), m2=("#9fb2e0", "#3e5290"), pino=("#1d4a46", "#b8cce8"),
                  copo="#dfeaff", suelo=("#b8c8e8", "#9fb0d4", "#3a3050", "#2c2440", "#8fa2d0")),
}


def nevado(f, momento):
    p = NEVADO[momento]
    lz = Lienzo()
    cielo(lz, cols(*p["cielo"]))
    if momento == "DIA":
        for y in range(8, 21):  # sol palido con halo
            for x in range(66, 82):
                d = (x - 74) ** 2 + (y - 14) ** 2
                if d <= 25:
                    lz.poner(x, y, c("#fff6d6"))
                elif d <= 40:
                    lz.poner(x, y, c("#e9f0f6"))
    elif momento == "TARDE":
        sol(lz, 50, 56, 9, c("#fff0a8"), c("#ff9a5a"), f)
    else:
        estrellas(lz, f)
        luna(lz)
    nubes(lz, f, [(8, 16, 1.0), (56, 30, 0.8)] if momento != "NOCHE" else [(8, 30, 0.9)], c(p["nube"][0]), c(p["nube"][1]))
    montanas(lz, 58, 14, 0.9, c(p["m1"][0]), c(p["m1"][1]), 80)
    montanas(lz, 65, 9, 2.4, c(p["m2"][0]), c(p["m2"][1]), 80)
    for x0, base, alto in ((6, 72, 9), (20, 72, 7), (34, 72, 10), (62, 72, 8), (78, 72, 11), (90, 72, 7)):
        pino(lz, x0, base, alto, c(p["pino"][0]), c(p["pino"][1]))
    # nieve que cae con un leve vaiven (1 px por frame)
    copo = random.Random(3)
    for k in range(46):
        x0, y0 = copo.randint(0, N - 1), copo.randint(0, N - 1)
        y = (y0 + f) % N
        x = (x0 + round(2 * math.sin(ciclo(f, 2, k)))) % N
        if y < 86:
            lz.poner(x, y, c(p["copo"]))
            if k % 3 == 0:
                lz.poner(x + 1, y, c(p["copo"]))
    piso(lz, *cols(*p["suelo"]))
    return lz.imagen()


# ----------------------------------------------------------------------------------- desierto

def cactus(lz, x, base, alto, verde, claro):
    for y in range(base - alto, base):
        lz.poner(x, y, verde)
        lz.poner(x + 1, y, claro)
    brazo = base - alto // 2
    for dx, dy in ((-3, 0), (-3, -1), (-3, -2), (-2, 0), (-1, 0)):
        lz.poner(x + dx, brazo + dy, verde)
    for dx, dy in ((4, -1), (4, -2), (4, -3), (3, -1), (2, -1)):
        lz.poner(x + dx, brazo + dy, verde)


DESIERTO = {
    "DIA": dict(cielo=("#3f9fe0", "#68b8ee", "#93d0f5", "#c0e6fa", "#f0f6fa"), nube=("#ffffff", "#d8e8f4"),
                d1=("#f2d28a", "#e3b867"), d2=("#f6dc9a", "#e8c070"), cactus=("#2f6a3c", "#4a8f52"),
                suelo=("#f8e0a0", "#f0cc7c", "#c8924e", "#b07c3c", "#e0a864"), rodadora=("#8a6a3a", "#6a4e2a")),
    "TARDE": dict(cielo=("#ff8a3c", "#ffa545", "#ffbf5e", "#ffd980", "#ffeeb0"), nube=("#ffe3b8", "#f0a868"),
                  d1=("#e9b36c", "#d4954e"), d2=("#f0c27c", "#dba55c"), cactus=("#2f6a3c", "#4a8f52"),
                  suelo=("#f5d38a", "#e8bd6c", "#b9814a", "#a06a38", "#d49a5a"), rodadora=("#8a6a3a", "#6a4e2a")),
    "NOCHE": dict(cielo=("#050818", "#0b1238", "#142058", "#1e2f7a", "#2c428f"), nube=("#34478f", "#1c2a66"),
                  d1=("#5a6aa0", "#46538a"), d2=("#6a78ae", "#55639a"), cactus=("#1c4a50", "#2a6a6a"),
                  suelo=("#4a5278", "#3c4468", "#2a2c48", "#202238", "#5a6490"), rodadora=("#6a6a80", "#4e4e66")),
}


def desierto(f, momento):
    p = DESIERTO[momento]
    lz = Lienzo()
    cielo(lz, cols(*p["cielo"]))
    if momento == "TARDE":
        lz.circulo(48, 52, 15, c("#ffb23c"))
        lz.circulo(48, 52, 12, c("#fff1a8"))
        largo = 2 + (f // 12) % 2
        for ang in range(0, 360, 30):
            a = math.radians(ang)
            for d in range(17, 17 + largo):
                lz.poner(48 + round(math.cos(a) * d), 52 + round(math.sin(a) * d), c("#ffd45c"))
    elif momento == "DIA":
        sol(lz, 70, 20, 8, c("#fffbe0"), c("#ffe066"), f)
    else:
        estrellas(lz, f)
        luna(lz, 22, 20)
    nubes(lz, f, [(14, 18, 0.9), (60, 28, 0.7)] if momento != "NOCHE" else [(60, 30, 0.7)], c(p["nube"][0]), c(p["nube"][1]))
    montanas(lz, 62, 7, 0.4, c(p["d1"][0]), c(p["d1"][1]), 80)
    montanas(lz, 67, 5, 2.2, c(p["d2"][0]), c(p["d2"][1]), 80)
    for x, alto in ((14, 12), (80, 9), (64, 6)):
        cactus(lz, x, 72, alto, c(p["cactus"][0]), c(p["cactus"][1]))
    # planta rodadora que cruza el suelo (1 px por frame, rebotando)
    x = f % N
    y = 69 - round(2 * abs(math.sin(ciclo(f, 8))))
    for dx, dy in ((0, 0), (1, 0), (2, 0), (-1, 1), (0, 1), (1, 1), (2, 1), (3, 1), (0, 2), (1, 2), (2, 2)):
        lz.poner((x + dx) % N, y + dy, c(p["rodadora"][0]) if (dx + dy) % 2 else c(p["rodadora"][1]))
    piso(lz, *cols(*p["suelo"]))
    return lz.imagen()


# ------------------------------------------------------------------------------------ sakura

def cerezo(lz, cx, base, alto, rng, copa, hojas):
    for y in range(base - alto, base):
        for dx in (-1, 0, 1):
            lz.poner(cx + dx, y, c("#6b4630") if dx else c("#4e3322"))
    for dx, dy, r in ((0, -alto - 2, 9), (-8, -alto + 3, 7), (8, -alto + 3, 7), (-3, -alto - 7, 6), (4, -alto - 6, 6)):
        lz.circulo(cx + dx, base + dy, r, c(copa))
    for _ in range(70):
        px, py = cx + rng.randint(-14, 14), base - alto + rng.randint(-14, 6)
        lz.poner(px, py, c(hojas[0]) if rng.random() < 0.5 else c(hojas[1]))


SAKURA = {
    "DIA": dict(cielo=("#9fd3ff", "#bde0ff", "#d7ebff", "#eef5ff", "#ffeef4"), nube=("#ffffff", "#ead7e6"),
                m1=("#d0dcf0", "#b2c1e0"), m2=("#9ed0a8", "#76b58a"), copa="#ff9fc4", hojas=("#ffd3e4", "#ff7fb0"),
                petalo=("#ffb7d0", "#ff8fb8"), suelo=("#8fd16a", "#5fae4a", "#8b5e3c", "#74492c", "#ffc2d6")),
    "TARDE": dict(cielo=("#8a6aa8", "#d27aa0", "#f4a08a", "#ffc89a", "#ffe6b8"), nube=("#ffd0c0", "#d890a0"),
                  m1=("#c8a0b8", "#a07898"), m2=("#a8a070", "#80884e"), copa="#ffa8a0", hojas=("#ffd0b8", "#ff8a8a"),
                  petalo=("#ffc0a8", "#ff9a90"), suelo=("#a8b05a", "#7e9440", "#7a5238", "#64402c", "#ffc8a8")),
    "NOCHE": dict(cielo=("#070a24", "#0d1440", "#16215c", "#22337a", "#33489a"), nube=("#34478f", "#1c2a66"),
                  m1=("#2c3a78", "#1c2860"), m2=("#244a50", "#183a3c"), copa="#b06a9a", hojas=("#d890c0", "#8a4a7e"),
                  petalo=("#ffd0f0", "#e0a0d8"), suelo=("#2f6a4c", "#215540", "#2e2430", "#221a26", "#d890c0")),
}


def sakura(f, momento):
    p = SAKURA[momento]
    lz = Lienzo()
    rng = random.Random(21)
    cielo(lz, cols(*p["cielo"]))
    if momento == "NOCHE":
        estrellas(lz, f)
        luna(lz)
    elif momento == "TARDE":
        sol(lz, 48, 56, 9, c("#fff0b0"), c("#ff9a6a"), f)
    nubes(lz, f, [(6, 14, 1.0), (54, 26, 0.8)] if momento != "NOCHE" else [(54, 30, 0.8)], c(p["nube"][0]), c(p["nube"][1]))
    montanas(lz, 60, 12, 0.5, c(p["m1"][0]), c(p["m1"][1]), 80)
    montanas(lz, 66, 8, 2.0, c(p["m2"][0]), c(p["m2"][1]), 80)
    cerezo(lz, 16, 72, 20, rng, p["copa"], p["hojas"])
    cerezo(lz, 80, 72, 17, rng, p["copa"], p["hojas"])
    petalo = random.Random(9)
    for k in range(34):
        x0, y0 = petalo.randint(0, N - 1), petalo.randint(0, N - 1)
        y = (y0 + f) % N  # 1 px por frame (8 px/s): petalos que flotan
        x = (x0 + round(3 * math.sin(ciclo(f, 2, k)))) % N
        if y < 84:
            lz.poner(x, y, c(p["petalo"][0]))
            if k % 4 == 0:
                lz.poner(x + 1, y, c(p["petalo"][1]))
    piso(lz, *cols(*p["suelo"]))
    if momento == "NOCHE":  # luciernagas entre los arboles
        for k, (x0, y0) in enumerate(((20, 80), (46, 84), (70, 78), (88, 86))):
            ang = ciclo(f, 2, k * 1.3)
            x, y = x0 + round(4 * math.sin(ang)), y0 + round(3 * math.cos(ang))
            if (f // 6 + k) % 4 != 0:
                lz.poner(x, y, c("#e4ff6a"))
    return lz.imagen()


# Tema -> (nombre visible, descripcion, generador(frame, momento), momento de la vista previa)
TEMAS = {
    "cyberpunk": ("Ciudad cyberpunk", "Neones, lluvia y autos voladores. Cambia con el dia, la tarde y la noche.", cyberpunk, "NOCHE"),
    "nevado": ("Campo nevado", "Nieve cayendo sobre pinos, de dia, de tarde y de noche.", nevado, "DIA"),
    "desierto": ("Desierto dorado", "Dunas, cactus y una planta rodadora a todas horas.", desierto, "TARDE"),
    "sakura": ("Cerezos en flor", "Petalos de cerezo al viento, del dia a la noche.", sakura, "DIA"),
}
