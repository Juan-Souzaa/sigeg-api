package com.siseg.util;

import com.siseg.dto.geocoding.ResultadoCalculo;
import com.siseg.model.enumerations.TipoVeiculo;
import com.siseg.service.GeocodingService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.logging.Logger;

@Component
public class TempoEstimadoCalculator {
    
    private static final Logger logger = Logger.getLogger(TempoEstimadoCalculator.class.getName());
    private static final BigDecimal DISTANCIA_MINIMA_KM = new BigDecimal("0.1");
    
    private final GeocodingService geocodingService;
    
    public TempoEstimadoCalculator(GeocodingService geocodingService) {
        this.geocodingService = geocodingService;
    }
    
    public ResultadoCalculo calculateDistanceAndTime(BigDecimal origemLat, BigDecimal origemLon,
                                                     BigDecimal destinoLat, BigDecimal destinoLon,
                                                     TipoVeiculo tipoVeiculo) {
        if (temCoordenadasInvalidas(origemLat, origemLon, destinoLat, destinoLon)) {
            return new ResultadoCalculo(null, VehicleConstants.TEMPO_PADRAO_ENTREGA_MINUTOS, false);
        }
        
        var resultadoOSRM = calcularViaOSRM(origemLat, origemLon, destinoLat, destinoLon, tipoVeiculo);
        if (resultadoOSRM != null) {
            return resultadoOSRM;
        }
        
        var resultadoHaversine = calcularViaHaversine(origemLat, origemLon, destinoLat, destinoLon, tipoVeiculo);
        return normalizarResultado(resultadoHaversine);
    }
    
    private boolean temCoordenadasInvalidas(BigDecimal origemLat, BigDecimal origemLon, 
                                           BigDecimal destinoLat, BigDecimal destinoLon) {
        return origemLat == null || origemLon == null || destinoLat == null || destinoLon == null;
    }
    
    private ResultadoCalculo calcularViaOSRM(BigDecimal origemLat, BigDecimal origemLon,
                                              BigDecimal destinoLat, BigDecimal destinoLon,
                                              TipoVeiculo tipoVeiculo) {
        String routeProfile = VehicleConstants.getOsrmProfile(tipoVeiculo);
        
        try {
            var routeResult = geocodingService.calculateRoute(
                origemLat, origemLon, destinoLat, destinoLon, routeProfile
            );
            
            if (routeResult.isPresent()) {
                BigDecimal distanciaKm = routeResult.get().getDistanciaKm();
                int tempoMinutos = routeResult.get().getTempoMinutos();
                logger.fine("Distância calculada via OSRM (profile=" + routeProfile + "): " 
                    + distanciaKm + " km, " + tempoMinutos + " min");
                return new ResultadoCalculo(distanciaKm, tempoMinutos, true);
            }
        } catch (Exception e) {
            logger.fine("Erro ao calcular rota via OSRM, usando fallback Haversine: " + e.getMessage());
        }
        
        return null;
    }
    
    private ResultadoCalculo calcularViaHaversine(BigDecimal origemLat, BigDecimal origemLon,
                                                  BigDecimal destinoLat, BigDecimal destinoLon,
                                                  TipoVeiculo tipoVeiculo) {
        BigDecimal distanciaKm = DistanceCalculator.calculateDistance(origemLat, origemLon, destinoLat, destinoLon);
        
        if (distanciaKm == null || distanciaKm.compareTo(BigDecimal.ZERO) <= 0) {
            return new ResultadoCalculo(null, 0, false);
        }
        
        String tipoVeiculoStr = tipoVeiculo != null ? tipoVeiculo.name() : null;
        int tempoMinutos = DistanceCalculator.estimateDeliveryTime(distanciaKm, tipoVeiculoStr);
        logger.fine("Distância calculada via Haversine: " + distanciaKm + " km, " + tempoMinutos + " min");
        
        return new ResultadoCalculo(distanciaKm, tempoMinutos, false);
    }
    
    private ResultadoCalculo normalizarResultado(ResultadoCalculo resultado) {
        if (resultado == null || resultado.getDistanciaKm() == null || 
            resultado.getDistanciaKm().compareTo(BigDecimal.ZERO) <= 0) {
            return new ResultadoCalculo(DISTANCIA_MINIMA_KM, VehicleConstants.TEMPO_PADRAO_ENTREGA_MINUTOS, false);
        }
        
        if (resultado.getDistanciaKm().compareTo(DISTANCIA_MINIMA_KM) < 0) {
            int tempoMinutos = Math.max(VehicleConstants.TEMPO_MINIMO_ENTREGA_MINUTOS, resultado.getTempoMinutos());
            return new ResultadoCalculo(DISTANCIA_MINIMA_KM, tempoMinutos, resultado.isUsadoOSRM());
        }
        
        return resultado;
    }
}

