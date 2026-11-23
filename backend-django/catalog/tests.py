"""Autenticacion del panel admin (admin_views.py). Sin base de datos: se simula el usuario."""
import os
from datetime import timedelta
from unittest.mock import MagicMock, patch

from django.test import SimpleTestCase
from django.utils import timezone
from rest_framework.test import APIRequestFactory

from . import admin_views, eventos_imagen, fondos_imagen
from .models import Usuario

TOKEN = "a" * 32


def _usuario(rol="ADMIN", token=TOKEN, expira_en=timedelta(days=1)):
    return Usuario(
        id_usuario=8,
        rol=rol,
        admin_session_token=admin_views._hash_token(token) if token else None,
        admin_session_expira=timezone.now() + expira_en,
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

    def test_fecha_sin_zona_se_lee_como_hora_de_lima(self):
        naive = timezone.make_naive(timezone.now() + timedelta(hours=1), timezone.get_default_timezone())
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
            admin_views.EventoDesastre.objects, "filter", return_value=MagicMock(first=lambda: None)
        ), patch.object(
            admin_views.Region.objects, "filter", return_value=MagicMock(first=lambda: None if region_creada is None else region)
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

    def test_fin_antes_del_inicio_devuelve_400(self):
        from .models import TipoDesastre

        tipo = TipoDesastre(id_tipo_desastre=1, codigo="HUAYCO", nombre="Huayco")
        datos = {**self.DATOS, "fecha_inicio": "2026-11-15T16:00:00Z", "fecha_fin": "2026-11-15T10:00:00Z"}
        respuesta, crear = self._post(datos, tipo=tipo)
        self.assertEqual(respuesta.status_code, 400)
        crear.assert_not_called()

    def test_fecha_que_no_es_fecha_devuelve_400(self):
        from .models import TipoDesastre

        tipo = TipoDesastre(id_tipo_desastre=1, codigo="HUAYCO", nombre="Huayco")
        respuesta, crear = self._post({**self.DATOS, "fecha_inicio": "ayer"}, tipo=tipo)
        self.assertEqual(respuesta.status_code, 400)
        crear.assert_not_called()

    def test_tipo_inexistente_devuelve_400(self):
        respuesta, crear = self._post(self.DATOS, tipo=None)
        self.assertEqual(respuesta.status_code, 400)
        crear.assert_not_called()

    def test_region_inexistente_da_400_y_no_se_crea(self):
        """QA-17: antes una region escrita a mano se creaba activa para todos."""
        from .models import TipoDesastre

        tipo = TipoDesastre(id_tipo_desastre=1, codigo="HUAYCO", nombre="Huayco")
        respuesta, crear = self._post({**self.DATOS, "region": "zona inventada <b>"}, tipo=tipo, region_creada=None)
        self.assertEqual(respuesta.status_code, 400)
        crear.assert_not_called()

    def test_severidad_o_mensaje_invalidos_dan_400(self):
        from .models import TipoDesastre

        tipo = TipoDesastre(id_tipo_desastre=1, codigo="HUAYCO", nombre="Huayco")
        for datos in ({**self.DATOS, "severidad": "XYZ"}, {**self.DATOS, "mensaje_notificacion": []},
                      {**self.DATOS, "tipo_evento": ["HUAYCO"]}):
            respuesta, crear = self._post(datos, tipo=tipo)
            self.assertEqual(respuesta.status_code, 400, datos)
            crear.assert_not_called()

    def test_un_evento_futuro_nace_programado(self):
        """QA-18: un evento que empieza en 30 dias ya no nace ACTIVO."""
        from datetime import timedelta

        from .models import TipoDesastre

        tipo = TipoDesastre(id_tipo_desastre=1, codigo="HUAYCO", nombre="Huayco")
        inicio = timezone.now() + timedelta(days=30)
        datos = {**self.DATOS, "fecha_inicio": inicio.isoformat(), "fecha_fin": (inicio + timedelta(hours=6)).isoformat()}
        respuesta, crear = self._post(datos, tipo=tipo)
        self.assertEqual(respuesta.status_code, 201)
        self.assertEqual(crear.call_args.kwargs["estado"], "PROGRAMADO")

    def test_tipo_valido_guarda_solo_las_llaves(self):
        from .models import TipoDesastre

        tipo = TipoDesastre(id_tipo_desastre=1, codigo="HUAYCO", nombre="Huayco")
        respuesta, crear = self._post(self.DATOS, tipo=tipo, region_creada=True)
        self.assertEqual(respuesta.status_code, 201)
        argumentos = crear.call_args.kwargs
        self.assertNotIn("tipo_evento", argumentos)
        self.assertNotIn("region", argumentos)
        self.assertIs(argumentos["id_tipo_desastre"], tipo)
        self.assertEqual(argumentos["id_region"].id_region, 9)
        self.assertEqual(argumentos["id_region"].codigo, "Arequipa")


class EventoSerializerTests(SimpleTestCase):
    def test_evento_sin_tipo_enlazado_no_falla(self):
        from datetime import datetime

        from .models import EventoDesastre, Region, TipoDesastre
        from .serializers import EventoDesastreSerializer

        evento = EventoDesastre(
            id_desastre=1, id_tipo_desastre=TipoDesastre(id_tipo_desastre=1, codigo="HUAYCO", nombre="Huayco"),
            id_region=Region(id_region=1, codigo="GLOBAL", nombre="Global"), mensaje_notificacion="m",
            fecha_inicio=datetime(2026, 11, 15), fecha_fin=datetime(2026, 11, 16),
        )
        datos = EventoDesastreSerializer(evento).data
        self.assertEqual(datos["tipo_evento"], "HUAYCO")
        self.assertEqual(datos["region"], "GLOBAL")
        self.assertEqual(datos["tipo_nombre"], "Huayco")

    def test_evento_sin_tipo_ni_region_no_falla(self):
        from datetime import datetime

        from .models import EventoDesastre
        from .serializers import EventoDesastreSerializer

        evento = EventoDesastre(
            id_desastre=1, mensaje_notificacion="m", fecha_inicio=datetime(2026, 11, 15), fecha_fin=datetime(2026, 11, 16),
        )
        datos = EventoDesastreSerializer(evento).data
        self.assertEqual(datos["tipo_evento"], "")
        self.assertEqual(datos["region"], "GLOBAL")
        self.assertIsNone(datos["tipo_nombre"])


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

    def test_el_tipo_cuidado_es_valido(self):
        from unittest.mock import MagicMock

        mision = admin_views.Mision(id_mision=8, titulo="Cuidado Completo", tipo="CUIDADO", meta=3)
        with patch.object(admin_views.Mision.objects, "create", return_value=mision):
            respuesta = self._llamar(admin_views.AdminMisionListCreateView, "post", dict(self.DATOS, tipo="CUIDADO"))
        self.assertEqual(respuesta.status_code, 201)

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


def _png(ancho, alto, color=(200, 30, 30)):
    import io

    from PIL import Image

    salida = io.BytesIO()
    Image.new("RGB", (ancho, alto), color).save(salida, format="PNG")
    return salida.getvalue()


def _gif(cuadros=3, lado=(40, 30), duracion=200):
    import io

    from PIL import Image

    imagenes = [Image.new("RGB", lado, (30 * i, 80, 200 - 30 * i)) for i in range(cuadros)]
    salida = io.BytesIO()
    imagenes[0].save(salida, format="GIF", save_all=True, append_images=imagenes[1:], duration=duracion, loop=0)
    return salida.getvalue()


class FondosImagenTests(SimpleTestCase):
    """Conversion de lo que sube el admin a la tira de cuadros cuadrados del jardin."""

    def _tira(self, resultado):
        import io

        from PIL import Image

        return Image.open(io.BytesIO(resultado["tira"]))

    def test_un_gif_animado_pasa_a_ser_una_tira_de_cuadros_cuadrados(self):
        resultado = fondos_imagen.procesar(_gif(cuadros=3, lado=(40, 30), duracion=200))
        self.assertEqual(resultado["cuadros"], 3)
        self.assertEqual(resultado["lado"], 30)  # se recorta al centro
        self.assertEqual(self._tira(resultado).size, (90, 30))
        self.assertEqual(resultado["ms"], 200)  # sale de la duracion del GIF

    def test_una_tira_png_se_parte_en_los_cuadros_indicados(self):
        resultado = fondos_imagen.procesar(_png(64, 16), cuadros=4)
        self.assertEqual((resultado["cuadros"], resultado["lado"]), (4, 16))
        self.assertEqual(self._tira(resultado).size, (64, 16))
        self.assertEqual(resultado["ms"], fondos_imagen.MS_DEFECTO)

    def test_una_tira_mal_medida_da_error(self):
        with self.assertRaises(fondos_imagen.ErrorImagen):
            fondos_imagen.procesar(_png(65, 16), cuadros=4)
        with self.assertRaises(fondos_imagen.ErrorImagen):
            fondos_imagen.procesar(_png(100, 16), cuadros=4)  # cuadros de 25x16: no son cuadrados

    def test_una_imagen_fija_queda_como_un_solo_cuadro(self):
        resultado = fondos_imagen.procesar(_png(120, 80))
        self.assertEqual((resultado["cuadros"], resultado["lado"]), (1, 80))

    def test_un_archivo_que_no_es_imagen_da_error(self):
        with self.assertRaises(fondos_imagen.ErrorImagen):
            fondos_imagen.procesar(b"esto no es una imagen")

    def test_un_archivo_muy_pesado_da_error(self):
        with self.assertRaises(fondos_imagen.ErrorImagen):
            fondos_imagen.procesar(b"0" * (fondos_imagen.MAX_BYTES + 1))

    def test_los_cuadros_grandes_se_reducen_y_la_tira_no_pasa_el_limite(self):
        grande = fondos_imagen.procesar(_gif(cuadros=4, lado=(400, 400)))
        self.assertEqual(grande["lado"], fondos_imagen.LADO_MAX)
        muchos = fondos_imagen.procesar(_png(96 * 200, 96), cuadros=200)
        self.assertLessEqual(muchos["cuadros"], fondos_imagen.MAX_CUADROS)
        self.assertLessEqual(self._tira(muchos).width, fondos_imagen.ANCHO_MAX_TIRA)

    def test_la_vista_previa_mide_96(self):
        import io

        from PIL import Image

        vista = Image.open(io.BytesIO(fondos_imagen.procesar(_gif(lado=(40, 40)))["vista"]))
        self.assertEqual(vista.size, (96, 96))

    def test_guardar_escribe_la_tira_y_la_vista_con_huella(self):
        import tempfile

        resultado = fondos_imagen.procesar(_gif())
        with tempfile.TemporaryDirectory() as carpeta:
            tira, vista = fondos_imagen.guardar(carpeta, "tema5_dia", resultado)
            self.assertTrue(os.path.exists(os.path.join(carpeta, tira)))
            self.assertTrue(os.path.exists(os.path.join(carpeta, vista)))
        self.assertRegex(tira, r"^tema5_dia_[0-9a-f]{8}\.png$")
        self.assertTrue(vista.endswith("_vista.png"))


class AdminTiendaTests(SimpleTestCase):
    """Tienda del admin: precios, activar o desactivar y agregar temas de fondo con sus imagenes."""

    def _llamar(self, vista, metodo, datos=None, admin=True, formato="json", **kwargs):
        fabrica = getattr(APIRequestFactory(), metodo)
        request = fabrica("/api/admin/tienda/", datos, format=formato)
        from contextlib import nullcontext

        with patch.object(admin_views, "_admin_desde_request", return_value=_usuario() if admin else None),                 patch.object(admin_views.transaction, "atomic", return_value=nullcontext()):
            return vista.as_view()(request, **kwargs)

    def _formulario(self, **extra):
        from django.core.files.uploadedfile import SimpleUploadedFile

        datos = {"nombre": "Cyber", "descripcion": "Neones", "precio_monedas": "250", "nivel_requerido": "3",
                 "dia": SimpleUploadedFile("dia.gif", _gif(), content_type="image/gif"),
                 "noche": SimpleUploadedFile("noche.png", _png(64, 16), content_type="image/png"), "cuadros_noche": "4"}
        datos.update(extra)
        return datos

    def test_sin_admin_devuelve_401(self):
        respuesta = self._llamar(admin_views.AdminTiendaListCreateView, "post", {}, admin=False)
        self.assertEqual(respuesta.status_code, 401)

    def test_solo_se_agregan_fondos(self):
        datos = {"tipo": "MACETA", "nombre": "Maceta", "precio_monedas": 10}
        respuesta = self._llamar(admin_views.AdminTiendaListCreateView, "post", datos)
        self.assertEqual(respuesta.status_code, 400)

    def test_precio_negativo_o_nivel_cero_no_se_aceptan(self):
        for cambios in ({"precio_monedas": "-5"}, {"nivel_requerido": "0"}):
            respuesta = self._llamar(admin_views.AdminTiendaListCreateView, "post", self._formulario(**cambios), formato="multipart")
            self.assertEqual(respuesta.status_code, 400, cambios)

    def test_hay_que_subir_al_menos_una_escena(self):
        datos = {"nombre": "Cyber", "precio_monedas": "250", "nivel_requerido": "3"}
        respuesta = self._llamar(admin_views.AdminTiendaListCreateView, "post", datos, formato="multipart")
        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("escena", respuesta.data["error"])

    def test_una_imagen_que_no_sirve_da_400_con_el_momento(self):
        from django.core.files.uploadedfile import SimpleUploadedFile

        datos = self._formulario(tarde=SimpleUploadedFile("tarde.txt", b"hola", content_type="text/plain"))
        respuesta = self._llamar(admin_views.AdminTiendaListCreateView, "post", datos, formato="multipart")
        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("tarde", respuesta.data["error"])

    def test_agregar_un_fondo_crea_el_tema_sus_escenas_y_el_producto(self):
        from contextlib import nullcontext
        from unittest.mock import MagicMock

        tema = admin_views.TemaFondo(id_tema=9, nombre="Cyber")
        creado = admin_views.ItemTienda(id_item=7, nombre="Cyber", tipo="FONDO", precio_monedas=250, nivel_requerido=3,
                                        id_tema=9, asset_url="/files/fondos/x_vista.png", activo=True)
        with patch.object(admin_views.transaction, "atomic", return_value=nullcontext()), \
                patch.object(admin_views.TemaFondo.objects, "create", return_value=tema) as crear_tema, \
                patch.object(admin_views, "_guardar_escena") as guardar, \
                patch.object(admin_views, "_vista_principal", return_value="/files/fondos/x_vista.png"), \
                patch.object(admin_views.ItemTienda.objects, "create", return_value=creado) as crear_item, \
                patch.object(admin_views, "_variantes_por_tema", return_value={}):
            respuesta = self._llamar(admin_views.AdminTiendaListCreateView, "post", self._formulario(), formato="multipart")
        self.assertEqual(respuesta.status_code, 201)
        self.assertEqual(crear_tema.call_args.kwargs["nombre"], "Cyber")
        self.assertEqual([c.args[1] for c in guardar.call_args_list], ["DIA", "NOCHE"])  # solo las que subio
        self.assertEqual(guardar.call_args_list[1].args[2]["cuadros"], 4)  # la tira de la noche se partio en 4
        kwargs = crear_item.call_args.kwargs
        self.assertEqual((kwargs["tipo"], kwargs["id_tema"], kwargs["precio_monedas"], kwargs["nivel_requerido"]), ("FONDO", 9, 250, 3))

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

        item = admin_views.ItemTienda(id_item=2, nombre="Nevado", tipo="FONDO", precio_monedas=100, id_tema=5, activo=True)
        item.save = MagicMock()
        with patch.object(admin_views.ItemTienda.objects, "filter", return_value=MagicMock(first=lambda: item)), \
                patch.object(admin_views.TemaFondo.objects, "filter", return_value=MagicMock(first=lambda: None)), \
                patch.object(admin_views, "_variantes_por_tema", return_value={}):
            respuesta = self._llamar(admin_views.AdminTiendaDetailView, "patch", {"activo": False}, pk=2)
        self.assertFalse(respuesta.data["activo"])
        item.save.assert_called_once_with(update_fields=["activo"])

    def test_reemplazar_una_escena_cambia_la_vista_del_producto(self):
        from contextlib import nullcontext
        from unittest.mock import MagicMock

        item = admin_views.ItemTienda(id_item=2, nombre="Nevado", tipo="FONDO", precio_monedas=100, id_tema=5, asset_url="/viejo.png")
        item.save = MagicMock()
        tema = admin_views.TemaFondo(id_tema=5, nombre="Nevado")
        with patch.object(admin_views.ItemTienda.objects, "filter", return_value=MagicMock(first=lambda: item)), \
                patch.object(admin_views.TemaFondo.objects, "filter", return_value=MagicMock(first=lambda: tema)), \
                patch.object(admin_views.transaction, "atomic", return_value=nullcontext()), \
                patch.object(admin_views, "_guardar_escena") as guardar, \
                patch.object(admin_views, "_vista_principal", return_value="/nueva_vista.png"), \
                patch.object(admin_views, "_variantes_por_tema", return_value={}):
            from django.core.files.uploadedfile import SimpleUploadedFile

            datos = {"tarde": SimpleUploadedFile("t.gif", _gif(), content_type="image/gif")}
            respuesta = self._llamar(admin_views.AdminTiendaDetailView, "patch", datos, formato="multipart", pk=2)
        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(guardar.call_args.args[1], "TARDE")
        item.save.assert_called_once_with(update_fields=["asset_url"])
        self.assertEqual(item.asset_url, "/nueva_vista.png")

    def test_una_maceta_desactivada_se_puede_volver_a_activar(self):
        """QA-22: Spring vende macetas; antes el admin no podia reactivar una que habia desactivado."""
        from unittest.mock import MagicMock

        item = admin_views.ItemTienda(id_item=3, nombre="Maceta", tipo="MACETA", precio_monedas=80, activo=False)
        item.save = MagicMock()
        with patch.object(admin_views.ItemTienda.objects, "filter", return_value=MagicMock(first=lambda: item)):
            respuesta = self._llamar(admin_views.AdminTiendaDetailView, "patch", {"activo": True}, pk=3)
        self.assertEqual(respuesta.status_code, 200)
        item.save.assert_called_once_with(update_fields=["activo"])

    def test_un_fondo_sin_tema_no_se_puede_activar(self):
        from unittest.mock import MagicMock

        item = admin_views.ItemTienda(id_item=3, nombre="Fondo viejo", tipo="FONDO", precio_monedas=150, id_tema=None, activo=False)
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


class AdminParametrosTests(SimpleTestCase):
    """Parametros de juego (costo de revivir, plazo de recuperacion...): permisos y rangos."""

    def _llamar(self, vista, metodo, datos=None, admin=True, **kwargs):
        fabrica = getattr(APIRequestFactory(), metodo)
        request = fabrica("/api/admin/parametros/", datos, format="json")
        with patch.object(admin_views, "_admin_desde_request", return_value=_usuario() if admin else None):
            return vista.as_view()(request, **kwargs)

    def _fila(self, clave="COSTO_REVIVIR", valor=250):
        from unittest.mock import MagicMock

        fila = admin_views.ParametroJuego(clave=clave, valor=valor, descripcion="d")
        fila.save = MagicMock()
        return fila

    def _con(self, fila):
        from unittest.mock import MagicMock

        return patch.object(admin_views.ParametroJuego.objects, "filter", return_value=MagicMock(first=lambda: fila))

    def test_sin_admin_devuelve_401(self):
        self.assertEqual(self._llamar(admin_views.AdminParametrosView, "get", admin=False).status_code, 401)
        respuesta = self._llamar(admin_views.AdminParametroDetailView, "patch", {"valor": 1}, admin=False,
                                 clave="COSTO_REVIVIR")
        self.assertEqual(respuesta.status_code, 401)

    def test_lista_solo_los_parametros_que_usa_spring(self):
        with patch.object(admin_views.ParametroJuego.objects, "filter") as filtrar:
            filtrar.return_value.order_by.return_value = [self._fila()]
            respuesta = self._llamar(admin_views.AdminParametrosView, "get")
        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.data[0]["clave"], "COSTO_REVIVIR")
        self.assertCountEqual(filtrar.call_args.kwargs["clave__in"], list(admin_views.PARAMETROS_JUEGO))

    def test_cambia_el_costo_de_revivir(self):
        fila = self._fila()
        with self._con(fila):
            respuesta = self._llamar(admin_views.AdminParametroDetailView, "patch", {"valor": 300},
                                     clave="COSTO_REVIVIR")
        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.data["valor"], 300)
        fila.save.assert_called_once_with(update_fields=["valor"])

    def test_un_valor_fuera_de_rango_da_400_y_no_guarda(self):
        fila = self._fila("SALUD_REVIVIDA", 50)
        with self._con(fila):
            respuesta = self._llamar(admin_views.AdminParametroDetailView, "patch", {"valor": 150},
                                     clave="SALUD_REVIVIDA")
        self.assertEqual(respuesta.status_code, 400)
        fila.save.assert_not_called()

    def test_el_tope_de_plantas_activas_acepta_de_6_a_60(self):
        fila = self._fila("MAX_PLANTAS_ACTIVAS", 12)
        with self._con(fila):
            bien = self._llamar(admin_views.AdminParametroDetailView, "patch", {"valor": 18},
                                clave="MAX_PLANTAS_ACTIVAS")
            mal = self._llamar(admin_views.AdminParametroDetailView, "patch", {"valor": 3},
                               clave="MAX_PLANTAS_ACTIVAS")
        self.assertEqual(bien.status_code, 200)
        self.assertEqual(mal.status_code, 400)

    def test_un_valor_que_no_es_entero_da_400(self):
        fila = self._fila()
        for malo in ("300", 3.5, True, None):
            with self._con(fila):
                respuesta = self._llamar(admin_views.AdminParametroDetailView, "patch", {"valor": malo},
                                         clave="COSTO_REVIVIR")
            self.assertEqual(respuesta.status_code, 400, malo)

    def test_una_clave_desconocida_da_404(self):
        respuesta = self._llamar(admin_views.AdminParametroDetailView, "patch", {"valor": 1}, clave="OTRA")
        self.assertEqual(respuesta.status_code, 404)

    def test_una_clave_sin_fila_da_404(self):
        with self._con(None):
            respuesta = self._llamar(admin_views.AdminParametroDetailView, "patch", {"valor": 5},
                                     clave="COSTO_REVIVIR")
        self.assertEqual(respuesta.status_code, 404)


