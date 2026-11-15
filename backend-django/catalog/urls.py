from django.urls import path

from . import admin_views, views

urlpatterns = [
    path("", views.CatalogoListView.as_view(), name="catalogo-list"),
    path("eventos/", views.EventoListView.as_view(), name="evento-list"),
    path("eventos/activo/", views.EventoActivoView.as_view(), name="evento-activo"),
    path("<int:pk>/", views.CatalogoDetailView.as_view(), name="catalogo-detail"),
    # --- administracion (escritura) ---
    path("admin/login/", admin_views.AdminLoginView.as_view(), name="admin-login"),
    path("admin/catalogo/<int:pk>/", admin_views.AdminCatalogoUpdateView.as_view(), name="admin-catalogo-update"),
    path("admin/eventos/", admin_views.AdminEventoCreateView.as_view(), name="admin-evento-create"),
    path("admin/eventos/<int:pk>/", admin_views.AdminEventoFinalizarView.as_view(), name="admin-evento-finalize"),
    path("admin/stats/", admin_views.AdminStatsView.as_view(), name="admin-stats"),
]
