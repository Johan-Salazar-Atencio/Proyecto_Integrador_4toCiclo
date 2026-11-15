"""Fondos en pixel art (96x96, 8 frames) para dia, tarde y noche.

Todo se dibuja por codigo con paletas cortas. Salida: una tira PNG por fondo
(8 frames de 96x96 en horizontal) y un manifiesto.json.

Uso: python generar_fondos.py
"""
import json
import math
import os
import random

from PIL import Image

N = 96            # lado de la escena
FRAMES = 8        # frames por animacion
MS = 250          # milisegundos por frame
CIELO_ALTO = 66   # hasta aqui llega el cielo
SALIDA = os.path.dirname(os.path.abspath(__file__))

BAYER = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]


def hex_rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


class Lienzo:
    def __init__(self):
        self.px = [[(0, 0, 0)] * N for _ in range(N)]

    def poner(self, x, y, c):
        if 0 <= x < N and 0 <= y < N:
            self.px[y][x] = c

    def circulo(self, cx, cy, r, c):
        for y in range(cy - r, cy + r + 1):
            for x in range(cx - r, cx + r + 1):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r + r * 0.6:
                    self.poner(x, y, c)

    def imagen(self):
        im = Image.new("RGB", (N, N))
        im.putdata([p for fila in self.px for p in fila])
        return im


def cielo(lz, colores):
    """Bandas de color con un degradado tramado (dither) solo al final de cada banda."""
    n = len(colores)
    for y in range(CIELO_ALTO):
        t = y / CIELO_ALTO * (n - 1)
        i = min(int(t), n - 2)
        frac = min(1.0, max(0.0, (t - i - 0.55) / 0.45))
        for x in range(N):
            umbral = (BAYER[y % 4][x % 4] + 0.5) / 16
            lz.poner(x, y, colores[i + 1] if frac > umbral else colores[i])


def cresta(x, base, amp, fase):
    return int(base - amp * (math.sin(x * 0.07 + fase) + 0.55 * math.sin(x * 0.19 + fase * 2.1) + 1.4) / 2.9)


def montanas(lz, base, amp, fase, claro, oscuro, hasta):
    """Silueta de cerros: una linea de cresta con una franja de luz arriba."""
    for x in range(N):
        top = cresta(x, base, amp, fase)
        for y in range(top, hasta):
            lz.poner(x, y, claro if y - top < 2 else oscuro)


def nube(lz, cx, cy, escala, luz, sombra):
    partes = [(0, 0, 5), (-6, 2, 4), (6, 2, 4), (-11, 4, 3), (11, 4, 3)]
    celdas = set()
    for dx, dy, r in partes:
        r = max(2, int(r * escala))
        for y in range(-r, r + 1):
            for x in range(-r, r + 1):
                if x * x + y * y <= r * r:
                    celdas.add((cx + int(dx * escala) + x, cy + int(dy * escala) + y))
    for (x, y) in celdas:
        inferior = (x, y + 1) not in celdas or (x, y + 2) not in celdas
        lz.poner(x, y, sombra if inferior else luz)


def nubes(lz, f, lista, luz, sombra):
    """Las nubes se desplazan 12 px por frame: 8 frames = 96 px, el ciclo cierra sin saltos."""
    for (x0, y, escala) in lista:
        base = (x0 + f * 12) % N
        for desp in (-N, 0, N):
            nube(lz, base + desp, y, escala, luz, sombra)


def suelo(lz, f, c_pasto, c_pasto2, c_tierra, c_tierra2, rng):
    for y in range(72, N):
        for x in range(N):
            if y < 75:
                lz.poner(x, y, c_pasto if y == 72 else c_pasto2)
            else:
                lz.poner(x, y, c_tierra2 if (x * 7 + y * 13) % 11 == 0 else c_tierra)
    # matas de pasto que se mecen con el viento
    for k in range(18):
        x = 3 + k * 5 + rng.randint(-1, 1)
        balanceo = round(math.sin(2 * math.pi * f / FRAMES + k))
        lz.poner(x, 71, c_pasto)
        lz.poner(x + balanceo, 70, c_pasto)


def sol(lz, cx, cy, r, color, borde, f):
    lz.circulo(cx, cy, r + 1, borde)
    lz.circulo(cx, cy, r, color)
    largo = 3 if f % 2 == 0 else 4
    for ang in range(0, 360, 45):
        a = math.radians(ang)
        for d in range(r + 3, r + 3 + largo):
            lz.poner(cx + round(math.cos(a) * d), cy + round(math.sin(a) * d), color)


def pajaros(lz, f, color, y0):
    for k, x0 in enumerate((6, 54)):
        x = (x0 + f * 12) % N
        y = y0 + k * 5 + (f % 2)
        alas = [(-2, 0), (-1, 1), (0, 1), (1, 1), (2, 0)] if f % 2 == 0 else [(-2, 1), (-1, 0), (0, 1), (1, 0), (2, 1)]
        for dx, dy in alas:
            lz.poner(x + dx, y + dy, color)


