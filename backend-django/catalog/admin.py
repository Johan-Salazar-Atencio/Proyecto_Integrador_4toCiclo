"""Panel de administracion de Plantia (curacion del catalogo y cuidados)."""
import os

from django import forms
from django.conf import settings
from django.contrib import admin
from django.utils import timezone
from django.utils.html import format_html

from . import eventos_imagen

from .models import (
    CuidadoSeccion,
    EscaneoHistorial,
    EventoDesastre,
    FondoEscena,
    ItemTienda,
    Logro,
    Mision,
    ParametroJuego,
    PlantaCatalogo,
    PlantaUsuario,
    ReglaProgreso,
    Region,
    RegistroDesastreUsuario,
    TemaFondo,
    TipoDesastre,
    Usuario,
)


class CuidadoSeccionInline(admin.TabularInline):
    model = CuidadoSeccion
    extra = 0
    fields = ("tipo", "titulo", "contenido", "orden", "generado_por_ia")


@admin.register(PlantaCatalogo)
class PlantaCatalogoAdmin(admin.ModelAdmin):
    list_display = (
        "id_catalogo",
        "nombre_comun",
        "nombre_cientifico",
        "frecuencia_riego_dias",
        "frecuencia_sol_horas",
        "es_toxica_mascotas",
        "updated_at",
    )
    list_filter = ("es_toxica_mascotas",)
    search_fields = ("nombre_comun", "nombre_cientifico")
    inlines = [CuidadoSeccionInline]
    readonly_fields = ("fecha_creacion", "updated_at")


@admin.register(CuidadoSeccion)
class CuidadoSeccionAdmin(admin.ModelAdmin):
    list_display = ("id_seccion", "id_catalogo", "tipo", "titulo", "orden", "generado_por_ia")
    list_filter = ("tipo", "generado_por_ia")
    search_fields = ("titulo", "contenido", "id_catalogo__nombre_comun")


class RegistroDesastreInline(admin.TabularInline):
    """Resultado del evento en cada planta (solo lectura: lo escribe Spring)."""

    model = RegistroDesastreUsuario
    extra = 0
    can_delete = False
    readonly_fields = ("id_planta_usuario", "estado_final", "dano_salud_aplicado", "estaba_refugiada", "xp_otorgado")
    fields = readonly_fields

    def has_add_permission(self, request, obj=None):
        return False


@admin.register(EventoDesastre)
class EventoDesastreAdmin(admin.ModelAdmin):
    list_display = (
        "id_desastre",
        "id_tipo_desastre",
        "id_region",
        "severidad",
        "estado",
        "fecha_inicio",
        "fecha_fin",
    )
    list_filter = ("id_tipo_desastre", "estado", "severidad", "id_region")
    search_fields = ("mensaje_notificacion", "id_region__nombre")
    inlines = [RegistroDesastreInline]
    actions = ["activar", "finalizar"]

    # QA-21: no se borra (se perderia el historial de los usuarios); se desactiva o se cancela
    def has_delete_permission(self, request, obj=None):
        return False

    def get_actions(self, request):
        acciones = super().get_actions(request)
        acciones.pop("delete_selected", None)
        return acciones

    @admin.action(description="Marcar como ACTIVO")
    def activar(self, request, queryset):
        queryset.update(estado="ACTIVO")

    @admin.action(description="Cancelar (terminar sin danos)")
    def finalizar(self, request, queryset):
        # QA-07 y QA-34: como el panel, inicio = fin = ahora es la marca de cancelado; Spring lo procesa sin danos
        # y quita la prevencion con su orden de candados
        ahora = timezone.now()
        queryset.exclude(estado="FINALIZADO").update(estado="FINALIZADO", procesado=False, fecha_inicio=ahora, fecha_fin=ahora)


