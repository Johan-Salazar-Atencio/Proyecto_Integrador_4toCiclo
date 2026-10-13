"""Proveedor local (Ollama o servidor compatible) para inferencia sin nube."""
import base64
import json

import requests
from django.conf import settings

from .base import (
    PROMPT_CUIDADOS,
    PROMPT_IDENTIFICAR,
    IaProvider,
    normalizar_cuidados,
    normalizar_identificacion,
)


class LocalProvider(IaProvider):
    name = "local"

    def __init__(self):
        self.base_url = settings.LOCAL_IA_URL.rstrip("/")
        self.model = settings.LOCAL_IA_MODEL

    def _generar(self, prompt, imagenes=None):
        payload = {
            "model": self.model,
            "prompt": prompt,
            "stream": False,
            "format": "json",
        }
        if imagenes:
            payload["images"] = imagenes
        resp = requests.post(f"{self.base_url}/api/generate", json=payload, timeout=120)
        resp.raise_for_status()
        texto = resp.json().get("response", "")
        return texto

    def identificar(self, image_bytes: bytes, filename: str = "scan.jpg") -> dict:
        b64 = base64.b64encode(image_bytes).decode("ascii")
        contenido = self._generar(PROMPT_IDENTIFICAR, imagenes=[b64])
        return normalizar_identificacion(json.loads(contenido), contenido)

    def cuidados_de(self, nombre_comun: str, nombre_cientifico: str = "") -> dict:
        prompt = PROMPT_CUIDADOS.format(nombre=nombre_comun, cientifico=nombre_cientifico)
        contenido = self._generar(prompt)
        return normalizar_cuidados(json.loads(contenido), contenido)
