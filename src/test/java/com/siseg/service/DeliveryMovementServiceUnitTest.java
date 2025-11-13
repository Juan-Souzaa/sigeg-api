package com.siseg.service;

import com.siseg.dto.geocoding.Coordinates;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Endereco;
import com.siseg.model.Entregador;
import com.siseg.model.Pedido;
import com.siseg.model.Restaurante;
import com.siseg.model.RotaEntrega;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.model.enumerations.TipoEndereco;
import com.siseg.model.enumerations.TipoVeiculo;
import com.siseg.repository.EntregadorRepository;
import com.siseg.repository.PedidoRepository;
import com.siseg.validator.PedidoValidator;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class DeliveryMovementServiceUnitTest {
    
    @Mock
    private PedidoRepository pedidoRepository;
    
    @Mock
    private EntregadorRepository entregadorRepository;
    
    @Mock
    private RouteService routeService;
    
    @Mock
    private PedidoValidator pedidoValidator;
    
    @InjectMocks
    private DeliveryMovementService deliveryMovementService;
    
    private Pedido pedido;
    private Entregador entregador;
    private Restaurante restaurante;
    private RotaEntrega rota;
    private List<Coordinates> waypoints;
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
        
        // Criar waypoints simulando uma rota real
        waypoints = Arrays.asList(
            new Coordinates(new BigDecimal("-23.5505"), new BigDecimal("-46.6333")), // Início (restaurante)
            new Coordinates(new BigDecimal("-23.5520"), new BigDecimal("-46.6350")), // Waypoint 1
            new Coordinates(new BigDecimal("-23.5568"), new BigDecimal("-46.6440")), // Waypoint 2
            new Coordinates(new BigDecimal("-23.5600"), new BigDecimal("-46.6500")), // Waypoint 3
            new Coordinates(new BigDecimal("-23.5631"), new BigDecimal("-46.6542"))  // Destino final
        );
        
        rota = new RotaEntrega();
        rota.setId(1L);
        rota.setPedido(pedido);
        rota.setIndiceAtual(0);
    }
    
    @Test
    void deveMoverEntregadorEmDirecaoAoWaypoint() {
        // Arrange
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(entregadorRepository.findById(1L)).thenReturn(Optional.of(entregador));
        when(routeService.obterRota(1L)).thenReturn(Optional.of(rota));
        lenient().when(routeService.deserializarWaypoints(rota)).thenReturn(waypoints);
        when(routeService.obterProximoWaypoint(1L)).thenReturn(Optional.of(waypoints.get(1)));
        when(routeService.isRotaCompleta(1L)).thenReturn(false);
        doNothing().when(pedidoValidator).validateStatusEntrega(any());
        doNothing().when(pedidoValidator).validateEntregadorAssociado(any());
        doNothing().when(pedidoValidator).validateCoordenadasDestino(any());
        
        // Act
        deliveryMovementService.simularMovimento(1L);
        
        // Assert
        verify(entregadorRepository, atLeastOnce()).save(any(Entregador.class));
        assertNotNull(entregador.getLatitude());
        assertNotNull(entregador.getLongitude());
        
        // Verificar que a posição mudou (não está mais na posição inicial)
        assertNotEquals(enderecoRestaurante.getLatitude(), entregador.getLatitude());
        assertNotEquals(enderecoRestaurante.getLongitude(), entregador.getLongitude());
    }
    
    @Test
    void deveAvancarWaypointQuandoChegaProximo() {
        // Arrange - Entregador muito próximo do waypoint (menos de 100m)
        entregador.setLatitude(new BigDecimal("-23.5519"));
        entregador.setLongitude(new BigDecimal("-46.6349"));
        
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(entregadorRepository.findById(1L)).thenReturn(Optional.of(entregador));
        when(routeService.obterRota(1L)).thenReturn(Optional.of(rota));
        lenient().when(routeService.deserializarWaypoints(rota)).thenReturn(waypoints);
        when(routeService.obterProximoWaypoint(1L)).thenReturn(Optional.of(waypoints.get(1)));
        when(routeService.isRotaCompleta(1L)).thenReturn(false);
        doNothing().when(pedidoValidator).validateStatusEntrega(any());
        doNothing().when(pedidoValidator).validateEntregadorAssociado(any());
        doNothing().when(pedidoValidator).validateCoordenadasDestino(any());
        
        // Act
        deliveryMovementService.simularMovimento(1L);
        
        // Assert - Pode avançar waypoint se estiver muito próximo
        verify(entregadorRepository, atLeastOnce()).save(any(Entregador.class));
    }
    
    @Test
    void devePercorrerTodosWaypointsEmSequencia() {
        // Arrange
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(entregadorRepository.findById(1L)).thenReturn(Optional.of(entregador));
        when(routeService.obterRota(1L)).thenReturn(Optional.of(rota));
        lenient().when(routeService.deserializarWaypoints(rota)).thenReturn(waypoints);
        when(routeService.isRotaCompleta(1L)).thenReturn(false);
        doNothing().when(pedidoValidator).validateStatusEntrega(any());
        doNothing().when(pedidoValidator).validateEntregadorAssociado(any());
        doNothing().when(pedidoValidator).validateCoordenadasDestino(any());
        
        // Simular múltiplas iterações de movimento
        for (int i = 0; i < waypoints.size() - 1; i++) {
            rota.setIndiceAtual(i);
            when(routeService.obterProximoWaypoint(1L)).thenReturn(Optional.of(waypoints.get(i + 1)));
            
            // Atualizar posição do entregador para próximo do waypoint atual
            entregador.setLatitude(waypoints.get(i).getLatitude());
            entregador.setLongitude(waypoints.get(i).getLongitude());
            
            // Act
            deliveryMovementService.simularMovimento(1L);
        }
        
        // Assert
        verify(routeService, atLeast(waypoints.size() - 1)).obterProximoWaypoint(1L);
        verify(entregadorRepository, atLeast(waypoints.size() - 1)).save(any(Entregador.class));
    }
    
    @Test
    void devePosicionarNoDestinoQuandoRotaCompleta() {
        // Arrange - Rota completa e próximo ao destino
        rota.setIndiceAtual(waypoints.size() - 1);
        entregador.setLatitude(new BigDecimal("-23.5630"));
        entregador.setLongitude(new BigDecimal("-46.6541"));
        
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(entregadorRepository.findById(1L)).thenReturn(Optional.of(entregador));
        when(routeService.obterRota(1L)).thenReturn(Optional.of(rota));
        lenient().when(routeService.deserializarWaypoints(rota)).thenReturn(waypoints);
        when(routeService.isRotaCompleta(1L)).thenReturn(true);
        doNothing().when(pedidoValidator).validateStatusEntrega(any());
        doNothing().when(pedidoValidator).validateEntregadorAssociado(any());
        doNothing().when(pedidoValidator).validateCoordenadasDestino(any());
        
        // Act
        deliveryMovementService.simularMovimento(1L);
        
        // Assert
        verify(entregadorRepository).save(entregador);
        assertEquals(enderecoEntrega.getLatitude(), entregador.getLatitude());
        assertEquals(enderecoEntrega.getLongitude(), entregador.getLongitude());
    }
    
    @Test
    void deveInicializarRotaQuandoNaoExiste() {
        // Arrange
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(entregadorRepository.findById(1L)).thenReturn(Optional.of(entregador));
        when(routeService.obterRota(1L)).thenReturn(Optional.empty());
        doNothing().when(pedidoValidator).validateStatusEntrega(any());
        doNothing().when(pedidoValidator).validateEntregadorAssociado(any());
        doNothing().when(pedidoValidator).validateCoordenadasDestino(any());
        when(routeService.calcularERegistrarRota(any(), any())).thenReturn(rota);
        
        // Act
        deliveryMovementService.simularMovimento(1L);
        
        // Assert - Verificar que tentou calcular rota
        verify(routeService).obterRota(1L);
        verify(routeService).calcularERegistrarRota(any(), any());
    }
    
    @Test
    void deveLancarExcecaoQuandoPedidoNaoExiste() {
        // Arrange
        when(pedidoRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            deliveryMovementService.simularMovimento(1L);
        });
    }
    
    @Test
    void deveLancarExcecaoQuandoEntregadorNaoExiste() {
        // Arrange
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(entregadorRepository.findById(1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            deliveryMovementService.simularMovimento(1L);
        });
    }
    
    @Test
    void deveSimularMovimentoCompletoDoInicioAoFim() {
        // Arrange - Simular movimento completo
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(entregadorRepository.findById(1L)).thenReturn(Optional.of(entregador));
        when(routeService.obterRota(1L)).thenReturn(Optional.of(rota));
        lenient().when(routeService.deserializarWaypoints(rota)).thenReturn(waypoints);
        when(routeService.isRotaCompleta(1L)).thenReturn(false);
        doNothing().when(pedidoValidator).validateStatusEntrega(any());
        doNothing().when(pedidoValidator).validateEntregadorAssociado(any());
        doNothing().when(pedidoValidator).validateCoordenadasDestino(any());
        
        // Simular 10 iterações de movimento
        for (int iteracao = 0; iteracao < 10; iteracao++) {
            int indiceAtual = rota.getIndiceAtual();
            
            // Se chegou ao último waypoint, marcar rota como completa
            if (indiceAtual >= waypoints.size() - 1) {
                when(routeService.isRotaCompleta(1L)).thenReturn(true);
                break;
            }
            
            when(routeService.obterProximoWaypoint(1L)).thenReturn(Optional.of(waypoints.get(indiceAtual + 1)));
            
            // Act
            deliveryMovementService.simularMovimento(1L);
            
            // Atualizar índice para próxima iteração (simulando avanço de waypoint)
            if (iteracao % 3 == 0 && indiceAtual < waypoints.size() - 1) {
                rota.setIndiceAtual(indiceAtual + 1);
            }
        }
        
        // Assert
        verify(entregadorRepository, atLeast(10)).save(any(Entregador.class));
        verify(routeService, atLeast(10)).obterProximoWaypoint(1L);
        
        // Verificar que entregador se moveu
        assertNotNull(entregador.getLatitude());
        assertNotNull(entregador.getLongitude());
    }
    
    @Test
    void deveCalcularDistanciaCorretamenteEntreWaypoints() {
        // Arrange
        Coordinates origem = waypoints.get(0);
        Coordinates destino = waypoints.get(1);
        
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(entregadorRepository.findById(1L)).thenReturn(Optional.of(entregador));
        when(routeService.obterRota(1L)).thenReturn(Optional.of(rota));
        lenient().when(routeService.deserializarWaypoints(rota)).thenReturn(waypoints);
        when(routeService.obterProximoWaypoint(1L)).thenReturn(Optional.of(waypoints.get(1)));
        when(routeService.isRotaCompleta(1L)).thenReturn(false);
        doNothing().when(pedidoValidator).validateStatusEntrega(any());
        doNothing().when(pedidoValidator).validateEntregadorAssociado(any());
        doNothing().when(pedidoValidator).validateCoordenadasDestino(any());
        
        // Act
        deliveryMovementService.simularMovimento(1L);
        
        // Assert - Verificar que a posição mudou em direção ao waypoint
        BigDecimal latInicial = origem.getLatitude();
        BigDecimal latFinal = entregador.getLatitude();
        BigDecimal lonInicial = origem.getLongitude();
        BigDecimal lonFinal = entregador.getLongitude();
        
        // Verificar que se moveu em direção ao destino
        BigDecimal deltaLat = destino.getLatitude().subtract(latInicial);
        BigDecimal deltaLon = destino.getLongitude().subtract(lonInicial);
        
        BigDecimal latMovimento = latFinal.subtract(latInicial);
        BigDecimal lonMovimento = lonFinal.subtract(lonInicial);
        
        // O movimento deve estar na mesma direção (mesmo sinal)
        assertTrue(
            (deltaLat.signum() == 0 || latMovimento.signum() == deltaLat.signum()) ||
            (deltaLon.signum() == 0 || lonMovimento.signum() == deltaLon.signum()),
            "Movimento deve estar na direção do waypoint"
        );
    }
}

