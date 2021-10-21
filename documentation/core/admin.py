from django.contrib import admin
from .models import *

admin.site.register(FHIRProfile)
admin.site.register(FHIRAttribute)
admin.site.register(TemplateAttribute)
admin.site.register(TemplateClass)