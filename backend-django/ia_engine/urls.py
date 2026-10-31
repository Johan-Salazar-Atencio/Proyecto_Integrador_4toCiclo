from django.urls import path

from . import views

urlpatterns = [
    path("identificar", views.IdentificarView.as_view(), name="ia-identificar"),
    path("cuidados", views.CuidadosView.as_view(), name="ia-cuidados"),
]