def _png_apaisado(ancho=1600, alto=900):
    import io

    from PIL import Image

    salida = io.BytesIO()
    Image.new("RGB", (ancho, alto), (200, 120, 40)).save(salida, "PNG")
    return salida.getvalue()


class EventosImagenTests(SimpleTestCase):
    """La imagen del evento: se reduce, se pasa a JPG y se rechaza lo que no sirve."""

    def test_una_imagen_grande_se_reduce_a_1280_y_queda_en_jpg(self):
        import io

        from PIL import Image

        jpg = eventos_imagen.procesar(_png_apaisado(2720, 1536))
        img = Image.open(io.BytesIO(jpg))
        self.assertEqual(img.format, "JPEG")
        self.assertEqual(img.width, eventos_imagen.ANCHO_MAX)
        self.assertEqual(img.height, round(1536 * 1280 / 2720))

    def test_una_imagen_mediana_no_se_agranda(self):
        import io

        from PIL import Image

        img = Image.open(io.BytesIO(eventos_imagen.procesar(_png_apaisado(800, 450))))
        self.assertEqual(img.width, 800)

    def test_una_imagen_muy_chica_da_error(self):
        with self.assertRaises(eventos_imagen.ErrorImagen):
            eventos_imagen.procesar(_png_apaisado(200, 112))

    def test_un_archivo_que_no_es_imagen_da_error(self):
        with self.assertRaises(eventos_imagen.ErrorImagen):
            eventos_imagen.procesar(b"no soy una imagen")

    def test_un_archivo_muy_pesado_da_error(self):
        with self.assertRaises(eventos_imagen.ErrorImagen):
            eventos_imagen.procesar(b"0" * (eventos_imagen.MAX_BYTES + 1))

    def test_guardar_escribe_el_archivo_con_huella(self):
        import tempfile

        jpg = eventos_imagen.procesar(_png_apaisado())
        with tempfile.TemporaryDirectory() as carpeta:
            nombre = eventos_imagen.guardar(os.path.join(carpeta, "eventos"), "OLA_DE_CALOR", jpg)
            self.assertTrue(nombre.startswith("ola_de_calor_") and nombre.endswith(".jpg"))
            with open(os.path.join(carpeta, "eventos", nombre), "rb") as f:
                self.assertEqual(f.read(), jpg)


