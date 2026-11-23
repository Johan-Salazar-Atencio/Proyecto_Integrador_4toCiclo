from django.urls import path

from . import admin_views

urlpatterns = [
    path("login/", admin_views.AdminLoginView.as_view(), name="admin-login"),
    path("catalogo/", admin_views.AdminCatalogoListView.as_view(), name="admin-catalogo-list"),
    path("catalogo/<int:pk>/", admin_views.AdminCatalogoUpdateView.as_view(), name="admin-catalogo-update"),
    path("eventos/", admin_views.AdminEventoCreateView.as_view(), name="admin-evento-create"),
    path("eventos/<int:pk>/", admin_views.AdminEventoFinalizarView.as_view(), name="admin-evento-finalize"),
    path("eventos/<int:pk>/efectos/", admin_views.AdminEventoEfectosView.as_view(), name="admin-evento-efectos"),
    path("stats/", admin_views.AdminStatsView.as_view(), name="admin-stats"),
    path("misiones/", admin_views.AdminMisionListCreateView.as_view(), name="admin-mision-list"),
    path("misiones/<int:pk>/", admin_views.AdminMisionDetailView.as_view(), name="admin-mision-detail"),
    path("logros/", admin_views.AdminLogroListCreateView.as_view(), name="admin-logro-list"),
    path("logros/opciones/", admin_views.AdminLogroOpcionesView.as_view(), name="admin-logro-opciones"),
    path("logros/<int:pk>/", admin_views.AdminLogroDetailView.as_view(), name="admin-logro-detail"),
    path("parametros/", admin_views.AdminParametrosView.as_view(), name="admin-parametros"),
    path("parametros/<str:clave>/", admin_views.AdminParametroDetailView.as_view(), name="admin-parametro-detail"),
    path("tipos-desastre/", admin_views.AdminTiposDesastreView.as_view(), name="admin-tipos-desastre"),
    path("tipos-desastre/<int:pk>/", admin_views.AdminTipoDesastreDetailView.as_view(), name="admin-tipo-desastre-detail"),
    path("reglas/", admin_views.AdminReglasView.as_view(), name="admin-reglas"),
    path("reglas/<int:pk>/", admin_views.AdminReglaDetailView.as_view(), name="admin-regla-detail"),
    path("tienda/", admin_views.AdminTiendaListCreateView.as_view(), name="admin-tienda-list"),
    path("tienda/<int:pk>/", admin_views.AdminTiendaDetailView.as_view(), name="admin-tienda-detail"),
]
