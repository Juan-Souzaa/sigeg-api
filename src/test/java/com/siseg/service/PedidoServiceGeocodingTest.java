package com.siseg.service;

import com.siseg.dto.geocoding.Coordinates;
import com.siseg.dto.geocoding.ResultadoCalculo;
import com.siseg.dto.pedido.PedidoItemResponseDTO;
import com.siseg.dto.pedido.PedidoRequestDTO;
import com.siseg.dto.pedido.PedidoResponseDTO;
import com.siseg.model.*;
import com.siseg.model.enumerations.CategoriaMenu;
import com.siseg.model.enumerations.MetodoPagamento;
import com.siseg.model.enumerations.StatusEntregador;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.model.enumerations.TipoVeiculo;
import com.siseg.mapper.PedidoMapper;
import com.siseg.repository.*;
import com.siseg.util.SecurityUtils;
import com.siseg.util.TempoEstimadoCalculator;
import com.siseg.validator.PedidoValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceGeocodingTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private RestauranteRepository restauranteRepository;

    @Mock
    private PratoRepository pratoRepository;

    @Mock
    private EntregadorRepository entregadorRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private GeocodingService geocodingService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private PedidoMapper pedidoMapper;

    @Mock
    private PedidoValidator pedidoValidator;

    @Mock
    private TempoEstimadoCalculator tempoEstimadoCalculator;

    @InjectMocks
    private PedidoService pedidoService;

    private Cliente cliente;
    private Restaurante restaurante;
    private Prato prato;
    private PedidoRequestDTO pedidoRequestDTO;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("cliente@teste.com");

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Cliente Teste");
        cliente.setEndereco("Rua do Cliente, 123");
        cliente.setUser(user);

        restaurante = new Restaurante();
        restaurante.setId(1L);
        restaurante.setNome("Restaurante Teste");
        restaurante.setLatitude(new BigDecimal("-23.5505"));
        restaurante.setLongitude(new BigDecimal("-46.6333"));

        prato = new Prato();
        prato.setId(1L);
        prato.setNome("Prato Teste");
        prato.setPreco(new BigDecimal("25.50"));
        prato.setDisponivel(true);
        prato.setCategoria(CategoriaMenu.MAIN);
        prato.setRestaurante(restaurante);

        pedidoRequestDTO = new PedidoRequestDTO();
        pedidoRequestDTO.setRestauranteId(1L);
        pedidoRequestDTO.setMetodoPagamento(MetodoPagamento.PIX);
        pedidoRequestDTO.setEnderecoEntrega("Rua de Entrega, 456");
        
        var itemDTO = new com.siseg.dto.pedido.PedidoItemRequestDTO();
        itemDTO.setPratoId(1L);
        itemDTO.setQuantidade(2);
        pedidoRequestDTO.setItens(List.of(itemDTO));
    }

    @Test
    void deveGeocodificarEnderecoEntregaAoCriarPedido() {
        // Given
        Pedido pedidoSalvo = new Pedido();
        pedidoSalvo.setId(1L);
        pedidoSalvo.setCliente(cliente);
        pedidoSalvo.setRestaurante(restaurante);
        pedidoSalvo.setEnderecoEntrega(pedidoRequestDTO.getEnderecoEntrega());
        pedidoSalvo.setStatus(StatusPedido.CREATED);
        pedidoSalvo.setItens(new ArrayList<>());

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            when(clienteRepository.findByUserId(user.getId())).thenReturn(Optional.of(cliente));
            when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
            when(pratoRepository.findById(1L)).thenReturn(Optional.of(prato));
            when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
                Pedido pedido = invocation.getArgument(0);
                pedido.setId(1L);
                return pedido;
            });

            // Mock geocodificação do endereço de entrega
            Coordinates coordsEntrega = new Coordinates(
                new BigDecimal("-23.5506"), 
                new BigDecimal("-46.6334")
            );
            when(geocodingService.geocodeAddress(pedidoRequestDTO.getEnderecoEntrega()))
                    .thenReturn(Optional.of(coordsEntrega));

            // Mock PedidoMapper para retornar DTO
            PedidoResponseDTO responseDTO = new PedidoResponseDTO();
            responseDTO.setId(1L);
            responseDTO.setClienteId(cliente.getId());
            responseDTO.setRestauranteId(restaurante.getId());
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(responseDTO);

            // When
            PedidoResponseDTO result = pedidoService.criarPedido(null, pedidoRequestDTO);

            // Then
            assertNotNull(result);
            verify(geocodingService, times(1)).geocodeAddress(pedidoRequestDTO.getEnderecoEntrega());
            verify(pedidoRepository, times(1)).save(argThat(p ->
                p.getLatitudeEntrega() != null &&
                p.getLongitudeEntrega() != null &&
                p.getLatitudeEntrega().equals(coordsEntrega.getLatitude()) &&
                p.getLongitudeEntrega().equals(coordsEntrega.getLongitude())
            ));
        }
    }

    @Test
    void deveReutilizarCoordenadasDoClienteSeEnderecoForIgual() {
        // Given
        cliente.setLatitude(new BigDecimal("-23.5506"));
        cliente.setLongitude(new BigDecimal("-46.6334"));
        
        // Mesmo endereço do cliente
        pedidoRequestDTO.setEnderecoEntrega(cliente.getEndereco());

        Pedido pedidoSalvo = new Pedido();
        pedidoSalvo.setId(1L);
        pedidoSalvo.setCliente(cliente);
        pedidoSalvo.setRestaurante(restaurante);
        pedidoSalvo.setEnderecoEntrega(pedidoRequestDTO.getEnderecoEntrega());
        pedidoSalvo.setStatus(StatusPedido.CREATED);
        pedidoSalvo.setItens(new ArrayList<>());

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            when(clienteRepository.findByUserId(user.getId())).thenReturn(Optional.of(cliente));
            when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
            when(pratoRepository.findById(1L)).thenReturn(Optional.of(prato));
            when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
                Pedido pedido = invocation.getArgument(0);
                pedido.setId(1L);
                return pedido;
            });

            // Mock PedidoMapper para retornar DTO
            PedidoResponseDTO responseDTO = new PedidoResponseDTO();
            responseDTO.setId(1L);
            responseDTO.setClienteId(cliente.getId());
            responseDTO.setRestauranteId(restaurante.getId());
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(responseDTO);

            // When
            PedidoResponseDTO result = pedidoService.criarPedido(null, pedidoRequestDTO);

            // Then
            assertNotNull(result);
            // Não deve chamar geocodificação se reutilizar coordenadas
            verify(geocodingService, never()).geocodeAddress(anyString());
            verify(pedidoRepository, times(1)).save(argThat(p ->
                p.getLatitudeEntrega() != null &&
                p.getLongitudeEntrega() != null &&
                p.getLatitudeEntrega().equals(cliente.getLatitude()) &&
                p.getLongitudeEntrega().equals(cliente.getLongitude())
            ));
        }
    }

    @Test
    void deveCalcularDistanciaRealAoAceitarPedido() {
        // Given
        Entregador entregador = new Entregador();
        entregador.setId(1L);
        entregador.setNome("Entregador Teste");
        entregador.setUser(user);
        entregador.setTipoVeiculo(TipoVeiculo.MOTO);
        entregador.setStatus(StatusEntregador.APPROVED);

        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setCliente(cliente);
        pedido.setRestaurante(restaurante);
        pedido.setStatus(StatusPedido.PREPARING);
        pedido.setLatitudeEntrega(new BigDecimal("-23.5506"));
        pedido.setLongitudeEntrega(new BigDecimal("-46.6334"));
        pedido.setSubtotal(new BigDecimal("51.00"));
        pedido.setTaxaEntrega(new BigDecimal("5.00"));
        pedido.setTotal(new BigDecimal("56.00"));
        pedido.setItens(new ArrayList<>());

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

            when(pedidoValidator.validateEntregadorAprovado(any(User.class))).thenReturn(entregador);
            doNothing().when(pedidoValidator).validatePedidoAceitavel(any(Pedido.class));
            
            ResultadoCalculo resultado = new ResultadoCalculo(
                new BigDecimal("2.0"), 20, false
            );
            when(tempoEstimadoCalculator.calculateDistanceAndTime(
                any(), any(), any(), any(), any()
            )).thenReturn(resultado);

            // Mock PedidoMapper para retornar DTO
            PedidoResponseDTO responseDTO = new PedidoResponseDTO();
            responseDTO.setId(1L);
            responseDTO.setClienteId(cliente.getId());
            responseDTO.setRestauranteId(restaurante.getId());
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(responseDTO);

            // When
            PedidoResponseDTO result = pedidoService.aceitarPedido(1L);

            // Then
            assertNotNull(result);
            // Deve calcular distância usando coordenadas reais
            verify(pedidoRepository, times(1)).save(argThat(p ->
                p.getTempoEstimadoEntrega() != null &&
                p.getEntregador() != null &&
                p.getEntregador().getId().equals(entregador.getId())
            ));
        }
    }
}

