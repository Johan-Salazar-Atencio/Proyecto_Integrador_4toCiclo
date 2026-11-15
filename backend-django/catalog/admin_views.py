"""Endpoints de administracion (escritura) consumidos por el panel React.

Autenticacion: el login devuelve un token de sesion; en `usuarios.session_token`
solo se guarda su SHA-256, con caducidad en `session_expira` (mismo esquema que
TokenSesion.java en Spring). Cada peticion de escritura debe traer `X-Admin-Id`
y `X-Admin-Token`; se comprueba el token, que no haya caducado y el rol ADMIN.
"""
import hashlib
import hmac
import secrets
from datetime import timedelta
from datetime import timezone as dt_timezone

import bcrypt
from django.utils import timezone
from rest_framework import status
from rest_framework.parsers import JSONParser
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import (
    EventoDesastre, Mision, PlantaCatalogo, Region, RegistroDesastreUsuario, TipoDesastre, Usuario,
)

# Hash BCrypt valido para comparar cuando el correo no existe: asi el tiempo de
# respuesta no revela si esta registrado (mismo criterio que AuthService de Spring).
_HASH_FICTICIO = b"$2a$10$n7uGz2GzFEIhdSm3KDEOYO0ZMzGL132epZ61oqWefVWQYQIufMQVy"
_CREDENCIALES_INVALIDAS = {"error": "Credenciales invalidas"}
_DURACION_SESION = timedelta(days=30)


def _como_aware(valor):
    """session_expira es TIMESTAMP sin zona y Django la guarda en UTC: si llega sin zona, es UTC.
    (Spring la escribe en hora local; con 30 dias de duracion, unas horas de diferencia no importan.)"""
    if timezone.is_naive(valor):
        return timezone.make_aware(valor, dt_timezone.utc)
    return valor


def _hash_token(token):
    return hashlib.sha256(token.encode("utf-8")).hexdigest()


def _admin_desde_request(request):
    """Devuelve el Usuario ADMIN si X-Admin-Id y X-Admin-Token son validos; si no, None."""
    raw_id = request.headers.get("X-Admin-Id")
    token = request.headers.get("X-Admin-Token") or ""
    try:
        uid = int(raw_id)
    except (TypeError, ValueError):
        return None
    try:
        u = Usuario.objects.get(pk=uid)
    except Usuario.DoesNotExist:
        return None
    guardado = u.session_token or ""
    caducada = u.session_expira is None or _como_aware(u.session_expira) < timezone.now()
    if u.rol != "ADMIN" or caducada or not guardado or not token:
        return None
    if not hmac.compare_digest(guardado, _hash_token(token)):
        return None
    return u


def _password_valida(password, password_hash):
    try:
        return bcrypt.checkpw(password.encode("utf-8"), password_hash.encode("utf-8"))
    except (ValueError, TypeError):
        return False


