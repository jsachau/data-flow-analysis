from django.shortcuts import render, get_object_or_404, redirect
from .models import *
from .graph import MappingGraph
from django.contrib.auth.mixins import LoginRequiredMixin

from django.views.generic import CreateView, UpdateView
from django.contrib.auth.decorators import login_required


class MappingCreateView(LoginRequiredMixin, CreateView):
    model = Mapping
    fields = ('name', 'template_id', 'description', 'author', 'email', 'organisation', 'date')
    template_name = "core/admin/create_mapping.html"


class TemplateClassUpdateView(LoginRequiredMixin, UpdateView):
    model = TemplateClass
    fields = ('name', 'name')
    template_name = "core/admin/edit_template_class.html"


class TemplateAttributeUpdateView(LoginRequiredMixin, UpdateView):
    model = TemplateAttribute
    fields = ('name', 'type_name', 'path', 'type_class')
    template_name = "core/admin/edit_template_attribute.html"

    def get_form(self, *args, **kwargs):
        form = super(TemplateAttributeUpdateView, self).get_form(*args, **kwargs)
        form.fields['type_class'].queryset = self.get_object().parent_template_class.mapping.template_classes.all()
        # self.request.user.a_set.all()
        # form.fields['b_a'].queryset = A.objects.filter(a_user=self.request.user)
        return form


def mapping(request, id):
    mapping = get_object_or_404(Mapping, pk=id)
    return render(request, 'core/mapping.html',
                  dict(graph=MappingGraph(mapping),
                       mapping=mapping,
                       page_title="Mapping documentation " + mapping.display_name))


def mapping_overview(request):
    return render(request, 'core/mapping_overview.html', dict(mappings=Mapping.objects.all()))


@login_required
def edit_mapping(request, id):
    mapping = get_object_or_404(Mapping, pk=id)
    return render(request, 'core/admin/edit_mapping.html',
                  dict(mapping=mapping))


@login_required
def delete_template_attribute(request, id):
    template_attribute = get_object_or_404(TemplateAttribute, pk=id)
    mapping = template_attribute.parent_template_class.mapping
    template_attribute.delete()

    return redirect(mapping.get_edit_url())


@login_required
def add_template_attribute(request, id):
    template_class = get_object_or_404(TemplateClass, pk=id)
    mapping = template_class.mapping

    TemplateAttribute.objects.create(name="New attribute", parent_template_class=template_class)
    return redirect(mapping.get_edit_url())


@login_required
def add_template_class(request, id):
    mapping = get_object_or_404(Mapping, pk=id)
    TemplateClass.objects.create(name="New Template Class", mapping=mapping)
    return redirect(mapping.get_edit_url())


@login_required
def delete_template_class(request, id):
    template_class = get_object_or_404(TemplateClass, pk=id)
    mapping = template_class.mapping
    template_class.delete()
    return redirect(mapping.get_edit_url())
