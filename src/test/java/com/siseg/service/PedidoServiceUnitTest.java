package com.siseg.service;

import com.siseg.dto.geocoding.ResultadoCalculo;
import com.siseg.dto.pedido.PedidoItemRequestDTO;
import com.siseg.dto.pedido.PedidoRequestDTO;
import com.siseg.dto.pedido.PedidoResponseDTO;
import com.siseg.dto.rastreamento.RastreamentoDTO;
import com.siseg.exception.AccessDeniedException;
import com.siseg.exception.PedidoAlreadyProcessedException;
import com.siseg.exception.PratoNotAvailableException;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.*;
import com.siseg.model.enumerations.CategoriaMenu;
import com.siseg.model.enumerations.MetodoPagamento;
import com.siseg.model.enumerations.DisponibilidadeEntregador;
import com.siseg.model.enumerations.StatusEntregador;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.model.enumerations.TipoDesconto;
import com.siseg.model.enumerations.TipoEndereco;
import com.siseg.model.enumerations.TipoVeiculo;
import com.siseg.mapper.PedidoMapper;
import com.siseg.repository.*;
import com.siseg.service.pedido.PedidoEnderecoService;
import com.siseg.service.pedido.PedidoEntregadorService;
import com.siseg.service.pedido.PedidoFinanceiroService;
import com.siseg.service.pedido.PedidoNotificacaoService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceUnitTest {

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
    private RastreamentoService rastreamentoService;

    @Mock
    private TempoEstimadoCalculator tempoEstimadoCalculator;

    @Mock
    private PedidoMapper pedidoMapper;

    @Mock
    private PedidoValidator pedidoValidator;

    @Mock
    private CarrinhoService carrinhoService;

    @Mock
    private CupomService cupomService;

    @Mock
    private TaxaCalculoService taxaCalculoService;

    @Mock
    private EnderecoService enderecoService;

    @Mock
    private PedidoEnderecoService pedidoEnderecoService;

    @Mock
    private PedidoFinanceiroService pedidoFinanceiroService;

    @Mock
    private PedidoNotificacaoService pedidoNotificacaoService;

    @Mock
    private PedidoEntregadorService pedidoEntregadorService;

    @InjectMocks
    private PedidoService pedidoService;

    private User user;
    private Cliente cliente;
    private Restaurante restaurante;
    private Prato prato;
    private Entregador entregador;
    private Pedido pedido;
    private PedidoRequestDTO pedidoRequestDTO;
    private PedidoResponseDTO pedidoResponseDTO;
    private Carrinho carrinho;
    private Cupom cupom;
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
        enderecoCliente.setLatitude(new BigDecimal("-23.5505"));
        enderecoCliente.setLongitude(new BigDecimal("-46.6333"));
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
        restaurante.setStatus(com.siseg.model.enumerations.StatusRestaurante.APPROVED);
        restaurante.setUser(user);

        // Criar endereço de entrega (pode ser o mesmo do cliente ou diferente)
        enderecoEntrega = new Endereco();
        enderecoEntrega.setId(3L);
        enderecoEntrega.setLogradouro("Rua de Entrega");
        enderecoEntrega.setNumero("789");
        enderecoEntrega.setBairro("Centro");
        enderecoEntrega.setCidade("São Paulo");
        enderecoEntrega.setEstado("SP");
        enderecoEntrega.setCep("01310100");
        enderecoEntrega.setLatitude(new BigDecimal("-23.5506"));
        enderecoEntrega.setLongitude(new BigDecimal("-46.6334"));
        enderecoEntrega.setPrincipal(false);
        enderecoEntrega.setTipo(TipoEndereco.OUTRO);
        enderecoEntrega.setCliente(cliente);

        prato = new Prato();
        prato.setId(1L);
        prato.setNome("Prato Teste");
        prato.setPreco(new BigDecimal("25.50"));
        prato.setDisponivel(true);
        prato.setCategoria(CategoriaMenu.MAIN);
        prato.setRestaurante(restaurante);

        entregador = new Entregador();
        entregador.setId(1L);
        entregador.setNome("Entregador Teste");
        entregador.setTipoVeiculo(TipoVeiculo.MOTO);
        entregador.setStatus(StatusEntregador.APPROVED);
        entregador.setDisponibilidade(DisponibilidadeEntregador.AVAILABLE);
        entregador.setUser(user);

        pedido = new Pedido();
        pedido.setId(1L);
        pedido.setCliente(cliente);
        pedido.setRestaurante(restaurante);
        pedido.setStatus(StatusPedido.CREATED);
        pedido.setSubtotal(new BigDecimal("51.00"));
        pedido.setTaxaEntrega(new BigDecimal("0.00"));
        pedido.setTotal(new BigDecimal("51.00"));
        pedido.setItens(new ArrayList<>());
        pedido.setEnderecoEntrega(enderecoEntrega);

        pedidoRequestDTO = new PedidoRequestDTO();
        pedidoRequestDTO.setRestauranteId(1L);
        pedidoRequestDTO.setMetodoPagamento(MetodoPagamento.PIX);
        
        var itemDTO = new PedidoItemRequestDTO();
        itemDTO.setPratoId(1L);
        itemDTO.setQuantidade(2);
        pedidoRequestDTO.setItens(List.of(itemDTO));

        pedidoResponseDTO = new PedidoResponseDTO();
        pedidoResponseDTO.setId(1L);
        pedidoResponseDTO.setClienteId(cliente.getId());
        pedidoResponseDTO.setRestauranteId(restaurante.getId());

        carrinho = new Carrinho();
        carrinho.setId(1L);
        carrinho.setCliente(cliente);
        carrinho.setItens(new ArrayList<>());
        carrinho.setSubtotal(new BigDecimal("50.00"));
        carrinho.setTotal(new BigDecimal("50.00"));

        cupom = new Cupom();
        cupom.setId(1L);
        cupom.setCodigo("DESCONTO10");
        cupom.setTipoDesconto(TipoDesconto.PERCENTUAL);
        cupom.setValorDesconto(new BigDecimal("10.00"));
        cupom.setValorMinimo(new BigDecimal("50.00"));
        cupom.setAtivo(true);
    }

    // ============ TESTES DE CRIAÇÃO DE PEDIDO ============

    @Test
    void deveCriarPedidoComItensDiretos() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            when(clienteRepository.findByUserId(user.getId())).thenReturn(Optional.of(cliente));
            when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
            doNothing().when(pedidoEnderecoService).processarEnderecoEntrega(any(), any(), any());
            doNothing().when(pedidoFinanceiroService).processarItensPedido(any(), any());
            doNothing().when(pedidoFinanceiroService).calcularValoresPedido(any());
            when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
                Pedido p = invocation.getArgument(0);
                p.setId(1L);
                return p;
            });
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(pedidoResponseDTO);

            PedidoResponseDTO result = pedidoService.criarPedido(null, pedidoRequestDTO);

            assertNotNull(result);
            verify(pedidoRepository, times(1)).save(any(Pedido.class));
            verify(pedidoEnderecoService).processarEnderecoEntrega(any(), eq(cliente), eq(pedidoRequestDTO));
            verify(pedidoFinanceiroService).processarItensPedido(any(), any());
            verify(pedidoFinanceiroService, never()).processarCarrinhoParaPedido(any(), anyLong(), anyLong());
        }
    }

    @Test
    void deveCriarPedidoComCarrinhoId() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            pedidoRequestDTO.setCarrinhoId(1L);
            pedidoRequestDTO.setItens(null);

            when(clienteRepository.findByUserId(user.getId())).thenReturn(Optional.of(cliente));
            when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
            doNothing().when(pedidoEnderecoService).processarEnderecoEntrega(any(), any(), any());
            doNothing().when(pedidoFinanceiroService).processarCarrinhoParaPedido(any(), anyLong(), anyLong());
            when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(pedidoResponseDTO);
            doNothing().when(pedidoFinanceiroService).limparCarrinho();

            PedidoResponseDTO result = pedidoService.criarPedido(null, pedidoRequestDTO);

            assertNotNull(result);
            verify(pedidoFinanceiroService).processarCarrinhoParaPedido(any(), eq(1L), eq(cliente.getId()));
            verify(pedidoFinanceiroService).limparCarrinho();
            verify(pedidoRepository, times(1)).save(any(Pedido.class));
        }
    }

    @Test
    void deveLancarExcecaoQuandoClienteSemPermissao() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            Cliente outroCliente = new Cliente();
            outroCliente.setId(2L);
            outroCliente.setUser(new User());

            when(clienteRepository.findById(2L)).thenReturn(Optional.of(outroCliente));
            doThrow(new AccessDeniedException("Você não tem permissão para criar pedidos para este cliente"))
                    .when(pedidoValidator).validatePermissaoCliente(any(Cliente.class), any(User.class));

            assertThrows(AccessDeniedException.class, 
                    () -> pedidoService.criarPedido(2L, pedidoRequestDTO));
        }
    }

    // ============ TESTES DE BUSCA/CONSULTA ============

    @Test
    void deveBuscarPedidoComRastreamentoQuandoSaiuParaEntrega() {
        pedido.setStatus(StatusPedido.OUT_FOR_DELIVERY);

        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        when(pedidoMapper.toResponseDTO(pedido)).thenReturn(pedidoResponseDTO);
        when(rastreamentoService.obterRastreamento(pedido.getId())).thenReturn(new RastreamentoDTO());

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(() -> SecurityUtils.validatePedidoOwnership(pedido)).thenAnswer(inv -> null);

            PedidoResponseDTO response = pedidoService.buscarPorId(pedido.getId());

            assertNotNull(response.getRastreamento());
            verify(rastreamentoService).obterRastreamento(pedido.getId());
        }
    }

    @Test
    void naoDeveBuscarRastreamentoQuandoPedidoNaoSaiu() {
        pedido.setStatus(StatusPedido.CREATED);

        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        when(pedidoMapper.toResponseDTO(pedido)).thenReturn(pedidoResponseDTO);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(() -> SecurityUtils.validatePedidoOwnership(pedido)).thenAnswer(inv -> null);

            pedidoService.buscarPorId(pedido.getId());

            verify(rastreamentoService, never()).obterRastreamento(anyLong());
        }
    }

    @Test
    void deveLancarExcecaoQuandoPedidoNaoEncontrado() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.empty());

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(() -> SecurityUtils.validatePedidoOwnership(any())).thenAnswer(inv -> null);

            assertThrows(ResourceNotFoundException.class, 
                    () -> pedidoService.buscarPorId(1L));
        }
    }

    // ============ TESTES DE CONFIRMAÇÃO ============

    @Test
    void deveConfirmarPedido() {
        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(pedido)).thenReturn(pedido);
        when(pedidoMapper.toResponseDTO(pedido)).thenReturn(pedidoResponseDTO);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(() -> SecurityUtils.validatePedidoOwnership(pedido)).thenAnswer(inv -> null);

            PedidoResponseDTO response = pedidoService.confirmarPedido(pedido.getId());

            assertNotNull(response);
            assertEquals(StatusPedido.CONFIRMED, pedido.getStatus());
            verify(pedidoValidator).validateStatusParaConfirmacao(pedido);
            verify(pedidoNotificacaoService).enviarNotificacoesConfirmacaoPedido(pedido);
        }
    }

    // ============ TESTES DE PREPARO ============

    @Test
    void deveMarcarPedidoComoPreparando() {
        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(pedido)).thenReturn(pedido);
        when(pedidoMapper.toResponseDTO(pedido)).thenReturn(pedidoResponseDTO);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(() -> SecurityUtils.validateRestauranteOwnership(restaurante)).thenAnswer(inv -> null);

            PedidoResponseDTO response = pedidoService.marcarComoPreparando(pedido.getId());

            assertNotNull(response);
            assertEquals(StatusPedido.PREPARING, pedido.getStatus());
            verify(pedidoValidator).validateStatusPreparo(pedido);
            verify(pedidoNotificacaoService).notificarClienteStatusPedido(pedido, "PREPARING");
        }
    }

    // ============ TESTES DE ENTREGA (DELEGAÇÃO) ============

    @Test
    void deveDelegarMarcacaoSaiuParaEntrega() {
        PedidoResponseDTO esperado = new PedidoResponseDTO();
        when(pedidoEntregadorService.marcarSaiuEntrega(1L)).thenReturn(esperado);

        PedidoResponseDTO response = pedidoService.marcarSaiuEntrega(1L);

        assertEquals(esperado, response);
        verify(pedidoEntregadorService).marcarSaiuEntrega(1L);
    }

    @Test
    void deveDelegarMarcacaoComoEntregue() {
        PedidoResponseDTO esperado = new PedidoResponseDTO();
        when(pedidoEntregadorService.marcarComoEntregue(1L)).thenReturn(esperado);

        PedidoResponseDTO response = pedidoService.marcarComoEntregue(1L);

        assertEquals(esperado, response);
        verify(pedidoEntregadorService).marcarComoEntregue(1L);
    }

    @Test
    void deveDelegarAceitarPedido() {
        PedidoResponseDTO esperado = new PedidoResponseDTO();
        when(pedidoEntregadorService.aceitarPedido(1L)).thenReturn(esperado);

        PedidoResponseDTO response = pedidoService.aceitarPedido(1L);

        assertEquals(esperado, response);
        verify(pedidoEntregadorService).aceitarPedido(1L);
    }

    @Test
    void deveDelegarRecusarPedido() {
        doNothing().when(pedidoEntregadorService).recusarPedido(1L);

        pedidoService.recusarPedido(1L);

        verify(pedidoEntregadorService).recusarPedido(1L);
    }

    // ============ TESTES DE CANCELAMENTO ============

    @Test
    void deveCancelarPedidoComReembolso() {
        pedido.setStatus(StatusPedido.CONFIRMED);

        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(pedido)).thenReturn(pedido);
        when(pedidoMapper.toResponseDTO(pedido)).thenReturn(pedidoResponseDTO);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(() -> SecurityUtils.validatePedidoOwnership(pedido)).thenAnswer(inv -> null);

            PedidoResponseDTO response = pedidoService.cancelarPedido(pedido.getId());

            assertNotNull(response);
            assertEquals(StatusPedido.CANCELED, pedido.getStatus());
            verify(pedidoFinanceiroService).processarReembolsoSeNecessario(pedido);
        }
    }

    @Test
    void deveRejeitarCancelamentoQuandoStatusNaoPermite() {
        pedido.setStatus(StatusPedido.PREPARING);
        when(pedidoRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(() -> SecurityUtils.validatePedidoOwnership(pedido)).thenAnswer(inv -> null);

            assertThrows(IllegalStateException.class,
                () -> pedidoService.cancelarPedido(pedido.getId()));
            verify(pedidoFinanceiroService, never()).processarReembolsoSeNecessario(any());
        }
    }

    // ============ TESTES DE LISTAGEM ============

    @Test
    void deveListarPedidosDoClienteLogado() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pedido> page = new PageImpl<>(List.of(pedido));

        when(clienteRepository.findByUserId(user.getId())).thenReturn(Optional.of(cliente));
        when(pedidoRepository.findByClienteId(cliente.getId(), pageable)).thenReturn(page);
        when(pedidoMapper.toResponseDTO(pedido)).thenReturn(pedidoResponseDTO);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(user);

            Page<PedidoResponseDTO> result = pedidoService.listarMeusPedidos(null, null, null, null, pageable);

            assertEquals(1, result.getTotalElements());
            verify(pedidoRepository).findByClienteId(cliente.getId(), pageable);
        }
    }

    @Test
    void deveListarPedidosComFiltroStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pedido> page = new PageImpl<>(List.of(pedido));

        when(clienteRepository.findByUserId(user.getId())).thenReturn(Optional.of(cliente));
        when(pedidoRepository.findByClienteIdAndStatus(cliente.getId(), StatusPedido.DELIVERED, pageable))
                .thenReturn(page);
        when(pedidoMapper.toResponseDTO(pedido)).thenReturn(pedidoResponseDTO);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(user);

            Page<PedidoResponseDTO> result = pedidoService.listarMeusPedidos(
                    StatusPedido.DELIVERED, null, null, null, pageable);

            assertEquals(1, result.getTotalElements());
            verify(pedidoRepository).findByClienteIdAndStatus(cliente.getId(), StatusPedido.DELIVERED, pageable);
        }
    }

    @Test
    void deveListarPedidosComFiltroPeriodo() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pedido> page = new PageImpl<>(List.of(pedido));
        Instant dataInicio = Instant.now().minusSeconds(86400);
        Instant dataFim = Instant.now();

        when(clienteRepository.findByUserId(user.getId())).thenReturn(Optional.of(cliente));
        when(pedidoRepository.findByClienteIdAndCriadoEmBetween(cliente.getId(), dataInicio, dataFim, pageable))
                .thenReturn(page);
        when(pedidoMapper.toResponseDTO(pedido)).thenReturn(pedidoResponseDTO);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(user);

            Page<PedidoResponseDTO> result = pedidoService.listarMeusPedidos(
                    null, dataInicio, dataFim, null, pageable);

            assertEquals(1, result.getTotalElements());
            verify(pedidoRepository).findByClienteIdAndCriadoEmBetween(cliente.getId(), dataInicio, dataFim, pageable);
        }
    }

    @Test
    void deveListarPedidosComFiltroRestaurante() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pedido> page = new PageImpl<>(List.of(pedido));

        when(clienteRepository.findByUserId(user.getId())).thenReturn(Optional.of(cliente));
        when(pedidoRepository.findByClienteIdAndRestauranteId(cliente.getId(), 1L, pageable))
                .thenReturn(page);
        when(pedidoMapper.toResponseDTO(pedido)).thenReturn(pedidoResponseDTO);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(user);

            Page<PedidoResponseDTO> result = pedidoService.listarMeusPedidos(
                    null, null, null, 1L, pageable);

            assertEquals(1, result.getTotalElements());
            verify(pedidoRepository).findByClienteIdAndRestauranteId(cliente.getId(), 1L, pageable);
        }
    }

    @Test
    void deveListarPedidosRestauranteSemFiltros() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pedido> page = new PageImpl<>(List.of(pedido));

        when(pedidoValidator.validateRestauranteAprovado(user)).thenReturn(restaurante);
        when(pedidoRepository.findByRestauranteId(restaurante.getId(), pageable)).thenReturn(page);
        when(pedidoMapper.toResponseDTO(pedido)).thenReturn(pedidoResponseDTO);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(user);

            Page<PedidoResponseDTO> result = pedidoService.listarPedidosRestaurante(null, null, null, pageable);

            assertEquals(1, result.getTotalElements());
            verify(pedidoRepository).findByRestauranteId(restaurante.getId(), pageable);
        }
    }

    @Test
    void deveListarPedidosRestauranteComFiltroStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pedido> page = new PageImpl<>(List.of(pedido));

        when(pedidoValidator.validateRestauranteAprovado(user)).thenReturn(restaurante);
        when(pedidoRepository.findByRestauranteIdAndStatus(restaurante.getId(), StatusPedido.CREATED, pageable))
                .thenReturn(page);
        when(pedidoMapper.toResponseDTO(pedido)).thenReturn(pedidoResponseDTO);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(user);

            Page<PedidoResponseDTO> result = pedidoService.listarPedidosRestaurante(
                    StatusPedido.CREATED, null, null, pageable);

            assertEquals(1, result.getTotalElements());
            verify(pedidoRepository).findByRestauranteIdAndStatus(restaurante.getId(), StatusPedido.CREATED, pageable);
        }
    }

    @Test
    void deveListarPedidosRestauranteComFiltroPeriodo() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pedido> page = new PageImpl<>(List.of(pedido));
        Instant dataInicio = Instant.now().minusSeconds(86400);
        Instant dataFim = Instant.now();

        when(pedidoValidator.validateRestauranteAprovado(user)).thenReturn(restaurante);
        when(pedidoRepository.findByRestauranteIdAndCriadoEmBetween(
                restaurante.getId(), dataInicio, dataFim, pageable))
                .thenReturn(page);
        when(pedidoMapper.toResponseDTO(pedido)).thenReturn(pedidoResponseDTO);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(user);

            Page<PedidoResponseDTO> result = pedidoService.listarPedidosRestaurante(
                    null, dataInicio, dataFim, pageable);

            assertEquals(1, result.getTotalElements());
            verify(pedidoRepository).findByRestauranteIdAndCriadoEmBetween(
                    restaurante.getId(), dataInicio, dataFim, pageable);
        }
    }

    @Test
    void deveListarPedidosRestauranteComFiltroStatusEPeriodo() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pedido> page = new PageImpl<>(List.of(pedido));
        Instant dataInicio = Instant.now().minusSeconds(86400);
        Instant dataFim = Instant.now();

        when(pedidoValidator.validateRestauranteAprovado(user)).thenReturn(restaurante);
        when(pedidoRepository.findByRestauranteIdAndStatusAndCriadoEmBetween(
                restaurante.getId(), StatusPedido.CREATED, dataInicio, dataFim, pageable))
                .thenReturn(page);
        when(pedidoMapper.toResponseDTO(pedido)).thenReturn(pedidoResponseDTO);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(user);

            Page<PedidoResponseDTO> result = pedidoService.listarPedidosRestaurante(
                    StatusPedido.CREATED, dataInicio, dataFim, pageable);

            assertEquals(1, result.getTotalElements());
            verify(pedidoRepository).findByRestauranteIdAndStatusAndCriadoEmBetween(
                    restaurante.getId(), StatusPedido.CREATED, dataInicio, dataFim, pageable);
        }
    }

    // ============ TESTES DE PEDIDOS DISPONÍVEIS (ENTREGADOR) ============

    @Test
    void deveDelegarListarPedidosDisponiveis() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PedidoResponseDTO> page = new PageImpl<>(List.of(pedidoResponseDTO));

        when(pedidoEntregadorService.listarPedidosDisponiveis(pageable)).thenReturn(page);

        Page<PedidoResponseDTO> result = pedidoService.listarPedidosDisponiveis(pageable);

        assertEquals(1, result.getTotalElements());
        verify(pedidoEntregadorService).listarPedidosDisponiveis(pageable);
    }

    @Test
    void deveDelegarListarEntregasAtivas() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PedidoResponseDTO> page = new PageImpl<>(List.of(pedidoResponseDTO));

        when(pedidoEntregadorService.listarEntregasAtivas(pageable)).thenReturn(page);

        Page<PedidoResponseDTO> result = pedidoService.listarEntregasAtivas(pageable);

        assertEquals(1, result.getTotalElements());
        verify(pedidoEntregadorService).listarEntregasAtivas(pageable);
    }

    @Test
    void deveDelegarListarHistoricoEntregas() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PedidoResponseDTO> page = new PageImpl<>(List.of(pedidoResponseDTO));

        when(pedidoEntregadorService.listarHistoricoEntregas(pageable)).thenReturn(page);

        Page<PedidoResponseDTO> result = pedidoService.listarHistoricoEntregas(pageable);

        assertEquals(1, result.getTotalElements());
        verify(pedidoEntregadorService).listarHistoricoEntregas(pageable);
    }
}
