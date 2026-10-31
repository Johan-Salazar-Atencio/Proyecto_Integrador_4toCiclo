"""Endpoints del motor de IA consumidos por Spring Boot (y por React)."""
import logging

from rest_framework import status
from rest_framework.parsers import FormParser, MultiPartParser
from rest_framework.response import Response
from rest_framework.views import APIView

from .providers.factory import es_indisponibilidad, get_provider_chain, marcar_caido
from .services import persistir_catalogo, reemplazar_cuidados

logger = logging.getLogger(__name__)


def _es_verdad(valor) -> bool:
    return str(valor).strip().lower() in {"1", "true", "si", "yes", "on"}


def _intentar(cadena, operacion):
    """Ejecuta `operacion` sobre cada proveedor de la cadena hasta que funcione.

    Si un motor esta indisponible (caido, sin red, saturado) se marca para
    saltarlo durante un rato y se prueba el siguiente: por ejemplo, la IA local
    institucional cae a Gemini de forma transparente.
    """
    errores = []
    for provider in cadena:
        try:
            return provider, operacion(provider), None
        except Exception as exc:  # noqa: BLE001 - se reintenta con el siguiente motor
            logger.warning("Proveedor de IA '%s' fallo: %s", provider.name, exc)
            errores.append(f"{provider.name}: {exc}")
            if es_indisponibilidad(exc):
                marcar_caido(provider.name)
    return None, None, "; ".join(errores)


class IdentificarView(APIView):
    """POST multipart: campo `imagen`. Devuelve el contrato normalizado."""

    parser_classes = [MultiPartParser, FormParser]

    def post(self, request):
        imagen = request.FILES.get("imagen")
        if imagen is None:
            return Response({"error": "Falta el archivo 'imagen'"}, status=status.HTTP_400_BAD_REQUEST)

        proveedor = request.data.get("provider")
        persistir = _es_verdad(request.data.get("persist", "false"))

        # La imagen se lee una sola vez: el flujo de archivos no se puede releer.
        datos = imagen.read()
        cadena = get_provider_chain(proveedor)
        provider, resultado, error = _intentar(
            cadena, lambda p: p.identificar(datos, filename=imagen.name)
        )

        if provider is None:
            logger.error("Todos los motores de IA fallaron: %s", error)
            return Response(
                {"error": f"No se pudo identificar la imagen: {error}"},
                status=status.HTTP_502_BAD_GATEWAY,
            )

        resultado["provider"] = provider.name

        if persistir and resultado.get("cuidados"):
            try:
                resultado["idCatalogo"] = persistir_catalogo(resultado)
            except Exception as exc:  # noqa: BLE001
                logger.exception("No se pudo persistir el catalogo")
                resultado["avisoPersistencia"] = str(exc)

        return Response(resultado)


class CuidadosView(APIView):
    """POST JSON: {nombre, cientifico, persistir} -> ficha de cuidados por IA."""

    def post(self, request):
        nombre = request.data.get("nombre") or request.data.get("nombreComun")
        if not nombre:
            return Response({"error": "Falta 'nombre'"}, status=status.HTTP_400_BAD_REQUEST)
        cientifico = request.data.get("cientifico") or request.data.get("nombreCientifico") or ""
        proveedor = request.data.get("provider")
        persistir = _es_verdad(request.data.get("persistir", "false"))
        id_catalogo = request.data.get("idCatalogo")

        cadena = get_provider_chain(proveedor)
        provider, resultado, error = _intentar(
            cadena, lambda p: p.cuidados_de(nombre, cientifico)
        )

        if provider is None:
            logger.error("Todos los motores de IA fallaron: %s", error)
            return Response(
                {"error": f"No se pudieron generar los cuidados: {error}"},
                status=status.HTTP_502_BAD_GATEWAY,
            )

        resultado["provider"] = provider.name

        if persistir and id_catalogo:
            try:
                resultado["seccionesGuardadas"] = reemplazar_cuidados(int(id_catalogo), resultado)
            except Exception as exc:  # noqa: BLE001
                logger.exception("No se pudieron persistir los cuidados")
                resultado["avisoPersistencia"] = str(exc)

        return Response(resultado)