def dia(f):
    lz = Lienzo()
    rng = random.Random(1)
    cielo(lz, [hex_rgb(c) for c in ("#4aa8f0", "#6cc0f7", "#92d4fa", "#b9e6fc", "#dff5fd")])
    sol(lz, 74, 16, 7, hex_rgb("#ffe55c"), hex_rgb("#ffb938"), f)
    nubes(lz, f, [(6, 14, 1.0), (50, 30, 0.8)], hex_rgb("#ffffff"), hex_rgb("#cfe3f5"))
    montanas(lz, 60, 12, 0.6, hex_rgb("#a9c8d8"), hex_rgb("#86aabe"), 80)
    montanas(lz, 66, 8, 2.0, hex_rgb("#6fbf73"), hex_rgb("#4f9d5b"), 80)
    pajaros(lz, f, hex_rgb("#34495e"), 24)
    suelo(lz, f, hex_rgb("#8fd16a"), hex_rgb("#5fae4a"), hex_rgb("#8b5e3c"), hex_rgb("#74492c"), rng)
    return lz.imagen()


def tarde(f):
    lz = Lienzo()
    rng = random.Random(2)
    cielo(lz, [hex_rgb(c) for c in ("#5b4b8a", "#9a5fa0", "#d9718f", "#f59a7a", "#ffc27a", "#ffe0a0")])
    sol(lz, 48, 58, 11, hex_rgb("#fff0a8"), hex_rgb("#ffb45a"), f)
    nubes(lz, f, [(10, 18, 1.0), (58, 32, 0.9)], hex_rgb("#ffb3a0"), hex_rgb("#c8607e"))
    montanas(lz, 62, 12, 0.6, hex_rgb("#b0709a"), hex_rgb("#7a4a82"), 80)
    montanas(lz, 67, 8, 2.0, hex_rgb("#8d9a4a"), hex_rgb("#66763a"), 80)
    pajaros(lz, f, hex_rgb("#3a2a4a"), 22)
    suelo(lz, f, hex_rgb("#a9b84e"), hex_rgb("#7f9a3c"), hex_rgb("#7a4a30"), hex_rgb("#633a26"), rng)
    return lz.imagen()


def noche(f):
    lz = Lienzo()
    rng = random.Random(3)
    cielo(lz, [hex_rgb(c) for c in ("#070a24", "#0d1440", "#16215c", "#22337a", "#33489a")])
    # estrellas: parpadean con fases distintas
    est = random.Random(7)
    for k in range(30):
        x, y, fase = est.randint(1, N - 2), est.randint(1, 46), est.randint(0, FRAMES - 1)
        brillo = (f + fase) % FRAMES in (0, 1)
        lz.poner(x, y, hex_rgb("#ffffff") if brillo else hex_rgb("#7f8fd6"))
        if brillo and k % 4 == 0:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                lz.poner(x + dx, y + dy, hex_rgb("#aab6f0"))
    # luna
    lz.circulo(72, 17, 9, hex_rgb("#2b3b86"))
    lz.circulo(72, 17, 7, hex_rgb("#f7f2c6"))
    for (dx, dy) in ((-2, -2), (2, 1), (-1, 3), (3, -3)):
        lz.poner(72 + dx, 17 + dy, hex_rgb("#d8d09a"))
    nubes(lz, f, [(8, 30, 0.9)], hex_rgb("#34478f"), hex_rgb("#1c2a66"))
    montanas(lz, 60, 12, 0.6, hex_rgb("#2d3e7e"), hex_rgb("#1d2b5c"), 80)
    montanas(lz, 66, 8, 2.0, hex_rgb("#245a4a"), hex_rgb("#1a4237"), 80)
    suelo(lz, f, hex_rgb("#3a7a52"), hex_rgb("#2a5a3e"), hex_rgb("#3a2a38"), hex_rgb("#2c2030"), rng)
    # luciernagas
    for k, (x0, y0) in enumerate(((14, 76), (40, 82), (66, 78), (84, 84), (28, 88))):
        ang = 2 * math.pi * f / FRAMES + k * 1.3
        x, y = x0 + round(4 * math.sin(ang)), y0 + round(3 * math.cos(ang))
        if (f + k) % 3 != 0:
            lz.poner(x, y, hex_rgb("#e4ff6a"))
            lz.poner(x + 1, y, hex_rgb("#7fa03a"))
    return lz.imagen()


def tira(generador):
    hoja = Image.new("RGB", (N * FRAMES, N))
    for f in range(FRAMES):
        hoja.paste(generador(f), (f * N, 0))
    return hoja


if __name__ == "__main__":
    for nombre, g in (("dia", dia), ("tarde", tarde), ("noche", noche)):
        tira(g).save(os.path.join(SALIDA, f"fondo_{nombre}.png"))
    with open(os.path.join(SALIDA, "manifiesto.json"), "w", encoding="utf-8") as fh:
        json.dump({"lado": N, "frames": FRAMES, "ms_por_frame": MS,
                   "fondos": ["fondo_dia.png", "fondo_tarde.png", "fondo_noche.png"]}, fh, indent=2)
    # montaje para revisar: primer frame de cada fondo, ampliado x4
    montaje = Image.new("RGB", (N * 3 * 4 + 16, N * 4), (30, 30, 30))
    for i, g in enumerate((dia, tarde, noche)):
        montaje.paste(g(0).resize((N * 4, N * 4), Image.NEAREST), (i * (N * 4 + 8), 0))
    montaje.save(os.path.join(SALIDA, "revision.png"))
    print("listo")