class AdminTiposDesastreTests(SimpleTestCase):
    """Texto, consejo, salud final e imagen de cada tipo de evento."""

    def _tipo(self):
        from unittest.mock import MagicMock

        tipo = admin_views.TipoDesastre(id_tipo_desastre=3, codigo="OLA_DE_CALOR", nombre="Ola de calor")
        tipo.save = MagicMock()
        return tipo

    def _llamar(self, vista, metodo, datos=None, formato="json", admin=True, tipo="por-defecto", **kwargs):
        from unittest.mock import MagicMock

        tipo = self._tipo() if tipo == "por-defecto" else tipo
        request = getattr(APIRequestFactory(), metodo)("/api/admin/tipos-desastre/", datos, format=formato)
        with patch.object(admin_views, "_admin_desde_request", return_value=_usuario() if admin else None), \
                patch.object(admin_views.TipoDesastre.objects, "filter", return_value=MagicMock(first=lambda: tipo)):
            return vista.as_view()(request, **kwargs), tipo

    def test_sin_admin_devuelve_401(self):
        respuesta, _ = self._llamar(admin_views.AdminTipoDesastreDetailView, "patch", {"consejo": "x"}, admin=False, pk=3)
        self.assertEqual(respuesta.status_code, 401)

    def test_tipo_inexistente_devuelve_404(self):
        respuesta, _ = self._llamar(admin_views.AdminTipoDesastreDetailView, "patch", {"consejo": "x"}, tipo=None, pk=9)
        self.assertEqual(respuesta.status_code, 404)

    def test_cambia_el_texto_y_el_consejo(self):
        respuesta, tipo = self._llamar(admin_views.AdminTipoDesastreDetailView, "patch",
                                       {"descripcion": " Texto nuevo ", "consejo": "Cubre el suelo"}, pk=3)
        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.data["descripcion"], "Texto nuevo")
        self.assertEqual(tipo.consejo, "Cubre el suelo")
        tipo.save.assert_called_once()
        self.assertCountEqual(tipo.save.call_args.kwargs["update_fields"], ["descripcion", "consejo"])

    def test_un_texto_vacio_queda_sin_valor(self):
        respuesta, tipo = self._llamar(admin_views.AdminTipoDesastreDetailView, "patch", {"consejo": "  "}, pk=3)
        self.assertEqual(respuesta.status_code, 200)
        self.assertIsNone(tipo.consejo)

    def test_el_consejo_largo_da_400(self):
        respuesta, tipo = self._llamar(admin_views.AdminTipoDesastreDetailView, "patch", {"consejo": "x" * 251}, pk=3)
        self.assertEqual(respuesta.status_code, 400)
        tipo.save.assert_not_called()

    def test_la_salud_final_debe_bajar_con_la_severidad(self):
        respuesta, tipo = self._llamar(admin_views.AdminTipoDesastreDetailView, "patch",
                                       {"salud_final_critico": 30}, pk=3)  # moderado queda en 20
        self.assertEqual(respuesta.status_code, 400)
        tipo.save.assert_not_called()

    def test_la_salud_final_valida_se_guarda(self):
        respuesta, tipo = self._llamar(admin_views.AdminTipoDesastreDetailView, "patch",
                                       {"salud_final_bajo": 60, "salud_final_moderado": 30, "salud_final_critico": 5}, pk=3)
        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual((tipo.salud_final_bajo, tipo.salud_final_moderado, tipo.salud_final_critico), (60, 30, 5))

    def test_la_salud_final_fuera_de_rango_da_400(self):
        for malo in (0, 101, "x", True):
            respuesta, _ = self._llamar(admin_views.AdminTipoDesastreDetailView, "patch", {"salud_final_bajo": malo}, pk=3)
            self.assertEqual(respuesta.status_code, 400, malo)

    def test_subir_una_imagen_la_guarda_y_deja_su_ruta(self):
        import tempfile

        from django.core.files.uploadedfile import SimpleUploadedFile
        from django.test import override_settings

        archivo = SimpleUploadedFile("evento.png", _png_apaisado(), content_type="image/png")
        with tempfile.TemporaryDirectory() as carpeta, override_settings(STORAGE_IMAGES_DIR=carpeta):
            respuesta, tipo = self._llamar(admin_views.AdminTipoDesastreDetailView, "patch",
                                           {"imagen": archivo}, formato="multipart", pk=3)
            self.assertEqual(respuesta.status_code, 200)
            self.assertTrue(tipo.imagen_url.startswith("/files/eventos/ola_de_calor_"))
            self.assertTrue(os.path.exists(os.path.join(carpeta, "eventos", tipo.imagen_url.rsplit("/", 1)[1])))

    def test_una_imagen_que_no_sirve_da_400_y_no_guarda(self):
        from django.core.files.uploadedfile import SimpleUploadedFile

        archivo = SimpleUploadedFile("evento.png", b"no soy imagen", content_type="image/png")
        respuesta, tipo = self._llamar(admin_views.AdminTipoDesastreDetailView, "patch",
                                       {"imagen": archivo}, formato="multipart", pk=3)
        self.assertEqual(respuesta.status_code, 400)
        tipo.save.assert_not_called()


