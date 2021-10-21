import django
import os

from django.utils.datetime_safe import datetime

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'documentation.settings')

django.setup()

import argparse
from core.models import *
from core.mapping_resolution import MappingResolution
import json
from tqdm import tqdm
import re
parser = argparse.ArgumentParser()

parser.add_argument('-f', '--file', required=True)

args = parser.parse_args()

with open(args.file, 'r') as f:
    mapping_data = json.load(f)

Mapping.objects.all().delete()

for mapping_raw in tqdm(mapping_data):
    info_raw = mapping_raw['info']

    mapping = Mapping.objects.create(name=mapping_raw['name'],
                                     template_id=info_raw['templateId'],
                                     description=info_raw['description'],
                                     author=info_raw['author'],
                                     organisation=info_raw['organisation'],
                                     email=info_raw['email'],
                                     date=datetime.strptime(info_raw['date'], "%Y-%m-%d").date() if info_raw['date'] else None)

    profile_name = next((cls["className"] for cls in mapping_raw["templateClasses"] if cls["isCompositionBase"]))
    fhir_profile = FHIRProfile.objects.create(name="Profile:" + profile_name.replace('Composition', ''), mapping=mapping)

    for class_raw in tqdm(mapping_raw["templateClasses"]):
        archetype = ""
        if "archetype" in class_raw:
            archetype=class_raw["archetype"]
        template_class = TemplateClass.objects.create(name=class_raw["className"],
                                                      archetyp_id=archetype, mapping=mapping)


        for field_raw in class_raw["fields"]:
            field = TemplateAttribute.objects.create(name=field_raw["fieldName"],
                                             type_name=field_raw["type"],
                                             path=field_raw["path"],
                                             parent_template_class=template_class)
            for field_mapping_raw in field_raw["observationTemplateFieldMappings"]:
                MappingResolution.create_observation_attributes(field, fhir_profile, field_mapping_raw)


    for attribute in mapping.profile.attributes.all():

        if attribute.name == "[0]":

            can_delete = True

            for child in attribute.parent_attribute.child_attributes.all():
                if child.name.startswith("[") and child.name != "[0]":
                    can_delete = False

            if can_delete:
                for child in list(attribute.child_attributes.all()):
                    child.parent_attribute = attribute.parent_attribute
                    child.save()
                attribute.delete()





for attribute in tqdm(TemplateAttribute.objects.all()):

    type_name = attribute.type_name
    if "List" in type_name:
        result = re.search(r"<([A-Za-z0-9_]+)>", type_name)
        type_name = result.group(1)

    elif "<" in type_name:
        print(type_name)

    type_template_class = TemplateClass.objects.filter(mapping=attribute.parent_template_class.mapping,
                                                       name=type_name)

    if type_template_class.exists():
        attribute.type_class = type_template_class.first()
        attribute.save()



