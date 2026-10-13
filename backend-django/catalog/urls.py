from django.urls import path

from . import views

urlpatterns = [
    path("", views.CatalogoListView.as_view(), name="catalogo-list"),
    path("eventos/", views.EventoListView.as_view(), name="evento-list"),
    path("eventos/activo/", views.EventoActivoView.as_view(), name="evento-activo"),
    path("<int:pk>/", views.CatalogoDetailView.as_view(), name="catalogo-detail"),
]
