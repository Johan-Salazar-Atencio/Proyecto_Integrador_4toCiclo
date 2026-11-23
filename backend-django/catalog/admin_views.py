"""Endpoints de administracion (escritura) consumidos por el panel React.

Autenticacion: el login devuelve un token de sesion; en `usuarios.session_token`
solo se guarda su SHA-256, con caducidad en `session_expira` (mismo esquema que
TokenSesion.java en Spring). Cada peticion de escritura debe traer `X-Admin-Id`
y `X-Admin-Token`; se comprueba el token, que no haya caducado y el rol ADMIN.
"""
import hashlib
import hmac
import os
import re
import secrets
from datetime import timedelta

import bcrypt
from django.conf import settings
from django.core.cache import cache
from django.db import IntegrityError, transaction
from django.db.models import Count
from django.utils import timezone
from django.utils.dateparse import parse_datetime
from rest_framework import status
from rest_framework.parsers import FormParser, JSONParser, MultiPartParser
from rest_framework.response import Response
from rest_framework.views import APIView

from . import eventos_imagen, fondos_imagen
from .models import (
    EscaneoHistorial, EventoDesastre, FondoEscena, ItemTienda, Logro, LogroUsuario, Mision, ParametroJuego,
    PlantaCatalogo, PlantaUsuario, Region, RegistroDesastreUsuario, ReglaProgreso, TemaFondo, TipoDesastre, Usuario,
)

# Hash BCrypt valido para comparar cuando el correo no existe: asi el tiempo de
# respuesta no revela si esta registrado (mismo criterio que AuthService de Spring).
_HASH_FICTICIO = b"$2a$10$n7uGz2GzFEIhdSm3KDEOYO0ZMzGL132epZ61oqWefVWQYQIufMQVy"
_CREDENCIALES_INVALIDAS = {"error": "Credenciales invalidas"}
_DURACION_SESION = timedelta(days=30)
# QA-27: tras 5 claves equivocadas seguidas para un correo, 15 minutos sin poder intentarlo (como Spring)
_MAX_INTENTOS = 5
_ESPERA_INTENTOS = 15 * 60


def _como_aware(valor):
    """session_expira es TIMESTAMP sin zona: Spring y Django (DATABASES TIME_ZONE) la guardan en hora de Lima,
    asi que si llega sin zona es hora de Lima."""
    if timezone.is_naive(valor):
        return timezone.make_aware(valor, timezone.get_default_timezone())
    return valor


def _hash_token(token):
    return hashlib.sha256(token.encode("utf-8")).hexdigest()


# Las columnas enteras de Postgres son int4: un numero mayor revienta el UPDATE con un 500 (QA-10)
ENTERO_MAX = 2**31 - 1


def _es_entero(valor, minimo=None, maximo=ENTERO_MAX):
    """True si es un int de verdad (no bool) dentro del rango."""
    if isinstance(valor, bool) or not isinstance(valor, int):
        return False
    return (minimo is None or valor >= minimo) and valor <= maximo


class _CuerpoNoObjeto(Exception):
    pass


class AdminAPIView(APIView):
    """Base de todas las vistas del panel: un POST/PUT/PATCH cuyo cuerpo no es un objeto JSON ([], null, "x", 5)
    responde 400 con {"error"} en vez de reventar en request.data.get(...) con un 500 (QA-10)."""

    def initial(self, request, *args, **kwargs):
        super().initial(request, *args, **kwargs)
        if request.method in ("POST", "PUT", "PATCH") and not isinstance(request.data, dict):
            raise _CuerpoNoObjeto()

    def handle_exception(self, exc):
        if isinstance(exc, _CuerpoNoObjeto):
            return Response({"error": "El cuerpo debe ser un objeto JSON"}, status=status.HTTP_400_BAD_REQUEST)
        return super().handle_exception(exc)


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
    # Sesion propia del panel (V31): entrar a la app de usuario ya no cierra la del panel ni al reves (QA-19)
    guardado = u.admin_session_token or ""
    caducada = u.admin_session_expira is None or _como_aware(u.admin_session_expira) < timezone.now()
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


_MAX_INTENTOS_POR_IP = 100


def _sumar_intento(clave):
    """Suma 1 de forma atomica (add + incr de la cache) y devuelve el total del periodo."""
    if cache.add(clave, 1, _ESPERA_INTENTOS):
        return 1
    try:
        return cache.incr(clave)
    except ValueError:  # caduco justo entre add e incr
        cache.add(clave, 1, _ESPERA_INTENTOS)
        return 1


class AdminLoginView(AdminAPIView):
    """POST {email, password} -> {id, nombre, email, rol, token} si es ADMIN."""

    def post(self, request):
        email = request.data.get("email")
        password = request.data.get("password")
        email = email.strip().lower() if isinstance(email, str) else ""
        password = password if isinstance(password, str) else ""
        if not email or not password:
            return Response({"error": "Faltan credenciales"}, status=status.HTTP_400_BAD_REQUEST)
        # QA-37: el intento se cuenta ANTES de mirar la clave (atomico: en paralelo ya no pasan 13) y por correo + IP,
        # asi probar claves desde otra IP no deja fuera al admin. Ademas un tope por IP para todos los correos.
        ip = request.META.get("REMOTE_ADDR", "")
        clave_intentos = "admin-login:" + hashlib.sha256(f"{email}|{ip}".encode("utf-8")).hexdigest()
        if (_sumar_intento(clave_intentos) > _MAX_INTENTOS
                or _sumar_intento("admin-login-ip:" + ip) > _MAX_INTENTOS_POR_IP):
            return Response({"error": "Demasiados intentos fallidos. Espera unos minutos y vuelve a intentarlo."},
                            status=status.HTTP_429_TOO_MANY_REQUESTS)
        u = Usuario.objects.filter(email=email).first()
        if u is None:
            _password_valida(password, _HASH_FICTICIO.decode())
            return Response(_CREDENCIALES_INVALIDAS, status=status.HTTP_401_UNAUTHORIZED)
        # Mismo mensaje para "no es admin" y "clave incorrecta": no se revela que el correo existe
        if not _password_valida(password, u.password_hash) or u.rol != "ADMIN":
            return Response(_CREDENCIALES_INVALIDAS, status=status.HTTP_401_UNAUTHORIZED)
        cache.delete(clave_intentos)
        token = secrets.token_hex(16)
        u.admin_session_token = _hash_token(token)
        u.admin_session_expira = timezone.now() + _DURACION_SESION
        u.save(update_fields=["admin_session_token", "admin_session_expira"])
        return Response({"id": u.id_usuario, "nombre": u.nombre, "email": u.email, "rol": u.rol,
                         "token": token})


