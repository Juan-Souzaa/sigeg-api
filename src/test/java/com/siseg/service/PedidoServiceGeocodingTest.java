package com.siseg.service;

import com.siseg.dto.geocoding.ResultadoCalculo;
import com.siseg.dto.pedido.PedidoRequestDTO;
import com.siseg.dto.pedido.PedidoResponseDTO;
import com.siseg.model.*;
import com.siseg.model.enumerations.CategoriaMenu;
import com.siseg.model.enumerations.MetodoPagamento;
import com.siseg.model.enumerations.StatusEntregador;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.model.enumerations.TipoEndereco;
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
    private ModelMapper modelMapper;

    @Mock
    private PedidoMapper pedidoMapper;

    @Mock
    private PedidoValidator pedidoValidator;

    @Mock
    private TempoEstimadoCalculator tempoEstimadoCalculator;

    @Mock
    private EnderecoService enderecoService;

    @Mock
    private CarrinhoService carrinhoService;

    @Mock
    private CupomService cupomService;

    @Mock
    private TaxaCalculoService taxaCalculoService;

    @InjectMocks
    private PedidoService pedidoService;

    private Cliente cliente;
    private Restaurante restaurante;
    private Prato prato;
    private PedidoRequestDTO pedidoRequestDTO;
    private User user;
    private Endereco enderecoCliente;
    private Endereco enderecoRestaurante;
    private Endereco enderecoEntrega;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("cliente@teste.com");

        // Criar endereço do cliente
        enderecoCliente = new Endereco();
        enderecoCliente.setId(1L);
        enderecoCliente.setLogradouro("Rua do Cliente");
        enderecoCliente.setNumero("123");
        enderecoCliente.setBairro("Centro");
        enderecoCliente.setCidade("São Paulo");
        enderecoCliente.setEstado("SP");
        enderecoCliente.setCep("01310100");
        enderecoCliente.setLatitude(new BigDecimal("-23.5506"));
        enderecoCliente.setLongitude(new BigDecimal("-46.6334"));
        enderecoCliente.setPrincipal(true);
        enderecoCliente.setTipo(TipoEndereco.CASA);

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Cliente Teste");
        cliente.setEnderecos(List.of(enderecoCliente));
        enderecoCliente.setCliente(cliente);
        cliente.setUser(user);

        // Criar endereço do restaurante
        enderecoRestaurante = new Endereco();
        enderecoRestaurante.setId(2L);
        enderecoRestaurante.setLogradouro("Rua do Restaurante");
        enderecoRestaurante.setNumero("456");
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
        restaurante.setNome("Restaurante Teste");
        restaurante.setEnderecos(List.of(enderecoRestaurante));
        enderecoRestaurante.setRestaurante(restaurante);

        prato = new Prato();
        prato.setId(1L);
        prato.setNome("Prato Teste");
        prato.setPreco(new BigDecimal("25.50"));
        prato.setDisponivel(true);
        prato.setCategoria(CategoriaMenu.MAIN);
        prato.setRestaurante(restaurante);

        // Criar endereço de entrega
        enderecoEntrega = new Endereco();
        enderecoEntrega.setId(3L);
        enderecoEntrega.setLogradouro("Rua de Entrega");
        enderecoEntrega.setNumero("456");
        enderecoEntrega.setBairro("Centro");
        enderecoEntrega.setCidade("São Paulo");
        enderecoEntrega.setEstado("SP");
        enderecoEntrega.setCep("01310100");
        enderecoEntrega.setPrincipal(false);
        enderecoEntrega.setTipo(TipoEndereco.OUTRO);
        enderecoEntrega.setCliente(cliente);

        pedidoRequestDTO = new PedidoRequestDTO();
        pedidoRequestDTO.setRestauranteId(1L);
        pedidoRequestDTO.setMetodoPagamento(MetodoPagamento.PIX);
        // Não definir enderecoId - deve usar endereço principal do cliente
        
        var itemDTO = new com.siseg.dto.pedido.PedidoItemRequestDTO();
        itemDTO.setPratoId(1L);
        itemDTO.setQuantidade(2);
        pedidoRequestDTO.setItens(List.of(itemDTO));
    }

    @Test
    void deveGeocodificarEnderecoEntregaAoCriarPedido() {
        // Given - endereço sem coordenadas
        enderecoCliente.setLatitude(null);
        enderecoCliente.setLongitude(null);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            when(clienteRepository.findByUserId(user.getId())).thenReturn(Optional.of(cliente));
            when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
            when(pratoRepository.findById(1L)).thenReturn(Optional.of(prato));
            when(pedidoValidator.validatePratoDisponivel(any(Prato.class))).thenReturn(prato);
            when(enderecoService.buscarEnderecoPrincipalCliente(cliente.getId())).thenReturn(Optional.of(enderecoCliente));
            when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
                Pedido pedido = invocation.getArgument(0);
                pedido.setId(1L);
                return pedido;
            });

            // Mock geocodificação do endereço
            when(enderecoService.geocodificarESalvar(any(Endereco.class))).thenAnswer(invocation -> {
                Endereco endereco = invocation.getArgument(0);
                endereco.setLatitude(new BigDecimal("-23.5506"));
                endereco.setLongitude(new BigDecimal("-46.6334"));
                return endereco;
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
            verify(enderecoService, times(1)).geocodificarESalvar(any(Endereco.class));
        }
    }

    @Test
    void deveReutilizarCoordenadasDoClienteSeEnderecoJaTemCoordenadas() {
        // Given - endereço já tem coordenadas
        enderecoCliente.setLatitude(new BigDecimal("-23.5506"));
        enderecoCliente.setLongitude(new BigDecimal("-46.6334"));

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            when(clienteRepository.findByUserId(user.getId())).thenReturn(Optional.of(cliente));
            when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
            when(pratoRepository.findById(1L)).thenReturn(Optional.of(prato));
            when(pedidoValidator.validatePratoDisponivel(any(Prato.class))).thenReturn(prato);
            when(enderecoService.buscarEnderecoPrincipalCliente(cliente.getId())).thenReturn(Optional.of(enderecoCliente));
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
            // Não deve chamar geocodificação se endereço já tem coordenadas
            verify(enderecoService, never()).geocodificarESalvar(any(Endereco.class));
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
        pedido.setEnderecoEntrega(enderecoEntrega);
        enderecoEntrega.setLatitude(new BigDecimal("-23.5506"));
        enderecoEntrega.setLongitude(new BigDecimal("-46.6334"));
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

