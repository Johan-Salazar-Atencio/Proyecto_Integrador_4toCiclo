"""Quien puede usar el motor de IA.

El motor de IA solo lo consume Spring Boot. Cada identificacion gasta cuota del
proveedor (Gemini, OpenAI...), asi que no puede quedar abierto a cualquiera en la red:

- Si `IA_CLAVE_INTERNA` esta configurada, la peticion debe traer el header
  `X-Ia-Clave` con ese valor (Spring lo envia; ver IaConfig.java).
- Si no esta configurada, solo se aceptan peticiones desde la propia maquina
  (127.0.0.1 / ::1), que es como se ejecuta en desarrollo.
"""
import hmac

from django.conf import settings
from rest_framework.permissions import BasePermission

_LOCALES = {"127.0.0.1", "::1"}


class SoloClienteInterno(BasePermission):
    message = "Acceso al motor de IA no autorizado"

    def has_permission(self, request, view):
        clave = settings.IA_CLAVE_INTERNA
        if clave:
            return hmac.compare_digest(clave, request.headers.get("X-Ia-Clave", ""))
        return request.META.get("REMOTE_ADDR") in _LOCALES
