"""Seleccion del proveedor de IA segun configuracion (hibrido e intercambiable).

Prioridad con `IA_PROVIDER=auto`:
    1. OpenAI  (si hay OPENAI_API_KEY)
    2. WebUI   (si hay WEBUI_API_KEY) - IA institucional local
    3. Gemini  (si hay GEMINI_API_KEY)
    4. Local   (si hay LOCAL_IA_URL)
    5. Mock    (fallback sin dependencias externas)
"""
import os

from django.conf import settings

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


def get_provider(name: str | None = None) -> IaProvider:
    name = (name or settings.IA_PROVIDER or "auto").strip().lower()

    if name != "auto":
        if name not in _REGISTRO:
            raise ValueError(f"Proveedor de IA desconocido: {name}")
        return _REGISTRO[name]()

    if settings.OPENAI_API_KEY:
        return OpenAIProvider()
    if settings.WEBUI_API_KEY:
        return WebuiProvider()
    if settings.GEMINI_API_KEY:
        return GeminiProvider()
    if os.getenv("LOCAL_IA_URL"):
        return LocalProvider()
    return MockProvider()
