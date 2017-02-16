package test

import grails.plugins.elasticsearch.lite.mapping.ElasticSearchMarshaller
import groovy.json.JsonBuilder
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.XContentFactory

class Building {

    String name
    Date date = new Date()
    GeoPoint location

    static constraints = {
        name(nullable: true)
    }
}

class BuildingMarshaller implements ElasticSearchMarshaller<Building> {

    @Override
    JsonBuilder getMapping() {
        JsonBuilder building  = new JsonBuilder()
        building {
            "name"{
                "type" "string"
                "term_vector" "with_positions_offsets"
                "include_in_all"true
            }
            "date" {
                "type" "date"
                "include_in_all" true
            }
            "location" {
                "type" "geo_point"
            }
        }
        return building
    }

    @Override
    XContentBuilder toSource(XContentBuilder source = XContentFactory.jsonBuilder(),  Building building) {
        source.startObject()
                .field('name', building.name)
                .field('date', building.date)
        if(building.location) {
            GeoPointMarshaller m = TestMarshallers.getMarshaller(building.location)
            m.toSource(source, building.location)
        }
        source.endObject()
    }
}
