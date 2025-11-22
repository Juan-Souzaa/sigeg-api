package com.siseg.dto.geocoding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class LocationIQResponse {
    @JsonProperty("lat")
    private String lat;
    
    @JsonProperty("lon")
    private String lon;
    
    @JsonProperty("display_name")
    private String displayName;
}

