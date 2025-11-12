package com.siseg.service;

import com.siseg.model.Entregador;
import com.siseg.model.Pedido;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliverySimulationServiceUnitTest {
    
    @Mock
    private PedidoRepository pedidoRepository;
    
    @Mock
    private DeliveryMovementService deliveryMovementService;
    
    @InjectMocks
    private DeliverySimulationService deliverySimulationService;
    
    private Pedido pedido1;
    private Pedido pedido2;
    
    @BeforeEach
    void setUp() {
        Entregador entregador = new Entregador();
        entregador.setId(1L);
        
        pedido1 = new Pedido();
        pedido1.setId(1L);
        pedido1.setStatus(StatusPedido.OUT_FOR_DELIVERY);
        pedido1.setEntregador(entregador);
        
        pedido2 = new Pedido();
        pedido2.setId(2L);
        pedido2.setStatus(StatusPedido.OUT_FOR_DELIVERY);
        pedido2.setEntregador(entregador);
    }
    
    @Test
    void deveSimularTodosPedidosEmEntrega() {
        List<Pedido> pedidos = Arrays.asList(pedido1, pedido2);
        when(pedidoRepository.findByStatus(StatusPedido.OUT_FOR_DELIVERY)).thenReturn(pedidos);
        
        deliverySimulationService.simularEntregasAtivas();
        
        verify(deliveryMovementService, times(2)).simularMovimento(anyLong());
        verify(deliveryMovementService).simularMovimento(1L);
        verify(deliveryMovementService).simularMovimento(2L);
    }
    
    @Test
    void naoDeveFazerNadaQuandoNaoHaPedidosEmEntrega() {
        when(pedidoRepository.findByStatus(StatusPedido.OUT_FOR_DELIVERY)).thenReturn(Collections.emptyList());
        
        deliverySimulationService.simularEntregasAtivas();
        
        verify(deliveryMovementService, never()).simularMovimento(anyLong());
    }
    
    @Test
    void deveIgnorarPedidosSemEntregador() {
        pedido1.setEntregador(null);
        List<Pedido> pedidos = Arrays.asList(pedido1, pedido2);
        when(pedidoRepository.findByStatus(StatusPedido.OUT_FOR_DELIVERY)).thenReturn(pedidos);
        
        deliverySimulationService.simularEntregasAtivas();
        
        verify(deliveryMovementService, times(1)).simularMovimento(anyLong());
        verify(deliveryMovementService).simularMovimento(2L);
        verify(deliveryMovementService, never()).simularMovimento(1L);
    }
    
    @Test
    void deveContinuarQuandoErroEmUmPedido() {
        List<Pedido> pedidos = Arrays.asList(pedido1, pedido2);
        when(pedidoRepository.findByStatus(StatusPedido.OUT_FOR_DELIVERY)).thenReturn(pedidos);
        doThrow(new RuntimeException("Erro simulado")).when(deliveryMovementService).simularMovimento(1L);
        
        deliverySimulationService.simularEntregasAtivas();
        
        verify(deliveryMovementService).simularMovimento(1L);
        verify(deliveryMovementService).simularMovimento(2L);
    }
}

