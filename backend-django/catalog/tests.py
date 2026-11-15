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
