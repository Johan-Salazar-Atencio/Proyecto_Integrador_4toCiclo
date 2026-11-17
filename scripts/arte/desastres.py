"""Desastres en pixel art (96x96, 8 frames, con transparencia) para superponer a los fondos.

Son capas RGBA: se dibujan encima del fondo y de la planta. Solo de prueba.
Salida: desastre_<nombre>.png (tira de 8 frames) y se agrega al manifiesto.json.

Uso: python generar_desastres.py
"""
import json
import math
import os
import random

from PIL import Image

N = 96
FRAMES = 8
SALIDA = os.path.dirname(os.path.abspath(__file__))
MS = {"huayco": 150, "helada": 300, "calor": 250, "plaga": 200}


def capa(tinte=None):
    return Image.new("RGBA", (N, N), tinte or (0, 0, 0, 0))


def p(im, x, y, c, a=255):
    if 0 <= x < N and 0 <= y < N:
        im.putpixel((x, y), (c[0], c[1], c[2], a))


def ciclo(x, periodo, f):
    """Onda que se desplaza y cierra el ciclo exacto en FRAMES frames (periodo en px)."""
    return math.sin(2 * math.pi * (x / periodo - f / FRAMES))


def huayco(f, rayo=True):
    im = capa((40, 55, 85, 70))
    # nubes de tormenta arriba (se desplazan 12 px por frame: 2 periodos de 48 px en 8 frames)
    for x in range(N):
        alto = 11 + int(4 * math.sin(2 * math.pi * (x + 12 * f) / 48))
        for y in range(alto):
            p(im, x, y, (58, 66, 92) if y < alto - 2 else (84, 94, 126), 235)
    # lluvia: cada gota baja 12 px por frame y vuelve a entrar por arriba
    rng = random.Random(11)
    for k in range(48):
        x, y0 = rng.randint(0, N - 1), rng.randint(0, N - 1)
        y = (y0 + 12 * f) % N
        claro = k % 3 == 0
        for dy, dx in ((0, 0), (1, -1), (2, -1)):
            p(im, x + dx, (y + dy) % N, (205, 230, 250) if claro else (150, 190, 225), 235)
    # rayo en los frames 2 y 3
    if rayo and f in (2, 3):
        pts = [(60, 12), (56, 22), (61, 26), (53, 40), (58, 44), (50, 60)]
        for (x1, y1), (x2, y2) in zip(pts, pts[1:]):
            for t in range(0, 13):
                p(im, round(x1 + (x2 - x1) * t / 12), round(y1 + (y2 - y1) * t / 12), (255, 250, 200))
        if f == 2:
            for y in range(N):
                for x in range(N):
                    if im.getpixel((x, y))[3] < 100:
                        im.putpixel((x, y), (255, 255, 235, 70))
    # lodo que avanza por el suelo
    for x in range(N):
        tope = 74 + round(2 * math.sin(2 * math.pi * (x / 48 - f / FRAMES)))
        for y in range(tope, N):
            claro = (x + y * 3 + f * 6) % 17 == 0
            p(im, x, y, (126, 90, 58) if claro else (92, 64, 40))
    return im


def helada(f):
    im = capa((150, 200, 255, 55))
    rng = random.Random(5)
    # escarcha en los bordes
    for y in range(N):
        for x in range(N):
            d = min(x, N - 1 - x, y, N - 1 - y)
            if d < 9 and rng.random() < (9 - d) / 11:
                p(im, x, y, (228, 242, 255), 225)
    # copos: caen 12 px por frame con un balanceo
    nev = random.Random(9)
    for k in range(36):
        x0, y0, fase = nev.randint(0, N - 1), nev.randint(0, N - 1), nev.random() * 6.28
        y = (y0 + 12 * f) % N
        x = x0 + round(2 * math.sin(2 * math.pi * f / FRAMES + fase))
        p(im, x, y, (255, 255, 255))
        if k % 4 == 0:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                p(im, x + dx, (y + dy) % N, (205, 228, 255), 230)
    # escarcha sobre el pasto y destellos de hielo
    for x in range(N):
        if (x * 5 + 3) % 4 != 0:
            p(im, x, 71 + (x % 2), (235, 246, 255), 210)
    for k in range(6):
        x, y = 12 + k * 15, 78 + (k * 7) % 14
        if (f + k) % 4 == 0:
            for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
                p(im, x + dx, y + dy, (255, 255, 255), 240)
    return im


