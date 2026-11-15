from django.urls import path

from . import admin_views

urlpatterns = [
    path("login/", admin_views.AdminLoginView.as_view(), name="admin-login"),
    path("catalogo/<int:pk>/", admin_views.AdminCatalogoUpdateView.as_view(), name="admin-catalogo-update"),
    path("eventos/", admin_views.AdminEventoCreateView.as_view(), name="admin-evento-create"),
    path("eventos/<int:pk>/", admin_views.AdminEventoFinalizarView.as_view(), name="admin-evento-finalize"),
    path("stats/", admin_views.AdminStatsView.as_view(), name="admin-stats"),
]
