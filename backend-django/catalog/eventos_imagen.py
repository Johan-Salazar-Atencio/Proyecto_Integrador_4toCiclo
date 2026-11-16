"""Prepara la imagen promocional de un tipo de evento (la que se ve en la tarjeta del Inicio).

A diferencia de los fondos (tiras de cuadros pixelados), es una imagen normal: se acepta PNG, JPG o WebP,
se reduce a un ancho maximo para que pese poco en el celular y se guarda como JPG. La app la muestra
recortada a 16:9, asi que conviene que sea apaisada; aqui no se recorta nada, solo se reduce.
"""
import hashlib
import io
import os

from PIL import Image, UnidentifiedImageError

MAX_BYTES = 8 * 1024 * 1024
ANCHO_MAX = 1280
ANCHO_MIN = 400
CALIDAD = 85


class ErrorImagen(ValueError):
    """La imagen no sirve; el mensaje se le muestra al admin."""


def procesar(datos):
    """Devuelve los bytes de un JPG reducido a ANCHO_MAX como maximo."""
    if len(datos) > MAX_BYTES:
        raise ErrorImagen("La imagen pesa mas de 8 MB")
    try:
        img = Image.open(io.BytesIO(datos))
        img.load()
    except (UnidentifiedImageError, OSError):
        raise ErrorImagen("El archivo no es una imagen valida (usa PNG, JPG o WebP)")
    if img.width < ANCHO_MIN:
        raise ErrorImagen(f"La imagen es muy chica: debe medir al menos {ANCHO_MIN} px de ancho")
    if img.mode in ("RGBA", "LA", "P", "PA"):
        rgba = img.convert("RGBA")
        fondo = Image.new("RGBA", rgba.size, (255, 255, 255, 255))
        fondo.alpha_composite(rgba)
        img = fondo.convert("RGB")
    else:
        img = img.convert("RGB")
    if img.width > ANCHO_MAX:
        img = img.resize((ANCHO_MAX, round(img.height * ANCHO_MAX / img.width)), Image.LANCZOS)
    salida = io.BytesIO()
    img.save(salida, "JPEG", quality=CALIDAD, optimize=True)
    return salida.getvalue()


def guardar(directorio, codigo, jpg):
    """Escribe el JPG con una huella en el nombre (asi un cambio no se queda en cache) y devuelve el nombre."""
    os.makedirs(directorio, exist_ok=True)
    nombre = f"{codigo.lower()}_{hashlib.sha1(jpg).hexdigest()[:8]}.jpg"
    with open(os.path.join(directorio, nombre), "wb") as f:
        f.write(jpg)
    return nombre
