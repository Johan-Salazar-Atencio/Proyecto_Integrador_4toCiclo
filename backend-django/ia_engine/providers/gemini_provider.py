"""Proveedor en la nube basado en Google Gemini (modelo con vision)."""
import json

from django.conf import settings

from .base import (
    PROMPT_CUIDADOS,
    PROMPT_IDENTIFICAR,
    IaProvider,
    normalizar_cuidados,
    normalizar_identificacion,
)


class GeminiProvider(IaProvider):
    name = "gemini"

    def __init__(self):
        import google.generativeai as genai

        api_key = settings.GEMINI_API_KEY
        if not api_key:
            raise RuntimeError("GEMINI_API_KEY no esta configurada")
        genai.configure(api_key=api_key)
        self._genai = genai
        self.model_name = settings.GEMINI_MODEL

    def _generar(self, partes):
        modelo = self._genai.GenerativeModel(
            self.model_name,
            generation_config={"response_mime_type": "application/json"},
        )
        respuesta = modelo.generate_content(partes)
        return respuesta.text

    def identificar(self, image_bytes: bytes, filename: str = "scan.jpg") -> dict:
        mime = "image/png" if filename.lower().endswith(".png") else "image/jpeg"
        partes = [PROMPT_IDENTIFICAR, {"mime_type": mime, "data": image_bytes}]
        contenido = self._generar(partes)
        return normalizar_identificacion(json.loads(contenido), contenido)

    def cuidados_de(self, nombre_comun: str, nombre_cientifico: str = "") -> dict:
        prompt = PROMPT_CUIDADOS.format(nombre=nombre_comun, cientifico=nombre_cientifico)
        contenido = self._generar([prompt])
        return normalizar_cuidados(json.loads(contenido), contenido)