def calor(f):
    im = capa((255, 150, 40, 45))
    # resplandor de calor que cae desde arriba y late. No se dibuja otro sol: el fondo ya trae el suyo
    # (o la luna, de noche) y dos astros a la vez se veian mal
    for y in range(0, 44):
        alfa = int((70 + 18 * math.sin(2 * math.pi * f / FRAMES)) * (1 - y / 44))
        for x in range(N):
            p(im, x, y, (255, 120, 30), max(0, alfa))
    # ondas de calor sobre el suelo
    for y in range(48, 72, 3):
        for x in range(N):
            if (x + y) % 2 == 0:
                p(im, x, y + round(1.5 * ciclo(x, 24, f)), (255, 255, 255), 80)
    # brasas que suben
    rng = random.Random(4)
    for k in range(14):
        x0, y0, fase = rng.randint(0, N - 1), rng.randint(0, N - 1), rng.random() * 6.28
        y = (y0 - 12 * f) % N
        x = x0 + round(2 * math.sin(2 * math.pi * f / FRAMES + fase))
        p(im, x, y, (255, 190, 80), 230)
    # grietas en la tierra
    gr = random.Random(2)
    for k in range(6):
        x, y = 8 + k * 16, 76
        for paso in range(8):
            p(im, x, y, (60, 35, 20))
            y += 2
            x += gr.choice((-1, 0, 1))
            p(im, x, y - 1, (60, 35, 20))
    return im


def plaga(f):
    im = capa((120, 140, 40, 40))
    # pulgones caminando por el pasto: 12 px por frame, las patas alternan
    for k in range(9):
        x = (6 + k * 11 + 12 * f) % N
        y = 69 + (k % 3)
        for dx in (0, 1, 2):
            p(im, x + dx, y, (45, 80, 30))
        p(im, x + 1, y - 1, (110, 170, 60))
        p(im, x + 3, y, (190, 40, 40))  # ojo
        pata = f % 2
        p(im, x + pata, y + 1, (30, 40, 20))
        p(im, x + 2 - pata, y + 1, (30, 40, 20))
    # enjambre volando
    rng = random.Random(8)
    for k in range(8):
        x0, y0, fase = rng.randint(0, N - 1), rng.randint(28, 58), rng.random() * 6.28
        x = (x0 + 12 * f) % N
        y = y0 + round(3 * math.sin(2 * math.pi * f / FRAMES + fase))
        p(im, x, y, (30, 30, 30))
        p(im, x + 1, y, (30, 30, 30))
        ala = -1 if f % 2 == 0 else 1
        p(im, x, y + ala, (200, 220, 230), 190)
        p(im, x + 1, y + ala, (200, 220, 230), 190)
    return im


def tira(g):
    hoja = Image.new("RGBA", (N * FRAMES, N), (0, 0, 0, 0))
    for f in range(FRAMES):
        hoja.paste(g(f), (f * N, 0))
    return hoja


DESASTRES = {"huayco": huayco, "helada": helada, "calor": calor, "plaga": plaga}

if __name__ == "__main__":
    for nombre, g in DESASTRES.items():
        tira(g).save(os.path.join(SALIDA, f"desastre_{nombre}.png"))
    ruta = os.path.join(SALIDA, "manifiesto.json")
    man = json.load(open(ruta, encoding="utf-8"))
    man["desastres"] = {n: {"archivo": f"desastre_{n}.png", "ms_por_frame": MS[n]} for n in DESASTRES}
    json.dump(man, open(ruta, "w", encoding="utf-8"), indent=2)
    # revision: cada desastre sobre un fondo, frame 3, ampliado x3
    fondo = Image.open(os.path.join(SALIDA, "fondo_dia.png")).convert("RGBA")
    noche = Image.open(os.path.join(SALIDA, "fondo_noche.png")).convert("RGBA")
    montaje = Image.new("RGB", (N * 3 * 4 + 24, N * 3), (30, 30, 30))
    for i, (nombre, g) in enumerate(DESASTRES.items()):
        base = (noche if nombre == "huayco" else fondo).crop((3 * N, 0, 4 * N, N)).copy()
        base.alpha_composite(g(3))
        montaje.paste(base.convert("RGB").resize((N * 3, N * 3), Image.NEAREST), (i * (N * 3 + 8), 0))
    montaje.save(os.path.join(SALIDA, "revision_desastres.png"))
    print("listo")
