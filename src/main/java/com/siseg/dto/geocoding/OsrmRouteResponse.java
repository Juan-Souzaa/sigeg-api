package com.siseg.dto.geocoding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class OsrmRouteResponse {
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("routes")
    private List<OsrmRoute> routes;
}

