"""Genera el arte de Plantia en pixel art (96x96) listo para servir desde Spring.

Salida en backend-spring-boot/src/main/resources/arte/ (Spring lo expone en /files/arte/**):
  fondos/<dia|tarde|noche>.png            tira horizontal de 96 frames (9216x96)
  fondos/<cyberpunk|nevado|desierto|sakura>.png  fondos de la tienda, igual; con <nombre>_vista.png (96x96)
  desastres/<tipo>_pico_<n>.png           8 frames con transparencia, para superponer al fondo
  desastres/<tipo>_aviso_<n>.png          4 frames, la misma animacion mas tenue y sin rayo

Todo es ligero a proposito: cada archivo pesa pocos KB y el movimiento es suave.
Requiere Pillow. Uso (desde la raiz del repo): python scripts/arte/generar_arte.py
"""
import os

from PIL import Image

import desastres
import fondos
import fondos_extra

RAIZ = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SALIDA = os.path.join(RAIZ, "backend-spring-boot", "src", "main", "resources", "arte")

TENUE = 0.5  # que tanto se ve el aviso respecto al pico


def tenue(frame):
    """Misma imagen con la transparencia reducida."""
    r, g, b, a = frame.split()
    return Image.merge("RGBA", (r, g, b, a.point(lambda v: int(v * TENUE))))


def main():
    os.makedirs(os.path.join(SALIDA, "fondos"), exist_ok=True)
    os.makedirs(os.path.join(SALIDA, "desastres"), exist_ok=True)

    for nombre, generador in (("dia", fondos.dia), ("tarde", fondos.tarde), ("noche", fondos.noche)):
        fondos.tira(generador).save(os.path.join(SALIDA, "fondos", f"{nombre}.png"), optimize=True)

    # Fondos de la tienda: la tira de 96 cuadros y una vista previa (el primer cuadro) para el catalogo
    for nombre, (_, generador) in fondos_extra.EXTRAS.items():
        fondos.tira(generador).save(os.path.join(SALIDA, "fondos", f"{nombre}.png"), optimize=True)
        generador(0).save(os.path.join(SALIDA, "fondos", f"{nombre}_vista.png"), optimize=True)

    for nombre, generador in desastres.DESASTRES.items():
        for n in range(desastres.FRAMES):
            generador(n).save(os.path.join(SALIDA, "desastres", f"{nombre}_pico_{n + 1}.png"), optimize=True)
        # aviso: un frame de cada dos (4 frames) y mas tenue; el huayco sin rayo
        for i, n in enumerate(range(0, desastres.FRAMES, 2)):
            frame = generador(n, rayo=False) if nombre == "huayco" else generador(n)
            tenue(frame).save(os.path.join(SALIDA, "desastres", f"{nombre}_aviso_{i + 1}.png"), optimize=True)

    total = sum(os.path.getsize(os.path.join(d, f)) for d, _, fs in os.walk(SALIDA) for f in fs)
    print(f"listo: {sum(len(fs) for _, _, fs in os.walk(SALIDA))} archivos, {total / 1024:.0f} KB en {SALIDA}")


if __name__ == "__main__":
    main()
