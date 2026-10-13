"""Proveedor en la nube basado en OpenAI (modelo con vision, ej. gpt-4o)."""
import base64

from django.conf import settings

from .base import (
    PROMPT_IDENTIFICAR,
    IaProvider,
    normalizar_cuidados,
    normalizar_identificacion,
    parse_json,
    prompt_cuidados,
)


class OpenAIProvider(IaProvider):
    name = "openai"

    def __init__(self):
        from openai import OpenAI

        api_key = settings.OPENAI_API_KEY
        if not api_key:
            raise RuntimeError("OPENAI_API_KEY no esta configurada")
        self.client = OpenAI(api_key=api_key)
        self.model = settings.OPENAI_MODEL

    def _chat(self, mensajes):
        resp = self.client.chat.completions.create(
            model=self.model,
            response_format={"type": "json_object"},
            messages=mensajes,
        )
        return resp.choices[0].message.content

    def identificar(self, image_bytes: bytes, filename: str = "scan.jpg") -> dict:
        b64 = base64.b64encode(image_bytes).decode("ascii")
        mensajes = [
            {"role": "system", "content": PROMPT_IDENTIFICAR},
            {
                "role": "user",
                "content": [
                    {"type": "text", "text": "Identifica esta planta."},
                    {
                        "type": "image_url",
                        "image_url": {"url": f"data:image/jpeg;base64,{b64}"},
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
