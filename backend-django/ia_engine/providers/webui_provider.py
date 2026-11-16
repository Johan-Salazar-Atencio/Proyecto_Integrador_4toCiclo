"""Proveedor institucional: Open WebUI (proxy de vLLM) con API compatible con OpenAI.

Es el motor "local" de la institucion. Se accede por HTTP a
`WEBUI_BASE_URL` (por ejemplo `http://192.168.17.11:3000/api/v1`) con un
Bearer token de Open WebUI.

El modelo debe ser multimodal: la foto se envia como `image_url` con un data
URI en base64.
"""
import base64
import threading
import time

import httpx
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
# Fuera de la red del instituto el servidor no existe: se detecta al conectar
# (1 s) y se pasa enseguida al siguiente motor. Dentro, la identificacion real
# (especie + cuidados + apariencia) tarda ~5 s, asi que la respuesta tiene 15 s.
_INTENTOS = 2
_REINTENTABLES = {429, 500, 502, 503, 504}
_WEBUI_CONECTAR = 1.0  # segundos para establecer la conexion
_WEBUI_TIMEOUT = 15.0  # segundos para recibir la respuesta


def _thread_target(fn, args, result_holder):
    """Ayuda para ejecutar una funcion en hilo y capturar su resultado."""
    try:
        result_holder["value"] = fn(*args)
    except Exception as exc:  # noqa: BLE001
        result_holder["error"] = exc


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
            timeout=httpx.Timeout(_WEBUI_TIMEOUT, connect=_WEBUI_CONECTAR),
            max_retries=0,
        )
        self.model = settings.WEBUI_MODEL

    def _chat(self, mensajes):
        ultimo_error = None
        for intento in range(_INTENTOS):
            result_holder = {}
            thread = threading.Thread(
                target=_thread_target,
                args=(self._do_chat_wrapper, (mensajes,), result_holder),
                daemon=True,
            )
            thread.start()
            thread.join(timeout=_WEBUI_TIMEOUT)
            if thread.is_alive():
                ultimo_error = TimeoutError(
                    f"WebUI no respondi{chr(111) if intento % 2 else 'ó'} en "
                    f"{_WEBUI_TIMEOUT}s (intento {intento + 1})"
                )
                break
            if "error" in result_holder:
                exc = result_holder["error"]
                if isinstance(exc, APIStatusError) and (
                    exc.status_code in _REINTENTABLES
                    and intento < _INTENTOS - 1
                ):
                    time.sleep(0.5 * (intento + 1))
                    continue
                raise exc
            if "value" not in result_holder:
                ultimo_error = RuntimeError("WebUI retornó sin valor ni error")
                break
            return result_holder["value"]
        raise ultimo_error

    def _do_chat_wrapper(self, mensajes):
        """Envolvente llamada desde hilo para capturar resultado."""
        respuesta = self.client.chat.completions.create(
            model=self.model,
            messages=mensajes,
        )
        return respuesta.choices[0].message.content

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
