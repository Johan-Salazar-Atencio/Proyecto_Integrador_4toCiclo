"""Panel de administracion de Plantia (curacion del catalogo y cuidados)."""
from django.contrib import admin

from .models import (
    CuidadoSeccion,
    EscaneoHistorial,
    EventoDesastre,
    ItemTienda,
    Logro,
    PlantaCatalogo,
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


@admin.register(EventoDesastre)
class EventoDesastreAdmin(admin.ModelAdmin):
    list_display = (
        "id_desastre",
        "tipo_evento",
        "region",
        "severidad",
        "estado",
        "fecha_inicio",
        "fecha_fin",
    )
    list_filter = ("tipo_evento", "estado", "severidad", "region")
    search_fields = ("mensaje_notificacion", "region")
    actions = ["activar", "finalizar"]

    @admin.action(description="Marcar como ACTIVO")
    def activar(self, request, queryset):
        queryset.update(estado="ACTIVO")

    @admin.action(description="Marcar como FINALIZADO")
    def finalizar(self, request, queryset):
        queryset.update(estado="FINALIZADO")


@admin.register(ItemTienda)
class ItemTiendaAdmin(admin.ModelAdmin):
    list_display = ("id_item", "nombre", "tipo", "precio_monedas", "nivel_requerido")
    list_filter = ("tipo",)
    search_fields = ("nombre",)


@admin.register(Logro)
class LogroAdmin(admin.ModelAdmin):
    list_display = ("id_logro", "nombre", "recompensa_monedas")
    search_fields = ("nombre",)


@admin.register(Usuario)
class UsuarioAdmin(admin.ModelAdmin):
    list_display = ("id_usuario", "nombre", "email", "rol", "nivel", "monedas", "region")
    list_filter = ("rol", "region")
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