# Lo que acepta el admin al editar una especie: (tipo, minimo, maximo). Los textos se miden en caracteres.
CAMPOS_CATALOGO = {
    "nombre_comun": ("texto", 1, 100),
    "nombre_cientifico": ("texto", 2, 150),
    "frecuencia_riego_dias": ("entero", 1, 365),
    "frecuencia_sol_horas": ("entero", 0, 24),
    "temperatura_min_c": ("entero", -50, 60),
    "temperatura_max_c": ("entero", -50, 60),
    "es_toxica_mascotas": ("booleano", None, None),
    "sprite_base_url": ("texto", 1, 255),
    "publicada": ("booleano", None, None),
}


def _validar_catalogo(data, planta):
    """Devuelve (valores, error, codigo). Antes se copiaba cualquier cosa con setattr y salian 500 (QA-10) o un
    nombre cientifico que solo cambiaba mayusculas dejaba dos filas iguales para Spring y rompia los escaneos (QA-11)."""
    valores = {}
    for campo, (tipo, minimo, maximo) in CAMPOS_CATALOGO.items():
        if campo not in data:
            continue
        valor = data[campo]
        if campo == "sprite_base_url" and (valor is None or valor == ""):
            continue  # sin imagen: se deja la que tenia (la columna no admite vacio)
        if tipo == "texto":
            valor = valor.strip() if isinstance(valor, str) else None
            if not valor or not minimo <= len(valor) <= maximo:
                return None, f"{campo} debe ser un texto de {minimo} a {maximo} caracteres", 400
            if "\x00" in valor:
                return None, f"{campo} tiene caracteres no validos", 400
        elif tipo == "entero":
            if not _es_entero(valor, minimo, maximo):
                return None, f"{campo} debe ser un entero entre {minimo} y {maximo}", 400
        elif not isinstance(valor, bool):
            return None, f"{campo} debe ser verdadero o falso", 400
        valores[campo] = valor
    minima = valores.get("temperatura_min_c", planta.temperatura_min_c)
    maxima = valores.get("temperatura_max_c", planta.temperatura_max_c)
    if minima is not None and maxima is not None and minima > maxima:
        return None, "temperatura_min_c no puede ser mayor que temperatura_max_c", 400
    nombre = valores.get("nombre_cientifico")
    if nombre is not None and PlantaCatalogo.objects.filter(nombre_cientifico__iexact=nombre).exclude(pk=planta.pk).exists():
        return None, f"Ya existe otra especie con el nombre cientifico {nombre} (sin distinguir mayusculas)", 409
    return valores, None, None


class AdminCatalogoListView(AdminAPIView):
    """GET /api/admin/catalogo/  todas las especies, publicadas o no (el catalogo publico solo muestra las publicadas)."""

    def get(self, request):
        if _admin_desde_request(request) is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        from .serializers import PlantaCatalogoSerializer
        qs = PlantaCatalogo.objects.all().prefetch_related("cuidadoseccion_set").order_by("nombre_comun")
        return Response(PlantaCatalogoSerializer(qs, many=True).data)


class AdminCatalogoUpdateView(AdminAPIView):
    """PATCH /api/admin/catalogo/<id>/  actualiza campos (incluye `publicada`) con validacion de tipo y rango."""

    parser_classes = [JSONParser]

    CAMPOS = list(CAMPOS_CATALOGO)

    def patch(self, request, pk):
        admin = _admin_desde_request(request)
        if admin is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        try:
            planta = PlantaCatalogo.objects.get(pk=pk)
        except PlantaCatalogo.DoesNotExist:
            return Response({"error": "Especie no encontrada"}, status=status.HTTP_404_NOT_FOUND)
        valores, error, codigo = _validar_catalogo(request.data, planta)
        if error:
            return Response({"error": error}, status=codigo)
        for campo, valor in valores.items():
            setattr(planta, campo, valor)
        planta.updated_at = timezone.now()
        try:
            planta.save(update_fields=list(valores) + ["updated_at"])
        except IntegrityError:
            return Response({"error": "Ya existe otra especie con ese nombre cientifico"}, status=status.HTTP_409_CONFLICT)
        return Response({"ok": True, "id_catalogo": planta.id_catalogo})


SEVERIDADES_EVENTO = [codigo for codigo, _ in EventoDesastre.SEVERIDADES]