@admin.register(ItemTienda)
class ItemTiendaAdmin(admin.ModelAdmin):
    list_display = ("id_item", "nombre", "tipo", "precio_monedas", "nivel_requerido")
    list_filter = ("tipo",)
    search_fields = ("nombre",)

    # QA-21: no se borra (se perderia el historial de los usuarios); se desactiva o se cancela
    def has_delete_permission(self, request, obj=None):
        return False

    def get_actions(self, request):
        acciones = super().get_actions(request)
        acciones.pop("delete_selected", None)
        return acciones


@admin.register(Logro)
class LogroAdmin(admin.ModelAdmin):
    """Logros del juego. No se borran (se pierde el progreso de los usuarios): solo se activan o desactivan."""

    list_display = ("orden", "codigo", "titulo", "rareza", "condicion", "meta", "recompensa_xp",
                    "recompensa_monedas", "secreto", "activo")
    list_display_links = ("codigo", "titulo")
    list_filter = ("rareza", "condicion", "activo", "secreto")
    search_fields = ("codigo", "titulo", "descripcion")
    ordering = ("orden", "id_logro")
    actions = ["activar", "desactivar"]

    def get_readonly_fields(self, request, obj=None):
        # El codigo lo usa Spring para reconocer el logro: no cambia una vez creado
        base = ["desbloqueos_total"]
        return base + ["codigo"] if obj is not None else base

    @admin.display(description="Usuarios que lo desbloquearon")
    def desbloqueos_total(self, obj):
        return obj.desbloqueos() if obj is not None and obj.pk else 0

    def has_delete_permission(self, request, obj=None):
        return False

    def get_actions(self, request):
        acciones = super().get_actions(request)
        acciones.pop("delete_selected", None)
        return acciones

    @admin.action(description="Activar los logros seleccionados")
    def activar(self, request, queryset):
        queryset.update(activo=True)

    @admin.action(description="Desactivar los logros seleccionados")
    def desactivar(self, request, queryset):
        queryset.update(activo=False)


class PlantaUsuarioInline(admin.TabularInline):
    """Plantas digitales del usuario (solo lectura)."""

    model = PlantaUsuario
    extra = 0
    can_delete = False
    readonly_fields = ("apodo", "etapa_crecimiento", "esta_refugiada")
    fields = readonly_fields

    def has_add_permission(self, request, obj=None):
        return False


@admin.register(Usuario)
class UsuarioAdmin(admin.ModelAdmin):
    inlines = [PlantaUsuarioInline]
    list_display = ("id_usuario", "nombre", "email", "rol", "nivel", "monedas", "id_region")
    list_filter = ("rol", "id_region")
    search_fields = ("nombre", "email")
    readonly_fields = ("password_hash", "fecha_registro", "updated_at")


@admin.register(EscaneoHistorial)
class EscaneoHistorialAdmin(admin.ModelAdmin):
    list_display = (
        "id_escaneo",
        "id_usuario",
        "id_catalogo",
        "confianza_ia",
        "fecha_escaneo",
    )
    list_filter = ("fecha_escaneo",)
    search_fields = ("id_usuario__nombre", "id_catalogo__nombre_comun")
    readonly_fields = (
        "id_usuario",
        "id_catalogo",
        "imagen_foto_url",
        "confianza_ia",
        "respuesta_ia_raw",
        "fecha_escaneo",
    )

    def has_add_permission(self, request):
        return False


@admin.register(Region)
class RegionAdmin(admin.ModelAdmin):
    list_display = ("id_region", "codigo", "nombre", "activa")
    list_filter = ("activa",)
    search_fields = ("codigo", "nombre")


class TipoDesastreForm(forms.ModelForm):
    """Deja subir la imagen del evento (la de la tarjeta del Inicio) sin escribir su direccion a mano."""

    subir_imagen = forms.FileField(
        required=False,
        label="Subir imagen del evento",
        help_text="PNG, JPG o WebP, de al menos 400 px de ancho. Se reduce y reemplaza la actual.",
    )

    class Meta:
        model = TipoDesastre
        fields = "__all__"

    def clean_subir_imagen(self):
        archivo = self.cleaned_data.get("subir_imagen")
        if archivo is None:
            return None
        try:
            self.imagen_procesada = eventos_imagen.procesar(archivo.read())
        except eventos_imagen.ErrorImagen as exc:
            raise forms.ValidationError(str(exc))
        return archivo


