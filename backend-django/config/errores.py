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
