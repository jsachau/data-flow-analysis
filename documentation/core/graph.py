from .models import *
import json


class MappingGraph:

    def __init__(self, mapping: Mapping):
        self._mapping = mapping

    @staticmethod
    def create_profile_class_node(cls: FHIRProfile):
        return {
            "id": cls.graph_id,
            "shape": "box",
            "label": cls.name,
            "color": {
                "background": '#154D72',
                "backupBackground": '#154D72'
            },
            "font": {
                "color": "#ffffff"
            },
            "level": cls.depth,
            "hasMappingData": True
        }

    @staticmethod
    def create_profile_attribute_node(cls: FHIRAttribute):
        return {
            "id": cls.graph_id,
            "shape": "box",
            "label": cls.name + ": " + cls.cleaned_type_name,
            "color": {
                "background": '#8CCA9E',
                "backupBackground": '#8CCA9E'
            },
            "font": {
                "color": "#ffffff"
            },
            "level": cls.depth,
            "hasMappingData": cls.has_mapping_data
        }

    @staticmethod
    def create_template_class_node(cls: TemplateClass):
        return {
            "id": cls.graph_id,
            "shape": "box",
            "label": cls.archetyp_id if cls.archetyp_id else cls.name,
            "color": {
                "background": '#B13431',
                "backupBackground": '#B13431'
            },
            "font": {
                "color": "#ffffff"
            },
            "level": cls.depth,
            "hasMappingData": cls.has_mapping_data
        }

    @staticmethod
    def create_template_attribute_node(cls: TemplateAttribute):
        return {
            "id": cls.graph_id,
            "shape": "box",
            "label": cls.name + ": " + cls.cleaned_type_name,
            "color": {
                "background": '#CC8D6A',
                "backupBackground": '#CC8D6A'
            },
            "font": {
                "color": "#ffffff"
            },
            "level": cls.depth,
            "type": "template_attribute",
            "hasMappingData": cls.has_mapping_data
        }

    @property
    def nodes(self):
        all_nodes = []
        for cls in self._mapping.template_classes.all():
            if cls.attributes.exists():
                all_nodes.append(self.create_template_class_node(cls))
                for attribute in cls.attributes.all():
                    all_nodes.append(self.create_template_attribute_node(attribute))

        if len(all_nodes) == 0:
            return []

        max_level = max(map(lambda n: n["level"], all_nodes))

        profile = self._mapping.profile

        profile_nodes = list()
        profile_nodes.append(self.create_profile_class_node(profile))

        for attribute in profile.attributes.all():
            profile_nodes.append(self.create_profile_attribute_node(attribute))

        max_profile_level = max(map(lambda n: n["level"], profile_nodes))

        for node in profile_nodes:
            node["level"] = max_profile_level - node["level"]

        for node in profile_nodes:
            node["level"] += max_level + 1

        all_nodes.extend(profile_nodes)

        return json.dumps(all_nodes)

    @property
    def edges(self):

        profile_edge_color = "#6e6c6e"
        template_edge_color = "#6e6c6e"
        mapping_edge_color = "#1e81b0"

        all_edges = []
        for cls in self._mapping.template_classes.all():
            for attribute in cls.attributes.all():
                all_edges.append({
                    "from": cls.graph_id,
                    "to": attribute.graph_id,
                    "color": {"color": template_edge_color}
                })

                if attribute.type_class:
                    all_edges.append({
                        "from": attribute.graph_id,
                        "to": attribute.type_class.graph_id,
                        "color": {"color": template_edge_color}
                    })

        profile = self._mapping.profile
        for attribute in profile.attributes.all():
            if attribute.parent_attribute:
                all_edges.append({
                    "from": attribute.parent_attribute.graph_id,
                    "to": attribute.graph_id,
                    "color": {"color": profile_edge_color}
                })
            else:
                all_edges.append({
                    "from": profile.graph_id,
                    "to": attribute.graph_id,
                    "color": {"color": profile_edge_color}
                })

            for attr in attribute.maps_to.all():
                all_edges.append({
                    "from": attribute.graph_id,
                    "to": attr.graph_id,
                    "dashes": True,
                    "color": {"color": mapping_edge_color},
                    "width": 2
                })

        return json.dumps(all_edges)
