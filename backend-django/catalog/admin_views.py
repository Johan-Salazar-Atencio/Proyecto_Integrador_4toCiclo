"""Endpoints de administracion (escritura) consumidos por el panel React.

Autenticacion: el login devuelve un token de sesion; en `usuarios.session_token`
solo se guarda su SHA-256, con caducidad en `session_expira` (mismo esquema que
TokenSesion.java en Spring). Cada peticion de escritura debe traer `X-Admin-Id`
y `X-Admin-Token`; se comprueba el token, que no haya caducado y el rol ADMIN.
"""
import hashlib
import hmac
import os
import secrets
from datetime import timedelta

import bcrypt
from django.conf import settings
from django.db import transaction
from django.utils import timezone
from rest_framework import status
from rest_framework.parsers import FormParser, JSONParser, MultiPartParser
from rest_framework.response import Response
from rest_framework.views import APIView

from . import eventos_imagen, fondos_imagen
from .models import (
    EventoDesastre, FondoEscena, ItemTienda, Mision, ParametroJuego, PlantaCatalogo, Region,
    RegistroDesastreUsuario, ReglaProgreso, TemaFondo, TipoDesastre, Usuario,
)

# Hash BCrypt valido para comparar cuando el correo no existe: asi el tiempo de
# respuesta no revela si esta registrado (mismo criterio que AuthService de Spring).
_HASH_FICTICIO = b"$2a$10$n7uGz2GzFEIhdSm3KDEOYO0ZMzGL132epZ61oqWefVWQYQIufMQVy"
_CREDENCIALES_INVALIDAS = {"error": "Credenciales invalidas"}
_DURACION_SESION = timedelta(days=30)


def _como_aware(valor):
    """session_expira es TIMESTAMP sin zona: Spring y Django (DATABASES TIME_ZONE) la guardan en hora de Lima,
    asi que si llega sin zona es hora de Lima."""
    if timezone.is_naive(valor):
        return timezone.make_aware(valor, timezone.get_default_timezone())
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
                id_tipo_desastre=tipo,
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


class AdminParametrosView(APIView):
    """GET /api/admin/parametros/  los valores ajustables del juego (costo de revivir, plazos...)."""

    def get(self, request):
        if _admin_desde_request(request) is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        filas = ParametroJuego.objects.filter(clave__in=list(PARAMETROS_JUEGO)).order_by("clave")
        return Response([_parametro_a_dict(p) for p in filas])


class AdminParametroDetailView(APIView):
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


class AdminReglasView(APIView):
    """GET /api/admin/reglas/  lo que paga cada accion del juego, en hojas y XP."""

    def get(self, request):
        if _admin_desde_request(request) is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        return Response([_regla_a_dict(r) for r in ReglaProgreso.objects.order_by("id_regla")])


class AdminReglaDetailView(APIView):
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


class AdminTiposDesastreView(APIView):
    """GET /api/admin/tipos-desastre/  los tipos de evento con su texto, consejo, imagen y salud final."""

    def get(self, request):
        if _admin_desde_request(request) is None:
            return Response({"error": "Se requiere un administrador"}, status=status.HTTP_401_UNAUTHORIZED)
        return Response([_tipo_a_dict(t) for t in TipoDesastre.objects.order_by("id_tipo_desastre")])


class AdminTipoDesastreDetailView(APIView):
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
        nombre = str(data.get("nombre") or "").strip()
        if not nombre or len(nombre) > 100:
            return None, "El nombre es obligatorio (maximo 100 caracteres)"
        valores["nombre"] = nombre
    if not parcial or "precio_monedas" in data:
        precio = _entero(data.get("precio_monedas"))
        if isinstance(precio, bool) or not isinstance(precio, int) or precio < 0:
            return None, "El precio debe ser un entero >= 0"
        valores["precio_monedas"] = precio
    if "nivel_requerido" in data or not parcial:
        nivel = _entero(data.get("nivel_requerido", 1))
        if isinstance(nivel, bool) or not isinstance(nivel, int) or nivel < 1:
            return None, "El nivel requerido debe ser un entero >= 1"
        valores["nivel_requerido"] = nivel
    if "descripcion" in data:
        descripcion = str(data["descripcion"] or "").strip()
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


class AdminTiendaListCreateView(APIView):
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


class AdminTiendaDetailView(APIView):
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
        if valores.get("activo") and item.tipo == "MACETA":
            return Response({"error": "Las macetas aun no estan disponibles"}, status=status.HTTP_400_BAD_REQUEST)
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
