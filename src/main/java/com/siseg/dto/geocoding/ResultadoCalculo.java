package com.siseg.dto.geocoding;

import java.math.BigDecimal;

/**
 * DTO para resultado de cálculo de distância e tempo
 */
public class ResultadoCalculo {
    private final BigDecimal distanciaKm;
    private final int tempoMinutos;
    private final boolean usadoOSRM;
    
    public ResultadoCalculo(BigDecimal distanciaKm, int tempoMinutos, boolean usadoOSRM) {
        this.distanciaKm = distanciaKm;
        this.tempoMinutos = tempoMinutos;
        this.usadoOSRM = usadoOSRM;
    }
    
    public BigDecimal getDistanciaKm() {
        return distanciaKm;
    }
    
    public int getTempoMinutos() {
        return tempoMinutos;
    }
    
    public boolean isUsadoOSRM() {
        return usadoOSRM;
    }
}

