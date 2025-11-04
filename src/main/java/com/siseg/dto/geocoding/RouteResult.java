package com.siseg.dto.geocoding;

import java.math.BigDecimal;

/**
 * DTO para resultado de cálculo de rota
 */
public class RouteResult {
    private final BigDecimal distanciaKm;
    private final int tempoMinutos;
    
    public RouteResult(BigDecimal distanciaKm, int tempoMinutos) {
        this.distanciaKm = distanciaKm;
        this.tempoMinutos = tempoMinutos;
    }
    
    public BigDecimal getDistanciaKm() {
        return distanciaKm;
    }
    
    public int getTempoMinutos() {
        return tempoMinutos;
    }
}

