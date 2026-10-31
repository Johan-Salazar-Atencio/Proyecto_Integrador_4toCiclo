"""Proveedor institucional: Open WebUI (proxy de vLLM) con API compatible con OpenAI.

Es el motor "local" de la institucion. Se accede por HTTP a
`WEBUI_BASE_URL` (por ejemplo `http://192.168.17.11:3000/api/v1`) con un
Bearer token de Open WebUI.

El modelo debe ser multimodal: la foto se envia como `image_url` con un data
URI en base64.
"""
import base64
import time

from django.conf import settings
from openai import APIStatusError, OpenAI

from .base import (
    PROMPT_IDENTIFICAR,
    IaProvider,
    normalizar_cuidados,
    normalizar_identificacion,
    parse_json,
    prompt_cuidados,
)

# El backend vLLM puede devolver 429/503 si esta saturado; reintentamos.
_INTENTOS = 3
_REINTENTABLES = {429, 500, 502, 503, 504}


class WebuiProvider(IaProvider):
    name = "webui"

    def __init__(self):
        api_key = settings.WEBUI_API_KEY
        if not api_key:
            raise RuntimeError("WEBUI_API_KEY no esta configurada")
        base_url = (settings.WEBUI_BASE_URL or "").rstrip("/")
        if not base_url:
            raise RuntimeError("WEBUI_BASE_URL no esta configurada")
        self.client = OpenAI(
            api_key=api_key,
            base_url=base_url,
            # El servidor institucional es local: si no responde en 25 s es que
            # esta caido, y hay que pasar deprisa al siguiente motor (Gemini).
            timeout=25.0,
            max_retries=0,
        )
        self.model = settings.WEBUI_MODEL

    def _chat(self, mensajes):
        ultimo_error = None
        for intento in range(_INTENTOS):
            try:
                respuesta = self.client.chat.completions.create(
                    model=self.model,
                    messages=mensajes,
                )
                return respuesta.choices[0].message.content
            except APIStatusError as exc:
                ultimo_error = exc
                if exc.status_code in _REINTENTABLES and intento < _INTENTOS - 1:
                    time.sleep(2 * (intento + 1))
                    continue
                raise
        raise ultimo_error

    def identificar(self, image_bytes: bytes, filename: str = "scan.jpg") -> dict:
        mime = "image/png" if filename.lower().endswith(".png") else "image/jpeg"
        b64 = base64.b64encode(image_bytes).decode("ascii")
        mensajes = [
            {"role": "system", "content": PROMPT_IDENTIFICAR},
            {
                "role": "user",
                "content": [
                    {
                        "type": "text",
                        "text": "Identifica esta planta y responde solo con el JSON pedido.",
                    },
                    {
                        "type": "image_url",
                        "image_url": {"url": f"data:{mime};base64,{b64}"},
                    },
                ],
            },
        ]
        contenido = self._chat(mensajes)
        return normalizar_identificacion(parse_json(contenido), contenido)

    def cuidados_de(self, nombre_comun: str, nombre_cientifico: str = "") -> dict:
        prompt = prompt_cuidados(nombre_comun, nombre_cientifico)
        mensajes = [
            {"role": "system", "content": prompt},
            {"role": "user", "content": f"Genera la ficha de {nombre_comun}."},
        ]
        contenido = self._chat(mensajes)
        return normalizar_cuidados(parse_json(contenido), contenido)