class AdminLoginView(APIView):
    """POST {email, password} -> {id, nombre, email, rol, token} si es ADMIN."""

    def post(self, request):
        email = (request.data.get("email") or "").strip().lower()
        password = request.data.get("password") or ""
        if not email or not password:
            return Response({"error": "Faltan credenciales"}, status=status.HTTP_400_BAD_REQUEST)
        u = Usuario.objects.filter(email=email).first()
        if u is None:
            _password_valida(password, _HASH_FICTICIO.decode())
            return Response(_CREDENCIALES_INVALIDAS, status=status.HTTP_401_UNAUTHORIZED)
        # Mismo mensaje para "no es admin" y "clave incorrecta": no se revela que el correo existe
        if not _password_valida(password, u.password_hash) or u.rol != "ADMIN":
            return Response(_CREDENCIALES_INVALIDAS, status=status.HTTP_401_UNAUTHORIZED)
        token = secrets.token_hex(16)
        u.session_token = _hash_token(token)
        u.session_expira = timezone.now() + _DURACION_SESION
        u.save(update_fields=["session_token", "session_expira"])
        return Response({"id": u.id_usuario, "nombre": u.nombre, "email": u.email, "rol": u.rol,
                         "token": token})


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
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
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
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        data = request.data
        try:
            codigo_tipo = data["tipo_evento"]
            tipo = TipoDesastre.objects.filter(codigo=codigo_tipo, activo=True).first()
            if tipo is None:
                return Response(
                    {"error": f"Tipo de desastre no valido: {codigo_tipo}"},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            codigo_region = data.get("region") or "GLOBAL"
            # Si la region no existe todavia, se crea: el panel ofrece nombres libres
            region, _ = Region.objects.get_or_create(codigo=codigo_region, defaults={"nombre": codigo_region})
            evento = EventoDesastre.objects.create(
                id_creador=admin,
                tipo_evento=tipo.codigo,
                id_tipo_desastre=tipo,
                region=region.codigo,
                id_region=region,
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
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
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
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        from django.db.models import Count
        return Response({
            "especies": PlantaCatalogo.objects.count(),
            "eventos_activos": EventoDesastre.objects.filter(estado="ACTIVO").count(),
            "eventos_total": EventoDesastre.objects.count(),
            "usuarios": Usuario.objects.count(),
        })


def _mision_a_dict(m):
    return {
        "id_mision": m.id_mision, "titulo": m.titulo, "descripcion": m.descripcion, "tipo": m.tipo,
        "meta": m.meta, "recompensa_monedas": m.recompensa_monedas, "recompensa_xp": m.recompensa_xp,
        "activa": m.activa, "predeterminada": m.predeterminada, "orden": m.orden,
    }


def _validar_mision(data, parcial):
    """Devuelve (valores, error). Con parcial=True solo valida los campos que llegan."""
    valores = {}
    if not parcial or "titulo" in data:
        titulo = str(data.get("titulo") or "").strip()
        if not titulo or len(titulo) > 100:
            return None, "El titulo es obligatorio (maximo 100 caracteres)"
        valores["titulo"] = titulo
    if not parcial or "tipo" in data:
        if data.get("tipo") not in Mision.TIPOS:
            return None, "Tipo de mision no valido: " + ", ".join(Mision.TIPOS)
        valores["tipo"] = data["tipo"]
    enteros = (("meta", 1), ("recompensa_monedas", 0), ("recompensa_xp", 0), ("orden", 0))
    for campo, minimo in enteros:
        if campo in data or (not parcial and campo == "meta"):
            valor = data.get(campo)
            if isinstance(valor, bool) or not isinstance(valor, int) or valor < minimo:
                return None, f"{campo} debe ser un entero >= {minimo}"
            valores[campo] = valor
    if "descripcion" in data:
        valores["descripcion"] = data["descripcion"] or None
    if "activa" in data:
        if not isinstance(data["activa"], bool):
            return None, "activa debe ser verdadero o falso"
        valores["activa"] = data["activa"]
    return valores, None


class AdminMisionListCreateView(APIView):
    """GET /api/admin/misiones/ lista todas (activas e inactivas); POST crea una mision."""

    def get(self, request):
        if _admin_desde_request(request) is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        return Response([_mision_a_dict(m) for m in Mision.objects.order_by("orden", "id_mision")])

    def post(self, request):
        if _admin_desde_request(request) is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        valores, error = _validar_mision(request.data, parcial=False)
        if error:
            return Response({"error": error}, status=status.HTTP_400_BAD_REQUEST)
        mision = Mision.objects.create(**valores)
        return Response(_mision_a_dict(mision), status=status.HTTP_201_CREATED)


class AdminMisionDetailView(APIView):
    """PATCH /api/admin/misiones/<id>/ edita; para quitarla se envia {"activa": false} (no se borra,
    asi se conserva el progreso de los usuarios y se puede reactivar)."""

    def patch(self, request, pk):
        if _admin_desde_request(request) is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        mision = Mision.objects.filter(pk=pk).first()
        if mision is None:
            return Response({"error": "Mision no encontrada"}, status=status.HTTP_404_NOT_FOUND)
        valores, error = _validar_mision(request.data, parcial=True)
        if error:
            return Response({"error": error}, status=status.HTTP_400_BAD_REQUEST)
        for campo, valor in valores.items():
            setattr(mision, campo, valor)
        if valores:
            mision.save(update_fields=list(valores))
        return Response(_mision_a_dict(mision))


class AdminEventoEfectosView(APIView):
    """GET /api/admin/eventos/<id>/efectos/  que le paso a cada planta cuando termino el desastre:
    resumen (salvadas, danadas, destruidas, refugiadas, XP dado) y una fila por planta."""

    def get(self, request, pk):
        if _admin_desde_request(request) is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        if not EventoDesastre.objects.filter(pk=pk).exists():
            return Response({"error": "Evento no encontrado"}, status=status.HTTP_404_NOT_FOUND)
        registros = list(
            RegistroDesastreUsuario.objects.filter(id_desastre=pk)
            .select_related("id_planta_usuario__id_usuario")
            .order_by("id_registro")
        )
        return Response(_resumen_efectos(registros))


def _resumen_efectos(registros):
    filas = [
        {
            "id_registro": r.id_registro,
            "usuario": r.id_planta_usuario.id_usuario.nombre,
            "planta": r.id_planta_usuario.apodo or f"Planta {r.id_planta_usuario_id}",
            "estado_final": r.estado_final,
            "estaba_refugiada": r.estaba_refugiada,
            "dano_salud": r.dano_salud_aplicado,
            "xp_otorgado": r.xp_otorgado,
        }
        for r in registros
    ]
    contar = lambda estado: sum(1 for f in filas if f["estado_final"] == estado)  # noqa: E731
    return {
        "resumen": {
            "total": len(filas),
            "salvadas": contar("SALVADA"),
            "danadas": contar("DANADA"),
            "destruidas": contar("DESTRUIDA"),
            "refugiadas": sum(1 for f in filas if f["estaba_refugiada"]),
            "xp_total": sum(f["xp_otorgado"] for f in filas),
        },
        "registros": filas,
    }
