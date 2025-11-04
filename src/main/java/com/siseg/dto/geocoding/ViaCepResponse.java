package com.siseg.dto.geocoding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class ViaCepResponse {
    @JsonProperty("logradouro")
    private String logradouro;
    
    @JsonProperty("bairro")
    private String bairro;
    
    @JsonProperty("localidade")
    private String localidade;
    
    @JsonProperty("uf")
    private String uf;
    
    @JsonProperty("erro")
    private Boolean erro;
}

