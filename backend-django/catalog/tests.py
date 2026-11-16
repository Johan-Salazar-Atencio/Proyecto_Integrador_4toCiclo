"""Autenticacion del panel admin (admin_views.py). Sin base de datos: se simula el usuario."""
import os
from datetime import timedelta
from unittest.mock import patch

from django.test import SimpleTestCase
from django.utils import timezone
from rest_framework.test import APIRequestFactory

from . import admin_views, fondos_imagen
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

    def test_una_maceta_no_se_puede_activar_todavia(self):
        from unittest.mock import MagicMock

        item = admin_views.ItemTienda(id_item=3, nombre="Maceta", tipo="MACETA", precio_monedas=80, activo=False)
        item.save = MagicMock()
        with patch.object(admin_views.ItemTienda.objects, "filter", return_value=MagicMock(first=lambda: item)):
            respuesta = self._llamar(admin_views.AdminTiendaDetailView, "patch", {"activo": True}, pk=3)
        self.assertEqual(respuesta.status_code, 400)
        item.save.assert_not_called()

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
