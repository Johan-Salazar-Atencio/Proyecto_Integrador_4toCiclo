"""Seleccion del proveedor de IA segun configuracion (hibrido e intercambiable).

Prioridad con `IA_PROVIDER=auto`:
    1. OpenAI  (si hay OPENAI_API_KEY)
    2. WebUI   (si hay WEBUI_API_KEY) - IA institucional local
    3. Gemini  (si hay GEMINI_API_KEY)
    4. Local   (si hay LOCAL_IA_URL)
    5. Mock    (fallback sin dependencias externas)

Ademas existe una **cadena de respaldo**: si el motor configurado no esta
disponible (servidor caido, sin red, saturado), se prueba el siguiente de la
lista. Asi la IA local institucional cae a Gemini sin intervencion, y solo se
devuelve error si TODOS los motores reales fallan.

Un motor caido queda marcado durante un cooldown corto para no pagar su
timeout en cada escaneo.
"""
import os
import threading
import time

import requests
from django.conf import settings
from google.genai import errors as genai_errors
from openai import APIConnectionError, APITimeoutError

from .base import IaProvider
from .gemini_provider import GeminiProvider
from .local_provider import LocalProvider
from .mock_provider import MockProvider
from .openai_provider import OpenAIProvider
from .webui_provider import WebuiProvider

_REGISTRO = {
    "openai": OpenAIProvider,
    "webui": WebuiProvider,
    "gemini": GeminiProvider,
    "local": LocalProvider,
    "mock": MockProvider,
}

_ORDEN_AUTO = ("openai", "webui", "gemini", "local")

# Segundos que un motor se salta tras fallar por indisponibilidad.
_COOLDOWN_SEGUNDOS = 60

_bloqueo = threading.Lock()
_caidos: dict[str, float] = {}


def _configurado(nombre: str) -> bool:
    if nombre == "openai":
        return bool(settings.OPENAI_API_KEY)
    if nombre == "webui":
        return bool(settings.WEBUI_API_KEY)
    if nombre == "gemini":
        return bool(settings.GEMINI_API_KEY)
    if nombre == "local":
        return bool(os.getenv("LOCAL_IA_URL"))
    return False


def _prioridad() -> list[str]:
    """Motores reales configurados, en orden de prioridad."""
    return [n for n in _ORDEN_AUTO if _configurado(n)]


def _disponible(nombre: str) -> bool:
    with _bloqueo:
        hasta = _caidos.get(nombre)
        if hasta is None:
            return True
        if time.time() < hasta:
            return False
        _caidos.pop(nombre, None)
    return True


def marcar_caido(nombre: str) -> None:
    """Marca un motor como indisponible por un rato."""
    with _bloqueo:
        _caidos[nombre] = time.time() + _COOLDOWN_SEGUNDOS


def es_indisponibilidad(exc: Exception) -> bool:
    """Distingue 'el motor no esta disponible' de 'la IA respondio mal'."""
    if isinstance(exc, (requests.ConnectionError, requests.Timeout)):
        return True
    if isinstance(exc, (APIConnectionError, APITimeoutError)):
        return True
    if isinstance(exc, genai_errors.APIError):
        return exc.code in (429, 500, 502, 503, 504)
    status = getattr(exc, "status_code", None)
    if status in (429, 500, 502, 503, 504):
        return True
    return False


def get_provider(name: str | None = None) -> IaProvider:
    """Proveedor principal segun configuracion (sin cadena de respaldo)."""
    name = (name or settings.IA_PROVIDER or "auto").strip().lower()

    if name != "auto":
        if name not in _REGISTRO:
            raise ValueError(f"Proveedor de IA desconocido: {name}")
        return _REGISTRO[name]()

    for nombre in _prioridad():
        if _disponible(nombre):
            return _REGISTRO[nombre]()
    return MockProvider()


def get_provider_chain(name: str | None = None) -> list[IaProvider]:
    """Cadena de motores a probar, en orden.

    El primero es el configurado; los siguientes son respaldos para cuando ese
    motor no este disponible (por ejemplo, la IA local institucional caida
    -> cae a Gemini). `mock` solo entra si no hay ningun motor real.
    """
    name = (name or settings.IA_PROVIDER or "auto").strip().lower()

    if name != "auto" and name not in _REGISTRO:
        raise ValueError(f"Proveedor de IA desconocido: {name}")

    if name == "auto":
        orden = _prioridad()
    elif name == "mock":
        orden = []
    else:
        orden = [name] + [n for n in _prioridad() if n != name]

    cadena: list[IaProvider] = []
    for nombre in orden:
        if not _disponible(nombre):
            continue
        try:
            cadena.append(_REGISTRO[nombre]())
        except Exception:  # noqa: BLE001 - un motor mal configurado no debe romper la cadena
            continue

    if not cadena:
        cadena.append(MockProvider())
    return cadena
