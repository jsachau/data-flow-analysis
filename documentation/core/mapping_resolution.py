from .models import *


class MappingResolution:

    BREAK_METHODS = ["toZonedDateTime",
                     "getValueAsCalendar",
                     "isEmpty",
                     "equals",
                     "toString",
                     "doubleValue",
                     "toInstant",
                     "intValue"]

    @staticmethod
    def extract_getter(dependencies):

        nested_variables = []
        nested_variable_types = []

        dependencies = list(reversed(dependencies))

        current_scope = None

        for dependency in dependencies:
            if dependency["type"] == "VARIABLE" and dependency["variableType"].startswith("org.hl7.fhir.r4.model."):
                current_scope = dependency["variableName"]
                continue
            elif dependency["type"] == "METHOD_CALL":
                if "scope" in dependency and dependency["scope"] == current_scope:
                    method = dependency["method"]
                    if method in MappingResolution.BREAK_METHODS:
                        break

                    #print(method)

                    if method == "get":
                        method = "[" +  dependency["expression"][-2] + "]"
                    elif method.startswith('get'):
                        method = method[3].lower() + method[4:]

                    nested_variables.append(method)
                    nested_variable_types.append(dependency["returnType"])
                    current_scope = dependency["expression"]
                    continue
            break

        return nested_variables, nested_variable_types

    @staticmethod
    def create_observation_attributes(attribute: TemplateAttribute,
                                      profile: FHIRProfile,
                                      mapping_raw: dict):

        latest_parent = None

        #print(mapping_raw["dependencies"])
        #print(MappingResolution.extract_getter(mapping_raw["dependencies"]))

        latest_parent = None

        for var, var_type in zip(*MappingResolution.extract_getter(mapping_raw["dependencies"])):
            
            latest_parent, _ = FHIRAttribute.objects.get_or_create(profile=profile, name=var, type_name=var_type,
                                                                   parent_attribute=latest_parent)

        if latest_parent is not None:
            latest_parent.maps_to.add(attribute)
