"""Endpoints de administracion (escritura) consumidos por el panel React.

Autenticacion ligera: el cliente envia `X-Admin-Id` con el id del usuario
administrador; se verifica que exista y tenga rol ADMIN. No es un esquema
de tokens (pendiente), pero cierra el acceso anonimo a las escrituras.
"""
import bcrypt
from django.utils import timezone
from rest_framework import status
from rest_framework.parsers import JSONParser
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import EventoDesastre, PlantaCatalogo, Usuario


def _admin_desde_request(request):
    raw = request.headers.get("X-Admin-Id") or request.data.get("admin_id")
    try:
        uid = int(raw)
    except (TypeError, ValueError):
        return None
    try:
        u = Usuario.objects.get(pk=uid)
    except Usuario.DoesNotExist:
        return None
    return u if u.rol == "ADMIN" else None


class AdminLoginView(APIView):
    """POST {email, password} -> {id, nombre, email, rol} si es ADMIN."""

    def post(self, request):
        email = (request.data.get("email") or "").strip().lower()
        password = request.data.get("password") or ""
        if not email or not password:
            return Response({"error": "Faltan credenciales"}, status=status.HTTP_400_BAD_REQUEST)
        try:
            u = Usuario.objects.get(email=email)
        except Usuario.DoesNotExist:
            return Response({"error": "Credenciales invalidas"}, status=status.HTTP_401_UNAUTHORIZED)
        if u.rol != "ADMIN":
            return Response({"error": "El usuario no es administrador"}, status=status.HTTP_403_FORBIDDEN)
        try:
            ok = bcrypt.checkpw(password.encode("utf-8"), u.password_hash.encode("utf-8"))
        except (ValueError, TypeError):
            ok = False
        if not ok:
            return Response({"error": "Credenciales invalidas"}, status=status.HTTP_401_UNAUTHORIZED)
        return Response({"id": u.id_usuario, "nombre": u.nombre, "email": u.email, "rol": u.rol})


class AdminCatalogoUpdateView(APIView):
    """PATCH /api/admin/catalogo/<id>/  actualiza campos (incluye `publicada`)."""

    parser_classes = [JSONParser]

    CAMPOS = [
        "nombre_comun", "nombre_cientifico", "frecuencia_riego_dias",
        "frecuencia_sol_horas", "temperatura_min_c", "temperatura_max_c",
        "es_toxica_mascotas", "sprite_base_url", "publicada",
    ]

    def patch(self, request, pk):
        admin = _admin_desde_request(request)
        if admin is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_403_FORBIDDEN)
        try:
            planta = PlantaCatalogo.objects.get(pk=pk)
        except PlantaCatalogo.DoesNotExist:
            return Response({"error": "Especie no encontrada"}, status=status.HTTP_404_NOT_FOUND)
        for campo in self.CAMPOS:
            if campo in request.data:
                setattr(planta, campo, request.data[campo])
        planta.updated_at = timezone.now()
        planta.save(update_fields=[c for c in self.CAMPOS if c in request.data] + ["updated_at"])
        return Response({"ok": True, "id_catalogo": planta.id_catalogo})


class AdminEventoCreateView(APIView):
    """POST /api/admin/eventos/  crea un evento ACTIVO."""

    def post(self, request):
        admin = _admin_desde_request(request)
        if admin is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_403_FORBIDDEN)
        data = request.data
        try:
            evento = EventoDesastre.objects.create(
                id_creador=admin,
                tipo_evento=data["tipo_evento"],
                region=data.get("region", "GLOBAL"),
                mensaje_notificacion=data["mensaje_notificacion"],
                fecha_inicio=data["fecha_inicio"],
                fecha_fin=data["fecha_fin"],
                estado="ACTIVO",
                severidad=data.get("severidad", "MODERADO"),
            )
        except (KeyError, ValueError, TypeError) as exc:
            return Response({"error": f"Datos invalidos: {exc}"}, status=status.HTTP_400_BAD_REQUEST)
        return Response({"id_desastre": evento.id_desastre, "estado": evento.estado}, status=status.HTTP_201_CREATED)


class AdminEventoFinalizarView(APIView):
    """PATCH /api/admin/eventos/<id>/  marca el evento como FINALIZADO."""

    def patch(self, request, pk):
        admin = _admin_desde_request(request)
        if admin is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_403_FORBIDDEN)
        try:
            evento = EventoDesastre.objects.get(pk=pk)
        except EventoDesastre.DoesNotExist:
            return Response({"error": "Evento no encontrado"}, status=status.HTTP_404_NOT_FOUND)
        evento.estado = request.data.get("estado", "FINALIZADO")
        evento.updated_at = timezone.now()
        evento.save(update_fields=["estado", "updated_at"])
        return Response({"id_desastre": evento.id_desastre, "estado": evento.estado})


class AdminStatsView(APIView):
    """GET /api/admin/stats/  conteos basicos para el dashboard."""

    def get(self, request):
        admin = _admin_desde_request(request)
        if admin is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_403_FORBIDDEN)
        from django.db.models import Count
        return Response({
            "especies": PlantaCatalogo.objects.count(),
            "eventos_activos": EventoDesastre.objects.filter(estado="ACTIVO").count(),
            "eventos_total": EventoDesastre.objects.count(),
            "usuarios": Usuario.objects.count(),
        })