class AdminReglasTests(SimpleTestCase):
    """Reglas de progreso: lo que pagan las acciones en hojas y XP."""

    def _regla(self, motivo="REGAR", xp=2, monedas=1, tope=5):
        from unittest.mock import MagicMock

        regla = admin_views.ReglaProgreso(id_regla=4, motivo=motivo, xp=xp, monedas=monedas, tope_diario=tope, activa=True)
        regla.save = MagicMock()
        return regla

    def _llamar(self, vista, metodo, datos=None, admin=True, regla="por-defecto", **kwargs):
        from unittest.mock import MagicMock

        regla = self._regla() if regla == "por-defecto" else regla
        request = getattr(APIRequestFactory(), metodo)("/api/admin/reglas/", datos, format="json")
        with patch.object(admin_views, "_admin_desde_request", return_value=_usuario() if admin else None), \
                patch.object(admin_views.ReglaProgreso.objects, "filter", return_value=MagicMock(first=lambda: regla)), \
                patch.object(admin_views.ReglaProgreso.objects, "order_by", return_value=[regla]):
            return vista.as_view()(request, **kwargs), regla

    def test_sin_admin_devuelve_401(self):
        self.assertEqual(self._llamar(admin_views.AdminReglasView, "get", admin=False)[0].status_code, 401)
        respuesta, _ = self._llamar(admin_views.AdminReglaDetailView, "patch", {"xp": 1}, admin=False, pk=4)
        self.assertEqual(respuesta.status_code, 401)

    def test_la_lista_trae_el_nombre_legible(self):
        respuesta, _ = self._llamar(admin_views.AdminReglasView, "get")
        self.assertEqual(respuesta.data[0]["motivo"], "REGAR")
        self.assertEqual(respuesta.data[0]["nombre"], "Regar (cuidado util)")

    def test_cambia_hojas_xp_y_tope(self):
        respuesta, regla = self._llamar(admin_views.AdminReglaDetailView, "patch",
                                        {"xp": 4, "monedas": 2, "tope_diario": 8}, pk=4)
        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual((regla.xp, regla.monedas, regla.tope_diario), (4, 2, 8))
        self.assertCountEqual(regla.save.call_args.kwargs["update_fields"], ["xp", "monedas", "tope_diario"])

    def test_el_tope_vacio_quita_el_tope(self):
        respuesta, regla = self._llamar(admin_views.AdminReglaDetailView, "patch", {"tope_diario": None}, pk=4)
        self.assertEqual(respuesta.status_code, 200)
        self.assertIsNone(regla.tope_diario)

    def test_valores_invalidos_dan_400_y_no_guardan(self):
        for malo in ({"xp": -1}, {"monedas": 1.5}, {"xp": True}, {"tope_diario": 0}, {"activa": "si"}):
            respuesta, regla = self._llamar(admin_views.AdminReglaDetailView, "patch", malo, pk=4)
            self.assertEqual(respuesta.status_code, 400, malo)
            regla.save.assert_not_called()

    def test_el_motivo_no_se_puede_cambiar(self):
        respuesta, regla = self._llamar(admin_views.AdminReglaDetailView, "patch", {"motivo": "OTRO"}, pk=4)
        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(regla.motivo, "REGAR")
        regla.save.assert_not_called()

    def test_regla_inexistente_devuelve_404(self):
        respuesta, _ = self._llamar(admin_views.AdminReglaDetailView, "patch", {"xp": 1}, regla=None, pk=99)
        self.assertEqual(respuesta.status_code, 404)


