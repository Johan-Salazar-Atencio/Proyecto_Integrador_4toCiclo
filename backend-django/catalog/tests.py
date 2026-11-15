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


class AdminMisionesTests(SimpleTestCase):
    """CRUD de misiones del admin: validacion, permisos y desactivar en lugar de borrar."""

    DATOS = {"titulo": "Cuidador", "tipo": "RIEGO", "meta": 3, "recompensa_xp": 15}

    def _llamar(self, vista, metodo, datos=None, admin=True, **kwargs):
        fabrica = getattr(APIRequestFactory(), metodo)
        request = fabrica("/api/admin/misiones/", datos, format="json")
        with patch.object(admin_views, "_admin_desde_request", return_value=_usuario() if admin else None):
            return vista.as_view()(request, **kwargs)

    def test_sin_admin_devuelve_401(self):
        respuesta = self._llamar(admin_views.AdminMisionListCreateView, "post", self.DATOS, admin=False)
        self.assertEqual(respuesta.status_code, 401)

    def test_crear_valida_tipo(self):
        datos = dict(self.DATOS, tipo="VOLAR")
        respuesta = self._llamar(admin_views.AdminMisionListCreateView, "post", datos)
        self.assertEqual(respuesta.status_code, 400)

    def test_crear_exige_meta_positiva(self):
        respuesta = self._llamar(admin_views.AdminMisionListCreateView, "post", dict(self.DATOS, meta=0))
        self.assertEqual(respuesta.status_code, 400)

    def test_crear_rechaza_recompensa_negativa(self):
        respuesta = self._llamar(admin_views.AdminMisionListCreateView, "post", dict(self.DATOS, recompensa_xp=-1))
        self.assertEqual(respuesta.status_code, 400)

    def test_crear_guarda_la_mision(self):
        from unittest.mock import MagicMock

        mision = admin_views.Mision(id_mision=7, titulo="Cuidador", tipo="RIEGO", meta=3)
        with patch.object(admin_views.Mision.objects, "create", return_value=mision) as crear:
            respuesta = self._llamar(admin_views.AdminMisionListCreateView, "post", self.DATOS)
        self.assertEqual(respuesta.status_code, 201)
        self.assertEqual(crear.call_args.kwargs["meta"], 3)
        self.assertNotIn("predeterminada", crear.call_args.kwargs)

    def test_desactivar_solo_cambia_activa(self):
        from unittest.mock import MagicMock

        mision = admin_views.Mision(id_mision=1, titulo="Riego", tipo="RIEGO", meta=2, activa=True)
        mision.save = MagicMock()
        with patch.object(admin_views.Mision.objects, "filter", return_value=MagicMock(first=lambda: mision)):
            respuesta = self._llamar(admin_views.AdminMisionDetailView, "patch", {"activa": False}, pk=1)
        self.assertEqual(respuesta.status_code, 200)
        self.assertFalse(respuesta.data["activa"])
        mision.save.assert_called_once_with(update_fields=["activa"])

    def test_editar_mision_inexistente_devuelve_404(self):
        from unittest.mock import MagicMock

        with patch.object(admin_views.Mision.objects, "filter", return_value=MagicMock(first=lambda: None)):
            respuesta = self._llamar(admin_views.AdminMisionDetailView, "patch", {"activa": False}, pk=99)
        self.assertEqual(respuesta.status_code, 404)


class RegionListTests(SimpleTestCase):
    """RegionListView: solo regiones activas, con el codigo que guardan usuarios y eventos."""

    def test_devuelve_codigo_y_nombre_de_las_activas(self):
        from unittest.mock import MagicMock

        from . import views
        from .models import Region

        activas = [Region(id_region=1, codigo="GLOBAL", nombre="Global"),
                   Region(id_region=3, codigo="LIMA", nombre="Lima Metropolitana")]
        qs = MagicMock()
        qs.order_by.return_value = activas
        request = APIRequestFactory().get("/api/catalog/regiones/")
        with patch.object(views.Region.objects, "filter", return_value=qs) as filtrar:
            respuesta = views.RegionListView.as_view()(request)
        filtrar.assert_called_once_with(activa=True)
        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.data[1], {"codigo": "LIMA", "nombre": "Lima Metropolitana"})


