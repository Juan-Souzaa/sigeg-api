package com.siseg.dto.geocoding;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para resultado de cálculo de rota
 */
public class RouteResult {
    private final BigDecimal distanciaKm;
    private final int tempoMinutos;
    private final List<Coordinates> waypoints;
    
    public RouteResult(BigDecimal distanciaKm, int tempoMinutos) {
        this(distanciaKm, tempoMinutos, null);
    }
    
    public RouteResult(BigDecimal distanciaKm, int tempoMinutos, List<Coordinates> waypoints) {
        this.distanciaKm = distanciaKm;
        this.tempoMinutos = tempoMinutos;
        this.waypoints = waypoints;
    }
    
    public BigDecimal getDistanciaKm() {
        return distanciaKm;
    }
    
    public int getTempoMinutos() {
        return tempoMinutos;
    }
    
    public List<Coordinates> getWaypoints() {
        return waypoints;
    }
}

