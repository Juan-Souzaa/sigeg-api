package com.siseg.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utilitário para cálculo de distância entre coordenadas usando a fórmula de Haversine
 */
public class DistanceCalculator {
    
    // Raio médio da Terra em quilômetros
    private static final double EARTH_RADIUS_KM = 6371.0;
    
    /**
     * Calcula a distância em quilômetros entre dois pontos geográficos usando a fórmula de Haversine
     * 
     * @param lat1 Latitude do primeiro ponto
     * @param lon1 Longitude do primeiro ponto
     * @param lat2 Latitude do segundo ponto
     * @param lon2 Longitude do segundo ponto
     * @return Distância em quilômetros (arredondada para 2 casas decimais)
     */
    public static BigDecimal calculateDistance(BigDecimal lat1, BigDecimal lon1, 
                                               BigDecimal lat2, BigDecimal lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return null;
        }
        
        double lat1Rad = Math.toRadians(lat1.doubleValue());
        double lon1Rad = Math.toRadians(lon1.doubleValue());
        double lat2Rad = Math.toRadians(lat2.doubleValue());
        double lon2Rad = Math.toRadians(lon2.doubleValue());
        
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_KM * c;
        
        return BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Estima tempo de entrega em minutos baseado na distância
     * Usa velocidade média de 30 km/h para motos/carros e 15 km/h para bicicletas
     * 
     * @param distanciaKm Distância em quilômetros
     * @param tipoVeiculo Tipo de veículo do entregador (pode ser null)
     * @return Tempo estimado em minutos
     */
    public static int estimateDeliveryTime(BigDecimal distanciaKm, String tipoVeiculo) {
        if (distanciaKm == null || distanciaKm.compareTo(BigDecimal.ZERO) <= 0) {
            return 0; // Já chegou ou coordenadas inválidas
        }
        
        double velocidadeMediaKmh;
        if ("BICICLETA".equalsIgnoreCase(tipoVeiculo)) {
            velocidadeMediaKmh = 15.0;
        } else {
            // Moto ou carro
            velocidadeMediaKmh = 30.0;
        }
        
        double tempoHoras = distanciaKm.doubleValue() / velocidadeMediaKmh;
        int tempoMinutos = (int) Math.ceil(tempoHoras * 60);
        
        // Tempo mínimo de 1 minuto (para distâncias muito curtas) e máximo de 120 minutos
        // Remove o mínimo de 15 minutos para permitir atualização dinâmica
        return Math.max(1, Math.min(120, tempoMinutos));
    }
}

