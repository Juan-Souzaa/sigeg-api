package com.siseg.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DistanceCalculatorUnitTest {

    @Test
    void deveCalcularDistanciaCorretamente() {
        BigDecimal lat1 = new BigDecimal("-23.5505");
        BigDecimal lon1 = new BigDecimal("-46.6333");
        BigDecimal lat2 = new BigDecimal("-23.5515");
        BigDecimal lon2 = new BigDecimal("-46.6343");

        BigDecimal distancia = DistanceCalculator.calculateDistance(lat1, lon1, lat2, lon2);

        assertNotNull(distancia);
        assertTrue(distancia.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(distancia.compareTo(new BigDecimal("100")) < 0);
    }

    @Test
    void deveRetornarNullQuandoCoordenadasNulas() {
        BigDecimal distancia = DistanceCalculator.calculateDistance(null, null, null, null);

        assertNull(distancia);
    }

    @Test
    void deveEstimarTempoEntregaParaMoto() {
        BigDecimal distancia = new BigDecimal("5.0");
        int tempoMinutos = DistanceCalculator.estimateDeliveryTime(distancia, "MOTO");

        assertTrue(tempoMinutos > 0);
        assertTrue(tempoMinutos >= VehicleConstants.TEMPO_MINIMO_ENTREGA_MINUTOS);
        assertTrue(tempoMinutos <= VehicleConstants.TEMPO_MAXIMO_ENTREGA_MINUTOS);
    }

    @Test
    void deveEstimarTempoEntregaParaBicicleta() {
        BigDecimal distancia = new BigDecimal("5.0");
        int tempoMinutos = DistanceCalculator.estimateDeliveryTime(distancia, "BICICLETA");

        assertTrue(tempoMinutos > 0);
        assertTrue(tempoMinutos >= VehicleConstants.TEMPO_MINIMO_ENTREGA_MINUTOS);
    }

    @Test
    void deveAplicarLimiteMinimoDeTempo() {
        BigDecimal distancia = new BigDecimal("0.1");
        int tempoMinutos = DistanceCalculator.estimateDeliveryTime(distancia, "MOTO");

        assertTrue(tempoMinutos >= VehicleConstants.TEMPO_MINIMO_ENTREGA_MINUTOS);
    }

    @Test
    void deveAplicarLimiteMaximoDeTempo() {
        BigDecimal distancia = new BigDecimal("100.0");
        int tempoMinutos = DistanceCalculator.estimateDeliveryTime(distancia, "MOTO");

        assertTrue(tempoMinutos <= VehicleConstants.TEMPO_MAXIMO_ENTREGA_MINUTOS);
    }

    @Test
    void deveRetornarZeroParaDistanciaZero() {
        BigDecimal distancia = BigDecimal.ZERO;
        int tempoMinutos = DistanceCalculator.estimateDeliveryTime(distancia, "MOTO");

        assertEquals(0, tempoMinutos);
    }
}