class EfectosDesastreTests(SimpleTestCase):
    """Efecto de un desastre en el panel admin: resumen y una fila por planta."""

    @staticmethod
    def _registro(id_, estado, refugiada, xp, apodo="Mi planta", usuario="Ana", dano=0):
        from .models import PlantaUsuario, RegistroDesastreUsuario, Usuario

        planta = PlantaUsuario(id_planta_usuario=id_, apodo=apodo)
        planta.id_usuario = Usuario(id_usuario=1, nombre=usuario)
        return RegistroDesastreUsuario(
            id_registro=id_, id_planta_usuario=planta, estado_final=estado,
            estaba_refugiada=refugiada, xp_otorgado=xp, dano_salud_aplicado=dano,
        )

    def test_resumen_cuenta_estados_refugios_y_xp(self):
        registros = [
            self._registro(1, "SALVADA", True, 40),
            self._registro(2, "DANADA", False, 0, dano=25),
            self._registro(3, "DESTRUIDA", False, 0, dano=100),
            self._registro(4, "SALVADA", True, 40),
        ]

        resumen = admin_views._resumen_efectos(registros)["resumen"]

        self.assertEqual(resumen, {"total": 4, "salvadas": 2, "danadas": 1, "destruidas": 1,
                                   "refugiadas": 2, "xp_total": 80})

    def test_cada_fila_trae_usuario_planta_y_dano(self):
        fila = admin_views._resumen_efectos([self._registro(2, "DANADA", False, 0, "Helecho", "Luis", 25)])["registros"][0]

        self.assertEqual((fila["usuario"], fila["planta"], fila["dano_salud"]), ("Luis", "Helecho", 25))

    def test_sin_registros_el_resumen_es_cero(self):
        self.assertEqual(admin_views._resumen_efectos([])["resumen"]["total"], 0)

    def _get(self, admin=True, existe=True):
        from unittest.mock import MagicMock

        request = APIRequestFactory().get("/api/admin/eventos/5/efectos/")
        with patch.object(admin_views, "_admin_desde_request", return_value=_usuario() if admin else None), \
                patch.object(admin_views.EventoDesastre.objects, "filter", return_value=MagicMock(exists=lambda: existe)), \
                patch.object(admin_views.RegistroDesastreUsuario.objects, "filter") as registros:
            registros.return_value.select_related.return_value.order_by.return_value = []
            return admin_views.AdminEventoEfectosView.as_view()(request, pk=5)

    def test_sin_admin_devuelve_401(self):
        self.assertEqual(self._get(admin=False).status_code, 401)

    def test_evento_inexistente_devuelve_404(self):
        self.assertEqual(self._get(existe=False).status_code, 404)

    def test_evento_sin_registros_devuelve_resumen_vacio(self):
        respuesta = self._get()
        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.data["registros"], [])