class TipoDesastreEnElAdminTests(SimpleTestCase):
    """La imagen del evento se ve en el admin y se puede subir desde ahi."""

    def _admin(self):
        from django.contrib import admin as django_admin
        from .admin import TipoDesastreAdmin
        from .models import TipoDesastre

        return TipoDesastreAdmin(TipoDesastre, django_admin.site), TipoDesastre(id_tipo_desastre=3, codigo="OLA_DE_CALOR", nombre="Ola de calor")

    def test_sin_imagen_dice_sin_imagen(self):
        modelo, tipo = self._admin()
        self.assertEqual(modelo.vista_imagen(tipo), "Sin imagen")

    def test_con_imagen_muestra_la_miniatura_desde_spring(self):
        modelo, tipo = self._admin()
        tipo.imagen_url = "/files/eventos/ola_de_calor_ab12cd34.jpg"
        html = str(modelo.vista_imagen(tipo))
        self.assertIn("<img", html)
        self.assertIn("/files/eventos/ola_de_calor_ab12cd34.jpg", html)

    def test_subir_la_imagen_la_guarda_y_deja_su_direccion(self):
        import tempfile
        from unittest.mock import MagicMock

        from django.test import override_settings

        modelo, tipo = self._admin()
        form = MagicMock()
        form.cleaned_data = {"subir_imagen": object()}
        form.imagen_procesada = b"\xff\xd8jpg-de-prueba"
        with tempfile.TemporaryDirectory() as carpeta, override_settings(STORAGE_IMAGES_DIR=carpeta), \
                patch("django.contrib.admin.ModelAdmin.save_model") as guardar_modelo:
            modelo.save_model(MagicMock(), tipo, form, change=True)
            self.assertTrue(tipo.imagen_url.startswith("/files/eventos/ola_de_calor_"))
            self.assertTrue(os.path.exists(os.path.join(carpeta, "eventos", os.path.basename(tipo.imagen_url))))
            guardar_modelo.assert_called_once()

    def test_una_imagen_muy_chica_se_rechaza_en_el_formulario(self):
        from .admin import TipoDesastreForm
        from django.core.files.uploadedfile import SimpleUploadedFile
        from PIL import Image
        import io

        buf = io.BytesIO()
        Image.new("RGB", (100, 60)).save(buf, "PNG")
        form = TipoDesastreForm(data={}, files={"subir_imagen": SimpleUploadedFile("chica.png", buf.getvalue())})
        form.is_valid()
        self.assertIn("subir_imagen", form.errors)


class EventoUnoALaVezTests(SimpleTestCase):
    """Solo puede haber un evento en curso: el segundo se rechaza con 409."""

    def _datos(self):
        from datetime import timedelta
        ahora = timezone.now()
        return {"tipo_evento": "HUAYCO", "region": "GLOBAL", "mensaje_notificacion": "m",
                "fecha_inicio": ahora.isoformat(), "fecha_fin": (ahora + timedelta(hours=2)).isoformat()}

    def test_segundo_evento_activo_da_409(self):
        from unittest import mock
        from rest_framework.test import APIRequestFactory
        from . import admin_views
        from .models import EventoDesastre
        vigente = mock.MagicMock(id_desastre=7)
        with mock.patch.object(admin_views, "_admin_desde_request", return_value=object()), \
             mock.patch.object(admin_views.TipoDesastre.objects, "filter") as tipo, \
             mock.patch.object(admin_views.Region.objects, "filter") as region, \
             mock.patch.object(EventoDesastre.objects, "filter") as ev:
            tipo.return_value.first.return_value = object()
            region.return_value.first.return_value = admin_views.Region(id_region=1, codigo="GLOBAL", nombre="Global")
            ev.return_value.first.return_value = vigente
            req = APIRequestFactory().post("/api/admin/eventos/", self._datos(), format="json")
            resp = admin_views.AdminEventoCreateView.as_view()(req)
        self.assertEqual(resp.status_code, 409)
        self.assertEqual(resp.data["id_desastre_activo"], 7)


class LogroModeloYAdminTests(SimpleTestCase):
    """Modelo Logro/LogroUsuario (esquema V29 de Spring) y su pantalla en el Django admin."""

    def test_el_modelo_sigue_el_esquema_nuevo(self):
        from .models import Logro, LogroUsuario

        campos = {f.name for f in Logro._meta.get_fields()}
        self.assertEqual(campos, {"id_logro", "codigo", "titulo", "descripcion", "icono", "rareza", "condicion", "meta",
                                  "parametro", "recompensa_xp", "recompensa_monedas", "secreto", "activo", "orden"})
        self.assertFalse(Logro._meta.managed)
        self.assertFalse(LogroUsuario._meta.managed)
        self.assertEqual(LogroUsuario._meta.db_table, "logros_usuario")
        self.assertEqual(len(Logro.CONDICIONES), 17)

    def test_admin_no_permite_eliminar_ni_accion_masiva(self):
        from django.contrib import admin as dj_admin
        from .models import Logro

        ma = dj_admin.site._registry[Logro]
        request = APIRequestFactory().get("/admin/")
        self.assertFalse(ma.has_delete_permission(request))
        self.assertFalse(ma.has_delete_permission(request, Logro(id_logro=1)))
        self.assertEqual(ma.actions, ["activar", "desactivar"])

    def test_admin_columnas_filtros_y_busqueda(self):
        from django.contrib import admin as dj_admin
        from .models import Logro

        ma = dj_admin.site._registry[Logro]
        self.assertEqual(set(ma.list_filter), {"rareza", "condicion", "activo", "secreto"})
        self.assertIn("codigo", ma.search_fields)
        self.assertIn("rareza", ma.list_display)

    def test_codigo_solo_lectura_una_vez_creado(self):
        from django.contrib import admin as dj_admin
        from .models import Logro

        ma = dj_admin.site._registry[Logro]
        request = APIRequestFactory().get("/admin/")
        self.assertNotIn("codigo", ma.get_readonly_fields(request, None))
        self.assertIn("codigo", ma.get_readonly_fields(request, Logro(id_logro=1, codigo="X")))
        self.assertIn("desbloqueos_total", ma.get_readonly_fields(request, None))

    def test_desbloqueos_total_cuenta_usuarios(self):
        from django.contrib import admin as dj_admin
        from .models import Logro, LogroUsuario

        ma = dj_admin.site._registry[Logro]
        with patch.object(LogroUsuario.objects, "filter") as filtro:
            filtro.return_value.count.return_value = 4
            self.assertEqual(ma.desbloqueos_total(Logro(id_logro=3)), 4)
            filtro.assert_called_once_with(id_logro=3, desbloqueado_en__isnull=False)
        self.assertEqual(ma.desbloqueos_total(Logro()), 0)


