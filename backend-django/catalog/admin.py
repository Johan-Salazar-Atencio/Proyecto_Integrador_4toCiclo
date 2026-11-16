"""Panel de administracion de Plantia (curacion del catalogo y cuidados)."""
from django.contrib import admin

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
        "tipo_evento",
        "region",
        "severidad",
        "estado",
        "fecha_inicio",
        "fecha_fin",
    )
    list_filter = ("tipo_evento", "estado", "severidad", "region")
    search_fields = ("mensaje_notificacion", "region")
    inlines = [RegistroDesastreInline]
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


@admin.register(Region)
class RegionAdmin(admin.ModelAdmin):
    list_display = ("id_region", "codigo", "nombre", "activa")
    list_filter = ("activa",)
    search_fields = ("codigo", "nombre")


@admin.register(TipoDesastre)
class TipoDesastreAdmin(admin.ModelAdmin):
    list_display = ("id_tipo_desastre", "codigo", "nombre", "color_hex", "activo")
    list_filter = ("activo",)
    search_fields = ("codigo", "nombre", "descripcion")
    readonly_fields = ("fecha_creacion",)


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
