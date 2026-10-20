"""Proveedor en la nube basado en Google Gemini (modelo con vision).

Usa el SDK vigente `google-genai` (paquete `google-genai`, import `from
google import genai`). El SDK antiguo `google-generativeai` y el modelo
`gemini-1.5-flash` quedaron retirados, por eso se migro a este.
"""
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


# El tier gratuito devuelve 503/429 de forma intermitente por alta demanda;
# reintentamos con backoff exponencial antes de rendirnos.
_INTENTOS = 4
_REINTENTABLES = {429, 500, 502, 503, 504}


class GeminiProvider(IaProvider):
    name = "gemini"

    def __init__(self):
        api_key = settings.GEMINI_API_KEY
        if not api_key:
            raise RuntimeError("GEMINI_API_KEY no esta configurada")
        self.client = genai.Client(api_key=api_key)
        self.model_name = settings.GEMINI_MODEL

    def _generar(self, partes):
        ultimo_error = None
        for intento in range(_INTENTOS):
            try:
                respuesta = self.client.models.generate_content(
                    model=self.model_name,
                    contents=partes,
                    config=types.GenerateContentConfig(
                        response_mime_type="application/json"
                    ),
                )
                return respuesta.text
            except errors.APIError as exc:
                ultimo_error = exc
                if exc.code in _REINTENTABLES and intento < _INTENTOS - 1:
                    time.sleep(1.5 * (intento + 1))
                    continue
                raise
        raise ultimo_error

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