class AdminTiendaTests(SimpleTestCase):
    """Tienda del admin: precios, activar o desactivar y agregar fondos; las macetas aun no."""

    def _llamar(self, vista, metodo, datos=None, admin=True, **kwargs):
        fabrica = getattr(APIRequestFactory(), metodo)
        request = fabrica("/api/admin/tienda/", datos, format="json")
        with patch.object(admin_views, "_admin_desde_request", return_value=_usuario() if admin else None):
            return vista.as_view()(request, **kwargs)

    def test_sin_admin_devuelve_401(self):
        respuesta = self._llamar(admin_views.AdminTiendaListCreateView, "post", {}, admin=False)
        self.assertEqual(respuesta.status_code, 401)

    def test_solo_se_agregan_fondos(self):
        datos = {"tipo": "MACETA", "nombre": "Maceta", "precio_monedas": 10}
        respuesta = self._llamar(admin_views.AdminTiendaListCreateView, "post", datos)
        self.assertEqual(respuesta.status_code, 400)

    def test_precio_negativo_o_nivel_cero_no_se_aceptan(self):
        for cambios in ({"precio_monedas": -5}, {"nivel_requerido": 0}):
            datos = dict({"nombre": "Fondo", "precio_monedas": 50, "id_fondo": 9}, **cambios)
            respuesta = self._llamar(admin_views.AdminTiendaListCreateView, "post", datos)
            self.assertEqual(respuesta.status_code, 400, cambios)

    def test_el_fondo_debe_existir_y_no_estar_ya_a_la_venta(self):
        from unittest.mock import MagicMock

        datos = {"nombre": "Fondo", "precio_monedas": 50, "id_fondo": 9}
        with patch.object(admin_views.FondoEscena.objects, "filter", return_value=MagicMock(first=lambda: None)):
            self.assertEqual(self._llamar(admin_views.AdminTiendaListCreateView, "post", datos).status_code, 400)
        fondo = admin_views.FondoEscena(id_fondo=9, momento="NOCHE", nombre="Cyber", imagen_url="/files/arte/fondos/x.png")
        with patch.object(admin_views.FondoEscena.objects, "filter", return_value=MagicMock(first=lambda: fondo)), \
                patch.object(admin_views.ItemTienda.objects, "filter", return_value=MagicMock(exists=lambda: True)):
            self.assertEqual(self._llamar(admin_views.AdminTiendaListCreateView, "post", datos).status_code, 400)

    def test_agregar_un_fondo_guarda_su_vista_previa(self):
        from unittest.mock import MagicMock

        datos = {"nombre": "Cyber", "precio_monedas": 250, "nivel_requerido": 3, "id_fondo": 9}
        fondo = admin_views.FondoEscena(id_fondo=9, momento="NOCHE", nombre="Cyber", imagen_url="/files/arte/fondos/cyberpunk.png")
        creado = admin_views.ItemTienda(id_item=7, nombre="Cyber", tipo="FONDO", precio_monedas=250, nivel_requerido=3, id_fondo=9,
                                        asset_url="/files/arte/fondos/cyberpunk_vista.png", activo=True)
        with patch.object(admin_views.FondoEscena.objects, "filter", return_value=MagicMock(first=lambda: fondo)), \
                patch.object(admin_views.ItemTienda.objects, "filter", return_value=MagicMock(exists=lambda: False)), \
                patch.object(admin_views.ItemTienda.objects, "create", return_value=creado) as crear:
            respuesta = self._llamar(admin_views.AdminTiendaListCreateView, "post", datos)
        self.assertEqual(respuesta.status_code, 201)
        self.assertEqual(crear.call_args.kwargs["asset_url"], "/files/arte/fondos/cyberpunk_vista.png")
        self.assertEqual(crear.call_args.kwargs["tipo"], "FONDO")

    def test_cambiar_el_precio_solo_guarda_el_precio(self):
        from unittest.mock import MagicMock

        item = admin_views.ItemTienda(id_item=2, nombre="Nevado", tipo="FONDO", precio_monedas=100, nivel_requerido=1, activo=True)
        item.save = MagicMock()
        with patch.object(admin_views.ItemTienda.objects, "filter", return_value=MagicMock(first=lambda: item)):
            respuesta = self._llamar(admin_views.AdminTiendaDetailView, "patch", {"precio_monedas": 80}, pk=2)
        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.data["precio_monedas"], 80)
        item.save.assert_called_once_with(update_fields=["precio_monedas"])

    def test_desactivar_no_borra(self):
        from unittest.mock import MagicMock

        item = admin_views.ItemTienda(id_item=2, nombre="Nevado", tipo="FONDO", precio_monedas=100, activo=True)
        item.save = MagicMock()
        with patch.object(admin_views.ItemTienda.objects, "filter", return_value=MagicMock(first=lambda: item)):
            respuesta = self._llamar(admin_views.AdminTiendaDetailView, "patch", {"activo": False}, pk=2)
        self.assertFalse(respuesta.data["activo"])
        item.save.assert_called_once_with(update_fields=["activo"])

    def test_una_maceta_no_se_puede_activar_todavia(self):
        from unittest.mock import MagicMock

        item = admin_views.ItemTienda(id_item=3, nombre="Maceta", tipo="MACETA", precio_monedas=80, activo=False)
        item.save = MagicMock()
        with patch.object(admin_views.ItemTienda.objects, "filter", return_value=MagicMock(first=lambda: item)):
            respuesta = self._llamar(admin_views.AdminTiendaDetailView, "patch", {"activo": True}, pk=3)
        self.assertEqual(respuesta.status_code, 400)
        item.save.assert_not_called()

    def test_producto_inexistente_devuelve_404(self):
        from unittest.mock import MagicMock

        with patch.object(admin_views.ItemTienda.objects, "filter", return_value=MagicMock(first=lambda: None)):
            respuesta = self._llamar(admin_views.AdminTiendaDetailView, "patch", {"activo": False}, pk=99)
        self.assertEqual(respuesta.status_code, 404)