class AdminLogrosTests(SimpleTestCase):
    """API de logros del panel React: permisos, validaciones, listado, PATCH y opciones."""

    DATOS = {"codigo": "REGADOR_100", "titulo": "Regador", "descripcion": "Riega 100 veces.", "icono": "gota",
             "rareza": "RARA", "condicion": "RIEGOS_TOTAL", "meta": 100, "recompensa_xp": 80, "recompensa_monedas": 30}
    DESASTRES = ["HELADA", "HUAYCO", "OLA_DE_CALOR", "PLAGA"]

    def _llamar(self, vista, metodo, datos=None, admin=True, **kwargs):
        request = getattr(APIRequestFactory(), metodo)("/api/admin/logros/", datos, format="json")
        with patch.object(admin_views, "_admin_desde_request", return_value=_usuario() if admin else None):
            return vista.as_view()(request, **kwargs)

    def _crear(self, **cambios):
        """POST sin base de datos: codigo libre y tipos de desastre conocidos."""
        datos = dict(self.DATOS, **cambios)
        logro = admin_views.Logro(id_logro=50, **datos)
        with patch.object(admin_views.Logro.objects, "filter", return_value=MagicMock(exists=lambda: False)), \
                patch.object(admin_views.Logro.objects, "create", return_value=logro) as crear, \
                patch.object(admin_views, "_codigos_desastre", return_value=self.DESASTRES):
            respuesta = self._llamar(admin_views.AdminLogroListCreateView, "post", datos)
        return respuesta, crear

    def _logro(self, **campos):
        base = dict(id_logro=5, codigo="NIVEL_5", titulo="Brote", descripcion="Nivel 5", icono="nivel", rareza="COMUN",
                    condicion="NIVEL", meta=5, parametro=None, recompensa_xp=50, recompensa_monedas=20,
                    secreto=False, activo=True, orden=24)
        base.update(campos)
        logro = admin_views.Logro(**base)
        logro.save = MagicMock()
        return logro

    def _patch(self, logro, datos, desbloqueos=0):
        with patch.object(admin_views.Logro.objects, "filter", return_value=MagicMock(first=lambda: logro)), \
                patch.object(admin_views.Logro, "desbloqueos", return_value=desbloqueos), \
                patch.object(admin_views, "_codigos_desastre", return_value=self.DESASTRES):
            return self._llamar(admin_views.AdminLogroDetailView, "patch", datos, pk=logro.id_logro)

    # --- permisos y metodos ---
    def test_sin_admin_devuelve_401_en_todo(self):
        for vista, metodo, kw in ((admin_views.AdminLogroListCreateView, "get", {}),
                                  (admin_views.AdminLogroListCreateView, "post", {}),
                                  (admin_views.AdminLogroDetailView, "patch", {"pk": 1}),
                                  (admin_views.AdminLogroOpcionesView, "get", {})):
            respuesta = self._llamar(vista, metodo, self.DATOS if metodo != "get" else None, admin=False, **kw)
            self.assertEqual(respuesta.status_code, 401, (vista, metodo))

    def test_no_existe_delete(self):
        for vista, kw in ((admin_views.AdminLogroListCreateView, {}), (admin_views.AdminLogroDetailView, {"pk": 1})):
            respuesta = self._llamar(vista, "delete", None, **kw)
            self.assertEqual(respuesta.status_code, 405)

    def test_cuerpo_que_no_es_objeto_da_400_y_no_500(self):
        # Regresion QA: POST con [1, 2] o "texto" daba 500 (AttributeError en data.get)
        for cuerpo in ([1, 2], "texto", 5):
            respuesta = self._llamar(admin_views.AdminLogroListCreateView, "post", cuerpo)
            self.assertEqual(respuesta.status_code, 400, cuerpo)
            respuesta = self._patch(self._logro(), cuerpo)
            self.assertEqual(respuesta.status_code, 400, cuerpo)

    def test_rutas_registradas(self):
        from django.urls import resolve, reverse

        self.assertEqual(reverse("admin-logro-list"), "/api/admin/logros/")
        self.assertEqual(reverse("admin-logro-opciones"), "/api/admin/logros/opciones/")
        self.assertEqual(resolve("/api/admin/logros/7/").func.view_class, admin_views.AdminLogroDetailView)
        self.assertEqual(resolve("/api/admin/logros/opciones/").func.view_class, admin_views.AdminLogroOpcionesView)

    # --- listado ---
    def test_lista_todos_con_desbloqueos(self):
        activo, inactivo = self._logro(id_logro=1), self._logro(id_logro=2, codigo="OTRO", activo=False)
        with patch.object(admin_views.Logro.objects, "order_by", return_value=[activo, inactivo]), \
                patch.object(admin_views, "_desbloqueos_por_logro", return_value={1: 7}):
            respuesta = self._llamar(admin_views.AdminLogroListCreateView, "get")
        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual([x["desbloqueos"] for x in respuesta.data], [7, 0])
        self.assertEqual([x["activo"] for x in respuesta.data], [True, False])
        self.assertIn("recompensa_xp", respuesta.data[0])

    def test_desbloqueos_cuenta_solo_desbloqueados(self):
        with patch.object(admin_views.LogroUsuario.objects, "filter") as filtro:
            filtro.return_value.values.return_value.annotate.return_value = [{"id_logro": 1, "n": 2}]
            self.assertEqual(admin_views._desbloqueos_por_logro(), {1: 2})
            filtro.assert_called_once_with(desbloqueado_en__isnull=False)

    # --- creacion y validaciones ---
    def test_crear_guarda_y_devuelve_201(self):
        respuesta, crear = self._crear()
        self.assertEqual(respuesta.status_code, 201)
        self.assertEqual(crear.call_args.kwargs["codigo"], "REGADOR_100")
        self.assertIsNone(crear.call_args.kwargs["parametro"])
        self.assertEqual(respuesta.data["desbloqueos"], 0)

    def test_codigo_debe_ser_mayusculas_con_guion_bajo(self):
        for malo in ("regador", "Regador_1", "REGADOR 1", "_X", "X__Y", "X_", "", "A" * 51, "ÑANDÚ"):
            respuesta, _ = self._crear(codigo=malo)
            self.assertEqual(respuesta.status_code, 400, malo)
            self.assertIn("codigo", respuesta.data["error"])

    def test_codigo_repetido_da_400(self):
        with patch.object(admin_views.Logro.objects, "filter", return_value=MagicMock(exists=lambda: True)):
            respuesta = self._llamar(admin_views.AdminLogroListCreateView, "post", dict(self.DATOS))
        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("Ya existe", respuesta.data["error"])

    def test_codigo_repetido_por_carrera_da_400(self):
        from django.db import IntegrityError

        with patch.object(admin_views.Logro.objects, "filter", return_value=MagicMock(exists=lambda: False)), \
                patch.object(admin_views.Logro.objects, "create", side_effect=IntegrityError):
            respuesta = self._llamar(admin_views.AdminLogroListCreateView, "post", dict(self.DATOS))
        self.assertEqual(respuesta.status_code, 400)

    def test_limites_de_titulo_y_descripcion(self):
        self.assertEqual(self._crear(titulo="T" * 80)[0].status_code, 201)
        self.assertEqual(self._crear(titulo="T" * 81)[0].status_code, 400)
        self.assertEqual(self._crear(titulo="  ")[0].status_code, 400)
        self.assertEqual(self._crear(descripcion="D" * 250)[0].status_code, 201)
        self.assertEqual(self._crear(descripcion="D" * 251)[0].status_code, 400)
        self.assertEqual(self._crear(icono="")[0].status_code, 400)

    def test_condicion_y_rareza_de_la_lista(self):
        self.assertEqual(self._crear(condicion="VOLAR")[0].status_code, 400)
        self.assertEqual(self._crear(rareza="MITICA")[0].status_code, 400)
        self.assertEqual(self._crear(rareza="LEGENDARIA")[0].status_code, 201)

    def test_numeros(self):
        self.assertEqual(self._crear(meta=0)[0].status_code, 400)
        self.assertEqual(self._crear(meta=True)[0].status_code, 400)
        self.assertEqual(self._crear(meta="3")[0].status_code, 400)
        self.assertEqual(self._crear(recompensa_xp=-1)[0].status_code, 400)
        self.assertEqual(self._crear(recompensa_monedas=-5)[0].status_code, 400)
        self.assertEqual(self._crear(recompensa_xp=100001)[0].status_code, 400)
        self.assertEqual(self._crear(recompensa_monedas=100001)[0].status_code, 400)
        self.assertEqual(self._crear(orden=1.5)[0].status_code, 400)
        self.assertEqual(self._crear(orden=7)[0].status_code, 201)

    def test_escaneo_hora_exige_parametro_valido(self):
        self.assertEqual(self._crear(condicion="ESCANEO_HORA")[0].status_code, 400)
        self.assertEqual(self._crear(condicion="ESCANEO_HORA", parametro="TARDE")[0].status_code, 400)
        self.assertEqual(self._crear(condicion="ESCANEO_HORA", parametro="NOCHE")[0].status_code, 201)

    def test_desastre_afrontado_parametro_opcional_pero_valido(self):
        self.assertEqual(self._crear(condicion="DESASTRE_AFRONTADO")[0].status_code, 201)
        self.assertEqual(self._crear(condicion="DESASTRE_AFRONTADO", parametro="")[0].status_code, 201)
        self.assertEqual(self._crear(condicion="DESASTRE_AFRONTADO", parametro="HUAYCO")[0].status_code, 201)
        respuesta, _ = self._crear(condicion="DESASTRE_AFRONTADO", parametro="TORNADO")
        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("HUAYCO", respuesta.data["error"])

    def test_parametro_prohibido_en_las_demas(self):
        respuesta, _ = self._crear(condicion="NIVEL", parametro="HUAYCO")
        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("no lleva parametro", respuesta.data["error"])

    # --- PATCH ---
    def test_patch_inexistente_da_404(self):
        with patch.object(admin_views.Logro.objects, "filter", return_value=MagicMock(first=lambda: None)):
            respuesta = self._llamar(admin_views.AdminLogroDetailView, "patch", {"activo": False}, pk=99)
        self.assertEqual(respuesta.status_code, 404)

    def test_desactivar_solo_cambia_activo(self):
        logro = self._logro()
        respuesta = self._patch(logro, {"activo": False}, desbloqueos=3)
        self.assertEqual(respuesta.status_code, 200)
        self.assertFalse(respuesta.data["activo"])
        self.assertEqual(respuesta.data["desbloqueos"], 3)
        self.assertNotIn("advertencia", respuesta.data)
        logro.save.assert_called_once_with(update_fields=["activo"])

    def test_el_codigo_es_inmutable(self):
        logro = self._logro()
        respuesta = self._patch(logro, {"codigo": "OTRO_CODIGO"})
        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("no se puede cambiar", respuesta.data["error"])
        logro.save.assert_not_called()

    def test_mismo_codigo_se_ignora(self):
        logro = self._logro()
        respuesta = self._patch(logro, {"codigo": "NIVEL_5", "meta": 6})
        self.assertEqual(respuesta.status_code, 200)
        logro.save.assert_called_once_with(update_fields=["meta"])

    def test_editar_meta_no_toca_logros_usuario(self):
        logro = self._logro()
        with patch.object(admin_views.LogroUsuario.objects, "filter") as filtro, \
                patch.object(admin_views.LogroUsuario.objects, "update") as actualizar, \
                patch.object(admin_views.Logro.objects, "filter", return_value=MagicMock(first=lambda: logro)):
            filtro.return_value.count.return_value = 9
            respuesta = self._llamar(admin_views.AdminLogroDetailView, "patch", {"meta": 50}, pk=5)
        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.data["meta"], 50)
        self.assertNotIn("advertencia", respuesta.data)
        actualizar.assert_not_called()
        # logros_usuario solo se lee (conteo de desbloqueos): nunca se borra ni se actualiza
        self.assertFalse(filtro.return_value.delete.called)
        self.assertFalse(filtro.return_value.update.called)

    def test_cambiar_condicion_con_desbloqueados_avisa_pero_permite(self):
        logro = self._logro()
        respuesta = self._patch(logro, {"condicion": "RIEGOS_TOTAL"}, desbloqueos=4)
        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.data["condicion"], "RIEGOS_TOTAL")
        self.assertIn("4 usuario(s)", respuesta.data["advertencia"])
        logro.save.assert_called_once_with(update_fields=["condicion", "parametro"])

    def test_cambiar_parametro_con_desbloqueados_avisa(self):
        logro = self._logro(condicion="DESASTRE_AFRONTADO", parametro="HUAYCO")
        respuesta = self._patch(logro, {"parametro": "PLAGA"}, desbloqueos=2)
        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.data["parametro"], "PLAGA")
        self.assertIn("advertencia", respuesta.data)

    def test_cambiar_regla_sin_desbloqueados_no_avisa(self):
        respuesta = self._patch(self._logro(), {"condicion": "RIEGOS_TOTAL"}, desbloqueos=0)
        self.assertEqual(respuesta.status_code, 200)
        self.assertNotIn("advertencia", respuesta.data)

    def test_mismo_valor_de_condicion_no_avisa(self):
        respuesta = self._patch(self._logro(), {"condicion": "NIVEL"}, desbloqueos=5)
        self.assertNotIn("advertencia", respuesta.data)

    def test_cambiar_a_condicion_que_no_admite_el_parametro_actual_da_400(self):
        logro = self._logro(condicion="ESCANEO_HORA", parametro="NOCHE")
        respuesta = self._patch(logro, {"condicion": "NIVEL"})
        self.assertEqual(respuesta.status_code, 400)
        logro.save.assert_not_called()
        # enviando parametro null a la vez, si se permite
        respuesta = self._patch(self._logro(condicion="ESCANEO_HORA", parametro="NOCHE"),
                                {"condicion": "NIVEL", "parametro": None}, desbloqueos=1)
        self.assertEqual(respuesta.status_code, 200)
        self.assertIsNone(respuesta.data["parametro"])

    def test_patch_valida_igual_que_el_alta(self):
        for datos in ({"titulo": "T" * 81}, {"descripcion": "D" * 251}, {"meta": 0}, {"recompensa_xp": -1},
                      {"rareza": "X"}, {"condicion": "X"}, {"activo": "si"}, {"orden": "uno"}):
            self.assertEqual(self._patch(self._logro(), datos).status_code, 400, datos)

    # --- opciones ---
    def test_opciones_para_armar_el_formulario(self):
        with patch.object(admin_views, "_codigos_desastre", return_value=self.DESASTRES):
            respuesta = self._llamar(admin_views.AdminLogroOpcionesView, "get")
        self.assertEqual(respuesta.status_code, 200)
        condiciones = {c["codigo"]: c for c in respuesta.data["condiciones"]}
        self.assertEqual(len(condiciones), 17)
        self.assertTrue(all(c["explicacion"] for c in condiciones.values()))
        self.assertTrue(condiciones["ESCANEO_HORA"]["requiere_parametro"])
        self.assertEqual(condiciones["ESCANEO_HORA"]["valores_permitidos"], ["MADRUGADA", "NOCHE"])
        self.assertEqual(condiciones["DESASTRE_AFRONTADO"]["parametro"], "opcional")
        self.assertFalse(condiciones["DESASTRE_AFRONTADO"]["requiere_parametro"])
        self.assertEqual(condiciones["DESASTRE_AFRONTADO"]["valores_permitidos"], self.DESASTRES)
        self.assertEqual(condiciones["NIVEL"]["parametro"], "ninguno")
        self.assertEqual(condiciones["NIVEL"]["valores_permitidos"], [])
        self.assertEqual(respuesta.data["rarezas"], ["COMUN", "RARA", "EPICA", "LEGENDARIA"])
        self.assertIn("luna", respuesta.data["iconos"])
        self.assertEqual(len(respuesta.data["iconos"]), 21)


