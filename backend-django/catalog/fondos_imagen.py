"""Convierte lo que sube el admin en la tira horizontal de cuadros cuadrados que dibuja el jardin.

Acepta:
  - un GIF, WebP o APNG animado: cada cuadro de la animacion pasa a ser un cuadro de la tira;
  - una tira PNG/JPG (indicando cuantos cuadros tiene): se parte en cuadros iguales y cuadrados;
  - una imagen fija (sin indicar cuadros): queda como un solo cuadro, un fondo sin movimiento.
Los cuadros se recortan al centro para que sean cuadrados (la escena es cuadrada) y se limitan en
tamano y cantidad para que la tira no sea enorme. No procesa videos (haria falta ffmpeg).
"""
import hashlib
import io
import os

from PIL import Image, ImageSequence, UnidentifiedImageError

MAX_BYTES = 15 * 1024 * 1024
LADO_MAX = 192          # lado maximo de un cuadro, en pixeles
MAX_CUADROS = 120
ANCHO_MAX_TIRA = 16384  # ancho maximo de la tira (limite de textura de muchos navegadores y celulares)
LADO_VISTA = 96
MS_DEFECTO = 120
MS_MIN, MS_MAX = 50, 1000


class ErrorImagen(ValueError):
    """La imagen no sirve; el mensaje se le muestra al admin."""


def _a_rgb(img):
    if img.mode in ("RGBA", "LA", "P", "PA"):
        rgba = img.convert("RGBA")
        base = Image.new("RGBA", rgba.size, (0, 0, 0, 255))
        base.alpha_composite(rgba)
        return base.convert("RGB")
    return img.convert("RGB")


def _cuadrado(img):
    ancho, alto = img.size
    lado = min(ancho, alto)
    izq, arr = (ancho - lado) // 2, (alto - lado) // 2
    return img.crop((izq, arr, izq + lado, arr + lado))


def _a_png(img):
    salida = io.BytesIO()
    img.save(salida, format="PNG", optimize=True)
    return salida.getvalue()


def procesar(datos, cuadros=None, ms=None):
    """Devuelve {"tira": bytes PNG, "vista": bytes PNG, "cuadros": n, "lado": px, "ms": int}."""
    if len(datos) > MAX_BYTES:
        raise ErrorImagen("El archivo pesa mas de 15 MB")
    try:
        origen = Image.open(io.BytesIO(datos))
        origen.load()
    except (UnidentifiedImageError, OSError):
        raise ErrorImagen("No se pudo leer la imagen (usa PNG, JPG, GIF, WebP o APNG)") from None

    n = getattr(origen, "n_frames", 1)
    duraciones = []
    if n > 1:
        frames = []
        for cuadro in ImageSequence.Iterator(origen):
            duraciones.append(cuadro.info.get("duration"))
            frames.append(_cuadrado(_a_rgb(cuadro.copy())))
    else:
        imagen = _a_rgb(origen)
        if cuadros and cuadros > 1:
            lado = imagen.width // cuadros
            if imagen.width % cuadros != 0 or lado != imagen.height:
                raise ErrorImagen(
                    f"Con {cuadros} cuadros, la imagen debe medir {cuadros} veces su alto de ancho "
                    f"(cada cuadro cuadrado); mide {imagen.width}x{imagen.height}")
            frames = [imagen.crop((i * lado, 0, (i + 1) * lado, lado)) for i in range(cuadros)]
        else:
            frames = [_cuadrado(imagen)]

    lado = min(frames[0].width, LADO_MAX)
    frames = [f if f.width == lado else f.resize((lado, lado), Image.LANCZOS) for f in frames]
    # Si hay demasiados cuadros o la tira queda muy ancha, se toman cuadros repartidos
    tope = max(1, min(MAX_CUADROS, ANCHO_MAX_TIRA // lado))
    if len(frames) > tope:
        frames = [frames[i * len(frames) // tope] for i in range(tope)]

    tira = Image.new("RGB", (lado * len(frames), lado))
    for i, f in enumerate(frames):
        tira.paste(f, (i * lado, 0))
    vista = frames[0] if lado == LADO_VISTA else frames[0].resize((LADO_VISTA, LADO_VISTA), Image.LANCZOS)

    if ms is None:
        validas = [d for d in duraciones if isinstance(d, (int, float)) and d > 0]
        ms = round(sum(validas) / len(validas)) if validas else MS_DEFECTO
    return {
        "tira": _a_png(tira), "vista": _a_png(vista), "cuadros": len(frames), "lado": lado,
        "ms": max(MS_MIN, min(MS_MAX, int(ms))),
    }


def guardar(directorio, base, resultado):
    """Guarda la tira y su vista en `directorio` y devuelve sus nombres de archivo.

    El nombre lleva una huella del contenido: al reemplazar una imagen cambia la URL y nadie ve la vieja
    guardada en la cache del navegador.
    """
    os.makedirs(directorio, exist_ok=True)
    huella = hashlib.sha1(resultado["tira"]).hexdigest()[:8]
    nombre_tira, nombre_vista = f"{base}_{huella}.png", f"{base}_{huella}_vista.png"
    with open(os.path.join(directorio, nombre_tira), "wb") as fh:
        fh.write(resultado["tira"])
    with open(os.path.join(directorio, nombre_vista), "wb") as fh:
        fh.write(resultado["vista"])
    return nombre_tira, nombre_vista
