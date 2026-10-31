from django.apps import AppConfig


class AdminPanelConfig(AppConfig):
    default_auto_field = "django.db.models.BigAutoField"
    name = "admin_panel"
    verbose_name = "Panel de administracion Plantia"

    def ready(self):
        from django.contrib import admin

        admin.site.site_header = "Plantia - Administracion"
        admin.site.site_title = "Plantia Admin"
        admin.site.index_title = "Curacion de catalogo, cuidados y eventos"