class CuerposRarosEnElAdminTests(SimpleTestCase):
    """QA-10: ningun endpoint del panel responde 500 con [], null, "x", 5 ni con tipos raros."""

    RUTAS = [
        (admin_views.AdminLoginView, "post", {}),
        (admin_views.AdminMisionListCreateView, "post", {}),
        (admin_views.AdminMisionDetailView, "patch", {"pk": 1}),
        (admin_views.AdminLogroDetailView, "patch", {"pk": 1}),
        (admin_views.AdminParametroDetailView, "patch", {"clave": "COSTO_REVIVIR"}),
        (admin_views.AdminTiendaListCreateView, "post", {}),
        (admin_views.AdminTiendaDetailView, "patch", {"pk": 10}),
        (admin_views.AdminEventoCreateView, "post", {}),
        (admin_views.AdminEventoFinalizarView, "patch", {"pk": 1}),
        (admin_views.AdminReglaDetailView, "patch", {"pk": 3}),
        (admin_views.AdminTipoDesastreDetailView, "patch", {"pk": 1}),
        (admin_views.AdminCatalogoUpdateView, "patch", {"pk": 1}),
    ]

    def test_cuerpo_que_no_es_objeto_da_400_en_todas_las_rutas(self):
        import json

        for vista, metodo, kwargs in self.RUTAS:
            for cuerpo in ([], None, "x", 5):
                request = getattr(APIRequestFactory(), metodo)("/api/admin/x/", json.dumps(cuerpo), content_type="application/json")
                with patch.object(admin_views, "_admin_desde_request", return_value=_usuario()):
                    respuesta = vista.as_view()(request, **kwargs)
                self.assertEqual(respuesta.status_code, 400, (vista.__name__, cuerpo))
                self.assertIn("error", respuesta.data)

    def test_login_con_correo_que_no_es_texto_da_400(self):
        request = APIRequestFactory().post("/api/admin/login/", {"email": 5, "password": ["x"]}, format="json")
        self.assertEqual(admin_views.AdminLoginView.as_view()(request).status_code, 400)

    def test_condicion_no_hashable_en_un_logro_da_400(self):
        from unittest.mock import MagicMock

        logro = admin_views.Logro(id_logro=1, codigo="X", condicion="ESCANEOS_TOTAL", parametro=None)
        for condicion in ([], {}):
            request = APIRequestFactory().patch("/api/admin/logros/1/", {"condicion": condicion}, format="json")
            with patch.object(admin_views, "_admin_desde_request", return_value=_usuario()), patch.object(
                    admin_views.Logro.objects, "filter", return_value=MagicMock(first=lambda: logro)):
                respuesta = admin_views.AdminLogroDetailView.as_view()(request, pk=1)
            self.assertEqual(respuesta.status_code, 400)

    def test_enteros_enormes_dan_400(self):
        from unittest.mock import MagicMock

        mision = admin_views.Mision(id_mision=1, titulo="Riego", tipo="RIEGO", meta=2, activa=True)
        mision.save = MagicMock()
        request = APIRequestFactory().patch("/api/admin/misiones/1/", {"meta": 2**40}, format="json")
        with patch.object(admin_views, "_admin_desde_request", return_value=_usuario()), patch.object(
                admin_views.Mision.objects, "filter", return_value=MagicMock(first=lambda: mision)):
            self.assertEqual(admin_views.AdminMisionDetailView.as_view()(request, pk=1).status_code, 400)
        mision.save.assert_not_called()
        valores, error = admin_views._validar_item({"precio_monedas": 2**40}, parcial=True)
        self.assertIsNotNone(error)

    def test_error_no_previsto_responde_json_sin_traza(self):
        from config.errores import manejar_excepcion

        respuesta = manejar_excepcion(RuntimeError("detalle interno"), {"view": None})
        self.assertEqual(respuesta.status_code, 500)
        self.assertEqual(respuesta.data, {"error": "Error interno del servidor"})


