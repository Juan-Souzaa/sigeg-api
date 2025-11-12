package com.siseg.dto.geocoding;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class OsrmRoute {
    @JsonProperty("distance")
    private double distance;
    
    @JsonProperty("duration")
    private double duration;
    
    @JsonProperty("geometry")
    private JsonNode geometry;
    
    /**
     * Extrai as coordenadas do geometry.
     * Quando geometry é GeoJSON (objeto), retorna geometry.coordinates.
     * Quando geometry é polyline (string), retorna null (deve ser decodificado).
     */
    public List<List<Double>> getCoordinates() {
        if (geometry == null) {
            return null;
        }
        
        // Se geometry é um objeto (GeoJSON)
        if (geometry.isObject()) {
            JsonNode coordinatesNode = geometry.get("coordinates");
            if (coordinatesNode != null && coordinatesNode.isArray()) {
                // Extrai coordenadas manualmente do JsonNode
                List<List<Double>> coords = new java.util.ArrayList<>();
                for (JsonNode coord : coordinatesNode) {
                    if (coord.isArray() && coord.size() >= 2) {
                        List<Double> point = new java.util.ArrayList<>();
                        point.add(coord.get(0).asDouble());
                        point.add(coord.get(1).asDouble());
                        coords.add(point);
                    }
                }
                return coords;
            }
        }
        
        return null;
    }
    
    /**
     * Retorna geometry como string (para polyline) ou null se for objeto
     */
    public String getGeometryAsString() {
        if (geometry == null) {
            return null;
        }
        if (geometry.isTextual()) {
            return geometry.asText();
        }
        return null;
    }
}

