"""Proveedor en la nube basado en Google Gemini (modelo con vision).

Usa el SDK vigente `google-genai` (paquete `google-genai`, import `from
google import genai`). El SDK antiguo `google-generativeai` y el modelo
`gemini-1.5-flash` quedaron retirados, por eso se migro a este.
"""
import logging
import time

from django.conf import settings
from google import genai
from google.genai import errors, types

from .base import (
    PROMPT_IDENTIFICAR,
    IaProvider,
    normalizar_cuidados,
    normalizar_identificacion,
    parse_json,
    prompt_cuidados,
)

logger = logging.getLogger(__name__)


# El tier gratuito devuelve 503/429 cuando un modelo esta saturado, y Google
# retira modelos (404). En esos casos se prueba el siguiente modelo de la lista
# en vez de rendirse: asi un modelo saturado no deja la app sin IA.
_SALTAR_MODELO = {404, 429, 500, 502, 503, 504}
_TIMEOUT_MS = 20000  # por llamada; Spring espera hasta 60 s en total
_PRESUPUESTO_S = 45  # tiempo maximo probando modelos, por debajo del timeout de Spring
_RESPALDOS_DEFECTO = "gemini-flash-lite-latest,gemini-3.5-flash,gemini-3.5-flash-lite"


def _modelos() -> list[str]:
    """Modelo principal + respaldos (GEMINI_FALLBACK_MODELS), sin duplicados."""
    respaldos = getattr(settings, "GEMINI_FALLBACK_MODELS", None) or _RESPALDOS_DEFECTO
    lista = [settings.GEMINI_MODEL] + [m.strip() for m in respaldos.split(",")]
    return list(dict.fromkeys(m for m in lista if m))


class GeminiProvider(IaProvider):
    name = "gemini"

    def __init__(self):
        api_key = settings.GEMINI_API_KEY
        if not api_key:
            raise RuntimeError("GEMINI_API_KEY no esta configurada")
        self.client = genai.Client(
            api_key=api_key,
            http_options=types.HttpOptions(timeout=_TIMEOUT_MS),
        )
        self.modelos = _modelos()

    def _generar(self, partes):
        inicio = time.monotonic()
        ultimo_error = None
        for modelo in self.modelos:
            if time.monotonic() - inicio > _PRESUPUESTO_S:
                break
            try:
                respuesta = self.client.models.generate_content(
                    model=modelo,
                    contents=partes,
                    config=types.GenerateContentConfig(
                        response_mime_type="application/json"
                    ),
                )
                self.modelo_usado = modelo
                return respuesta.text
            except errors.APIError as exc:
                ultimo_error = exc
                if exc.code in _SALTAR_MODELO:
                    logger.warning("Gemini '%s' no disponible (%s); probando el siguiente", modelo, exc.code)
                    continue
                raise
            except Exception as exc:  # noqa: BLE001 - timeout de red: probar otro modelo
                ultimo_error = exc
                logger.warning("Gemini '%s' fallo (%s); probando el siguiente", modelo, exc)
        raise ultimo_error or RuntimeError("Gemini: sin modelos configurados")

    def identificar(self, image_bytes: bytes, filename: str = "scan.jpg") -> dict:
        mime = "image/png" if filename.lower().endswith(".png") else "image/jpeg"
        partes = [
            PROMPT_IDENTIFICAR,
            types.Part.from_bytes(data=image_bytes, mime_type=mime),
        ]
        contenido = self._generar(partes)
        return normalizar_identificacion(parse_json(contenido), contenido)

    def cuidados_de(self, nombre_comun: str, nombre_cientifico: str = "") -> dict:
        prompt = prompt_cuidados(nombre_comun, nombre_cientifico)
        contenido = self._generar([prompt])
        return normalizar_cuidados(parse_json(contenido), contenido)
