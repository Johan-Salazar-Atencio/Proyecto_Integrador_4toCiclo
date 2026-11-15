"""Autenticacion del panel admin (admin_views.py). Sin base de datos: se simula el usuario."""
from datetime import timedelta
from unittest.mock import patch

from django.test import SimpleTestCase
from django.utils import timezone
from rest_framework.test import APIRequestFactory

from . import admin_views
from .models import Usuario

TOKEN = "a" * 32


def _usuario(rol="ADMIN", token=TOKEN, expira_en=timedelta(days=1)):
    return Usuario(
        id_usuario=8,
        rol=rol,
        session_token=admin_views._hash_token(token) if token else None,
        session_expira=timezone.now() + expira_en,
    )


class AdminDesdeRequestTests(SimpleTestCase):
    def _admin(self, usuario, headers):
        request = APIRequestFactory().get("/api/admin/stats/", **headers)
        with patch.object(Usuario.objects, "get", return_value=usuario):
            return admin_views._admin_desde_request(request)

    def test_id_y_token_validos(self):
        self.assertIsNotNone(self._admin(_usuario(), {"HTTP_X_ADMIN_ID": "8", "HTTP_X_ADMIN_TOKEN": TOKEN}))

    def test_solo_el_id_ya_no_basta(self):
        self.assertIsNone(self._admin(_usuario(), {"HTTP_X_ADMIN_ID": "8"}))

    def test_token_incorrecto(self):
        self.assertIsNone(self._admin(_usuario(), {"HTTP_X_ADMIN_ID": "8", "HTTP_X_ADMIN_TOKEN": "b" * 32}))

    def test_el_hash_de_la_bd_no_sirve_como_token(self):
        hash_guardado = admin_views._hash_token(TOKEN)
        self.assertIsNone(self._admin(_usuario(), {"HTTP_X_ADMIN_ID": "8", "HTTP_X_ADMIN_TOKEN": hash_guardado}))

    def test_sesion_caducada(self):
        usuario = _usuario(expira_en=timedelta(minutes=-1))
        self.assertIsNone(self._admin(usuario, {"HTTP_X_ADMIN_ID": "8", "HTTP_X_ADMIN_TOKEN": TOKEN}))

    def test_usuario_que_no_es_admin(self):
        usuario = _usuario(rol="USER")
        self.assertIsNone(self._admin(usuario, {"HTTP_X_ADMIN_ID": "8", "HTTP_X_ADMIN_TOKEN": TOKEN}))

    def test_fecha_sin_zona_se_lee_como_utc(self):
        naive = timezone.make_naive(timezone.now() + timedelta(hours=1), admin_views.dt_timezone.utc)
        self.assertTrue(admin_views._como_aware(naive) > timezone.now())


class CrearEventoTiposTests(SimpleTestCase):
    """AdminEventoCreateView: el tipo se valida contra tipos_desastre, la region se crea si falta."""

    def _post(self, datos, tipo, region_creada=False):
        from unittest.mock import MagicMock

        from .models import Region

        request = APIRequestFactory().post("/api/admin/eventos/", datos, format="json")
        region = Region(id_region=9, codigo=datos.get("region") or "GLOBAL", nombre="x")
        with patch.object(admin_views, "_admin_desde_request", return_value=_usuario()), patch.object(
            admin_views.TipoDesastre.objects, "filter", return_value=MagicMock(first=lambda: tipo)
        ), patch.object(
            admin_views.Region.objects, "get_or_create", return_value=(region, region_creada)
        ), patch.object(
            admin_views.EventoDesastre.objects, "create", return_value=MagicMock(id_desastre=5, estado="ACTIVO")
        ) as crear:
            respuesta = admin_views.AdminEventoCreateView.as_view()(request)
        return respuesta, crear

    DATOS = {
        "tipo_evento": "HUAYCO",
        "region": "Arequipa",
        "mensaje_notificacion": "m",
        "fecha_inicio": "2026-11-15T10:00:00Z",
        "fecha_fin": "2026-11-15T16:00:00Z",
    }

    def test_tipo_inexistente_devuelve_400(self):
        respuesta, crear = self._post(self.DATOS, tipo=None)
        self.assertEqual(respuesta.status_code, 400)
        crear.assert_not_called()

    def test_tipo_valido_guarda_texto_y_llaves(self):
        from .models import TipoDesastre

        tipo = TipoDesastre(id_tipo_desastre=1, codigo="HUAYCO", nombre="Huayco")
        respuesta, crear = self._post(self.DATOS, tipo=tipo, region_creada=True)
        self.assertEqual(respuesta.status_code, 201)
        argumentos = crear.call_args.kwargs
        self.assertEqual(argumentos["tipo_evento"], "HUAYCO")
        self.assertIs(argumentos["id_tipo_desastre"], tipo)
        self.assertEqual(argumentos["region"], "Arequipa")
        self.assertEqual(argumentos["id_region"].id_region, 9)


class EventoSerializerTests(SimpleTestCase):
    def test_evento_sin_tipo_enlazado_no_falla(self):
        from datetime import datetime

        from .models import EventoDesastre
        from .serializers import EventoDesastreSerializer

        evento = EventoDesastre(
            id_desastre=1, tipo_evento="HUAYCO", region="GLOBAL", mensaje_notificacion="m",
            fecha_inicio=datetime(2026, 11, 15), fecha_fin=datetime(2026, 11, 16),
        )
        datos = EventoDesastreSerializer(evento).data
        self.assertIsNone(datos["tipo_nombre"])
        self.assertEqual(datos["tipo_evento"], "HUAYCO")


class EventoActivoVigenciaTests(SimpleTestCase):
    """EventoActivoView solo devuelve eventos vigentes (estado, fechas y region)."""

    def _get(self, query="", evento=None):
        from unittest.mock import MagicMock

        from . import views

        qs = MagicMock()
        qs.filter.return_value = qs
        qs.order_by.return_value.first.return_value = evento
        request = APIRequestFactory().get("/api/catalog/eventos/activo/" + query)
        with patch.object(views.EventoDesastre.objects, "filter", return_value=qs) as filtrar:
            respuesta = views.EventoActivoView.as_view()(request)
        return respuesta, filtrar, qs

    def test_filtra_por_estado_y_ventana_de_fechas(self):
        respuesta, filtrar, _ = self._get()
        argumentos = filtrar.call_args.kwargs
        self.assertEqual(argumentos["estado"], "ACTIVO")
        self.assertIn("fecha_inicio__lte", argumentos)
        self.assertIn("fecha_fin__gt", argumentos)
        self.assertEqual(respuesta.status_code, 204)

    def test_sin_region_no_agrega_filtro_de_region(self):
        _, _, qs = self._get()
        qs.filter.assert_not_called()

    def test_con_region_agrega_el_filtro(self):
        _, _, qs = self._get("?region=Arequipa")
        qs.filter.assert_called_once()
