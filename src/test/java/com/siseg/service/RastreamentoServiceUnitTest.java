package com.siseg.service;

import com.siseg.dto.geocoding.ResultadoCalculo;
import com.siseg.dto.rastreamento.RastreamentoDTO;
import com.siseg.model.Endereco;
import com.siseg.model.Entregador;
import com.siseg.model.Pedido;
import com.siseg.model.Restaurante;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.model.enumerations.TipoEndereco;
import com.siseg.model.enumerations.TipoVeiculo;
import com.siseg.repository.EntregadorRepository;
import com.siseg.repository.PedidoRepository;
import com.siseg.util.TempoEstimadoCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RastreamentoServiceUnitTest {
    
    @Mock
    private PedidoRepository pedidoRepository;
    
    @Mock
    private EntregadorRepository entregadorRepository;

    @Mock
    private TempoEstimadoCalculator tempoEstimadoCalculator;
    
    @Mock
    private EnderecoService enderecoService;
    
    @Mock
    private RouteService routeService;
    
    @InjectMocks
    private RastreamentoService rastreamentoService;
    
    private Pedido pedido;
    private Entregador entregador;
    private Restaurante restaurante;
    private Endereco enderecoRestaurante;
    private Endereco enderecoEntrega;
    
    @BeforeEach
    void setUp() {
        // Criar endereço do restaurante
        enderecoRestaurante = new Endereco();
        enderecoRestaurante.setId(1L);
        enderecoRestaurante.setLogradouro("Rua do Restaurante");
        enderecoRestaurante.setNumero("123");
        enderecoRestaurante.setBairro("Centro");
        enderecoRestaurante.setCidade("São Paulo");
        enderecoRestaurante.setEstado("SP");
        enderecoRestaurante.setCep("01310100");
        enderecoRestaurante.setLatitude(new BigDecimal("-23.5505"));
        enderecoRestaurante.setLongitude(new BigDecimal("-46.6333"));
        enderecoRestaurante.setPrincipal(true);
        enderecoRestaurante.setTipo(TipoEndereco.OUTRO);
        
        restaurante = new Restaurante();
        restaurante.setId(1L);
        restaurante.setEnderecos(java.util.List.of(enderecoRestaurante));
        enderecoRestaurante.setRestaurante(restaurante);
        
        entregador = new Entregador();
        entregador.setId(1L);
        entregador.setTipoVeiculo(TipoVeiculo.MOTO);
        entregador.setLatitude(new BigDecimal("-23.5505"));
        entregador.setLongitude(new BigDecimal("-46.6333"));
        
        // Criar endereço de entrega
        enderecoEntrega = new Endereco();
        enderecoEntrega.setId(2L);
        enderecoEntrega.setLogradouro("Rua de Entrega");
        enderecoEntrega.setNumero("456");
        enderecoEntrega.setBairro("Centro");
        enderecoEntrega.setCidade("São Paulo");
        enderecoEntrega.setEstado("SP");
        enderecoEntrega.setCep("01310100");
        enderecoEntrega.setLatitude(new BigDecimal("-23.5631"));
        enderecoEntrega.setLongitude(new BigDecimal("-46.6542"));
        enderecoEntrega.setPrincipal(false);
        enderecoEntrega.setTipo(TipoEndereco.OUTRO);
        
        pedido = new Pedido();
        pedido.setId(1L);
        pedido.setStatus(StatusPedido.OUT_FOR_DELIVERY);
        pedido.setRestaurante(restaurante);
        pedido.setEntregador(entregador);
        pedido.setEnderecoEntrega(enderecoEntrega);
    }
    
    @Test
    void deveObterRastreamentoComSucesso() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(entregadorRepository.findById(1L)).thenReturn(Optional.of(entregador));
        when(enderecoService.buscarEnderecoPrincipalRestaurante(1L)).thenReturn(Optional.of(enderecoRestaurante));
        when(routeService.obterWaypointsRestantes(1L)).thenReturn(java.util.Collections.emptyList());
        
        ResultadoCalculo resultado = new ResultadoCalculo(
            new BigDecimal("1.5"), 15, false
        );
        when(tempoEstimadoCalculator.calculateDistanceAndTime(
            any(), any(), any(), any(), any()
        )).thenReturn(resultado);
        
        RastreamentoDTO rastreamento = rastreamentoService.obterRastreamento(1L);
        
        assertNotNull(rastreamento);
        assertEquals(StatusPedido.OUT_FOR_DELIVERY, rastreamento.getStatusEntrega());
        assertNotNull(rastreamento.getPosicaoAtualLat());
        assertNotNull(rastreamento.getPosicaoAtualLon());
        assertNotNull(rastreamento.getPosicaoDestinoLat());
        assertNotNull(rastreamento.getPosicaoDestinoLon());
        assertNotNull(rastreamento.getDistanciaRestanteKm());
        assertNotNull(rastreamento.getTempoEstimadoMinutos());
        assertTrue(rastreamento.getDistanciaRestanteKm().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(rastreamento.getTempoEstimadoMinutos() > 0);
    }
    
    @Test
    void deveLancarExcecaoQuandoPedidoNaoExiste() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.empty());
        
        assertThrows(com.siseg.exception.ResourceNotFoundException.class, () -> {
            rastreamentoService.obterRastreamento(1L);
        });
    }
    
    @Test
    void deveRetornarRastreamentoComDistanciaZeroQuandoCoordenadasInvalidas() {
        entregador.setLatitude(null);
        entregador.setLongitude(null);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(entregadorRepository.findById(1L)).thenReturn(Optional.of(entregador));
        when(enderecoService.buscarEnderecoPrincipalRestaurante(1L)).thenReturn(Optional.of(enderecoRestaurante));
        when(routeService.obterWaypointsRestantes(1L)).thenReturn(java.util.Collections.emptyList());
        
        RastreamentoDTO rastreamento = rastreamentoService.obterRastreamento(1L);
        
        assertNotNull(rastreamento);
        assertEquals(BigDecimal.ZERO, rastreamento.getDistanciaRestanteKm());
        assertEquals(0, rastreamento.getTempoEstimadoMinutos());
        assertTrue(rastreamento.getProximoAoDestino());
    }
    
    @Test
    void deveLancarExcecaoQuandoPedidoNaoTemEntregador() {
        pedido.setEntregador(null);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        
        assertThrows(com.siseg.exception.ResourceNotFoundException.class, () -> {
            rastreamentoService.obterRastreamento(1L);
        });
    }
    
}

