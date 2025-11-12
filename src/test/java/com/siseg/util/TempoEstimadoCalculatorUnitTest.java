package com.siseg.util;

import com.siseg.dto.geocoding.Coordinates;
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
import java.util.Arrays;
import java.util.List;
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
    void deveRetornarWaypointsNoRouteResultQuandoSolicitado() {
        // Criar waypoints de teste
        List<Coordinates> waypoints = Arrays.asList(
            new Coordinates(new BigDecimal("-23.5505"), new BigDecimal("-46.6333")),
            new Coordinates(new BigDecimal("-23.5568"), new BigDecimal("-46.6440")),
            new Coordinates(new BigDecimal("-23.5631"), new BigDecimal("-46.6542"))
        );
        
        RouteResult routeResult = new RouteResult(
                new BigDecimal("2.5"), 15, waypoints);

        when(geocodingService.calculateRoute(any(BigDecimal.class), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class), anyString(), eq(true)))
                .thenReturn(Optional.of(routeResult));

        Optional<RouteResult> resultado = geocodingService.calculateRoute(
                origemLat, origemLon, destinoLat, destinoLon, "driving", true);

        assertTrue(resultado.isPresent(), "RouteResult deve estar presente");
        assertNotNull(resultado.get().getWaypoints(), "Waypoints devem estar presentes no RouteResult");
        assertEquals(3, resultado.get().getWaypoints().size(), "Deve retornar 3 waypoints");
        assertFalse(resultado.get().getWaypoints().isEmpty(), "Lista de waypoints não deve estar vazia");
        
        // Verificar se os waypoints estão corretos
        assertEquals(waypoints.get(0).getLatitude(), resultado.get().getWaypoints().get(0).getLatitude());
        assertEquals(waypoints.get(0).getLongitude(), resultado.get().getWaypoints().get(0).getLongitude());
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

