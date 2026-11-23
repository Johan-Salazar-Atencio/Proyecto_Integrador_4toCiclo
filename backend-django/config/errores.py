"""Manejo de errores de la API: un error no previsto responde {"error"} en JSON (500) y nunca la pagina de depuracion
de Django con trazas y configuracion, aunque DJANGO_DEBUG este activo (QA-10, QA-28)."""
import logging

from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import exception_handler, set_rollback

logger = logging.getLogger(__name__)


def manejar_excepcion(exc, context):
    respuesta = exception_handler(exc, context)
    if respuesta is not None:
        return respuesta
    vista = context.get("view")
    logger.exception("Error no previsto en %s", vista.__class__.__name__ if vista else "la API")
    set_rollback()
    return Response({"error": "Error interno del servidor"}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


class SinPaginasDeDepuracion:
    """QA-44: con DJANGO_DEBUG=true, un 404 o 500 fuera de DRF (por ejemplo /noexiste o /api/admin/noexiste/) devolvia
    la pagina de depuracion de Django, que lista todas las rutas y muestra trazas. Este middleware la cambia por un
    {"error"} en JSON. Solo actua sobre esas paginas HTML de depuracion (DEBUG activo); el sitio /admin/ y la API
    siguen igual. DJANGO_PAGINAS_DEBUG=true la vuelve a mostrar (solo para depurar en la propia maquina)."""

    MENSAJES = {404: "No encontrado", 500: "Error interno del servidor"}

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        from django.conf import settings
        from django.http import JsonResponse

        respuesta = self.get_response(request)
        if (settings.DEBUG and not getattr(settings, "PAGINAS_DEBUG", False)
                and respuesta.status_code in self.MENSAJES
                and respuesta.get("Content-Type", "").startswith("text/html")):
            return JsonResponse({"error": self.MENSAJES[respuesta.status_code]}, status=respuesta.status_code)
        return respuesta
