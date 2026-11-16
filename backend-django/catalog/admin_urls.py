from django.urls import path

from . import admin_views

urlpatterns = [
    path("login/", admin_views.AdminLoginView.as_view(), name="admin-login"),
    path("catalogo/<int:pk>/", admin_views.AdminCatalogoUpdateView.as_view(), name="admin-catalogo-update"),
    path("eventos/", admin_views.AdminEventoCreateView.as_view(), name="admin-evento-create"),
    path("eventos/<int:pk>/", admin_views.AdminEventoFinalizarView.as_view(), name="admin-evento-finalize"),
    path("eventos/<int:pk>/efectos/", admin_views.AdminEventoEfectosView.as_view(), name="admin-evento-efectos"),
    path("stats/", admin_views.AdminStatsView.as_view(), name="admin-stats"),
    path("misiones/", admin_views.AdminMisionListCreateView.as_view(), name="admin-mision-list"),
    path("misiones/<int:pk>/", admin_views.AdminMisionDetailView.as_view(), name="admin-mision-detail"),
    path("parametros/", admin_views.AdminParametrosView.as_view(), name="admin-parametros"),
    path("parametros/<str:clave>/", admin_views.AdminParametroDetailView.as_view(), name="admin-parametro-detail"),
    path("tienda/", admin_views.AdminTiendaListCreateView.as_view(), name="admin-tienda-list"),
    path("tienda/<int:pk>/", admin_views.AdminTiendaDetailView.as_view(), name="admin-tienda-detail"),
]
