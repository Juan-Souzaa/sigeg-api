package com.siseg.service;

import com.siseg.dto.geocoding.ResultadoCalculo;
import com.siseg.dto.pedido.PedidoItemRequestDTO;
import com.siseg.dto.pedido.PedidoRequestDTO;
import com.siseg.dto.pedido.PedidoResponseDTO;
import com.siseg.exception.AccessDeniedException;
import com.siseg.exception.PedidoAlreadyProcessedException;
import com.siseg.exception.PratoNotAvailableException;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.*;
import com.siseg.model.enumerations.CategoriaMenu;
import com.siseg.model.enumerations.MetodoPagamento;
import com.siseg.model.enumerations.StatusEntregador;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.model.enumerations.TipoDesconto;
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
import static org.mockito.Mockito.lenient;

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
        // Não definir enderecoId - deve usar endereço principal do cliente
        
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

    @Test
    void deveCriarPedidoComItensDiretos() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            when(clienteRepository.findByUserId(user.getId())).thenReturn(Optional.of(cliente));
            when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
            when(pratoRepository.findById(1L)).thenReturn(Optional.of(prato));
            when(pedidoValidator.validatePratoDisponivel(any(Prato.class))).thenReturn(prato);
            when(enderecoService.buscarEnderecoPrincipalCliente(cliente.getId())).thenReturn(Optional.of(enderecoCliente));
            when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
                Pedido p = invocation.getArgument(0);
                p.setId(1L);
                return p;
            });
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(pedidoResponseDTO);

            PedidoResponseDTO result = pedidoService.criarPedido(null, pedidoRequestDTO);

            assertNotNull(result);
            verify(pedidoRepository, times(1)).save(any(Pedido.class));
            verify(pedidoValidator, times(1)).validatePratoDisponivel(any(Prato.class));
        }
    }

    @Test
    void deveCriarPedidoComCarrinhoId() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            CarrinhoItem carrinhoItem = new CarrinhoItem();
            carrinhoItem.setPrato(prato);
            carrinhoItem.setQuantidade(2);
            carrinhoItem.setPrecoUnitario(prato.getPreco());
            carrinhoItem.setSubtotal(prato.getPreco().multiply(BigDecimal.valueOf(2)));
            carrinho.getItens().add(carrinhoItem);

            pedidoRequestDTO.setCarrinhoId(1L);
            pedidoRequestDTO.setItens(null);

            when(clienteRepository.findByUserId(user.getId())).thenReturn(Optional.of(cliente));
            when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
            when(enderecoService.buscarEnderecoPrincipalCliente(cliente.getId())).thenReturn(Optional.of(enderecoCliente));
            when(carrinhoService.obterCarrinhoParaPedido(cliente.getId())).thenReturn(carrinho);
            when(pedidoValidator.validatePratoDisponivel(any(Prato.class))).thenReturn(prato);
            when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
                Pedido p = invocation.getArgument(0);
                p.setId(1L);
                return p;
            });
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(pedidoResponseDTO);
            doNothing().when(carrinhoService).limparCarrinho();

            PedidoResponseDTO result = pedidoService.criarPedido(null, pedidoRequestDTO);

            assertNotNull(result);
            verify(carrinhoService, times(1)).obterCarrinhoParaPedido(cliente.getId());
            verify(carrinhoService, times(1)).limparCarrinho();
            verify(pedidoRepository, times(1)).save(any(Pedido.class));
        }
    }

    @Test
    void deveCriarPedidoComCupomAplicadoNoCarrinho() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            CarrinhoItem carrinhoItem = new CarrinhoItem();
            carrinhoItem.setPrato(prato);
            carrinhoItem.setQuantidade(2);
            carrinhoItem.setPrecoUnitario(prato.getPreco());
            carrinhoItem.setSubtotal(prato.getPreco().multiply(BigDecimal.valueOf(2)));
            carrinho.getItens().add(carrinhoItem);
            carrinho.setCupom(cupom);
            carrinho.setSubtotal(new BigDecimal("51.00"));

            pedidoRequestDTO.setCarrinhoId(1L);
            pedidoRequestDTO.setItens(null);

            when(clienteRepository.findByUserId(user.getId())).thenReturn(Optional.of(cliente));
            when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
            when(enderecoService.buscarEnderecoPrincipalCliente(cliente.getId())).thenReturn(Optional.of(enderecoCliente));
            when(carrinhoService.obterCarrinhoParaPedido(cliente.getId())).thenReturn(carrinho);
            when(pedidoValidator.validatePratoDisponivel(any(Prato.class))).thenReturn(prato);
            when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
                Pedido p = invocation.getArgument(0);
                p.setId(1L);
                return p;
            });
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(pedidoResponseDTO);
            doNothing().when(cupomService).incrementarUsoCupom(any(Cupom.class));
            doNothing().when(carrinhoService).limparCarrinho();

            PedidoResponseDTO result = pedidoService.criarPedido(null, pedidoRequestDTO);

            assertNotNull(result);
            verify(cupomService, times(1)).incrementarUsoCupom(cupom);
            verify(carrinhoService, times(1)).limparCarrinho();
        }
    }

    @Test
    void deveLancarExcecaoQuandoPratoIndisponivel() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            prato.setDisponivel(false);

            when(clienteRepository.findByUserId(user.getId())).thenReturn(Optional.of(cliente));
            when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
            when(pratoRepository.findById(1L)).thenReturn(Optional.of(prato));
            when(enderecoService.buscarEnderecoPrincipalCliente(cliente.getId())).thenReturn(Optional.of(enderecoCliente));
            when(pedidoValidator.validatePratoDisponivel(any(Prato.class)))
                    .thenThrow(new PratoNotAvailableException("Prato não disponível: " + prato.getNome()));

            assertThrows(PratoNotAvailableException.class, 
                    () -> pedidoService.criarPedido(null, pedidoRequestDTO));
        }
    }

    @Test
    void deveLancarExcecaoQuandoClienteSemPermissao() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(false);

            Cliente outroCliente = new Cliente();
            outroCliente.setId(2L);
            outroCliente.setUser(new User());

            when(clienteRepository.findById(2L)).thenReturn(Optional.of(outroCliente));
            doThrow(new AccessDeniedException("Você não tem permissão para criar pedidos para este cliente"))
                    .when(pedidoValidator).validatePermissaoCliente(any(Cliente.class), any(User.class));

            pedidoRequestDTO.setRestauranteId(1L);

            assertThrows(AccessDeniedException.class, 
                    () -> pedidoService.criarPedido(2L, pedidoRequestDTO));
        }
    }

    @Test
    void deveAceitarPedidoComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            pedido.setStatus(StatusPedido.PREPARING);
            pedido.setEnderecoEntrega(enderecoEntrega);

            ResultadoCalculo resultado = new ResultadoCalculo(
                    new BigDecimal("2.0"), 20, false);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(pedidoValidator.validateEntregadorAprovado(any(User.class))).thenReturn(entregador);
            doNothing().when(pedidoValidator).validatePedidoAceitavel(any(Pedido.class));
            when(tempoEstimadoCalculator.calculateDistanceAndTime(
                    any(BigDecimal.class), any(BigDecimal.class),
                    any(BigDecimal.class), any(BigDecimal.class),
                    any(TipoVeiculo.class))).thenReturn(resultado);
            when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(pedidoResponseDTO);
            lenient().doNothing().when(notificationService).notifyRestaurantNewOrder(anyLong(), any(), any(BigDecimal.class));
            lenient().doNothing().when(notificationService).notifyOrderStatusChange(anyLong(), any(), any(), anyString());

            PedidoResponseDTO result = pedidoService.aceitarPedido(1L);

            assertNotNull(result);
            assertEquals(entregador, pedido.getEntregador());
            verify(pedidoRepository, times(1)).save(pedido);
            verify(tempoEstimadoCalculator, times(1)).calculateDistanceAndTime(
                    any(BigDecimal.class), any(BigDecimal.class),
                    any(BigDecimal.class), any(BigDecimal.class),
                    any(TipoVeiculo.class));
        }
    }

    @Test
    void deveLancarExcecaoQuandoEntregadorNaoAprovado() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            lenient().when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            doThrow(new AccessDeniedException("Entregador não está aprovado"))
                    .when(pedidoValidator).validateEntregadorAprovado(any(User.class));

            assertThrows(AccessDeniedException.class, 
                    () -> pedidoService.aceitarPedido(1L));
        }
    }

    @Test
    void deveLancarExcecaoQuandoPedidoJaAceito() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            pedido.setStatus(StatusPedido.PREPARING);
            pedido.setEntregador(entregador);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(pedidoValidator.validateEntregadorAprovado(any(User.class))).thenReturn(entregador);
            doThrow(new PedidoAlreadyProcessedException("Pedido já foi aceito por outro entregador"))
                    .when(pedidoValidator).validatePedidoAceitavel(any(Pedido.class));

            assertThrows(PedidoAlreadyProcessedException.class, 
                    () -> pedidoService.aceitarPedido(1L));
        }
    }

    @Test
    void deveLancarExcecaoQuandoPedidoEmStatusInvalidoParaAceitar() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            pedido.setStatus(StatusPedido.CREATED);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(pedidoValidator.validateEntregadorAprovado(any(User.class))).thenReturn(entregador);
            doThrow(new PedidoAlreadyProcessedException("Pedido deve estar PREPARING para ser aceito"))
                    .when(pedidoValidator).validatePedidoAceitavel(any(Pedido.class));

            assertThrows(PedidoAlreadyProcessedException.class, 
                    () -> pedidoService.aceitarPedido(1L));
        }
    }

    @Test
    void deveConfirmarPedidoComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            pedido.setStatus(StatusPedido.CREATED);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(pedidoResponseDTO);
            lenient().doNothing().when(notificationService).notifyOrderStatusChange(anyLong(), any(), any(), anyString());
            lenient().doNothing().when(notificationService).notifyRestaurantNewOrder(anyLong(), any(), any(BigDecimal.class));

            PedidoResponseDTO result = pedidoService.confirmarPedido(1L);

            assertNotNull(result);
            assertEquals(StatusPedido.CONFIRMED, pedido.getStatus());
            verify(pedidoRepository, times(1)).save(pedido);
        }
    }

    @Test
    void deveLancarExcecaoQuandoPedidoJaProcessado() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            pedido.setStatus(StatusPedido.CONFIRMED);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            doThrow(new PedidoAlreadyProcessedException("Pedido já foi processado"))
                    .when(pedidoValidator).validateStatusParaConfirmacao(any(Pedido.class));

            assertThrows(PedidoAlreadyProcessedException.class, 
                    () -> pedidoService.confirmarPedido(1L));
        }
    }

    @Test
    void deveMarcarComoPreparandoComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            pedido.setStatus(StatusPedido.CONFIRMED);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(pedidoResponseDTO);
            lenient().doNothing().when(notificationService).notifyOrderStatusChange(anyLong(), any(), any(), anyString());

            PedidoResponseDTO result = pedidoService.marcarComoPreparando(1L);

            assertNotNull(result);
            assertEquals(StatusPedido.PREPARING, pedido.getStatus());
            verify(pedidoRepository, times(1)).save(pedido);
        }
    }

    @Test
    void deveLancarExcecaoQuandoStatusInvalidoParaPreparando() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            pedido.setStatus(StatusPedido.CREATED);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            doThrow(new PedidoAlreadyProcessedException("Pedido deve estar CONFIRMED para ser marcado como PREPARING"))
                    .when(pedidoValidator).validateStatusPreparo(any(Pedido.class));

            assertThrows(PedidoAlreadyProcessedException.class, 
                    () -> pedidoService.marcarComoPreparando(1L));
        }
    }

    @Test
    void deveMarcarSaiuEntregaComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(false);

            pedido.setStatus(StatusPedido.PREPARING);
            pedido.setEntregador(entregador);
            entregador.setLatitude(null);
            entregador.setLongitude(null);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(entregadorRepository.save(any(Entregador.class))).thenReturn(entregador);
            when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(pedidoResponseDTO);
            lenient().doNothing().when(notificationService).notifyOrderStatusChange(anyLong(), any(), any(), anyString());

            PedidoResponseDTO result = pedidoService.marcarSaiuEntrega(1L);

            assertNotNull(result);
            assertEquals(StatusPedido.OUT_FOR_DELIVERY, pedido.getStatus());
            verify(pedidoRepository, times(1)).save(pedido);
            verify(entregadorRepository, times(1)).save(entregador);
        }
    }

    @Test
    void deveInicializarPosicaoEntregadorQuandoNaoTemCoordenadas() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(false);

            pedido.setStatus(StatusPedido.PREPARING);
            pedido.setEntregador(entregador);
            entregador.setLatitude(null);
            entregador.setLongitude(null);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(entregadorRepository.save(any(Entregador.class))).thenReturn(entregador);
            when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(pedidoResponseDTO);
            lenient().doNothing().when(notificationService).notifyOrderStatusChange(anyLong(), any(), any(), anyString());

            pedidoService.marcarSaiuEntrega(1L);

            // Verificar se a posição inicial foi definida (usando coordenadas do restaurante)
            verify(entregadorRepository, times(1)).save(entregador);
        }
    }

    @Test
    void deveMarcarComoEntregueComSucessoECalcularTaxas() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(false);

            pedido.setStatus(StatusPedido.OUT_FOR_DELIVERY);
            pedido.setEntregador(entregador);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(pedidoResponseDTO);
            doNothing().when(taxaCalculoService).calcularEAtualizarValoresFinanceiros(any(Pedido.class));
            lenient().doNothing().when(notificationService).notifyOrderStatusChange(anyLong(), any(), any(), anyString());
            lenient().doNothing().when(notificationService).notifyRestaurantNewOrder(anyLong(), any(), any(BigDecimal.class));

            PedidoResponseDTO result = pedidoService.marcarComoEntregue(1L);

            assertNotNull(result);
            assertEquals(StatusPedido.DELIVERED, pedido.getStatus());
            verify(taxaCalculoService, times(1)).calcularEAtualizarValoresFinanceiros(pedido);
            verify(pedidoRepository, times(1)).save(pedido);
        }
    }

    @Test
    void deveLancarExcecaoQuandoAcessoNegadoParaMarcarEntregue() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(false);

            pedido.setStatus(StatusPedido.OUT_FOR_DELIVERY);
            pedido.setEntregador(null);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            doThrow(new AccessDeniedException("Apenas o entregador associado pode marcar como entregue"))
                    .when(pedidoValidator).validateEntregadorDoPedido(any(Pedido.class), anyString());

            assertThrows(AccessDeniedException.class, 
                    () -> pedidoService.marcarComoEntregue(1L));
        }
    }

    @Test
    void deveBuscarPedidoPorIdComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(pedidoResponseDTO);

            PedidoResponseDTO result = pedidoService.buscarPorId(1L);

            assertNotNull(result);
            verify(pedidoRepository, times(1)).findById(1L);
        }
    }

    @Test
    void deveLancarExcecaoQuandoPedidoNaoEncontrado() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, 
                    () -> pedidoService.buscarPorId(1L));
        }
    }

    @Test
    void deveListarPedidosDisponiveisComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Pedido> pedidosPage = new PageImpl<>(List.of(pedido), pageable, 1);

            when(entregadorRepository.findByUserId(user.getId())).thenReturn(Optional.of(entregador));
            when(pedidoRepository.findByStatusAndEntregadorIsNull(StatusPedido.PREPARING, pageable))
                    .thenReturn(pedidosPage);
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(pedidoResponseDTO);
            lenient().doNothing().when(notificationService).notifyNewOrderAvailable(
                    anyLong(), any(), any(), any(), any(BigDecimal.class));

            Page<PedidoResponseDTO> result = pedidoService.listarPedidosDisponiveis(pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(pedidoRepository, times(1)).findByStatusAndEntregadorIsNull(StatusPedido.PREPARING, pageable);
        }
    }

    @Test
    void deveCalcularTaxaEntregaQuandoSubtotalMenorQueMinimo() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            pedido.setSubtotal(new BigDecimal("30.00"));

            when(clienteRepository.findByUserId(user.getId())).thenReturn(Optional.of(cliente));
            when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
            when(pratoRepository.findById(1L)).thenReturn(Optional.of(prato));
            when(pedidoValidator.validatePratoDisponivel(any(Prato.class))).thenReturn(prato);
            when(enderecoService.buscarEnderecoPrincipalCliente(cliente.getId())).thenReturn(Optional.of(enderecoCliente));
            when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
                Pedido p = invocation.getArgument(0);
                p.setId(1L);
                return p;
            });
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(pedidoResponseDTO);

            pedidoService.criarPedido(null, pedidoRequestDTO);

            verify(pedidoRepository, times(1)).save(any(Pedido.class));
        }
    }

    @Test
    void deveCalcularTaxaEntregaZeroQuandoSubtotalMaiorOuIgualMinimo() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            prato.setPreco(new BigDecimal("30.00"));
            var itemDTO = new PedidoItemRequestDTO();
            itemDTO.setPratoId(1L);
            itemDTO.setQuantidade(2);
            pedidoRequestDTO.setItens(List.of(itemDTO));

            when(clienteRepository.findByUserId(user.getId())).thenReturn(Optional.of(cliente));
            when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
            when(pratoRepository.findById(1L)).thenReturn(Optional.of(prato));
            when(pedidoValidator.validatePratoDisponivel(any(Prato.class))).thenReturn(prato);
            when(enderecoService.buscarEnderecoPrincipalCliente(cliente.getId())).thenReturn(Optional.of(enderecoCliente));
            when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
                Pedido p = invocation.getArgument(0);
                p.setId(1L);
                return p;
            });
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(pedidoResponseDTO);

            pedidoService.criarPedido(null, pedidoRequestDTO);

            verify(pedidoRepository, times(1)).save(argThat(p -> 
                p.getTaxaEntrega().compareTo(BigDecimal.ZERO) == 0));
        }
    }

    @Test
    void deveListarMeusPedidosSemFiltros() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Pedido> pedidosPage = new PageImpl<>(List.of(pedido), pageable, 1);

            when(clienteRepository.findByUserId(user.getId())).thenReturn(Optional.of(cliente));
            when(pedidoRepository.findByClienteId(cliente.getId(), pageable)).thenReturn(pedidosPage);
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(pedidoResponseDTO);

            Page<PedidoResponseDTO> result = pedidoService.listarMeusPedidos(null, null, null, null, pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(pedidoRepository, times(1)).findByClienteId(cliente.getId(), pageable);
        }
    }

    @Test
    void deveListarMeusPedidosComFiltroStatus() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Pedido> pedidosPage = new PageImpl<>(List.of(pedido), pageable, 1);

            when(clienteRepository.findByUserId(user.getId())).thenReturn(Optional.of(cliente));
            when(pedidoRepository.findByClienteIdAndStatus(cliente.getId(), StatusPedido.DELIVERED, pageable))
                    .thenReturn(pedidosPage);
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(pedidoResponseDTO);

            Page<PedidoResponseDTO> result = pedidoService.listarMeusPedidos(
                    StatusPedido.DELIVERED, null, null, null, pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(pedidoRepository, times(1))
                    .findByClienteIdAndStatus(cliente.getId(), StatusPedido.DELIVERED, pageable);
        }
    }

    @Test
    void deveListarMeusPedidosComFiltroRestaurante() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Pedido> pedidosPage = new PageImpl<>(List.of(pedido), pageable, 1);

            when(clienteRepository.findByUserId(user.getId())).thenReturn(Optional.of(cliente));
            when(pedidoRepository.findByClienteIdAndRestauranteId(cliente.getId(), 1L, pageable))
                    .thenReturn(pedidosPage);
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(pedidoResponseDTO);

            Page<PedidoResponseDTO> result = pedidoService.listarMeusPedidos(
                    null, null, null, 1L, pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(pedidoRepository, times(1))
                    .findByClienteIdAndRestauranteId(cliente.getId(), 1L, pageable);
        }
    }

    @Test
    void deveListarPedidosRestauranteSemFiltros() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Pedido> pedidosPage = new PageImpl<>(List.of(pedido), pageable, 1);

            when(pedidoValidator.validateRestauranteAprovado(user)).thenReturn(restaurante);
            when(pedidoRepository.findByRestauranteId(restaurante.getId(), pageable)).thenReturn(pedidosPage);
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(pedidoResponseDTO);

            Page<PedidoResponseDTO> result = pedidoService.listarPedidosRestaurante(null, null, null, pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(pedidoValidator, times(1)).validateRestauranteAprovado(user);
            verify(pedidoRepository, times(1)).findByRestauranteId(restaurante.getId(), pageable);
        }
    }

    @Test
    void deveListarPedidosRestauranteComFiltroStatus() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Pedido> pedidosPage = new PageImpl<>(List.of(pedido), pageable, 1);

            when(pedidoValidator.validateRestauranteAprovado(user)).thenReturn(restaurante);
            when(pedidoRepository.findByRestauranteIdAndStatus(restaurante.getId(), StatusPedido.CREATED, pageable))
                    .thenReturn(pedidosPage);
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(pedidoResponseDTO);

            Page<PedidoResponseDTO> result = pedidoService.listarPedidosRestaurante(
                    StatusPedido.CREATED, null, null, pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(pedidoValidator, times(1)).validateRestauranteAprovado(user);
            verify(pedidoRepository, times(1))
                    .findByRestauranteIdAndStatus(restaurante.getId(), StatusPedido.CREATED, pageable);
        }
    }

    @Test
    void deveListarPedidosRestauranteComFiltroPeriodo() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Pedido> pedidosPage = new PageImpl<>(List.of(pedido), pageable, 1);
            Instant dataInicio = Instant.now().minusSeconds(86400);
            Instant dataFim = Instant.now();

            when(pedidoValidator.validateRestauranteAprovado(user)).thenReturn(restaurante);
            when(pedidoRepository.findByRestauranteIdAndCriadoEmBetween(
                    restaurante.getId(), dataInicio, dataFim, pageable))
                    .thenReturn(pedidosPage);
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(pedidoResponseDTO);

            Page<PedidoResponseDTO> result = pedidoService.listarPedidosRestaurante(
                    null, dataInicio, dataFim, pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(pedidoValidator, times(1)).validateRestauranteAprovado(user);
            verify(pedidoRepository, times(1))
                    .findByRestauranteIdAndCriadoEmBetween(restaurante.getId(), dataInicio, dataFim, pageable);
        }
    }

    @Test
    void deveListarPedidosRestauranteComFiltroStatusEPeriodo() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Pedido> pedidosPage = new PageImpl<>(List.of(pedido), pageable, 1);
            Instant dataInicio = Instant.now().minusSeconds(86400);
            Instant dataFim = Instant.now();

            when(pedidoValidator.validateRestauranteAprovado(user)).thenReturn(restaurante);
            when(pedidoRepository.findByRestauranteIdAndStatusAndCriadoEmBetween(
                    restaurante.getId(), StatusPedido.CREATED, dataInicio, dataFim, pageable))
                    .thenReturn(pedidosPage);
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(pedidoResponseDTO);

            Page<PedidoResponseDTO> result = pedidoService.listarPedidosRestaurante(
                    StatusPedido.CREATED, dataInicio, dataFim, pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(pedidoValidator, times(1)).validateRestauranteAprovado(user);
            verify(pedidoRepository, times(1))
                    .findByRestauranteIdAndStatusAndCriadoEmBetween(
                            restaurante.getId(), StatusPedido.CREATED, dataInicio, dataFim, pageable);
        }
    }

    @Test
    void deveLancarExcecaoQuandoRestauranteNaoEncontrado() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            Pageable pageable = PageRequest.of(0, 10);

            when(pedidoValidator.validateRestauranteAprovado(user))
                    .thenThrow(new ResourceNotFoundException("Restaurante não encontrado para o usuário autenticado"));

            assertThrows(ResourceNotFoundException.class,
                    () -> pedidoService.listarPedidosRestaurante(null, null, null, pageable));
            verify(pedidoValidator, times(1)).validateRestauranteAprovado(user);
        }
    }

    @Test
    void deveLancarExcecaoQuandoRestauranteNaoAprovado() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            Pageable pageable = PageRequest.of(0, 10);

            when(pedidoValidator.validateRestauranteAprovado(user))
                    .thenThrow(new AccessDeniedException("Restaurante não está aprovado"));

            assertThrows(AccessDeniedException.class,
                    () -> pedidoService.listarPedidosRestaurante(null, null, null, pageable));
            verify(pedidoValidator, times(1)).validateRestauranteAprovado(user);
        }
    }

    @Test
    void devePermitirListarPedidosQuandoAdminMesmoRestauranteNaoAprovado() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);

            Restaurante restauranteNaoAprovado = new Restaurante();
            restauranteNaoAprovado.setId(2L);
            restauranteNaoAprovado.setStatus(com.siseg.model.enumerations.StatusRestaurante.PENDING_APPROVAL);
            restauranteNaoAprovado.setUser(user);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Pedido> pedidosPage = new PageImpl<>(List.of(pedido), pageable, 1);

            when(pedidoValidator.validateRestauranteAprovado(user)).thenReturn(restauranteNaoAprovado);
            when(pedidoRepository.findByRestauranteId(restauranteNaoAprovado.getId(), pageable))
                    .thenReturn(pedidosPage);
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(pedidoResponseDTO);

            Page<PedidoResponseDTO> result = pedidoService.listarPedidosRestaurante(null, null, null, pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(pedidoValidator, times(1)).validateRestauranteAprovado(user);
            verify(pedidoRepository, times(1))
                    .findByRestauranteId(restauranteNaoAprovado.getId(), pageable);
        }
    }

    @Test
    void deveCancelarPedidoComStatusCreated() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            pedido.setStatus(StatusPedido.CREATED);
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(pedidoResponseDTO);

            PedidoResponseDTO result = pedidoService.cancelarPedido(1L);

            assertNotNull(result);
            assertEquals(StatusPedido.CANCELED, pedido.getStatus());
            verify(pedidoRepository, times(1)).save(pedido);
        }
    }

    @Test
    void deveCancelarPedidoComStatusConfirmed() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            pedido.setStatus(StatusPedido.CONFIRMED);
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);
            when(pedidoMapper.toResponseDTO(any(Pedido.class))).thenReturn(pedidoResponseDTO);

            PedidoResponseDTO result = pedidoService.cancelarPedido(1L);

            assertNotNull(result);
            assertEquals(StatusPedido.CANCELED, pedido.getStatus());
            verify(pedidoRepository, times(1)).save(pedido);
        }
    }

    @Test
    void deveLancarExcecaoQuandoPedidoNaoPodeSerCancelado() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            pedido.setStatus(StatusPedido.PREPARING);
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

            assertThrows(IllegalStateException.class, 
                    () -> pedidoService.cancelarPedido(1L));
        }
    }

    @Test
    void deveLancarExcecaoQuandoPedidoJaSaiuParaEntrega() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            pedido.setStatus(StatusPedido.OUT_FOR_DELIVERY);
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

            assertThrows(IllegalStateException.class, 
                    () -> pedidoService.cancelarPedido(1L));
        }
    }
}