def url_de_imagen(ruta):
    """Direccion completa de una imagen que Spring sirve en /files (None si no hay)."""
    if not ruta:
        return None
    return ruta if ruta.startswith("http") else f"{settings.SPRING_PUBLIC_URL}{ruta}"


@admin.register(TipoDesastre)
class TipoDesastreAdmin(admin.ModelAdmin):
    form = TipoDesastreForm
    list_display = ("id_tipo_desastre", "vista_imagen", "codigo", "nombre", "color_hex", "activo")
    list_filter = ("activo",)
    search_fields = ("codigo", "nombre", "descripcion")
    readonly_fields = ("fecha_creacion", "vista_imagen")

    @admin.display(description="Imagen")
    def vista_imagen(self, obj):
        url = url_de_imagen(obj.imagen_url) if obj else None
        if not url:
            return "Sin imagen"
        return format_html('<img src="{}" alt="Imagen del evento" style="max-height:80px;border-radius:6px">', url)

    def save_model(self, request, obj, form, change):
        jpg = getattr(form, "imagen_procesada", None) if form.cleaned_data.get("subir_imagen") else None
        if jpg is not None:
            nombre = eventos_imagen.guardar(os.path.join(settings.STORAGE_IMAGES_DIR, "eventos"), obj.codigo, jpg)
            obj.imagen_url = f"/files/eventos/{nombre}"
        super().save_model(request, obj, form, change)


@admin.register(TemaFondo)
class TemaFondoAdmin(admin.ModelAdmin):
    list_display = ("id_tema", "nombre", "es_predeterminado", "activo")
    list_filter = ("es_predeterminado", "activo")
    search_fields = ("nombre",)


@admin.register(FondoEscena)
class FondoEscenaAdmin(admin.ModelAdmin):
    list_display = ("id_fondo", "nombre", "momento", "id_tema", "total_frames", "activo")
    list_filter = ("momento", "activo")
    search_fields = ("nombre",)


@admin.register(Mision)
class MisionAdmin(admin.ModelAdmin):
    list_display = ("id_mision", "titulo", "tipo", "meta", "recompensa_monedas", "recompensa_xp", "activa")
    list_filter = ("tipo", "activa", "predeterminada")
    search_fields = ("titulo", "descripcion")

    # QA-21: no se borra (se perderia el historial de los usuarios); se desactiva o se cancela
    def has_delete_permission(self, request, obj=None):
        return False

    def get_actions(self, request):
        acciones = super().get_actions(request)
        acciones.pop("delete_selected", None)
        return acciones


@admin.register(ParametroJuego)
class ParametroJuegoAdmin(admin.ModelAdmin):
    list_display = ("clave", "valor", "descripcion")
    search_fields = ("clave", "descripcion")


@admin.register(ReglaProgreso)
class ReglaProgresoAdmin(admin.ModelAdmin):
    list_display = ("id_regla", "motivo", "xp", "monedas", "tope_diario", "activa")
    list_filter = ("activa",)
    search_fields = ("motivo",)


@admin.register(PlantaUsuario)
class PlantaUsuarioAdmin(admin.ModelAdmin):
    list_display = ("id_planta_usuario", "apodo", "id_usuario", "etapa_crecimiento", "esta_refugiada")
    list_filter = ("etapa_crecimiento", "esta_refugiada")
    search_fields = ("apodo", "id_usuario__nombre")


@admin.register(RegistroDesastreUsuario)
class RegistroDesastreUsuarioAdmin(admin.ModelAdmin):
    list_display = ("id_registro", "id_desastre", "id_planta_usuario", "estado_final", "dano_salud_aplicado", "xp_otorgado")
    list_filter = ("estado_final", "estaba_refugiada")
    readonly_fields = ("id_desastre", "id_planta_usuario", "estado_final", "dano_salud_aplicado", "fecha_procesado", "estaba_refugiada", "xp_otorgado")