class AdminEventoCreateView(AdminAPIView):
    """POST /api/admin/eventos/  crea un evento: ACTIVO si ya empezo, PROGRAMADO si empieza mas adelante (Spring lo
    activa a su hora). La region debe existir (QA-17) y solo puede haber un evento a la vez en la misma ventana."""

    def post(self, request):
        admin = _admin_desde_request(request)
        if admin is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        data = request.data
        try:
            codigo_tipo = data["tipo_evento"]
            if not isinstance(codigo_tipo, str):
                return Response({"error": "tipo_evento debe ser un texto"}, status=status.HTTP_400_BAD_REQUEST)
            tipo = TipoDesastre.objects.filter(codigo=codigo_tipo, activo=True).first()
            if tipo is None:
                return Response(
                    {"error": f"Tipo de desastre no valido: {codigo_tipo}"},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            inicio = parse_datetime(str(data["fecha_inicio"]))
            fin = parse_datetime(str(data["fecha_fin"]))
            if inicio is None or fin is None:
                return Response({"error": "fecha_inicio y fecha_fin deben ser fechas ISO validas"},
                                status=status.HTTP_400_BAD_REQUEST)
            if timezone.is_naive(inicio) != timezone.is_naive(fin):
                return Response({"error": "fecha_inicio y fecha_fin deben llevar zona horaria las dos o ninguna"},
                                status=status.HTTP_400_BAD_REQUEST)
            if fin <= inicio:
                return Response({"error": "fecha_fin debe ser posterior a fecha_inicio"},
                                status=status.HTTP_400_BAD_REQUEST)
            severidad = data.get("severidad", "MODERADO")
            if severidad not in SEVERIDADES_EVENTO:
                return Response({"error": "severidad no valida: " + ", ".join(SEVERIDADES_EVENTO)},
                                status=status.HTTP_400_BAD_REQUEST)
            mensaje = data["mensaje_notificacion"]
            if not isinstance(mensaje, str) or not mensaje.strip() or len(mensaje) > 1000:
                return Response({"error": "mensaje_notificacion es obligatorio (maximo 1000 caracteres)"},
                                status=status.HTTP_400_BAD_REQUEST)
            codigo_region = data.get("region") or "GLOBAL"
            # La region debe existir: antes un nombre escrito a mano creaba una region nueva y activa para todos (QA-17)
            region = Region.objects.filter(codigo=codigo_region).first() if isinstance(codigo_region, str) else None
            if region is None:
                return Response({"error": f"Region no valida: {codigo_region}"}, status=status.HTTP_400_BAD_REQUEST)
            # Solo un evento a la vez: ninguno ACTIVO o PROGRAMADO puede cruzarse con esta ventana
            vigente = EventoDesastre.objects.filter(
                estado__in=["ACTIVO", "PROGRAMADO"], fecha_fin__gt=max(inicio, _ahora_como(inicio)), fecha_inicio__lt=fin,
            ).first()
            if vigente is not None:
                return Response(
                    {"error": "Ya hay un evento en curso o programado en esas fechas. Cancelalo o elige otras fechas.",
                     "id_desastre_activo": vigente.id_desastre},
                    status=status.HTTP_409_CONFLICT,
                )
            # Un evento que empieza mas adelante nace PROGRAMADO: antes nacia ACTIVO y bloqueaba lanzar otro hoy (QA-18)
            estado_inicial = "PROGRAMADO" if inicio > _ahora_como(inicio) else "ACTIVO"
            evento = EventoDesastre.objects.create(
                id_creador=admin,
                id_tipo_desastre=tipo,
                id_region=region,
                mensaje_notificacion=mensaje.strip(),
                fecha_inicio=data["fecha_inicio"],
                fecha_fin=data["fecha_fin"],
                estado=estado_inicial,
                severidad=severidad,
            )
        except (KeyError, ValueError, TypeError) as exc:
            return Response({"error": f"Datos invalidos: {exc}"}, status=status.HTTP_400_BAD_REQUEST)
        return Response({"id_desastre": evento.id_desastre, "estado": evento.estado}, status=status.HTTP_201_CREATED)


def _ahora_como(referencia):
    """La hora actual con o sin zona, igual que la fecha con la que se compara (el panel manda las dos formas)."""
    ahora = timezone.now()
    return timezone.make_naive(ahora) if timezone.is_naive(referencia) else ahora


class AdminEventoFinalizarView(AdminAPIView):
    """PATCH /api/admin/eventos/<id>/  CANCELA el evento: termina ya y SIN danos (QA-07).

    Queda FINALIZADO con fecha_inicio = fecha_fin = ahora: es la marca de "cancelado" que Spring reconoce al procesarlo
    (ResultadoDesastreService): no dana a nadie, no paga premios y quita la prevencion de las plantas, bloqueando
    usuario y plantas en el mismo orden que el resto de Spring. QA-34: antes Django hacia aqui un UPDATE masivo de
    plantas_usuario sin ese orden y, con usuarios activos, chocaba con Spring (deadlock, 500 y el evento seguia
    ACTIVO). Aqui solo se toca la fila del evento. Solo acepta {"estado": "FINALIZADO"} o cuerpo vacio."""

    def patch(self, request, pk):
        admin = _admin_desde_request(request)
        if admin is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        if request.data.get("estado", "FINALIZADO") != "FINALIZADO":
            return Response({"error": "Solo se puede cancelar un evento (estado FINALIZADO)"},
                            status=status.HTTP_400_BAD_REQUEST)
        with transaction.atomic():
            evento = EventoDesastre.objects.select_for_update().filter(pk=pk).first()
            if evento is None:
                return Response({"error": "Evento no encontrado"}, status=status.HTTP_404_NOT_FOUND)
            if evento.estado == "FINALIZADO":
                # Ya termino (o ya se cancelo): no se toca, ni se le quitan resultados que Spring ya aplico
                return Response({"id_desastre": evento.id_desastre, "estado": evento.estado, "cancelado": False})
            ahora = _ahora_como(evento.fecha_fin)
            evento.estado = "FINALIZADO"
            evento.procesado = False  # Spring lo procesa como cancelado (sin danos) y quita la prevencion
            evento.fecha_inicio = ahora
            evento.fecha_fin = ahora
            evento.updated_at = timezone.now()
            evento.save(update_fields=["estado", "procesado", "fecha_inicio", "fecha_fin", "updated_at"])
        return Response({"id_desastre": evento.id_desastre, "estado": evento.estado, "cancelado": True})


class AdminStatsView(AdminAPIView):
    """GET /api/admin/stats/  cifras reales del dashboard (todo sale de la base de datos)."""

    def get(self, request):
        admin = _admin_desde_request(request)
        if admin is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        from django.db.models import Count
        from django.db.models.functions import TruncDate, TruncMinute

        ahora = timezone.localtime()
        hoy = ahora.date()
        inicio_hoy = ahora.replace(hour=0, minute=0, second=0, microsecond=0)
        inicio_mes = inicio_hoy.replace(day=1)
        mes_pasado = (inicio_mes - timedelta(days=1)).replace(day=1)

        usuarios = Usuario.objects.count()
        nuevos_mes = Usuario.objects.filter(fecha_registro__gte=inicio_mes).count()
        nuevos_mes_pasado = Usuario.objects.filter(fecha_registro__gte=mes_pasado, fecha_registro__lt=inicio_mes).count()
        if nuevos_mes_pasado:
            crecimiento = round((nuevos_mes - nuevos_mes_pasado) * 100 / nuevos_mes_pasado)
        else:
            crecimiento = None

        escaneos_hoy_qs = EscaneoHistorial.objects.filter(fecha_escaneo__gte=inicio_hoy)
        pico = (
            escaneos_hoy_qs.annotate(m=TruncMinute("fecha_escaneo")).values("m").annotate(n=Count("id_escaneo"))
            .order_by("-n").values_list("n", flat=True).first()
        ) or 0

        desde = inicio_hoy - timedelta(days=13)
        por_dia_usuarios = dict(
            Usuario.objects.filter(fecha_registro__gte=desde).annotate(d=TruncDate("fecha_registro"))
            .values("d").annotate(n=Count("id_usuario")).values_list("d", "n")
        )
        por_dia_escaneos = dict(
            EscaneoHistorial.objects.filter(fecha_escaneo__gte=desde).annotate(d=TruncDate("fecha_escaneo"))
            .values("d").annotate(n=Count("id_escaneo")).values_list("d", "n")
        )
        actividad = []
        for i in range(14):
            d = hoy - timedelta(days=13 - i)
            actividad.append({"fecha": d.isoformat(), "usuarios": por_dia_usuarios.get(d, 0),
                              "escaneos": por_dia_escaneos.get(d, 0)})

        ultimos = []
        for e in EscaneoHistorial.objects.select_related("id_usuario", "id_catalogo").order_by("-fecha_escaneo")[:8]:
            ultimos.append({
                "id": e.id_escaneo,
                "usuario": e.id_usuario.nombre if e.id_usuario else "Usuario eliminado",
                "especie": e.id_catalogo.nombre_comun if e.id_catalogo else "Sin identificar",
                "cientifico": e.id_catalogo.nombre_cientifico if e.id_catalogo else "",
                "confianza": float(e.confianza_ia) if e.confianza_ia is not None else None,
                "fecha": e.fecha_escaneo.isoformat(),
            })

        return Response({
            "especies": PlantaCatalogo.objects.count(),
            "toxicas": PlantaCatalogo.objects.filter(es_toxica_mascotas=True).count(),
            "eventos_activos": EventoDesastre.objects.filter(estado="ACTIVO", fecha_fin__gt=timezone.now()).count(),
            "eventos_total": EventoDesastre.objects.count(),
            "usuarios": usuarios,
            "usuarios_nuevos_mes": nuevos_mes,
            "crecimiento_usuarios": crecimiento,
            "escaneos_hoy": escaneos_hoy_qs.count(),
            "pico_por_minuto": pico,
            "actividad": actividad,
            "ultimos_escaneos": ultimos,
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
        if not isinstance(data.get("tipo"), str) or data.get("tipo") not in Mision.TIPOS:
            return None, "Tipo de mision no valido: " + ", ".join(Mision.TIPOS)
        valores["tipo"] = data["tipo"]
    # QA-35: mismos topes que los logros; con 2^31-1 Spring desbordaba el saldo y tumbaba los escaneos de todos
    enteros = (("meta", 1, LIMITES_LOGRO["meta_max"]), ("recompensa_monedas", 0, LIMITES_LOGRO["recompensa_monedas_max"]),
               ("recompensa_xp", 0, LIMITES_LOGRO["recompensa_xp_max"]), ("orden", 0, LIMITES_LOGRO["orden_max"]))
    for campo, minimo, maximo in enteros:
        if campo in data or (not parcial and campo == "meta"):
            valor = data.get(campo)
            if not _es_entero(valor, minimo, maximo):
                return None, f"{campo} debe ser un entero entre {minimo} y {maximo}"
            valores[campo] = valor
    if "descripcion" in data:
        descripcion = data["descripcion"]
        if descripcion is not None and not isinstance(descripcion, str):
            return None, "descripcion debe ser un texto"
        valores["descripcion"] = (descripcion or "").strip()[:500] or None
    if "activa" in data:
        if not isinstance(data["activa"], bool):
            return None, "activa debe ser verdadero o falso"
        valores["activa"] = data["activa"]
    return valores, None


class AdminMisionListCreateView(AdminAPIView):
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


class AdminMisionDetailView(AdminAPIView):
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


# ---------------------------------------------------------------------------------------------
# Logros. El esquema es de Spring (V29). Nunca se borran ni se toca logros_usuario: asi, editar la
# meta o la condicion no quita logros ya desbloqueados.
# ---------------------------------------------------------------------------------------------
ICONOS_LOGRO = [
    "semilla", "hoja", "huayco", "copo", "sol", "bicho", "escudo", "refugio", "corazon", "gota", "abono",
    "luz", "racha", "nivel", "tienda", "maceta", "invernadero", "fondo", "mision", "amanecer", "luna",
]
_CODIGO_LOGRO = re.compile(r"^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$")
LIMITES_LOGRO = {
    "codigo": 50, "titulo": 80, "descripcion": 250, "icono": 40,
    "meta_max": 1000000, "recompensa_xp_max": 100000, "recompensa_monedas_max": 100000, "orden_max": 100000,
}


def _logro_a_dict(logro, desbloqueos=0):
    return {
        "id_logro": logro.id_logro, "codigo": logro.codigo, "titulo": logro.titulo,
        "descripcion": logro.descripcion, "icono": logro.icono, "rareza": logro.rareza,
        "condicion": logro.condicion, "meta": logro.meta, "parametro": logro.parametro,
        "recompensa_xp": logro.recompensa_xp, "recompensa_monedas": logro.recompensa_monedas,
        "secreto": logro.secreto, "activo": logro.activo, "orden": logro.orden, "desbloqueos": desbloqueos,
    }


def _desbloqueos_por_logro():
    """{id_logro: usuarios que lo tienen desbloqueado}, en una sola consulta."""
    filas = (LogroUsuario.objects.filter(desbloqueado_en__isnull=False)
             .values("id_logro").annotate(n=Count("id_logro")))
    return {f["id_logro"]: f["n"] for f in filas}


def _codigos_desastre():
    return list(TipoDesastre.objects.order_by("codigo").values_list("codigo", flat=True))


def _validar_texto_logro(data, campo, etiqueta, maximo):
    texto = data.get(campo)
    texto = texto.strip() if isinstance(texto, str) else ""
    if not texto or len(texto) > maximo:
        return None, f"{etiqueta} es obligatorio (maximo {maximo} caracteres)"
    return texto, None


def _validar_parametro_logro(condicion, parametro):
    """Devuelve (parametro_limpio, error) segun la condicion."""
    if isinstance(parametro, str):
        parametro = parametro.strip() or None
    elif parametro is not None:
        return None, "parametro debe ser un texto o null"
    if condicion == Logro.PARAMETRO_OBLIGATORIO:
        if parametro not in Logro.FRANJAS_HORARIAS:
            return None, "La condicion ESCANEO_HORA exige parametro: " + " o ".join(Logro.FRANJAS_HORARIAS)
    elif condicion == Logro.PARAMETRO_OPCIONAL:
        if parametro is not None and parametro not in _codigos_desastre():
            return None, ("Tipo de desastre no valido en parametro (o dejalo vacio para cualquiera): "
                          + ", ".join(_codigos_desastre()))
    elif parametro is not None:
        return None, f"La condicion {condicion} no lleva parametro: dejalo vacio (null)"
    return parametro, None


def _validar_logro(data, parcial, actual=None):
    """Devuelve (valores, error). Con parcial=True solo valida los campos que llegan; `actual` es el logro
    que se edita (el codigo no se puede cambiar y condicion/parametro se revisan juntos)."""
    if not isinstance(data, dict):
        # Un cuerpo JSON que no es objeto ([1, 2], "texto") rompia data.get(...) con un 500
        return None, "El cuerpo debe ser un objeto JSON con los campos del logro"
    valores = {}
    if not parcial:
        codigo = str(data.get("codigo") or "").strip()
        if not codigo or len(codigo) > LIMITES_LOGRO["codigo"] or not _CODIGO_LOGRO.match(codigo):
            return None, "codigo es obligatorio: MAYUSCULAS_CON_GUION_BAJO, solo letras A-Z y numeros (maximo 50)"
        if Logro.objects.filter(codigo=codigo).exists():
            return None, f"Ya existe un logro con el codigo {codigo}"
        valores["codigo"] = codigo
    elif "codigo" in data and data["codigo"] != actual.codigo:
        return None, "El codigo no se puede cambiar una vez creado el logro"
    for campo, etiqueta in (("titulo", "El titulo"), ("descripcion", "La descripcion"), ("icono", "El icono")):
        if not parcial or campo in data:
            texto, error = _validar_texto_logro(data, campo, etiqueta, LIMITES_LOGRO[campo])
            if error:
                return None, error
            valores[campo] = texto
    if not parcial or "rareza" in data:
        rareza = data.get("rareza", "COMUN")
        if not isinstance(rareza, str) or rareza not in Logro.RAREZAS:
            return None, "Rareza no valida: " + ", ".join(Logro.RAREZAS)
        valores["rareza"] = rareza
    if not parcial or "condicion" in data:
        if not isinstance(data.get("condicion"), str) or data.get("condicion") not in Logro.CONDICIONES:
            return None, "Condicion no valida: " + ", ".join(Logro.CONDICIONES)
        valores["condicion"] = data["condicion"]
    enteros = (("meta", 1, LIMITES_LOGRO["meta_max"]), ("recompensa_xp", 0, LIMITES_LOGRO["recompensa_xp_max"]),
               ("recompensa_monedas", 0, LIMITES_LOGRO["recompensa_monedas_max"]),
               ("orden", None, LIMITES_LOGRO["orden_max"]))
    for campo, minimo, maximo in enteros:
        if campo in data or (not parcial and campo == "meta"):
            valor = data.get(campo)
            minimo = -maximo if minimo is None else minimo
            if isinstance(valor, bool) or not isinstance(valor, int) or not minimo <= valor <= maximo:
                return None, f"{campo} debe ser un entero entre {minimo} y {maximo}"
            valores[campo] = valor
    for campo in ("secreto", "activo"):
        if campo in data:
            if not isinstance(data[campo], bool):
                return None, f"{campo} debe ser verdadero o falso"
            valores[campo] = data[campo]
    # condicion y parametro van juntos: al editar solo uno se revisa contra el valor que ya tenia el otro
    if not parcial or "condicion" in data or "parametro" in data:
        condicion = valores.get("condicion") or (actual.condicion if actual else None)
        bruto = data["parametro"] if "parametro" in data else (actual.parametro if actual else None)
        parametro, error = _validar_parametro_logro(condicion, bruto)
        if error:
            return None, error
        valores["parametro"] = parametro
    return valores, None


class AdminLogroListCreateView(AdminAPIView):
    """GET /api/admin/logros/ lista todos (activos e inactivos) con `desbloqueos`; POST crea uno."""

    def get(self, request):
        if _admin_desde_request(request) is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        conteo = _desbloqueos_por_logro()
        logros = Logro.objects.order_by("orden", "id_logro")
        return Response([_logro_a_dict(lg, conteo.get(lg.id_logro, 0)) for lg in logros])

    def post(self, request):
        if _admin_desde_request(request) is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        valores, error = _validar_logro(request.data, parcial=False)
        if error:
            return Response({"error": error}, status=status.HTTP_400_BAD_REQUEST)
        try:
            logro = Logro.objects.create(**valores)
        except IntegrityError:
            return Response({"error": f"Ya existe un logro con el codigo {valores['codigo']}"},
                            status=status.HTTP_400_BAD_REQUEST)
        return Response(_logro_a_dict(logro), status=status.HTTP_201_CREATED)


class AdminLogroDetailView(AdminAPIView):
    """PATCH /api/admin/logros/<id>/ edita; para quitarlo se envia {"activo": false}. Nunca se borra (se
    conserva el progreso de los usuarios) y no se toca logros_usuario: los ya desbloqueados siguen asi."""

    def patch(self, request, pk):
        if _admin_desde_request(request) is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        logro = Logro.objects.filter(pk=pk).first()
        if logro is None:
            return Response({"error": "Logro no encontrado"}, status=status.HTTP_404_NOT_FOUND)
        valores, error = _validar_logro(request.data, parcial=True, actual=logro)
        if error:
            return Response({"error": error}, status=status.HTTP_400_BAD_REQUEST)
        cambia_regla = any(c in valores and valores[c] != getattr(logro, c) for c in ("condicion", "parametro"))
        for campo, valor in valores.items():
            setattr(logro, campo, valor)
        if valores:
            logro.save(update_fields=list(valores))
        desbloqueos = logro.desbloqueos()
        cuerpo = _logro_a_dict(logro, desbloqueos)
        if cambia_regla and desbloqueos:
            cuerpo["advertencia"] = (
                f"{desbloqueos} usuario(s) ya desbloquearon este logro y lo conservan. "
                "Cambiar la condicion o el parametro solo afecta a quienes aun no lo tienen; "
                "el progreso ya guardado no se recalcula."
            )
        return Response(cuerpo)


class AdminLogroOpcionesView(AdminAPIView):
    """GET /api/admin/logros/opciones/ lo que necesita el formulario: condiciones (con su explicacion y
    parametro), rarezas, iconos sugeridos y limites."""

    def get(self, request):
        if _admin_desde_request(request) is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        condiciones = []
        for codigo, explicacion in Logro.CONDICIONES.items():
            if codigo == Logro.PARAMETRO_OBLIGATORIO:
                parametro, valores = "obligatorio", list(Logro.FRANJAS_HORARIAS)
            elif codigo == Logro.PARAMETRO_OPCIONAL:
                parametro, valores = "opcional", _codigos_desastre()
            else:
                parametro, valores = "ninguno", []
            condiciones.append({
                "codigo": codigo, "explicacion": explicacion, "parametro": parametro,
                "requiere_parametro": parametro == "obligatorio", "valores_permitidos": valores,
            })
        return Response({
            "condiciones": condiciones, "rarezas": list(Logro.RAREZAS),
            "iconos": ICONOS_LOGRO, "limites": LIMITES_LOGRO,
        })


# Parametros de juego que Spring lee de verdad, con el rango que acepta el admin (minimo, maximo)
PARAMETROS_JUEGO = {
    "COSTO_REVIVIR": (0, 100000),
    "HORAS_RECUPERACION": (1, 720),
    "ABONO_RECUPERACION": (1, 100),
    "SALUD_REVIVIDA": (1, 100),
    # Plantas virtuales activas; una pantalla de la coleccion tiene 6, asi que va de a una pantalla
    "MAX_PLANTAS_ACTIVAS": (6, 60),
}


def _parametro_a_dict(p):
    return {"clave": p.clave, "valor": p.valor, "descripcion": p.descripcion}


class AdminParametrosView(AdminAPIView):
    """GET /api/admin/parametros/  los valores ajustables del juego (costo de revivir, plazos...)."""

    def get(self, request):
        if _admin_desde_request(request) is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        filas = ParametroJuego.objects.filter(clave__in=list(PARAMETROS_JUEGO)).order_by("clave")
        return Response([_parametro_a_dict(p) for p in filas])


class AdminParametroDetailView(AdminAPIView):
    """PATCH /api/admin/parametros/<clave>/  {valor} cambia un parametro dentro de su rango."""

    def patch(self, request, clave):
        if _admin_desde_request(request) is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        if clave not in PARAMETROS_JUEGO:
            return Response({"error": "Parametro no encontrado"}, status=status.HTTP_404_NOT_FOUND)
        parametro = ParametroJuego.objects.filter(pk=clave).first()
        if parametro is None:
            return Response({"error": "Parametro no encontrado"}, status=status.HTTP_404_NOT_FOUND)
        minimo, maximo = PARAMETROS_JUEGO[clave]
        valor = request.data.get("valor")
        if isinstance(valor, bool) or not isinstance(valor, int) or not minimo <= valor <= maximo:
            return Response({"error": f"valor debe ser un entero entre {minimo} y {maximo}"},
                            status=status.HTTP_400_BAD_REQUEST)
        parametro.valor = valor
        parametro.save(update_fields=["valor"])
        return Response(_parametro_a_dict(parametro))


# Nombre que ve el admin de cada regla (el motivo es el codigo que usa Spring)
NOMBRES_REGLAS = {
    "ESCANEO_NUEVA": "Escanear una especie nueva",
    "ESCANEO_REPETIDO": "Escanear una especie ya escaneada",
    "DIGITALIZAR": "Digitalizar una planta nueva",
    "REGAR": "Regar (cuidado util)",
    "SOL": "Dar luz (cuidado util)",
    "ABONAR": "Abonar (cuidado util)",
    "RACHA": "Racha diaria",
    "REFUGIO": "Prevenir el evento en una planta",
    "EVENTO_COMPLETO": "Completar un evento (prevenirlo en todas las plantas)",
    "MISION": "Mision (cada una paga lo suyo)",
}


def _regla_a_dict(r):
    return {
        "id_regla": r.id_regla, "motivo": r.motivo, "nombre": NOMBRES_REGLAS.get(r.motivo, r.motivo),
        "xp": r.xp, "monedas": r.monedas, "tope_diario": r.tope_diario, "activa": r.activa,
    }


def _validar_regla(data):
    valores = {}
    for campo in ("xp", "monedas"):
        if campo in data:
            valor = data[campo]
            if isinstance(valor, bool) or not isinstance(valor, int) or not 0 <= valor <= 100000:
                return None, f"{campo} debe ser un entero entre 0 y 100000"
            valores[campo] = valor
    if "tope_diario" in data:
        valor = data["tope_diario"]
        if valor is not None and (isinstance(valor, bool) or not isinstance(valor, int) or valor < 1):
            return None, "tope_diario debe ser un entero >= 1 (o vacio para quitar el tope)"
        valores["tope_diario"] = valor
    if "activa" in data:
        if not isinstance(data["activa"], bool):
            return None, "activa debe ser verdadero o falso"
        valores["activa"] = data["activa"]
    return valores, None


class AdminReglasView(AdminAPIView):
    """GET /api/admin/reglas/  lo que paga cada accion del juego, en hojas y XP."""

    def get(self, request):
        if _admin_desde_request(request) is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        return Response([_regla_a_dict(r) for r in ReglaProgreso.objects.order_by("id_regla")])


class AdminReglaDetailView(AdminAPIView):
    """PATCH /api/admin/reglas/<id>/  {xp, monedas, tope_diario, activa}. El motivo no se cambia."""

    def patch(self, request, pk):
        if _admin_desde_request(request) is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        regla = ReglaProgreso.objects.filter(pk=pk).first()
        if regla is None:
            return Response({"error": "Regla no encontrada"}, status=status.HTTP_404_NOT_FOUND)
        valores, error = _validar_regla(request.data)
        if error:
            return Response({"error": error}, status=status.HTTP_400_BAD_REQUEST)
        for campo, valor in valores.items():
            setattr(regla, campo, valor)
        if valores:
            regla.save(update_fields=list(valores))
        return Response(_regla_a_dict(regla))


def _tipo_a_dict(t):
    return {
        "id_tipo_desastre": t.id_tipo_desastre, "codigo": t.codigo, "nombre": t.nombre,
        "descripcion": t.descripcion, "consejo": t.consejo, "imagen_url": t.imagen_url,
        "color_hex": t.color_hex, "activo": t.activo,
        "salud_final_bajo": t.salud_final_bajo, "salud_final_moderado": t.salud_final_moderado,
        "salud_final_critico": t.salud_final_critico,
    }


def _validar_tipo(data, tipo):
    """Devuelve (valores, error) con lo que se puede cambiar de un tipo de evento."""
    valores = {}
    for campo in ("descripcion", "consejo"):
        if campo in data and data[campo] is not None and not isinstance(data[campo], str):
            return None, f"{campo} debe ser un texto"
    if "descripcion" in data:
        texto = str(data.get("descripcion") or "").strip()
        if len(texto) > 2000:
            return None, "El texto informativo no puede pasar de 2000 caracteres"
        valores["descripcion"] = texto or None
    if "consejo" in data:
        texto = str(data.get("consejo") or "").strip()
        if len(texto) > 250:
            return None, "El consejo no puede pasar de 250 caracteres"
        valores["consejo"] = texto or None
    salud = {}
    for campo in ("salud_final_bajo", "salud_final_moderado", "salud_final_critico"):
        if campo in data:
            valor = _entero(data[campo])
            if isinstance(valor, bool) or not isinstance(valor, int) or not 1 <= valor <= 100:
                return None, f"{campo} debe ser un entero entre 1 y 100"
            valores[campo] = valor
        salud[campo] = valores.get(campo, getattr(tipo, campo))
    # Una severidad mayor nunca deja mas salud que una menor
    if not salud["salud_final_critico"] <= salud["salud_final_moderado"] <= salud["salud_final_bajo"]:
        return None, "La salud final debe bajar con la severidad: critico <= moderado <= bajo"
    return valores, None


class AdminTiposDesastreView(AdminAPIView):
    """GET /api/admin/tipos-desastre/  los tipos de evento con su texto, consejo, imagen y salud final."""

    def get(self, request):
        if _admin_desde_request(request) is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        return Response([_tipo_a_dict(t) for t in TipoDesastre.objects.order_by("id_tipo_desastre")])


class AdminTipoDesastreDetailView(AdminAPIView):
    """PATCH /api/admin/tipos-desastre/<id>/  cambia el texto, el consejo, la salud final o la imagen
    (campo `imagen`, un archivo; por eso acepta tambien formularios con archivos)."""

    parser_classes = [JSONParser, FormParser, MultiPartParser]

    def patch(self, request, pk):
        if _admin_desde_request(request) is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        tipo = TipoDesastre.objects.filter(pk=pk).first()
        if tipo is None:
            return Response({"error": "Tipo de evento no encontrado"}, status=status.HTTP_404_NOT_FOUND)
        valores, error = _validar_tipo(request.data, tipo)
        if error:
            return Response({"error": error}, status=status.HTTP_400_BAD_REQUEST)
        archivo = request.FILES.get("imagen")
        if archivo is not None:
            try:
                jpg = eventos_imagen.procesar(archivo.read())
            except eventos_imagen.ErrorImagen as exc:
                return Response({"error": str(exc)}, status=status.HTTP_400_BAD_REQUEST)
            nombre = eventos_imagen.guardar(os.path.join(settings.STORAGE_IMAGES_DIR, "eventos"), tipo.codigo, jpg)
            valores["imagen_url"] = f"/files/eventos/{nombre}"
        for campo, valor in valores.items():
            setattr(tipo, campo, valor)
        if valores:
            tipo.save(update_fields=list(valores))
        return Response(_tipo_a_dict(tipo))


MOMENTOS = ("DIA", "TARDE", "NOCHE")


def _entero(valor):
    """Los formularios con archivos llegan como texto: "100" pasa a 100; lo demas queda igual."""
    if isinstance(valor, str) and valor.strip().lstrip("-").isdigit():
        return int(valor.strip())
    return valor


def _directorio_fondos():
    return os.path.join(settings.STORAGE_IMAGES_DIR, "fondos")


def _variantes_por_tema(ids):
    variantes = {}
    if ids:
        for f in FondoEscena.objects.filter(id_tema__in=ids, activo=True):
            variantes.setdefault(f.id_tema, []).append(
                {"momento": f.momento, "vista": f.vista_url or f.imagen_url, "cuadros": f.total_frames, "ms": f.ms_por_frame})
        for lista in variantes.values():
            lista.sort(key=lambda v: MOMENTOS.index(v["momento"]) if v["momento"] in MOMENTOS else 9)
    return variantes


def _item_a_dict(i, variantes):
    return {
        "id_item": i.id_item, "nombre": i.nombre, "tipo": i.tipo, "descripcion": i.descripcion,
        "precio_monedas": i.precio_monedas, "nivel_requerido": i.nivel_requerido, "activo": i.activo,
        "id_tema": i.id_tema, "vista": i.asset_url, "variantes": variantes.get(i.id_tema, []),
    }


def _validar_item(data, parcial):
    """Devuelve (valores, error). Con parcial=True solo valida los campos que llegan."""
    valores = {}
    if not parcial or "nombre" in data:
        nombre = data.get("nombre")
        nombre = nombre.strip() if isinstance(nombre, str) else ""
        if not nombre or len(nombre) > 100:
            return None, "El nombre es obligatorio (maximo 100 caracteres)"
        valores["nombre"] = nombre
    if not parcial or "precio_monedas" in data:
        precio = _entero(data.get("precio_monedas"))
        if not _es_entero(precio, 0, 1000000):
            return None, "El precio debe ser un entero entre 0 y 1000000"
        valores["precio_monedas"] = precio
    if "nivel_requerido" in data or not parcial:
        nivel = _entero(data.get("nivel_requerido", 1))
        if not _es_entero(nivel, 1, 1000):
            return None, "El nivel requerido debe ser un entero entre 1 y 1000"
        valores["nivel_requerido"] = nivel
    if "descripcion" in data:
        if data["descripcion"] is not None and not isinstance(data["descripcion"], str):
            return None, "La descripcion debe ser un texto"
        descripcion = (data["descripcion"] or "").strip()
        if len(descripcion) > 200:
            return None, "La descripcion admite hasta 200 caracteres"
        valores["descripcion"] = descripcion or None
    if "activo" in data:
        activo = data["activo"]
        if isinstance(activo, str):
            activo = {"true": True, "false": False}.get(activo.lower(), activo)
        if not isinstance(activo, bool):
            return None, "activo debe ser verdadero o falso"
        valores["activo"] = activo
    return valores, None


def _procesar_escenas(request):
    """Lee las imagenes subidas (campos dia, tarde y noche; opcionales cuadros_<momento> y ms) y las convierte
    a tiras. Devuelve ({momento: resultado}, error)."""
    escenas = {}
    ms = _entero(request.data.get("ms")) if request.data.get("ms") not in (None, "") else None
    if ms is not None and (isinstance(ms, bool) or not isinstance(ms, int)):
        return None, "ms debe ser un entero"
    for momento in MOMENTOS:
        archivo = request.FILES.get(momento.lower())
        if archivo is None:
            continue
        cuadros = _entero(request.data.get(f"cuadros_{momento.lower()}")) if request.data.get(f"cuadros_{momento.lower()}") not in (None, "") else None
        if cuadros is not None and (isinstance(cuadros, bool) or not isinstance(cuadros, int) or cuadros < 1):
            return None, f"cuadros_{momento.lower()} debe ser un entero >= 1"
        try:
            escenas[momento] = fondos_imagen.procesar(archivo.read(), cuadros=cuadros, ms=ms)
        except fondos_imagen.ErrorImagen as e:
            return None, f"Escena de {momento.lower()}: {e}"
    return escenas, None


def _guardar_escena(tema, momento, resultado):
    """Guarda los archivos de la escena y crea o reemplaza su fila en fondos_escena."""
    tira, vista = fondos_imagen.guardar(_directorio_fondos(), f"tema{tema.id_tema}_{momento.lower()}", resultado)
    etiqueta = {"DIA": "dia", "TARDE": "tarde", "NOCHE": "noche"}[momento]
    FondoEscena.objects.update_or_create(
        id_tema=tema.id_tema, momento=momento,
        defaults=dict(
            nombre=f"{tema.nombre} ({etiqueta})", imagen_url=f"/files/fondos/{tira}", vista_url=f"/files/fondos/{vista}",
            total_frames=resultado["cuadros"], ancho_px=resultado["lado"], alto_px=resultado["lado"],
            ms_por_frame=resultado["ms"], es_predeterminado=False, activo=True))


def _vista_principal(tema):
    """Vista previa del articulo: la del dia, o la primera escena que tenga el tema."""
    escenas = {f.momento: f for f in FondoEscena.objects.filter(id_tema=tema.id_tema, activo=True)}
    for momento in MOMENTOS:
        if momento in escenas:
            return escenas[momento].vista_url or escenas[momento].imagen_url
    return ""


class AdminTiendaListCreateView(AdminAPIView):
    """GET /api/admin/tienda/ lista todos los productos (activos o no) con las escenas de cada fondo;
    POST (multipart) agrega un tema de fondo a la venta: nombre, precio_monedas, nivel_requerido, descripcion
    y las imagenes dia, tarde y noche (GIF/WebP/APNG animados o tiras PNG con cuadros_<momento>)."""

    parser_classes = [JSONParser, MultiPartParser, FormParser]

    def get(self, request):
        if _admin_desde_request(request) is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        items = list(ItemTienda.objects.order_by("tipo", "id_item"))
        variantes = _variantes_por_tema({i.id_tema for i in items if i.id_tema})
        return Response({"items": [_item_a_dict(i, variantes) for i in items]})

    def post(self, request):
        if _admin_desde_request(request) is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        if request.data.get("tipo", "FONDO") != "FONDO":
            return Response({"error": "Por ahora solo se pueden agregar fondos"}, status=status.HTTP_400_BAD_REQUEST)
        valores, error = _validar_item(request.data, parcial=False)
        if error:
            return Response({"error": error}, status=status.HTTP_400_BAD_REQUEST)
        escenas, error = _procesar_escenas(request)
        if error:
            return Response({"error": error}, status=status.HTTP_400_BAD_REQUEST)
        if not escenas:
            return Response({"error": "Sube al menos una escena (dia, tarde o noche)"}, status=status.HTTP_400_BAD_REQUEST)
        with transaction.atomic():
            tema = TemaFondo.objects.create(nombre=valores["nombre"], descripcion=valores.get("descripcion"))
            for momento, resultado in escenas.items():
                _guardar_escena(tema, momento, resultado)
            item = ItemTienda.objects.create(tipo="FONDO", id_tema=tema.id_tema, asset_url=_vista_principal(tema), **valores)
        return Response(_item_a_dict(item, _variantes_por_tema({tema.id_tema})), status=status.HTTP_201_CREATED)


class AdminTiendaDetailView(AdminAPIView):
    """PATCH /api/admin/tienda/<id>/ cambia nombre, descripcion, precio, nivel o activo, y puede reemplazar las
    imagenes de dia, tarde o noche (multipart). Quitar un producto es {"activo": false}: no se borra, asi quienes
    ya lo compraron lo conservan y se puede reactivar."""

    parser_classes = [JSONParser, MultiPartParser, FormParser]

    def patch(self, request, pk):
        if _admin_desde_request(request) is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        item = ItemTienda.objects.filter(pk=pk).first()
        if item is None:
            return Response({"error": "Producto no encontrado"}, status=status.HTTP_404_NOT_FOUND)
        valores, error = _validar_item(request.data, parcial=True)
        if error:
            return Response({"error": error}, status=status.HTTP_400_BAD_REQUEST)
        escenas, error = _procesar_escenas(request)
        if error:
            return Response({"error": error}, status=status.HTTP_400_BAD_REQUEST)
        if valores.get("activo") and item.tipo == "FONDO" and not item.id_tema:
            return Response({"error": "Este fondo no tiene escenas: no se puede poner a la venta"}, status=status.HTTP_400_BAD_REQUEST)
        if escenas and not item.id_tema:
            return Response({"error": "Este producto no tiene un tema de fondo"}, status=status.HTTP_400_BAD_REQUEST)
        with transaction.atomic():
            for campo, valor in valores.items():
                setattr(item, campo, valor)
            tema = TemaFondo.objects.filter(pk=item.id_tema).first() if item.id_tema else None
            if tema is not None and ("nombre" in valores or "descripcion" in valores):
                tema.nombre = valores.get("nombre", tema.nombre)
                tema.descripcion = valores.get("descripcion", tema.descripcion)
                tema.save(update_fields=["nombre", "descripcion"])
            for momento, resultado in escenas.items():
                _guardar_escena(tema, momento, resultado)
            campos = list(valores)
            if escenas:
                item.asset_url = _vista_principal(tema)
                campos.append("asset_url")
            if campos:
                item.save(update_fields=campos)
        return Response(_item_a_dict(item, _variantes_por_tema({item.id_tema} if item.id_tema else set())))


class AdminEventoEfectosView(AdminAPIView):
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
