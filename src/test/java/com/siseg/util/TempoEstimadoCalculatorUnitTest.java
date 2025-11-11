package com.siseg.util;

import com.siseg.dto.geocoding.ResultadoCalculo;
import com.siseg.dto.geocoding.RouteResult;
import com.siseg.model.enumerations.TipoVeiculo;
import com.siseg.service.GeocodingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TempoEstimadoCalculatorUnitTest {

    @Mock
    private GeocodingService geocodingService;

    @InjectMocks
    private TempoEstimadoCalculator tempoEstimadoCalculator;

    private BigDecimal origemLat;
    private BigDecimal origemLon;
    private BigDecimal destinoLat;
    private BigDecimal destinoLon;

    @BeforeEach
    void setUp() {
        origemLat = new BigDecimal("-23.5505");
        origemLon = new BigDecimal("-46.6333");
        destinoLat = new BigDecimal("-23.5515");
        destinoLon = new BigDecimal("-46.6343");
    }

    @Test
    void deveCalcularDistanciaETempoComOSRM() {
        RouteResult routeResult = new RouteResult(
                new BigDecimal("2.5"), 15);

        when(geocodingService.calculateRoute(any(BigDecimal.class), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class), anyString()))
                .thenReturn(Optional.of(routeResult));

        ResultadoCalculo resultado = tempoEstimadoCalculator.calculateDistanceAndTime(
                origemLat, origemLon, destinoLat, destinoLon, TipoVeiculo.MOTO);

        assertNotNull(resultado);
        assertEquals(new BigDecimal("2.5"), resultado.getDistanciaKm());
        assertEquals(15, resultado.getTempoMinutos());
        assertTrue(resultado.isUsadoOSRM());
    }

    @Test
    void deveUsarFallbackHaversineQuandoOSRMFalha() {
        when(geocodingService.calculateRoute(any(BigDecimal.class), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class), anyString()))
                .thenReturn(Optional.empty());

        ResultadoCalculo resultado = tempoEstimadoCalculator.calculateDistanceAndTime(
                origemLat, origemLon, destinoLat, destinoLon, TipoVeiculo.MOTO);

        assertNotNull(resultado);
        assertNotNull(resultado.getDistanciaKm());
        assertTrue(resultado.getTempoMinutos() > 0);
        assertFalse(resultado.isUsadoOSRM());
    }

    @Test
    void deveUsarFallbackHaversineQuandoOSRMLancaExcecao() {
        when(geocodingService.calculateRoute(any(BigDecimal.class), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class), anyString()))
                .thenThrow(new RuntimeException("Erro de conexão"));

        ResultadoCalculo resultado = tempoEstimadoCalculator.calculateDistanceAndTime(
                origemLat, origemLon, destinoLat, destinoLon, TipoVeiculo.MOTO);

        assertNotNull(resultado);
        assertNotNull(resultado.getDistanciaKm());
        assertTrue(resultado.getTempoMinutos() > 0);
        assertFalse(resultado.isUsadoOSRM());
    }

    @Test
    void deveRetornarTempoPadraoQuandoCoordenadasInvalidas() {
        ResultadoCalculo resultado = tempoEstimadoCalculator.calculateDistanceAndTime(
                null, null, destinoLat, destinoLon, TipoVeiculo.MOTO);

        assertNotNull(resultado);
        assertNull(resultado.getDistanciaKm());
        assertEquals(VehicleConstants.TEMPO_PADRAO_ENTREGA_MINUTOS, resultado.getTempoMinutos());
        assertFalse(resultado.isUsadoOSRM());
    }

    @Test
    void deveNormalizarDistanciaMinima() {
        when(geocodingService.calculateRoute(any(BigDecimal.class), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class), anyString()))
                .thenReturn(Optional.empty());

        ResultadoCalculo resultado = tempoEstimadoCalculator.calculateDistanceAndTime(
                origemLat, origemLon, destinoLat, destinoLon, TipoVeiculo.MOTO);

        assertNotNull(resultado);
        assertTrue(resultado.getDistanciaKm().compareTo(new BigDecimal("0.1")) >= 0);
    }

    @Test
    void deveCalcularComDiferentesTiposDeVeiculo() {
        when(geocodingService.calculateRoute(any(BigDecimal.class), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class), anyString()))
                .thenReturn(Optional.empty());

        ResultadoCalculo resultadoMoto = tempoEstimadoCalculator.calculateDistanceAndTime(
                origemLat, origemLon, destinoLat, destinoLon, TipoVeiculo.MOTO);

        ResultadoCalculo resultadoBicicleta = tempoEstimadoCalculator.calculateDistanceAndTime(
                origemLat, origemLon, destinoLat, destinoLon, TipoVeiculo.BICICLETA);

        assertNotNull(resultadoMoto);
        assertNotNull(resultadoBicicleta);
        assertTrue(resultadoBicicleta.getTempoMinutos() >= resultadoMoto.getTempoMinutos());
    }
}

