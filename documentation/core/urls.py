from django.contrib import admin
from django.urls import path
from .views import *
urlpatterns = [
    path('mapping/<int:id>', mapping, name="mapping"),
    path('', mapping_overview, name="overview"),

    path('mapping/<int:id>/edit', edit_mapping, name="edit-mapping"),
    path('mapping/<int:id>/edit/add', add_template_class, name="add-template-class"),
    path('template/<int:pk>/edit', TemplateClassUpdateView.as_view(), name="edit-template"),
    path('template_attribute/<int:id>/delete', delete_template_attribute, name="delete-template-attribute"),
    path('template_attribute/<int:pk>/edit', TemplateAttributeUpdateView.as_view(), name="edit-template-attribute"),
    path('template_class/<int:id>/delete', delete_template_class, name="delete-template-class"),
    path('template_class/<int:id>/add', add_template_attribute, name="add-template-attribute"),
    path('mapping/create', MappingCreateView.as_view(), name="mapping-create"),


]