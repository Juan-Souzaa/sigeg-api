package com.siseg.dto.geocoding;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String geometry;
    
    @JsonProperty("coordinates")
    private List<List<Double>> coordinates;
}