class CatalogoAdminValidacionTests(SimpleTestCase):
    """QA-10 y QA-11: editar una especie valida tipo y rango, y no deja dos nombres cientificos iguales."""

    def _patch(self, datos, colision=False):
        from unittest.mock import MagicMock

        planta = admin_views.PlantaCatalogo(id_catalogo=1, nombre_comun="Rosa", nombre_cientifico="Rosa qaensis",
                                            frecuencia_riego_dias=3, frecuencia_sol_horas=4, temperatura_min_c=10,
                                            temperatura_max_c=30)
        planta.save = MagicMock()
        consulta = MagicMock()
        consulta.exclude.return_value.exists.return_value = colision
        request = APIRequestFactory().patch("/api/admin/catalogo/1/", datos, format="json")
        with patch.object(admin_views, "_admin_desde_request", return_value=_usuario()), patch.object(
                admin_views.PlantaCatalogo.objects, "get", return_value=planta), patch.object(
                admin_views.PlantaCatalogo.objects, "filter", return_value=consulta) as filtro:
            respuesta = admin_views.AdminCatalogoUpdateView.as_view()(request, pk=1)
        return respuesta, planta, filtro

    def test_nombre_cientifico_que_solo_cambia_mayusculas_de_otra_da_409(self):
        respuesta, planta, filtro = self._patch({"nombre_cientifico": "rosa qaensis"}, colision=True)
        self.assertEqual(respuesta.status_code, 409)
        planta.save.assert_not_called()
        self.assertEqual(filtro.call_args.kwargs, {"nombre_cientifico__iexact": "rosa qaensis"})

    def test_valores_raros_dan_400_y_no_guardan(self):
        for datos in ({"nombre_cientifico": ""}, {"nombre_cientifico": None}, {"frecuencia_riego_dias": -5},
                      {"frecuencia_riego_dias": "abc"}, {"publicada": "quizas"}, {"nombre_comun": "x" * 500},
                      {"temperatura_min_c": 50, "temperatura_max_c": -10}, {"temperatura_min_c": 40}):
            respuesta, planta, _ = self._patch(datos)
            self.assertEqual(respuesta.status_code, 400, datos)
            planta.save.assert_not_called()

    def test_un_cambio_valido_se_guarda(self):
        respuesta, planta, _ = self._patch({"nombre_comun": "  Rosa china ", "publicada": False})
        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(planta.nombre_comun, "Rosa china")
        planta.save.assert_called_once_with(update_fields=["nombre_comun", "publicada", "updated_at"])


class CancelarEventoTests(SimpleTestCase):
    """QA-07: cancelar desde el panel termina el evento SIN danos (procesado) y quita la prevencion."""

    def _cancelar(self, evento, datos=None):
        from unittest.mock import MagicMock

        evento.save = MagicMock()
        plantas = MagicMock()
        plantas.filter.return_value = plantas
        plantas.update.return_value = 3
        request = APIRequestFactory().patch("/api/admin/eventos/5/", datos if datos is not None else {"estado": "FINALIZADO"},
                                            format="json")
        with patch.object(admin_views, "_admin_desde_request", return_value=_usuario()), patch.object(
                admin_views.EventoDesastre.objects, "select_for_update") as sfu, patch.object(
                admin_views.PlantaUsuario.objects, "filter", return_value=plantas), patch.object(
                admin_views.transaction, "atomic", return_value=MagicMock()):
            sfu.return_value.filter.return_value.first.return_value = evento
            respuesta = admin_views.AdminEventoFinalizarView.as_view()(request, pk=5)
        return respuesta, plantas

    def _evento(self, estado, empieza_en):
        from datetime import timedelta

        inicio = timezone.now() + empieza_en
        return admin_views.EventoDesastre(id_desastre=5, estado=estado, procesado=False, fecha_inicio=inicio,
                                          fecha_fin=inicio + timedelta(days=2), id_region=None)

    def test_cancelar_un_evento_futuro_lo_deja_procesado_y_sin_ventana(self):
        from datetime import timedelta

        evento = self._evento("PROGRAMADO", timedelta(days=30))
        respuesta, plantas = self._cancelar(evento)
        self.assertEqual(respuesta.status_code, 200)
        self.assertTrue(respuesta.data["cancelado"])
        self.assertEqual(evento.estado, "FINALIZADO")
        self.assertTrue(evento.procesado)  # Spring no aplicara danos
        self.assertLessEqual(evento.fecha_fin, timezone.now())
        plantas.update.assert_called_once_with(esta_refugiada=False, refugiada_desde=None)

    def test_cancelar_un_evento_en_curso_tampoco_dana(self):
        from datetime import timedelta

        evento = self._evento("ACTIVO", -timedelta(hours=1))
        respuesta, _ = self._cancelar(evento)
        self.assertEqual(respuesta.status_code, 200)
        self.assertTrue(evento.procesado)

    def test_un_evento_ya_terminado_no_se_toca(self):
        from datetime import timedelta

        evento = self._evento("FINALIZADO", -timedelta(days=5))
        respuesta, plantas = self._cancelar(evento)
        self.assertFalse(respuesta.data["cancelado"])
        self.assertFalse(evento.procesado)
        evento.save.assert_not_called()
        plantas.update.assert_not_called()

    def test_otro_estado_da_400(self):
        from datetime import timedelta

        evento = self._evento("ACTIVO", -timedelta(hours=1))
        respuesta, _ = self._cancelar(evento, {"estado": "XYZ"})
        self.assertEqual(respuesta.status_code, 400)
        evento.save.assert_not_called()


class SesionPropiaDelPanelTests(SimpleTestCase):
    """QA-19: el login del panel guarda su token aparte; la sesion de la app (session_token) no se toca."""

    def test_login_no_pisa_la_sesion_de_la_app(self):
        import bcrypt
        from unittest.mock import MagicMock

        clave = "clave-de-prueba-" + "x" * 4
        u = Usuario(id_usuario=8, email="a@b.c", nombre="A", rol="ADMIN", session_token="token-de-la-app",
                    password_hash=bcrypt.hashpw(clave.encode(), bcrypt.gensalt(4)).decode())
        u.save = MagicMock()
        request = APIRequestFactory().post("/api/admin/login/", {"email": "a@b.c", "password": clave}, format="json")
        with patch.object(Usuario.objects, "filter", return_value=MagicMock(first=lambda: u)):
            respuesta = admin_views.AdminLoginView.as_view()(request)
        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(u.session_token, "token-de-la-app")
        self.assertEqual(u.admin_session_token, admin_views._hash_token(respuesta.data["token"]))
        u.save.assert_called_once_with(update_fields=["admin_session_token", "admin_session_expira"])

    def test_el_token_de_la_app_no_sirve_en_el_panel(self):
        u = _usuario()
        u.session_token = admin_views._hash_token("otro")
        request = APIRequestFactory().get("/api/admin/stats/", HTTP_X_ADMIN_ID="8", HTTP_X_ADMIN_TOKEN="otro")
        with patch.object(Usuario.objects, "get", return_value=u):
            self.assertIsNone(admin_views._admin_desde_request(request))


class CatalogoPublicoTests(SimpleTestCase):
    """QA-20: el catalogo publico solo muestra especies publicadas; el panel lista todas en /api/admin/catalogo/."""

    def test_lista_y_detalle_filtran_publicadas(self):
        from unittest.mock import MagicMock

        from . import views

        consulta = MagicMock()
        with patch.object(views.PlantaCatalogo.objects, "filter", return_value=consulta) as filtro:
            vista = views.CatalogoListView()
            vista.request = MagicMock(query_params={"q": "ro\x00sa"})
            vista.get_queryset()
        filtro.assert_called_once_with(publicada=True)
        self.assertIn("publicada", str(views.CatalogoDetailView.queryset.query.where))

    def test_ruta_admin_del_catalogo_registrada(self):
        from django.urls import reverse

        self.assertEqual(reverse("admin-catalogo-list"), "/api/admin/catalogo/")


class BorradoProtegidoEnElAdminTests(SimpleTestCase):
    """QA-21: el sitio /admin/ no deja borrar articulos, misiones ni eventos (se perdia el historial)."""

    def test_sin_permiso_de_borrar_ni_accion_masiva(self):
        from django.contrib import admin as sitio

        from .models import EventoDesastre, ItemTienda, Mision, RegistroDesastreUsuario

        for modelo in (ItemTienda, Mision, EventoDesastre):
            modelo_admin = sitio.site._registry[modelo]
            self.assertFalse(modelo_admin.has_delete_permission(None), modelo.__name__)
        self.assertEqual(RegistroDesastreUsuario._meta.get_field("id_desastre").remote_field.on_delete.__name__, "PROTECT")


class LimiteDeIntentosDelPanelTests(SimpleTestCase):
    """QA-27: tras 5 claves equivocadas el login del panel responde 429."""

    def test_quinto_fallo_bloquea_el_correo(self):
        from unittest.mock import MagicMock

        from django.core.cache import cache

        cache.clear()
        codigos = []
        with patch.object(Usuario.objects, "filter", return_value=MagicMock(first=lambda: None)):
            for i in range(6):
                request = APIRequestFactory().post("/api/admin/login/", {"email": "x@y.z", "password": f"mala{i}"}, format="json")
                codigos.append(admin_views.AdminLoginView.as_view()(request).status_code)
        self.assertEqual(codigos, [401, 401, 401, 401, 401, 429])
        cache.clear()
