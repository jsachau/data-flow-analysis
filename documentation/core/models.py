from django.db import models
from django.shortcuts import reverse
NAME_LENGTH = 128
TYPE_LENGTH = 128
URL_LENGTH = 512
PATH_LENGTH = 512
TEMPLATE_ID_LENGTH = 128

def clean_type_name(name: str):

    if "<" in name:
        first, second = name.split("<")
        second = second[:-1]
        return f"{clean_type_name(first)}<{clean_type_name(second)}>"
    else:
        return name.split(".")[-1]


class Mapping(models.Model):
    name = models.CharField(max_length=NAME_LENGTH)
    template_id = models.CharField(max_length=TEMPLATE_ID_LENGTH)

    description = models.TextField()
    author = models.CharField(max_length=NAME_LENGTH)
    email = models.CharField(max_length=NAME_LENGTH)
    organisation = models.CharField(max_length=NAME_LENGTH)
    date = models.DateField(blank=True, null=True)


    @property
    def display_name(self):
        return self.name.replace("_", " ").title()

    def get_absolute_url(self):  # new
        return reverse('mapping', args=[self.pk])

    def get_edit_url(self):  # new
        return reverse('edit-mapping', args=[self.pk])

class TemplateClass(models.Model):
    name = models.CharField(max_length=NAME_LENGTH)
    archetyp_id = models.CharField(max_length=PATH_LENGTH)

    mapping = models.ForeignKey(Mapping, on_delete=models.CASCADE, null=True, default=None,
                                related_name='template_classes')

    @property
    def has_mapping_data(self):
        return any((a.has_mapping_data for a in self.attributes.all()))

    @property
    def depth(self):
        if TemplateAttribute.objects.filter(type_class=self).exists():
            attribute = TemplateAttribute.objects.get(type_class=self)
            return attribute.depth + 1

        return 0

    @property
    def graph_id(self):
        return f"template_{self.pk}"

    def get_absolute_url(self):  # new
        return self.mapping.get_edit_url()

    def __str__(self):
        return f"{self.name} ({self.archetyp_id})"


class TemplateAttribute(models.Model):
    name = models.CharField(max_length=NAME_LENGTH)
    type_name = models.CharField(max_length=TYPE_LENGTH)
    type_class = models.ForeignKey(TemplateClass, default=None, null=True, blank=True, on_delete=models.SET_NULL)
    path = models.CharField(max_length=PATH_LENGTH)

    parent_template_class = models.ForeignKey(TemplateClass, on_delete=models.CASCADE, related_name="attributes")

    @property
    def has_mapping_data(self):
        return self.mapped_from.exists() or (self.type_class is not None and self.type_class.has_mapping_data)

    @property
    def depth(self):
        return self.parent_template_class.depth + 1

    @property
    def graph_id(self):
        return f"template_attr_{self.pk}"

    @property
    def cleaned_type_name(self):
        return clean_type_name(self.type_name)

    def get_absolute_url(self):  # new
        return self.parent_template_class.mapping.get_edit_url()

class FHIRProfile(models.Model):
    name = models.CharField(max_length=NAME_LENGTH)
    url = models.CharField(max_length=URL_LENGTH)

    mapping = models.OneToOneField(Mapping, null=True, default=None,
                                   on_delete=models.CASCADE, related_name='profile')

    @property
    def graph_id(self):
        return f"profile_{self.pk}"

    @property
    def depth(self):
        return 0


class FHIRAttribute(models.Model):
    name = models.CharField(max_length=NAME_LENGTH)
    type_name = models.CharField(max_length=TYPE_LENGTH)

    profile = models.ForeignKey(FHIRProfile, on_delete=models.CASCADE, related_name="attributes")
    parent_attribute = models.ForeignKey('FHIRAttribute', default=None, null=True, on_delete=models.CASCADE, related_name="child_attributes")

    maps_to = models.ManyToManyField(TemplateAttribute, related_name='mapped_from')

    @property
    def has_mapping_data(self):
        return self.maps_to.exists() or any((a.has_mapping_data for a in self.child_attributes.all()))

    @property
    def graph_id(self):
        return f"profile_attr_{self.pk}"

    @property
    def depth(self):
        if self.parent_attribute is None:
            return self.profile.depth + 1
        return self.parent_attribute.depth + 1

    @property
    def cleaned_type_name(self):
        return clean_type_name(self.type_name)

    @property
    def full_path(self):
        if self.parent_attribute is not None:
            return self.parent_attribute.full_path + [self.name]

        return [self.name]